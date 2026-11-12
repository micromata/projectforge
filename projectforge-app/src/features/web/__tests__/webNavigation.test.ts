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

// Port of the earlier Flutter app's web_navigation_test.
import { WebNavigation } from '../webNavigation';

const navigation = new WebNavigation('https://pf.example.org');

describe('decide', () => {
  test('own server stays in the WebView', () => {
    expect(navigation.decide('https://pf.example.org/next/timesheet')).toBe('webView');
    expect(navigation.decide('https://pf.example.org/wa/wicket/page')).toBe('webView');
  });

  test.each([
    'https://other.example.org/',
    'http://pf.example.org/next/', // Other scheme, other origin.
    'https://pf.example.org:8443/next/',
    'mailto:someone@example.org',
    'tel:+49123',
  ])('other sites, mail and phone are opened by the system: %s', (url) => {
    expect(navigation.decide(url)).toBe('external');
  });

  test('another site never stays in the WebView, also the login of an SSO proxy', () => {
    expect(navigation.decide('https://sso.example.org/application/o/authorize/?client_id=pf')).toBe('external');
    expect(navigation.decide('https://pf.example.org/outpost.goauthentik.io/callback')).toBe('webView');
  });

  test('the login of an SSO proxy is a redirect to another site while a page of the server loads', () => {
    const login = 'https://sso.example.org/application/o/authorize/';
    const proxyLogin = (
      url: string,
      { redirect = true, loadingServerPage = true, behindProxy = true } = {},
    ) => navigation.isProxyLogin(url, { redirect, loadingServerPage, behindProxy });
    expect(proxyLogin(login)).toBe(true);
    // A script of a loaded page (an external entry of the menu).
    expect(proxyLogin(login, { loadingServerPage: false })).toBe(false);
    // A tapped link.
    expect(proxyLogin(login, { redirect: false })).toBe(false);
    // No proxy.
    expect(proxyLogin(login, { behindProxy: false })).toBe(false);
    // The own server.
    expect(proxyLogin('https://pf.example.org/outpost.goauthentik.io/start')).toBe(false);
    expect(proxyLogin('mailto:someone@example.org')).toBe(false);
  });

  test('unknown schemes are not opened', () => {
    expect(navigation.decide('javascript:alert(1)')).toBe('cancel');
    expect(navigation.decide('intent://scan/#Intent;end')).toBe('cancel');
    expect(navigation.decide('data:text/html,<form>Login</form>')).toBe('cancel');
    expect(navigation.decide('blob:https://pf.example.org/4711')).toBe('webView');
    expect(navigation.decide('not a url')).toBe('cancel');
  });
});

test('a page loaded by the app itself is always one of the server', () => {
  for (const path of [
    '/next/calendar',
    'https://pf.example.org/next/calendar?id=3',
    // A url of the server whose path starts with two slashes, loaded again as it was.
    'https://pf.example.org//evil.example.org/x',
  ]) {
    expect(navigation.pageUrl(path)?.hostname).toBe('pf.example.org');
  }
  for (const path of [
    '//evil.example.org/x',
    '////evil.example.org/x', // A path rebuilt of the url above: https:////evil… is evil… for the WebView.
    '\\\\evil.example.org/x', // Backslashes are slashes for http(s) urls (WHATWG).
    'https://evil.example.org/',
    'http://pf.example.org/next/',
  ]) {
    expect(navigation.pageUrl(path)).toBeNull();
  }
});

test('a page of another site', () => {
  expect(navigation.isForeignPage('https://evil.example.org/')).toBe(true);
  expect(navigation.isForeignPage('http://pf.example.org/next/')).toBe(true);
  for (const url of ['https://pf.example.org/next/', 'about:blank', 'blob:https://pf.example.org/4711']) {
    expect(navigation.isForeignPage(url)).toBe(false);
  }
});

test('pages of next, not the legacy pages nor another site', () => {
  for (const url of [
    'https://pf.example.org/next',
    'https://pf.example.org/next/',
    'https://pf.example.org/next/timesheet?id=5',
  ]) {
    expect(navigation.isNextPage(url)).toBe(true);
  }
  for (const url of [
    'https://pf.example.org/wa/wicket/page',
    'https://pf.example.org/nextcloud/',
    'https://pf.example.org/outpost.goauthentik.io/start?rd=%2Fnext%2F',
    'https://other.example.org/next/',
  ]) {
    expect(navigation.isNextPage(url)).toBe(false);
  }
});

test('login page of the own server', () => {
  expect(navigation.isLoginPage('https://pf.example.org/next/login?returnUrl=%2Fcalendar')).toBe(true);
  expect(navigation.isLoginPage('https://pf.example.org/next/login/')).toBe(true);
  expect(navigation.isLoginPage('https://other.example.org/next/login')).toBe(false);
  expect(navigation.isLoginPage('https://pf.example.org/next/loginx')).toBe(false);
});

describe('returnPath', () => {
  const returnPath = (returnUrl: string | null) => {
    const url = new URL('https://pf.example.org/next/login');
    if (returnUrl != null) url.searchParams.set('returnUrl', returnUrl);
    return navigation.returnPath(url.href);
  };

  test('a path below /next', () => {
    expect(returnPath('/calendar')).toBe('/next/calendar');
    expect(returnPath('/timesheet/edit?id=4711')).toBe('/next/timesheet/edit?id=4711');
    expect(navigation.url(returnPath('/calendar')).href).toBe('https://pf.example.org/next/calendar');
  });

  test.each([
    null,
    '',
    'calendar',
    '//evil.example.org/x',
    '/\\evil.example.org',
    'https://evil.example.org/',
    '/login?returnUrl=%2Fcalendar',
  ])('never another host, never the login again: %s', (url) => {
    expect(returnPath(url)).toBe(WebNavigation.defaultPath);
  });
});

test('the chat, the business cards, the addresses and the time sheets have a native tab', () => {
  const cases: [string, string | null][] = [
    ['https://pf.example.org/next/chat', 'chat'],
    ['https://pf.example.org/next/chat/', 'chat'],
    ['https://pf.example.org/next/chat/5?message=7', 'chat'],
    ['https://pf.example.org/next/myBusinessCards', 'businessCards'],
    ['https://pf.example.org/next/myBusinessCards/', 'businessCards'],
    ['https://pf.example.org/next/myBusinessCards?id=3', 'businessCards'],
    ['https://pf.example.org/next/notifications', 'notifications'],
    ['https://pf.example.org/next/address', 'addresses'],
    ['https://pf.example.org/next/address/', 'addresses'],
    ['https://pf.example.org/next/address/5', null],
    ['https://pf.example.org/next/address/5/edit', null],
    ['https://pf.example.org/next/chatty', null],
    // The time sheet tab of the app is no counterpart of the web list.
    ['https://pf.example.org/next/timesheet', null],
    ['https://pf.example.org/next/timesheet/', null],
    ['https://pf.example.org/next/timesheet/5', null],
    ['https://pf.example.org/next/timesheet/new', null],
    ['https://pf.example.org/rs/myBusinessCards/pkpass?id=3', null],
    ['https://other.example.org/next/chat', null],
  ];
  for (const [url, page] of cases) {
    expect(navigation.nativePage(url)).toBe(page);
  }
});
