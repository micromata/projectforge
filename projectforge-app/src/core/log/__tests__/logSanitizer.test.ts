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

// Port of projectforge-flutter/test/core/log/log_sanitizer_test.dart.
import { LogSanitizer } from '../logSanitizer';

test('url: query values masked, names kept, fragment and user info dropped', () => {
  expect(LogSanitizer.url('https://user:pw@pf.example.com/next/appLogin?challenge=abc&state=xyz#frag')).toBe(
    'https://pf.example.com/next/appLogin?challenge=***&state=***',
  );
  expect(LogSanitizer.url('https://pf.example.com/next/')).toBe('https://pf.example.com/next/');
  expect(LogSanitizer.url('mailto:a@b.de')).toBe('mailto:***');
  expect(LogSanitizer.url('projectforge://auth?code=c&state=s')).toBe('projectforge://auth?code=***&state=***');
  expect(LogSanitizer.url(null)).toBe('-');
});

test('text: authorization, jwt, secret parameters and mails masked', () => {
  const text = LogSanitizer.text(
    'Authorization: PFApp secret123, Bearer abc.def; ' +
      'jwt eyJhbGciOiJIUzI1NiJ9.eyJzdWIiOiIxIn0.sig ' +
      'code=4711&codeVerifier=v1 authentik_proxy_1=c00kie ' +
      '{"token": "t0k3n", "password":"pw"} mail max@example.com',
  );
  for (const secret of ['secret123', 'abc.def', 'eyJ', '4711', 'v1', 'c00kie', 't0k3n', '"pw"', 'max@example.com']) {
    expect(text).not.toContain(secret);
  }
  expect(text).toContain('PFApp ***');
  expect(text).toContain('authentik_proxy_1=***');
});

test('text: harmless text stays', () => {
  const message =
    'GET https://pf.example.com/rs/timesheet?week=*** → 302 (content-type: text/html, ' +
    'messageKey: app.proxy.refused, user 5, device 7)';
  expect(LogSanitizer.text(message)).toBe(message);
});

test('text: values of registered names masked (proxy cookies named by the server)', () => {
  const message = 'FormatException: {"_oauth2_proxy": "v4lue"} _oauth2_proxy=v4lue';
  expect(LogSanitizer.text(message)).toContain('v4lue');
  LogSanitizer.addSecretNames(['_oauth2_proxy']);
  const text = LogSanitizer.text(message);
  expect(text).not.toContain('v4lue');
  expect(text).toContain('"_oauth2_proxy": ***');
  expect(text).toContain('_oauth2_proxy=***');
});

test('text: control characters replaced, line breaks kept', () => {
  expect(LogSanitizer.text('a\rb\u001b[2Jc\nd')).toBe('a b [2Jc\nd');
});

test('text: other line breaks and marks of the text direction replaced', () => {
  expect(LogSanitizer.text('a\u2028b\u2029c\u0085d\u202ee\u2066f\u200eg\u009bh')).toBe('a b c d e f g h');
});

test('text: the query values of an url in a text masked', () => {
  const text = LogSanitizer.text(
    'Failed loading https://pf.example.com/next/x?sid=s3cr3t&rd=https://a.b/c?d=e#frag, ' +
      'see "https://pf.example.com/next/" (code 7)',
  );
  expect(text).not.toContain('s3cr3t');
  expect(text).not.toContain('d=e');
  expect(text).not.toContain('frag');
  expect(text).toContain('https://pf.example.com/next/x?sid=***&rd=***');
  expect(text).toContain('"https://pf.example.com/next/" (code 7)');
});

test('url: tokens in the path masked, ids and names kept', () => {
  expect(LogSanitizer.url('https://pf.example.com/next/appLogin/eyJjaGFsbGVuZ2UiOiJjIn0?login=x')).toBe(
    'https://pf.example.com/next/appLogin/***?login=***',
  );
  expect(LogSanitizer.url('https://pf.example.com/next/public/datatransfer/abc123')).toBe(
    'https://pf.example.com/next/public/datatransfer/***',
  );
  expect(LogSanitizer.url('https://pf.example.com/rsPublic/x/k7Fq2mZp9RtLw3XyB8nVc1/file?id=1')).toBe(
    'https://pf.example.com/rsPublic/x/***/file?id=***',
  );
  expect(LogSanitizer.url('https://pf.example.com/next/address/42')).toBe('https://pf.example.com/next/address/42');
  expect(LogSanitizer.url('https://pf.example.com/next/timesheet/edit/7')).toBe(
    'https://pf.example.com/next/timesheet/edit/7',
  );
  expect(LogSanitizer.url('https://pf.example.com/next/vacationAccount/')).toBe(
    'https://pf.example.com/next/vacationAccount/',
  );
});

test('text: tokens and the external access of an area masked', () => {
  const text = LogSanitizer.text(
    'Failed: https://pf.example.com/next/public/datatransfer/abc/x and k7Fq2mZp9RtLw3XyB8nVc1, ' +
      'src/features/web/WebScreen.tsx:443:12',
  );
  expect(text).not.toContain('abc');
  expect(text).not.toContain('k7Fq2');
  expect(text).toContain('/next/public/datatransfer/***');
  expect(text).toContain('WebScreen.tsx:443:12');
});

test('cookies: names only', () => {
  expect(LogSanitizer.cookieNames(['b', 'a'])).toBe('a, b');
  expect(LogSanitizer.cookieNames([])).toBe('-');
  expect(LogSanitizer.cookieHeader('a=1; b=2')).toBe('a=***; b=***');
});
