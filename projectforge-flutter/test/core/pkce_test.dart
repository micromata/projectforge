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
import 'package:projectforge_app/core/auth/app_login.dart';
import 'package:projectforge_app/core/auth/pkce.dart';

void main() {
  test('S256 challenge as in RFC 7636, appendix B', () {
    expect(
      Pkce.challengeOf('dBjftJeZ4CVP-mB92K27uhbUJU1p1r_wW1gFWFOEjXk'),
      'E9Melhoa2OwvFrEMTJguCHaoeK1t8URWbuGJSstw-cM',
    );
  });

  test(
    'verifier, challenge and state match the patterns of AppAuthService',
    () {
      final pkce = Pkce.create();
      expect(pkce.verifier, matches(RegExp(r'^[A-Za-z0-9._~-]{43,128}$')));
      expect(pkce.challenge, matches(RegExp(r'^[A-Za-z0-9_-]{43}$')));
      expect(pkce.state, matches(RegExp(r'^[A-Za-z0-9_-]{16,128}$')));
      expect(Pkce.create().verifier, isNot(pkce.verifier));
    },
  );

  test('server url as typed by the user', () {
    expect(
      AppLogin.normalizeServerUrl('pf.example.com'),
      'https://pf.example.com',
    );
    expect(
      AppLogin.normalizeServerUrl(' https://pf.example.com/ '),
      'https://pf.example.com',
    );
    expect(
      AppLogin.normalizeServerUrl('https://pf.example.com/next/calendar'),
      'https://pf.example.com',
    );
    expect(
      AppLogin.normalizeServerUrl('https://example.com/pf/next/'),
      'https://example.com/pf',
    );
    expect(
      AppLogin.normalizeServerUrl('http://localhost:8086'),
      'http://localhost:8086',
    );
    expect(AppLogin.normalizeServerUrl(''), isNull);
    expect(AppLogin.normalizeServerUrl('ftp://pf.example.com'), isNull);
  });

  test('plain http only for development builds', () {
    expect(
      AppLogin.isInsecure('http://pf.example.com', allowHttp: false),
      isTrue,
    );
    expect(
      AppLogin.isInsecure('https://pf.example.com', allowHttp: false),
      isFalse,
    );
    expect(
      AppLogin.isInsecure('http://localhost:8086', allowHttp: true),
      isFalse,
    );
  });
}
