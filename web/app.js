const $ = id => document.getElementById(id);
let session = null;
let listings = [];
let owners = [];
let current = [];
const statuses = { REQUESTED: '요청 대기', ACCOUNT_SENT: '시연 안내 전달됨', DEPOSIT_CONFIRMED_BY_OWNER: '차주가 입금 확인했다고 표시함 (은행 검증 아님)', UNLOCK_REQUESTED: '모의 문 열림 요청 (실제 차량 조작 아님)', COMPLETED: '시연 요청 종료', DECLINED: '요청 거절' };
const node = (tag, text) => { const element = document.createElement(tag); element.textContent = text; return element; };
const button = (label, action) => { const element = node('button', label); element.type = 'button'; element.addEventListener('click', action); return element; };
async function api(path, body) {
  const response = await fetch(path, body === undefined ? { cache: 'no-store' } : { method: 'POST', headers: { 'Content-Type': 'application/json' }, body: JSON.stringify(body) });
  const data = await response.json();
  if (!response.ok) throw new Error(data.error || '요청에 실패했습니다.');
  return data;
}
async function act(path, body) {
  try { await api(path, body); $('message').textContent = '시연 요청 상태가 변경되었습니다.'; await refresh(); }
  catch (error) { $('message').textContent = error.message; }
}
function render() {
  $('login').hidden = !!session?.role;
  $('customer').hidden = session?.role !== 'customer';
  $('owner').hidden = session?.role !== 'owner';
  $('logout').hidden = !session?.role;
  const ownerName = owners.find(o => o.id === session?.ownerId)?.name || '시연 차주';
  $('identity').textContent = session?.role ? `시연 ${session.role === 'owner' ? ownerName : '고객'} · 실제 신원 인증 없음` : '시연 역할을 선택하세요. 실제 신원 인증이 아닙니다.';
  $('shelters').replaceChildren();
  for (const shelter of listings) {
    const card = node('article', ''); card.className = 'card';
    card.append(node('h3', shelter.title), node('p', `${shelter.location} · 시간당 ${shelter.pricePerHour.toLocaleString()}원 (가상 안내) · ${owners.find(o => o.id === shelter.ownerId)?.name || '시연 차주'}`));
    const select = document.createElement('select'); select.setAttribute('aria-label', `${shelter.title} 시연 이용 시간`);
    for (const minutes of [30, 60, 120]) { const option = node('option', `${minutes}분`); option.value = minutes; select.append(option); }
    card.append(select, button('차주에게 시연 요청', () => act('/api/requests', { shelterId: shelter.id, durationMinutes: Number(select.value) })));
    $('shelters').append(card);
  }
  $('customerRequests').replaceChildren(); $('ownerRequests').replaceChildren();
  for (const item of current) {
    const title = listings.find(s => s.id === item.shelterId)?.title || item.shelterId;
    const card = node('article', ''); card.className = 'card';
    card.append(node('h3', `${title} · ${item.id}`), node('p', `${item.durationMinutes}분 · 시연 표시 금액 ${item.totalPrice.toLocaleString()}원 · ${statuses[item.status] || '상태 확인 불가'}`));
    if (item.bankAccount) card.append(node('p', `차주가 입력한 시연 안내: ${item.bankAccount}. 실제 계좌나 금융 정보를 입력하거나 송금하지 마세요.`));
    if (item.status === 'ACCOUNT_SENT' && session.role === 'customer') card.append(node('p', '차주의 수동 상태 표시 대기 중입니다. 이 사이트는 결제를 처리하거나 입금을 검증하지 않습니다.'));
    if (item.unlock) card.append(node('p', item.unlock === 'MOCK_REQUEST_ONLY' ? '모의 문 열림 요청만 기록되었습니다. 실제 차량 문은 열리지 않습니다.' : '외부 게이트웨이 응답 표시입니다. 실제 차량 문 열림 여부는 확인되지 않았습니다.'));
    if (session.role === 'owner') {
      card.append(node('p', `${item.customerLabel} · 이 시연 차주에게 할당된 요청`));
      const camera = node('section', ''); camera.className = 'camera-status';
      camera.append(node('h4', '영상 미연결/시연'), node('p', '실내 영상 없음 · 차량 콘솔은 오프라인이며 동의 상태가 웹과 동기화되지 않습니다. 고객 동의를 검증할 수 없으므로 차주 접근이 차단됩니다.'));
      card.append(camera);
      if (item.status === 'REQUESTED') {
        const account = document.createElement('input'); account.placeholder = '가상 안내 문구만 입력 (실제 계좌 금지)'; account.setAttribute('aria-label', `${item.id} 시연 안내 문구`); account.maxLength = 120;
        card.append(account, button('고객에게 시연 안내 보내기', () => act(`/api/requests/${item.id}/account`, { bankAccount: account.value })), button('요청 거절', () => act(`/api/requests/${item.id}/decline`, {})));
      }
      if (item.status === 'ACCOUNT_SENT') card.append(button('차주 수동 확인 표시 (은행 검증 아님)', () => act(`/api/requests/${item.id}/deposit-confirm`, {})));
      if (item.status === 'DEPOSIT_CONFIRMED_BY_OWNER') card.append(button('모의 문 열림 요청 (실제 조작 아님)', () => act(`/api/requests/${item.id}/unlock`, {})));
      if (item.status === 'UNLOCK_REQUESTED') card.append(button('시연 요청 종료', () => act(`/api/requests/${item.id}/complete`, {})));
      $('ownerRequests').append(card);
    } else $('customerRequests').append(card);
  }
}
async function refresh() {
  try {
    session = await api('/api/session');
    current = session.role ? (await api('/api/requests')).requests : [];
    render();
  } catch (error) { $('message').textContent = error.message; }
}
async function mapSetup() {
  try {
    const { kakaoJsKey } = await api('/api/config');
    if (!kakaoJsKey) { $('mapStatus').textContent = '지도 연결 안 됨: 카카오 JavaScript 키가 없습니다. 아래 가상 목록을 이용하세요.'; return; }
    const script = document.createElement('script');
    script.src = `https://dapi.kakao.com/v2/maps/sdk.js?appkey=${encodeURIComponent(kakaoJsKey)}&autoload=false`;
    script.onerror = () => { $('mapStatus').textContent = '지도 연결 안 됨. 아래 가상 목록을 이용하세요.'; };
    script.onload = () => {
      if (!window.kakao?.maps) { $('mapStatus').textContent = '지도 연결 안 됨. 아래 가상 목록을 이용하세요.'; return; }
      kakao.maps.load(() => {
        const center = listings[0];
        const map = new kakao.maps.Map($('map'), { center: new kakao.maps.LatLng(center.lat, center.lng), level: 6 });
        for (const shelter of listings) new kakao.maps.Marker({ position: new kakao.maps.LatLng(shelter.lat, shelter.lng), map, title: shelter.title });
        $('mapStatus').textContent = '지도에는 가상 목록의 좌표가 표시됩니다. 현재 위치가 아닙니다.';
        $('locate').hidden = false;
        $('locate').onclick = () => {
          if (!navigator.geolocation) { $('mapStatus').textContent = '브라우저 위치 기능을 이용할 수 없습니다.'; return; }
          navigator.geolocation.getCurrentPosition(({ coords }) => { map.setCenter(new kakao.maps.LatLng(coords.latitude, coords.longitude)); $('mapStatus').textContent = '동의한 위치로 지도를 이동했습니다. 이 서버로 위치를 전송하지 않습니다.'; }, () => { $('mapStatus').textContent = '위치 권한이 없거나 위치를 확인할 수 없습니다. 가상 목록은 계속 이용할 수 있습니다.'; }, { enableHighAccuracy: false, timeout: 10000 });
        };
      });
    };
    document.head.append(script);
  } catch { $('mapStatus').textContent = '지도 연결 안 됨. 아래 가상 목록을 이용하세요.'; }
}
$('customerLogin').onclick = async () => { await act('/api/demo-login', { role: 'customer' }); };
$('ownerLogin').onclick = async () => { await act('/api/demo-login', { role: 'owner', ownerId: $('ownerSelect').value }); };
$('logout').onclick = async () => { await act('/api/logout', {}); };
(async () => {
  try {
    const data = await api('/api/shelters'); listings = data.shelters; owners = data.owners;
    for (const owner of owners) { const option = node('option', owner.name); option.value = owner.id; $('ownerSelect').append(option); }
    await refresh(); await mapSetup(); setInterval(refresh, 5000);
  } catch (error) { $('message').textContent = error.message; }
})();
