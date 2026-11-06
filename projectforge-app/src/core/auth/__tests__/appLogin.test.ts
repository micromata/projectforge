/////////////////////////////////////////////////////////////////////////////
//
// Project ProjectForge Community Edition
//         www.projectforge.org
//
// Copyright (C) 2001-2026 Micromata GmbH, Germany (www.micromata.com)
//
// ProjectForge is dual-licensed.
//
// This community edition is free software; you can redistribute it and/or
// modify it under the terms of the GNU General Public License as published
// by the Free Software Foundation; version 3 of the License.
//
// This community edition is distributed in the hope that it will be useful,
// but WITHOUT ANY WARRANTY; without even the implied warranty of
// MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU General
// Public License for more details.
//
// You should have received a copy of the GNU General Public License along
// with this program; if not, see http://www.gnu.org/licenses/.
//
/////////////////////////////////////////////////////////////////////////////

// Port of projectforge-flutter/test/core/app_login_test.dart and pkce_test.dart.
import { fakeHttp, nodeCrypto } from '../../../testing/fakeHttp';
import { ApiException } from '../../api/apiException';
import { AppLogin, AppLoginCancelled, isInsecure, normalizeServerUrl } from '../appLogin';
import { challengeOf, createPkce } from '../pkce';
import { sessionFromJson } from '../session';

const SERVER = 'https://pf.example.com';

function decodePacked(packed: string): unknown {
  return JSON.parse(Buffer.from(packed, 'base64url').toString('utf8'));
}

test('S256 challenge as in RFC 7636, appendix B', async () => {
  expect(await challengeOf('dBjftJeZ4CVP-mB92K27uhbUJU1p1r_wW1gFWFOEjXk', nodeCrypto)).toBe(
    'E9Melhoa2OwvFrEMTJguCHaoeK1t8URWbuGJSstw-cM',
  );
});

test('verifier, challenge and state match the patterns of AppAuthService', async () => {
  const pkce = await createPkce(nodeCrypto);
  expect(pkce.verifier).toMatch(/^[A-Za-z0-9._~-]{43,128}$/);
  expect(pkce.challenge).toMatch(/^[A-Za-z0-9_-]{43}$/);
  expect(pkce.state).toMatch(/^[A-Za-z0-9_-]{16,128}$/);
  expect((await createPkce(nodeCrypto)).verifier).not.toBe(pkce.verifier);
});

test('exchanges the code of the callback with the verifier of the challenge', async () => {
  let opened: URL | undefined;
  const { http, requests } = fakeHttp(() => ({
    body: {
      userId: 7,
      username: 'kai',
      deviceId: 3,
      token: 'secret',
      proxyCookies: { authentik_proxy_1: 'proxySession' },
    },
  }));
  const login = new AppLogin(http, nodeCrypto, async (url) => {
    opened = new URL(url);
    return `projectforge://auth?code=theCode&state=${opened.searchParams.get('state')}`;
  });
  const session = await login.login({ serverUrl: SERVER, deviceName: 'iPhone', platform: 'ios' });
  expect(opened!.pathname).toBe('/next/appLogin');
  expect(opened!.searchParams.get('device')).toBe('iPhone');
  const request = requests[0];
  expect(requests).toHaveLength(1);
  expect(request.url).toBe(`${SERVER}/rsPublic/appAuth/token`);
  expect(request.init.redirect).toBe('manual');
  expect(request.init.credentials).toBe('omit');
  const body = request.body as { code: string; codeVerifier: string };
  expect(body.code).toBe('theCode');
  expect(await challengeOf(body.codeVerifier, nodeCrypto)).toBe(opened!.searchParams.get('challenge'));
  expect(session.token).toBe('secret');
  expect(session.deviceId).toBe(3);
  expect(session.proxyCookies).toEqual({ authentik_proxy_1: 'proxySession' });
  expect(sessionFromJson(JSON.parse(JSON.stringify(session))).proxyCookies).toEqual(session.proxyCookies);
});

test('the parameters go packed first, so an SSO proxy keeping only the first one passes them on', async () => {
  let opened = '';
  const { http } = fakeHttp(() => ({ body: { userId: 7, deviceId: 3, token: 't' } }));
  const login = new AppLogin(http, nodeCrypto, async (url) => {
    opened = url;
    return `projectforge://auth?code=c&state=${new URL(url).searchParams.get('state')}`;
  });
  await login.login({ serverUrl: SERVER, deviceName: 'Jörgs iPhone', platform: 'ios', deviceId: 3 });
  const query = new URL(opened).search.substring(1);
  expect(query.startsWith('login=')).toBe(true);
  // What an unencoded rd=…?login=…&challenge=… of the proxy leaves over.
  const kept = new URLSearchParams(query.split('&')[0]).get('login')!;
  const single = new URL(opened).searchParams;
  expect(decodePacked(kept)).toEqual({
    challenge: single.get('challenge'),
    state: single.get('state'),
    device: 'Jörgs iPhone',
    platform: 'ios',
    deviceId: 3,
  });
  expect(kept).not.toContain('=');
});

test('a stored session of an older version has no proxy cookies', () => {
  expect(sessionFromJson({ serverUrl: SERVER, token: 's', userId: 7, deviceId: 3 }).proxyCookies).toEqual({});
});

test('refuses a callback with another state', async () => {
  const { http, requests } = fakeHttp(() => ({ body: {} }));
  const login = new AppLogin(http, nodeCrypto, async () => 'projectforge://auth?code=x&state=foreign');
  await expect(login.login({ serverUrl: SERVER, deviceName: 'd', platform: 'ios' })).rejects.toBeInstanceOf(
    ApiException,
  );
  expect(requests).toHaveLength(0);
});

test('a declined or cancelled login is a cancellation', async () => {
  const { http } = fakeHttp(() => ({ body: {} }));
  const declined = new AppLogin(
    http,
    nodeCrypto,
    async (url) => `projectforge://auth?error=access_denied&state=${new URL(url).searchParams.get('state')}`,
  );
  await expect(declined.login({ serverUrl: SERVER, deviceName: 'd', platform: 'ios' })).rejects.toBeInstanceOf(
    AppLoginCancelled,
  );
  const cancelled = new AppLogin(http, nodeCrypto, async () => null);
  await expect(cancelled.login({ serverUrl: SERVER, deviceName: 'd', platform: 'ios' })).rejects.toBeInstanceOf(
    AppLoginCancelled,
  );
});

test('the parameters go into the path too, for a server taking them there', async () => {
  let opened: URL | undefined;
  const { http } = fakeHttp(() => ({ body: { userId: 7, deviceId: 3, token: 't', appLoginPath: true } }));
  const login = new AppLogin(http, nodeCrypto, async (url) => {
    opened = new URL(url);
    return `projectforge://auth?code=c&state=${opened.searchParams.get('state')}`;
  });
  const session = await login.login({ serverUrl: SERVER, deviceName: 'iPhone', platform: 'ios', loginPath: true });
  expect(opened!.pathname.split('/').filter(Boolean)).toEqual(['next', 'appLogin', opened!.searchParams.get('login')]);
  expect(session.appLoginPath).toBe(true);
});

test('server url as typed by the user', () => {
  expect(normalizeServerUrl('pf.example.com')).toBe('https://pf.example.com');
  expect(normalizeServerUrl(' https://pf.example.com/ ')).toBe('https://pf.example.com');
  expect(normalizeServerUrl('https://pf.example.com/next/calendar')).toBe('https://pf.example.com');
  expect(normalizeServerUrl('https://example.com/pf/next/')).toBe('https://example.com/pf');
  expect(normalizeServerUrl('http://localhost:8086')).toBe('http://localhost:8086');
  expect(normalizeServerUrl('')).toBeNull();
  expect(normalizeServerUrl('ftp://pf.example.com')).toBeNull();
});

test('plain http only for development builds', () => {
  expect(isInsecure('http://pf.example.com', false)).toBe(true);
  expect(isInsecure('https://pf.example.com', false)).toBe(false);
  expect(isInsecure('http://localhost:8086', true)).toBe(false);
});

test('the system status follows a redirect within the server', async () => {
  const { http, requests } = fakeHttp((request) =>
    request.url === `${SERVER}/rsPublic/systemStatus`
      ? { status: 301, headers: { location: '/pf/rsPublic/systemStatus' } }
      : { body: { appname: 'ProjectForge', version: '9.3' } },
  );
  const login = new AppLogin(http, nodeCrypto, async () => null);
  expect((await login.systemStatus(SERVER)).appname).toBe('ProjectForge');
  expect(requests.map((it) => it.url)).toEqual([
    `${SERVER}/rsPublic/systemStatus`,
    `${SERVER}/pf/rsPublic/systemStatus`,
  ]);
});

test('a redirect to another host is not followed, nor one of the token request', async () => {
  const foreign = fakeHttp(() => ({ status: 302, headers: { location: 'https://sso.example.com/login' } }));
  await expect(new AppLogin(foreign.http, nodeCrypto, async () => null).systemStatus(SERVER)).rejects.toBeInstanceOf(
    ApiException,
  );
  expect(foreign.requests).toHaveLength(1);

  const token = fakeHttp(() => ({ status: 302, headers: { location: `${SERVER}/elsewhere` } }));
  const login = new AppLogin(token.http, nodeCrypto, async (url) => {
    return `projectforge://auth?code=c&state=${new URL(url).searchParams.get('state')}`;
  });
  await expect(login.login({ serverUrl: SERVER, deviceName: 'iPhone', platform: 'ios' })).rejects.toBeInstanceOf(
    ApiException,
  );
  expect(token.requests).toHaveLength(1);
});
