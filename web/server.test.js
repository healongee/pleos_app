import test from 'node:test';
import assert from 'node:assert/strict';
import http from 'node:http';
import { createHandler } from './server.js';

test('request workflow scopes account to customer and owner, enforces order and mock unlock', async () => {
  const server = http.createServer(createHandler());
  await new Promise(resolve => server.listen(0, '127.0.0.1', resolve));
  const url = `http://127.0.0.1:${server.address().port}`;
  const client = () => {
    let cookie = '';
    return async (path, data) => {
      const response = await fetch(url + path, { method: data === undefined ? 'GET' : 'POST', headers: { ...(cookie ? { Cookie: cookie } : {}), ...(data === undefined ? {} : { Origin: url, 'Content-Type': 'application/json' }) }, ...(data === undefined ? {} : { body: JSON.stringify(data) }) });
      if (response.headers.get('set-cookie')) cookie = response.headers.get('set-cookie').split(';')[0];
      return { status: response.status, body: await response.json() };
    };
  };
  const customer = client(), stranger = client(), owner = client(), otherOwner = client();
  try {
    assert.equal((await customer('/api/requests')).status, 403);
    assert.equal((await customer('/api/demo-login', { role: 'customer' })).status, 200);
    assert.equal((await stranger('/api/demo-login', { role: 'customer' })).status, 200);
    assert.equal((await owner('/api/demo-login', { role: 'owner', ownerId: 'owner-a' })).status, 200);
    assert.equal((await otherOwner('/api/demo-login', { role: 'owner', ownerId: 'owner-b' })).status, 200);
    const created = await customer('/api/requests', { shelterId: 's1', durationMinutes: 60 });
    assert.equal(created.status, 201);
    const repeated = await customer('/api/requests', { shelterId: 's1', durationMinutes: 60 });
    assert.equal(repeated.status, 200);
    assert.equal(repeated.body.alreadyRequested, true);
    assert.equal(repeated.body.id, created.body.id);
    assert.equal((await stranger('/api/requests', { shelterId: 's1', durationMinutes: 60 })).status, 409);
    const path = `/api/requests/${created.body.id}`;
    assert.equal((await stranger('/api/requests')).body.requests.length, 0);
    assert.equal((await otherOwner('/api/requests')).body.requests.length, 0);
    assert.equal((await owner('/api/requests')).body.requests.length, 1);
    assert.equal((await otherOwner(`${path}/account`, { bankAccount: 'DEMO BANK 12345' })).status, 404);
    assert.equal((await customer(`${path}/account`, { bankAccount: 'DEMO BANK 12345' })).status, 403);
    assert.equal((await owner(`${path}/deposit-confirm`, {})).status, 409);
    assert.equal((await owner(`${path}/account`, { bankAccount: 'DEMO BANK 12345' })).status, 200);
    assert.equal((await customer('/api/requests')).body.requests[0].bankAccount, 'DEMO BANK 12345');
    assert.equal(JSON.stringify((await stranger('/api/requests')).body).includes('12345'), false);
    assert.equal(JSON.stringify((await otherOwner('/api/requests')).body).includes('12345'), false);
    assert.equal((await owner(`${path}/unlock`, {})).status, 409);
    assert.equal((await owner(`${path}/deposit-confirm`, {})).body.status, 'DEPOSIT_CONFIRMED_BY_OWNER');
    const unlock = await owner(`${path}/unlock`, {});
    assert.equal(unlock.body.unlock, 'MOCK_REQUEST_ONLY');
    assert.equal(unlock.body.status, 'UNLOCK_REQUESTED');
    assert.equal((await owner(`${path}/complete`, {})).body.status, 'COMPLETED');
    assert.equal((await customer('/api/requests', { shelterId: 's1', durationMinutes: 60 })).status, 201);
    assert.equal((await customer('/api/requests')).body.requests[0].status, 'COMPLETED');
  } finally { await new Promise(resolve => server.close(resolve)); }
});

test('camera status is owner/request scoped, always disconnected, and unavailable after session exit', async () => {
  const server = http.createServer(createHandler());
  await new Promise(resolve => server.listen(0, '127.0.0.1', resolve));
  const url = `http://127.0.0.1:${server.address().port}`;
  let customerCookie = '', ownerCookie = '', otherCookie = '';
  const request = async (path, data, cookie = '') => {
    const response = await fetch(url + path, { method: data === undefined ? 'GET' : 'POST', headers: { ...(cookie ? { Cookie: cookie } : {}), ...(data === undefined ? {} : { Origin: url, 'Content-Type': 'application/json' }) }, ...(data === undefined ? {} : { body: JSON.stringify(data) }) });
    return { status: response.status, body: await response.json(), cookie: response.headers.get('set-cookie')?.split(';')[0] };
  };
  try {
    customerCookie = (await request('/api/demo-login', { role: 'customer' })).cookie;
    ownerCookie = (await request('/api/demo-login', { role: 'owner', ownerId: 'owner-a' })).cookie;
    otherCookie = (await request('/api/demo-login', { role: 'owner', ownerId: 'owner-b' })).cookie;
    const created = await request('/api/requests', { shelterId: 's1', durationMinutes: 30 }, customerCookie);
    const camera = `/api/requests/${created.body.id}/camera`;
    assert.equal((await request(camera)).status, 403);
    assert.equal((await request(camera, undefined, customerCookie)).status, 403);
    assert.equal((await request(camera, undefined, otherCookie)).status, 404);
    const status = await request(camera, undefined, ownerCookie);
    assert.equal(status.status, 200);
    assert.deepEqual(status.body, { connected: false, consentVerified: false, ownerAccessAllowed: false, status: '영상 미연결/시연' });
    assert.equal((await request(`/api/requests/${created.body.id}/camera`, {}, ownerCookie)).status, 404);
    assert.equal((await request(`/api/requests/${created.body.id}/stream`, undefined, ownerCookie)).status, 404);
    assert.equal((await request('/api/logout', {}, ownerCookie)).status, 200);
    assert.equal((await request(camera, undefined, ownerCookie)).status, 403);
  } finally { await new Promise(resolve => server.close(resolve)); }
});

test('web entry and listings use Korean and explicitly disclose offline video', async () => {
  const server = http.createServer(createHandler());
  await new Promise(resolve => server.listen(0, '127.0.0.1', resolve));
  const url = `http://127.0.0.1:${server.address().port}`;
  try {
    const html = await (await fetch(url)).text();
    assert.match(html, /lang="ko"/);
    assert.match(html, /영상 미연결\/시연/);
    const listing = await (await fetch(`${url}/api/shelters`)).json();
    assert.match(listing.shelters[0].title, /시연/);
  } finally { await new Promise(resolve => server.close(resolve)); }
});

test('카카오 지도 SDK 하위 스크립트 로딩에 필요한 CSP 출처를 허용한다', async () => {
  const server = http.createServer(createHandler({ kakaoJsKey: 'test-public-js-key' }));
  await new Promise(resolve => server.listen(0, '127.0.0.1', resolve));
  try {
    const url = `http://127.0.0.1:${server.address().port}`;
    const response = await fetch(url);
    const csp = response.headers.get('content-security-policy');
    assert.match(csp, /script-src[^;]*https:\/\/dapi\.kakao\.com[^;]*http:\/\/t1\.kakaocdn\.net/);
    assert.match(csp, /img-src[^;]*http:\/\/t1\.kakaocdn\.net/);
    assert.match(csp, /img-src[^;]*http:\/\/mts\.kakaocdn\.net/);
    assert.equal((await (await fetch(`${url}/api/config`)).json()).kakaoJsKey, 'test-public-js-key');
  } finally { await new Promise(resolve => server.close(resolve)); }
});

test('mutations reject cross-origin requests', async () => {
  const server = http.createServer(createHandler());
  await new Promise(resolve => server.listen(0, '127.0.0.1', resolve));
  try {
    const response = await fetch(`http://127.0.0.1:${server.address().port}/api/demo-login`, { method: 'POST', headers: { Origin: 'https://attacker.example', 'Content-Type': 'application/json' }, body: JSON.stringify({ role: 'owner', ownerId: 'owner-a' }) });
    assert.equal(response.status, 403);
  } finally { await new Promise(resolve => server.close(resolve)); }
});
