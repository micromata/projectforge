// Uses local generated test accounts only. No production cookies or credentials are read.
import assert from 'node:assert/strict';
import { readFileSync } from 'node:fs';
import { randomBytes, createHash } from 'node:crypto';
const base = 'http://localhost:18080', issuer = `${base}/mcp/oauth`, resource = `${base}/mcp`;
const accounts = readFileSync(new URL('../.local/mcp/home/testAccounts.txt', import.meta.url), 'utf8');
let checks = 0;
function check(actual, expected, label) { assert.deepEqual(actual, expected, label); checks++; console.log(`PASS ${label}`); }
const post = (url, body, cookie) => fetch(url, { method: 'POST', redirect: 'manual', headers: { 'Content-Type': 'application/json', Origin: base, ...(cookie ? { Cookie: cookie } : {}) }, body: JSON.stringify(body) });
const form = (url, body, cookie) => fetch(url, { method: 'POST', redirect: 'manual', headers: { Origin: base, ...(cookie ? { Cookie: cookie } : {}) }, body: body instanceof URLSearchParams ? body : new URLSearchParams(body) });
async function browserLogin(username) {
  const row = accounts.split('\n').find(line => line.includes(`=${username}/`));
  assert.ok(row, 'Local generated account exists');
  const password = row.slice(row.indexOf('/') + 1).trim();
  const response = await post(`${base}/rsPublic/nextLogin`, { username, password, stayLoggedIn: false });
  const result = await response.json();
  check(result.status, 'SUCCESS', `${username}: normal ProjectForge login`);
  const cookies = response.headers.getSetCookie().map(s => s.split(';')[0]).join('; ');
  assert.ok(cookies.includes('JSESSIONID'), 'Session cookie issued');
  return cookies;
}
async function begin(cookie, { client, redirect, registeredRedirect = 'http://localhost:19876/callback' } = {}) {
  if (!client) {
    const clientRes = await post(`${issuer}/register`, { client_name: 'Session reuse test', redirect_uris: [registeredRedirect] });
    check(clientRes.status, 201, 'native client registration');
    client = await clientRes.json();
  }
  redirect ??= client.redirect_uris[0];
  const verifier = randomBytes(32).toString('base64url');
  const metadata = await (await fetch(`${base}/.well-known/oauth-protected-resource/mcp`)).json();
  check(metadata.authorization_servers, [issuer], 'ProjectForge is the authorization server');
  const query = new URLSearchParams({ client_id: client.client_id, redirect_uri: redirect, response_type: 'code',
    resource, scope: metadata.scopes_supported.join(' '), state: 'test-state', code_challenge_method: 'S256', code_challenge: createHash('sha256').update(verifier).digest('base64url') });
  const url = `${issuer}/authorize?${query}`;
  const anonymous = await fetch(url, { redirect: 'manual' });
  check(anonymous.status, 302, 'no session uses normal ProjectForge login');
  assert.ok(anonymous.headers.get('location').startsWith('/next/login?returnUrl='));
  const consent = await fetch(url, { headers: { Cookie: cookie }, redirect: 'manual' });
  check(consent.status, 200, 'existing session goes directly to consent');
  const html = await consent.text();
  check(/type=["']password/i.test(html), false, 'consent has no password field');
  check(/name=["']otp/i.test(html), false, 'consent has no OTP field');
  const pending = html.match(/name="pending" value="([^"]+)"/)?.[1];
  const csrf = html.match(/name="csrf" value="([^"]+)"/)?.[1];
  const scopes = [...html.matchAll(/name='scope' value='([^']+)'/g)].map(m => m[1]);
  assert.ok(pending && csrf, 'Session-bound consent request exists');
  return { html, scopes, client, redirect, verifier, pending, csrf, cookie };
}
async function approve(flow, scopes) {
  const data = new URLSearchParams({ pending: flow.pending, csrf: 'wrong', decision: 'allow' });
  scopes.forEach(s => data.append('scope', s));
  check((await form(`${issuer}/consent`, data, flow.cookie)).status, 403, 'forged CSRF rejected');
  data.set('csrf', flow.csrf);
  const approved = await form(`${issuer}/consent`, data, flow.cookie);
  check(approved.status, 302, 'consent accepted without another login');
  check((await form(`${issuer}/consent`, data, flow.cookie)).status, 400, 'consent form replay rejected');
  const callback = new URL(approved.headers.get('location'));
  check(callback.origin + callback.pathname, flow.redirect, 'consent uses the chosen callback address');
  check(callback.searchParams.get('state'), 'test-state', 'state preserved');
  const grant = { grant_type: 'authorization_code', client_id: flow.client.client_id, code: callback.searchParams.get('code'),
    redirect_uri: flow.redirect, resource, code_verifier: 'a'.repeat(43) };
  check((await form(`${issuer}/token`, grant)).status, 400, 'wrong PKCE rejected');
  grant.code_verifier = flow.verifier;
  if (flow.redirect !== flow.client.redirect_uris[0]) {
    check((await form(`${issuer}/token`, { ...grant, redirect_uri: flow.client.redirect_uris[0] })).status,
      400, 'code exchange rejects the registered port when a different port was authorized');
  }
  const tokenResponse = await form(`${issuer}/token`, grant);
  check(tokenResponse.status, 200, 'PKCE code exchange');
  const tokens = await tokenResponse.json();
  check(tokens.scope.split(' ').sort(), [...scopes].sort(), 'token contains only consented scopes');
  return { tokens, exchange: grant };
}
const rpc = (access, method, params = {}, headers = {}) => fetch(resource, {
  method: 'POST', headers: { 'Content-Type': 'application/json', Accept: 'application/json, text/event-stream',
    'MCP-Protocol-Version': '2025-06-18', Authorization: `Bearer ${access}`, ...headers },
  body: JSON.stringify({ jsonrpc: '2.0', id: 1, method, params }),
});
async function call(access, name, args = {}) {
  const response = await rpc(access, 'tools/call', { name, arguments: args });
  check(response.status, 200, 'MCP call with session-derived OAuth token');
  return (await response.json()).result;
}
const fullCookie = await browserLogin('e2e-full-access');
const full = await begin(fullCookie);
check(full.scopes.length, 7, 'privileged user offered seven read scopes');
const selected = ['projectforge.profile.read', 'projectforge.projects.read'];
const { tokens } = await approve(full, selected);
check((await rpc(tokens.access_token, 'ping', {}, { 'MCP-Protocol-Version': '1999-01-01' })).status,
  400, 'unsupported MCP protocol version rejected');
check((await rpc(tokens.access_token, 'ping')).status, 200, 'supported MCP protocol version accepted');
const who = await call(tokens.access_token, 'projectforge_whoami');
check(JSON.parse(who.content[0].text).username, 'e2e-full-access', 'identity comes from existing PF session');
check(JSON.parse(who.content[0].text).datasets, ['projects'], 'only granted datasets exposed');
const projects = await call(tokens.access_token, 'projectforge_search', { entity: 'projects', limit: 2 });
check(projects.isError, false, 'granted projects readable');
assert.ok(JSON.parse(projects.content[0].text).items.length > 0, 'Real test projects');
const invoices = await rpc(tokens.access_token, 'tools/call', { name: 'projectforge_search', arguments: { entity: 'invoices' } });
check(invoices.status, 403, 'unselected finance scope denied even for privileged user');
check((await invoices.json()).error, 'insufficient_scope', 'missing scope returns an OAuth error');
check(invoices.headers.get('www-authenticate'),
  `Bearer error="insufficient_scope", scope="projectforge.invoices.read projectforge.profile.read projectforge.projects.read", resource_metadata="${base}/.well-known/oauth-protected-resource/mcp"`,
  'scope challenge includes required scope, existing scopes and resource metadata');

// Replay must revoke the issued tokens. Keep this grant separate from the ordinary read/refresh/logout flow.
const replayFlow = await begin(fullCookie);
const replay = await approve(replayFlow, ['projectforge.profile.read']);
check((await rpc(replay.tokens.access_token, 'ping')).status, 200, 'separate replay grant initially works');
check((await form(`${issuer}/token`, replay.exchange)).status, 400, 'code replay rejected');
check((await rpc(replay.tokens.access_token, 'ping')).status, 401, 'code replay revokes its access token');
check((await form(`${issuer}/token`, { grant_type: 'refresh_token', client_id: replayFlow.client.client_id,
  refresh_token: replay.tokens.refresh_token, resource })).status, 400, 'code replay revokes its refresh token');
check((await rpc(tokens.access_token, 'ping')).status, 200, 'code replay leaves the independent read grant valid');

// Native loopback-IP clients reuse their registration even when the OS assigns a new listener port.
const portFlow = await begin(fullCookie, { registeredRedirect: 'http://127.0.0.1:19876/callback', redirect: 'http://127.0.0.1:19877/callback' });
const portGrant = await approve(portFlow, ['projectforge.profile.read']);
check((await rpc(portGrant.tokens.access_token, 'ping')).status, 200, 'grant works with a new loopback callback port');
const nextPortFlow = await begin(fullCookie, { client: portFlow.client, redirect: 'http://127.0.0.1:19878/callback' });
const nextPortGrant = await approve(nextPortFlow, ['projectforge.profile.read']);
check((await rpc(nextPortGrant.tokens.access_token, 'ping')).status, 200, 'same registration works with another callback port');
const normalCookie = await browserLogin('e2e-normalo');
const normal = await begin(normalCookie);
check(normal.scopes.includes('projectforge.invoices.read'), false, 'ordinary user not offered finance scope');
check(normal.scopes.includes('projectforge.profile.read'), true, 'ordinary user can authorize identity');
const escalated = new URLSearchParams({ pending: normal.pending, csrf: normal.csrf, decision: 'allow', scope: 'projectforge.invoices.read' });
check((await form(`${issuer}/consent`, escalated, normal.cookie)).status, 400, 'forged extra scope rejected');
const refresh = { grant_type: 'refresh_token', refresh_token: tokens.refresh_token, client_id: full.client.client_id, resource };
const renewedResponse = await form(`${issuer}/token`, refresh);
check(renewedResponse.status, 200, 'refresh without browser login');
const renewed = await renewedResponse.json();
check((await rpc(renewed.access_token, 'ping')).status, 200, 'renewed token works before logout');
check((await rpc(tokens.access_token, 'ping')).status, 401, 'refresh rotation retires the previous access token');
const logout = await post(`${base}/rsPublic/nextLogin/cancel`, {}, fullCookie);
check(logout.status, 200, 'normal ProjectForge logout');
const afterLogout = await rpc(renewed.access_token, 'tools/list');
check(afterLogout.status, 401, 'logging out invalidates session-derived access');
check((await form(`${issuer}/token`, { ...refresh, refresh_token: renewed.refresh_token })).status, 400,
  'logging out invalidates the current refresh token');
await post(`${base}/rsPublic/nextLogin/cancel`, {}, normalCookie);
console.log(`\n${checks} session OAuth checks passed.`);
