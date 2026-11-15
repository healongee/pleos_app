import http from 'node:http';
import { readFile } from 'node:fs/promises';
import { randomBytes, timingSafeEqual } from 'node:crypto';
import { fileURLToPath } from 'node:url';

// Fictional listings. Coordinates identify the listing, NEVER the user's location.
const owners = [{ id: 'owner-a', name: '시청 지역 시연 차주' }, { id: 'owner-b', name: '종로 지역 시연 차주' }];
const shelters = [
  { id: 's1', ownerId: 'owner-a', title: 'GV80 시연 쉼터', pricePerHour: 8000, location: '서울 시청 인근 (가상)', lat: 37.5665, lng: 126.9780 },
  { id: 's2', ownerId: 'owner-b', title: '아이오닉 6 시연 쉼터', pricePerHour: 6000, location: '종로 인근 (가상)', lat: 37.5729, lng: 126.9794 },
  { id: 's3', ownerId: 'owner-a', title: '팰리세이드 시연 쉼터', pricePerHour: 10000, location: '서대문 인근 (가상)', lat: 37.5658, lng: 126.9668 }
];
const token = () => randomBytes(32).toString('hex');
const equalToken = (a, b) => typeof a === 'string' && typeof b === 'string' && a.length === b.length && timingSafeEqual(Buffer.from(a), Buffer.from(b));
const error = (status, message) => Object.assign(new Error(message), { status });

export function createHandler({ kakaoJsKey = '', unlockGateway = null } = {}) {
  const sessions = new Map();
  const requests = [];
  let nextId = 1;
  const json = (res, status, body, cookie) => {
    res.writeHead(status, { 'Content-Type': 'application/json; charset=utf-8', 'Cache-Control': 'no-store', 'X-Content-Type-Options': 'nosniff', 'Referrer-Policy': 'no-referrer', ...(cookie ? { 'Set-Cookie': cookie } : {}) });
    res.end(JSON.stringify(body));
  };
  const bodyOf = async req => {
    let raw = '';
    for await (const chunk of req) {
      raw += chunk;
      if (raw.length > 4096) throw error(413, '요청 크기가 너무 큽니다.');
    }
    try { return JSON.parse(raw); } catch { throw error(400, '요청 형식이 올바르지 않습니다.'); }
  };
  const cookieToken = req => /(?:^|;\s*)demo_session=([a-f0-9]{64})(?:;|$)/.exec(req.headers.cookie || '')?.[1];
  const sessionOf = req => sessions.get(cookieToken(req));
  const requireSession = (req, role) => {
    const session = sessionOf(req);
    if (!session || (role && session.role !== role)) throw error(403, '이 요청에 접근할 권한이 없습니다.');
    return session;
  };
  const publicRequest = (item, viewer) => ({
    id: item.id, shelterId: item.shelterId, ownerId: item.ownerId,
    durationMinutes: item.durationMinutes, totalPrice: item.totalPrice, status: item.status,
    unlock: item.unlock,
    ...(item.status !== 'REQUESTED' && item.status !== 'DECLINED' ? { bankAccount: item.bankAccount } : {}),
    ...(viewer.role === 'owner' ? { customerLabel: item.customerLabel } : {})
  });
  const access = (req, id) => {
    const viewer = requireSession(req);
    const item = requests.find(value => value.id === id);
    if (!item || (viewer.role === 'customer' ? item.customerSession !== viewer.id : item.ownerId !== viewer.ownerId)) throw error(404, '요청을 찾을 수 없습니다.');
    return { item, viewer };
  };
  const originAllowed = req => {
    const origin = req.headers.origin;
    if (origin && origin !== `http://${req.headers.host}` && origin !== `https://${req.headers.host}`) throw error(403, '다른 출처의 요청은 허용되지 않습니다.');
    // Browsers include Origin or Fetch Metadata on mutating requests; require one of them.
    if (!origin && req.headers['sec-fetch-site'] !== 'same-origin') throw error(403, '동일 출처 브라우저 요청만 허용됩니다.');
  };
  return async (req, res) => {
    try {
      const path = new URL(req.url, 'http://localhost').pathname;
      if (req.method === 'GET' && ['/', '/app.js', '/style.css'].includes(path)) {
        const name = path === '/' ? 'index.html' : path.slice(1);
        const file = await readFile(fileURLToPath(new URL(`./${name}`, import.meta.url)));
        res.writeHead(200, { 'Content-Type': name.endsWith('.js') ? 'text/javascript; charset=utf-8' : name.endsWith('.css') ? 'text/css; charset=utf-8' : 'text/html; charset=utf-8', 'Cache-Control': 'no-store', 'X-Content-Type-Options': 'nosniff', 'Referrer-Policy': 'no-referrer', 'Content-Security-Policy': "default-src 'self'; script-src 'self' https://dapi.kakao.com http://t1.kakaocdn.net https://t1.kakaocdn.net; style-src 'self' 'unsafe-inline' http://t1.kakaocdn.net https://t1.kakaocdn.net https://*.kakaocdn.net; img-src 'self' data: https://*.daumcdn.net http://t1.kakaocdn.net https://*.kakaocdn.net http://mts.kakaocdn.net; connect-src 'self' https://*.kakao.com https://*.daum.net; base-uri 'none'; form-action 'self'; frame-ancestors 'none'" });
        res.end(file);
      } else if (req.method === 'GET' && path === '/api/config') {
        json(res, 200, { kakaoJsKey, demo: true, unlockGatewayConfigured: !!unlockGateway });
      } else if (req.method === 'GET' && path === '/api/shelters') {
        json(res, 200, { shelters, owners });
      } else if (req.method === 'GET' && path === '/api/session') {
        const session = sessionOf(req);
        json(res, 200, session ? { role: session.role, ownerId: session.ownerId ?? null } : { role: null });
      } else if (req.method === 'POST' && path === '/api/demo-login') {
        originAllowed(req);
        const body = await bodyOf(req);
        if (body.role !== 'customer' && !(body.role === 'owner' && owners.some(owner => owner.id === body.ownerId))) throw error(400, '시연 역할을 선택하세요.');
        const id = token();
        sessions.set(id, { id, role: body.role, ownerId: body.role === 'owner' ? body.ownerId : undefined });
        json(res, 200, { role: body.role, ownerId: body.role === 'owner' ? body.ownerId : null }, `demo_session=${id}; HttpOnly; SameSite=Strict; Path=/`);
      } else if (req.method === 'POST' && path === '/api/logout') {
        originAllowed(req);
        sessions.delete(cookieToken(req));
        json(res, 200, { ok: true }, 'demo_session=; HttpOnly; SameSite=Strict; Path=/; Max-Age=0');
      } else if (req.method === 'GET' && /^\/api\/requests\/demo-\d+\/camera$/.test(path)) {
        // No authenticated car/customer consent channel or video provider exists. Fail closed even
        // for the matching owner: never return a stream URL, image, token or claimed live status.
        const id = path.split('/')[3];
        const { viewer } = access(req, id);
        if (viewer.role !== 'owner') throw error(403, '차주 전용 상태입니다.');
        json(res, 200, { connected: false, consentVerified: false, ownerAccessAllowed: false, status: '영상 미연결/시연' });
      } else if (req.method === 'GET' && path === '/api/requests') {
        const viewer = requireSession(req);
        json(res, 200, { requests: requests.filter(item => viewer.role === 'customer' ? item.customerSession === viewer.id : item.ownerId === viewer.ownerId).map(item => publicRequest(item, viewer)) });
      } else if (req.method === 'POST' && path === '/api/requests') {
        originAllowed(req);
        const viewer = requireSession(req, 'customer');
        const body = await bodyOf(req);
        const shelter = shelters.find(value => value.id === body.shelterId);
        if (!shelter || ![30, 60, 120].includes(body.durationMinutes)) throw error(400, '시연 쉼터와 이용 시간을 선택하세요.');
        const active = requests.find(item => item.shelterId === shelter.id && !['DECLINED', 'COMPLETED'].includes(item.status));
        if (active) {
          if (active.customerSession === viewer.id) {
            json(res, 200, { ...publicRequest(active, viewer), alreadyRequested: true });
            return;
          }
          throw error(409, '이 차량은 다른 고객이 이용 요청 중입니다. 다른 시연 차량을 선택해 주세요.');
        }
        const item = { id: `demo-${nextId++}`, shelterId: shelter.id, ownerId: shelter.ownerId, customerSession: viewer.id, customerLabel: `시연 고객 ${nextId - 1}`, durationMinutes: body.durationMinutes, totalPrice: Math.round(shelter.pricePerHour * body.durationMinutes / 60), status: 'REQUESTED', bankAccount: null, unlock: null };
        requests.push(item);
        json(res, 201, publicRequest(item, viewer));
      } else if (req.method === 'POST' && /^\/api\/requests\/demo-\d+\/(account|decline|deposit-confirm|unlock|complete)$/.test(path)) {
        originAllowed(req);
        const [, , , id, action] = path.split('/');
        const { item, viewer } = access(req, id);
        if (viewer.role !== 'owner') throw error(403, '차주만 이용할 수 있습니다.');
        if (action === 'account' && item.status === 'REQUESTED') {
          const body = await bodyOf(req);
          if (typeof body.bankAccount !== 'string' || body.bankAccount.trim().length < 5 || body.bankAccount.length > 120) throw error(400, '시연 안내 문구를 5~120자로 입력하세요.');
          item.bankAccount = body.bankAccount.trim();
          item.status = 'ACCOUNT_SENT';
        } else if (action === 'decline' && item.status === 'REQUESTED') {
          item.status = 'DECLINED';
        } else if (action === 'deposit-confirm' && item.status === 'ACCOUNT_SENT') {
          item.status = 'DEPOSIT_CONFIRMED_BY_OWNER'; // Owner assertion, NOT bank verification.
        } else if (action === 'unlock' && item.status === 'DEPOSIT_CONFIRMED_BY_OWNER') {
          // Never call an unverified remote unlock integration. Gateway, if supplied, must authenticate and authorize independently.
          if (unlockGateway) {
            const result = await unlockGateway({ shelterId: item.shelterId, requestId: item.id, ownerId: item.ownerId });
            item.unlock = result?.acknowledged === true ? 'GATEWAY_ACKNOWLEDGED' : 'GATEWAY_NOT_ACKNOWLEDGED';
          } else item.unlock = 'MOCK_REQUEST_ONLY';
          item.status = 'UNLOCK_REQUESTED';
        } else if (action === 'complete' && item.status === 'UNLOCK_REQUESTED') {
          item.status = 'COMPLETED';
        } else throw error(409, '현재 상태에서는 처리할 수 없습니다.');
        json(res, 200, publicRequest(item, viewer));
      } else json(res, 404, { error: '페이지를 찾을 수 없습니다.' });
    } catch (cause) {
      json(res, cause.status || 500, { error: cause.status ? cause.message : '서버 오류가 발생했습니다.' });
    }
  };
}

if (process.argv[1] && fileURLToPath(import.meta.url) === fileURLToPath(new URL(`file://${process.argv[1]}`))) {
  const host = process.env.HOST || '127.0.0.1';
  const port = Number(process.env.PORT || 3000);
  http.createServer(createHandler({ kakaoJsKey: process.env.KAKAO_JS_KEY || '' })).listen(port, host, () => console.log(`비공개 시연: http://${host}:${port}`));
}
