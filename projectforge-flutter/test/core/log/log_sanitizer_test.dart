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
import 'package:projectforge_app/core/log/log_sanitizer.dart';

void main() {
  test(
    'url: query values masked, names kept, fragment and user info dropped',
    () {
      expect(
        LogSanitizer.url(
          Uri.parse(
            'https://user:pw@pf.example.com/next/appLogin?challenge=abc&state=xyz#frag',
          ),
        ),
        'https://pf.example.com/next/appLogin?challenge=***&state=***',
      );
      expect(
        LogSanitizer.url(Uri.parse('https://pf.example.com/next/')),
        'https://pf.example.com/next/',
      );
      expect(LogSanitizer.url(Uri.parse('mailto:a@b.de')), 'mailto:***');
      expect(
        LogSanitizer.url(Uri.parse('projectforge://auth?code=c&state=s')),
        'projectforge://auth?code=***&state=***',
      );
      expect(LogSanitizer.url(null), '-');
    },
  );

  test('text: authorization, jwt, secret parameters and mails masked', () {
    final text = LogSanitizer.text(
      'Authorization: PFApp secret123, Bearer abc.def; '
      'jwt eyJhbGciOiJIUzI1NiJ9.eyJzdWIiOiIxIn0.sig '
      'code=4711&codeVerifier=v1 authentik_proxy_1=c00kie '
      '{"token": "t0k3n", "password":"pw"} mail max@example.com',
    );
    for (final secret in [
      'secret123',
      'abc.def',
      'eyJ',
      '4711',
      'v1',
      'c00kie',
      't0k3n',
      '"pw"',
      'max@example.com',
    ]) {
      expect(text, isNot(contains(secret)), reason: secret);
    }
    expect(text, contains('PFApp ***'));
    expect(text, contains('authentik_proxy_1=***'));
  });

  test('text: harmless text stays', () {
    const message =
        'GET https://pf.example.com/rs/timesheet?week=*** → 302 (content-type: text/html, '
        'messageKey: app.proxy.refused, user 5, device 7)';
    expect(LogSanitizer.text(message), message);
  });

  test('cookies: names only', () {
    expect(LogSanitizer.cookieNames(['b', 'a']), 'a, b');
    expect(LogSanitizer.cookieNames(const []), '-');
    expect(LogSanitizer.cookieHeader('a=1; b=2'), 'a=***; b=***');
  });
}
