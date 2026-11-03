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

import 'package:flutter_test/flutter_test.dart';
import 'package:projectforge_app/core/web/native_page.dart';
import 'package:projectforge_app/core/web/web_cookies.dart';
import 'package:projectforge_app/features/web/web_navigation.dart';

void main() {
  final navigation = WebNavigation(Uri.parse('https://pf.example.org'));

  group('decide', () {
    test('own server stays in the WebView', () {
      expect(
        navigation.decide(Uri.parse('https://pf.example.org/next/timesheet')),
        WebNavigationTarget.webView,
      );
      expect(
        navigation.decide(Uri.parse('https://pf.example.org/wa/wicket/page')),
        WebNavigationTarget.webView,
      );
    });

    test('other sites, mail and phone are opened by the system', () {
      for (final url in [
        'https://other.example.org/',
        'http://pf.example.org/next/', // Other scheme, other origin.
        'https://pf.example.org:8443/next/',
        'mailto:someone@example.org',
        'tel:+49123',
      ]) {
        expect(
          navigation.decide(Uri.parse(url)),
          WebNavigationTarget.external,
          reason: url,
        );
      }
    });

    test(
      'another site never stays in the WebView, also the login of an SSO proxy',
      () {
        expect(
          navigation.decide(
            Uri.parse(
              'https://sso.example.org/application/o/authorize/?client_id=pf',
            ),
          ),
          WebNavigationTarget.external,
        );
        expect(
          navigation.decide(
            Uri.parse('https://pf.example.org/outpost.goauthentik.io/callback'),
          ),
          WebNavigationTarget.webView,
        );
      },
    );

    test('the login of an SSO proxy is a redirect to another site while a page of the server loads', () {
      final login = Uri.parse(
        'https://sso.example.org/application/o/authorize/',
      );
      bool proxyLogin(
        Uri url, {
        bool redirect = true,
        bool loadingServerPage = true,
        bool behindProxy = true,
      }) => navigation.isProxyLogin(
        url,
        redirect: redirect,
        loadingServerPage: loadingServerPage,
        behindProxy: behindProxy,
      );
      expect(proxyLogin(login), isTrue);
      expect(
        proxyLogin(login, loadingServerPage: false),
        isFalse,
        reason: 'A script of a loaded page (an external entry of the menu).',
      );
      expect(
        proxyLogin(login, redirect: false),
        isFalse,
        reason: 'A tapped link.',
      );
      expect(
        proxyLogin(login, behindProxy: false),
        isFalse,
        reason: 'No proxy.',
      );
      expect(
        proxyLogin(
          Uri.parse('https://pf.example.org/outpost.goauthentik.io/start'),
        ),
        isFalse,
        reason: 'The own server.',
      );
      expect(proxyLogin(Uri.parse('mailto:someone@example.org')), isFalse);
    });

    test('unknown schemes are not opened', () {
      expect(
        navigation.decide(Uri.parse('javascript:alert(1)')),
        WebNavigationTarget.cancel,
      );
      expect(
        navigation.decide(Uri.parse('intent://scan/#Intent;end')),
        WebNavigationTarget.cancel,
      );
      expect(
        navigation.decide(Uri.parse('data:text/html,<form>Login</form>')),
        WebNavigationTarget.cancel,
      );
      expect(
        navigation.decide(Uri.parse('blob:https://pf.example.org/4711')),
        WebNavigationTarget.webView,
      );
    });
  });

  test('a page loaded by the app itself is always one of the server', () {
    for (final path in [
      '/next/calendar',
      'https://pf.example.org/next/calendar?id=3',
      // A url of the server whose path starts with two slashes, loaded again as it was.
      'https://pf.example.org//evil.example.org/x',
    ]) {
      expect(navigation.pageUrl(path)?.host, 'pf.example.org', reason: path);
    }
    for (final path in [
      '//evil.example.org/x',
      '////evil.example.org/x', // A path rebuilt of the url above: https:////evil… is evil… for the WebView.
      'https://evil.example.org/',
      'http://pf.example.org/next/',
    ]) {
      expect(navigation.pageUrl(path), isNull, reason: path);
    }
  });

  test('a page of another site', () {
    expect(
      navigation.isForeignPage(Uri.parse('https://evil.example.org/')),
      isTrue,
    );
    expect(
      navigation.isForeignPage(Uri.parse('http://pf.example.org/next/')),
      isTrue,
    );
    for (final url in [
      'https://pf.example.org/next/',
      'about:blank',
      'blob:https://pf.example.org/4711',
    ]) {
      expect(navigation.isForeignPage(Uri.parse(url)), isFalse, reason: url);
    }
  });

  test('pages of next, not the legacy pages nor another site', () {
    for (final url in [
      'https://pf.example.org/next',
      'https://pf.example.org/next/',
      'https://pf.example.org/next/timesheet?id=5',
    ]) {
      expect(navigation.isNextPage(Uri.parse(url)), isTrue, reason: url);
    }
    for (final url in [
      'https://pf.example.org/wa/wicket/page',
      'https://pf.example.org/nextcloud/',
      'https://other.example.org/next/',
    ]) {
      expect(navigation.isNextPage(Uri.parse(url)), isFalse, reason: url);
    }
  });

  test('login page of the own server', () {
    expect(
      navigation.isLoginPage(
        Uri.parse('https://pf.example.org/next/login?returnUrl=%2Fcalendar'),
      ),
      isTrue,
    );
    expect(
      navigation.isLoginPage(Uri.parse('https://pf.example.org/next/login/')),
      isTrue,
    );
    expect(
      navigation.isLoginPage(Uri.parse('https://other.example.org/next/login')),
      isFalse,
    );
    expect(
      navigation.isLoginPage(Uri.parse('https://pf.example.org/next/loginx')),
      isFalse,
    );
  });

  group('returnPath', () {
    String returnPath(String? returnUrl) => navigation.returnPath(
      Uri.https('pf.example.org', '/next/login', {'returnUrl': ?returnUrl}),
    );

    test('a path below /next', () {
      expect(returnPath('/calendar'), '/next/calendar');
      expect(
        returnPath('/timesheet/edit?id=4711'),
        '/next/timesheet/edit?id=4711',
      );
      expect(
        navigation.url(returnPath('/calendar')).toString(),
        'https://pf.example.org/next/calendar',
      );
    });

    test('never another host, never the login again', () {
      for (final url in [
        null,
        '',
        'calendar',
        '//evil.example.org/x',
        '/\\evil.example.org',
        'https://evil.example.org/',
        '/login?returnUrl=%2Fcalendar',
      ]) {
        expect(returnPath(url), WebNavigation.defaultPath, reason: url);
      }
    });
  });

  test('set-cookie headers', () {
    final cookies = WebCookies.parseSetCookies([
      'JSESSIONID_6=ABC123; Path=/; HttpOnly; SameSite=Lax',
      'JSESSIONID=; Path=/; Max-Age=0', // Deleting.
      'not a cookie',
    ]);
    expect(cookies, hasLength(1));
    final cookie = cookies.single;
    expect(cookie.name, 'JSESSIONID_6');
    expect(cookie.value, 'ABC123');
    expect(cookie.path, '/');
    expect(cookie.httpOnly, isTrue);
    expect(cookie.secure, isFalse);
  });

  test('the chat, the business cards, the addresses and the time sheets have a native tab', () {
    for (final (url, page) in [
      ('https://pf.example.org/next/chat', NativePage.chat),
      ('https://pf.example.org/next/chat/', NativePage.chat),
      ('https://pf.example.org/next/chat/5?message=7', NativePage.chat),
      ('https://pf.example.org/next/myBusinessCards', NativePage.businessCards),
      (
        'https://pf.example.org/next/myBusinessCards/',
        NativePage.businessCards,
      ),
      (
        'https://pf.example.org/next/myBusinessCards?id=3',
        NativePage.businessCards,
      ),
      ('https://pf.example.org/next/notifications', NativePage.notifications),
      ('https://pf.example.org/next/address', NativePage.addresses),
      ('https://pf.example.org/next/address/', NativePage.addresses),
      ('https://pf.example.org/next/address/5', null),
      ('https://pf.example.org/next/address/5/edit', null),
      ('https://pf.example.org/next/chatty', null),
      ('https://pf.example.org/next/timesheet', NativePage.timesheets),
      ('https://pf.example.org/next/timesheet/', NativePage.timesheets),
      ('https://pf.example.org/next/timesheet/5', null),
      ('https://pf.example.org/next/timesheet/new', null),
      ('https://pf.example.org/rs/myBusinessCards/pkpass?id=3', null),
      ('https://other.example.org/next/chat', null),
    ]) {
      expect(navigation.nativePage(Uri.parse(url)), page, reason: url);
    }
  });
}
