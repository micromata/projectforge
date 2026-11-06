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


// Port of projectforge-flutter/test/features/web/web_session_repository_test.dart, and of the cookie test of
// web_navigation_test.dart (the set-cookie headers are parsed natively here, by pf-native).
import { fakeHttp } from '../../../testing/fakeHttp';
import { ApiClient } from '../../../core/api/apiClient';
import { ApiException } from '../../../core/api/apiException';
import { WebCookies, WebCookieStore } from '../../../core/web/webCookies';
import { WebSessionRepository } from '../webSessionRepository';

/** No WebView in tests: the cookies are only recorded. */
function fakeStore() {
  const set: { url: string; headers: string[] }[] = [];
  const store: WebCookieStore = {
    setCookies: async (url, headers) => {
      set.push({ url, headers });
      return headers.map((it) => it.split('=')[0]);
    },
    cookieHeader: async () => null,
    clearAll: async () => undefined,
  };
  return { store, set };
}

function repository() {
  let status = 200;
  const { http, requests } = fakeHttp(() => ({
    status,
    body: { startUrl: '/next/start' },
    headers: { 'set-cookie': 'JSESSIONID=abc; Path=/; HttpOnly' },
  }));
  const api = new ApiClient('https://pf.example.com', {
    http,
    appVersion: '1',
    token: () => 'token',
    proxyCookies: () => ({}),
    onUnauthorized: () => undefined,
    onReauthenticate: async () => false,
  });
  const { store, set } = fakeStore();
  return {
    repository: new WebSessionRepository(api, new WebCookies(store), () => ({})),
    requests,
    set,
    setStatus: (it: number) => (status = it),
  };
}

test('all tabs share one web session, its cookie goes into the WebView', async () => {
  const { repository: repo, requests, set } = repository();
  expect(await repo.ensure()).toBe('/next/start');
  expect(await repo.ensure()).toBe('/next/start');
  expect(requests).toHaveLength(1);
  expect(requests[0].url).toBe('https://pf.example.com/rs/appAuth/webSession');
  expect(set).toEqual([{ url: 'https://pf.example.com', headers: ['JSESSIONID=abc; Path=/; HttpOnly'] }]);
});

test('concurrent renewals open one new session, used by later tabs', async () => {
  const { repository: repo, requests } = repository();
  await repo.ensure();
  await Promise.all([repo.renew(), repo.renew()]);
  expect(requests).toHaveLength(2);
  await repo.ensure();
  expect(requests).toHaveLength(2);
});

test('a failed attempt is tried again', async () => {
  const { repository: repo, requests, setStatus } = repository();
  setStatus(500);
  await expect(repo.ensure()).rejects.toBeInstanceOf(ApiException);
  setStatus(200);
  expect(await repo.ensure()).toBe('/next/start');
  expect(requests).toHaveLength(2);
});

test('the cookies of an SSO proxy go into the WebView, secure for https', async () => {
  const { store, set } = fakeStore();
  const cookies = new WebCookies(store);
  await cookies.setProxyCookies('https://pf.example.com', { authentik_proxy_1: 'p' });
  await cookies.setProxyCookies('http://localhost:8080', { authentik_proxy_1: 'p' });
  await cookies.setProxyCookies('https://pf.example.com', {});
  expect(set).toEqual([
    { url: 'https://pf.example.com', headers: ['authentik_proxy_1=p; Path=/; HttpOnly; Secure'] },
    { url: 'http://localhost:8080', headers: ['authentik_proxy_1=p; Path=/; HttpOnly'] },
  ]);
});
