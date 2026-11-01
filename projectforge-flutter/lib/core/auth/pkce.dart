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

import 'dart:convert';
import 'dart:math';

import 'package:crypto/crypto.dart';

/// Proof key for code exchange (RFC 7636) of the app login, see AppAuthService of the server.
class Pkce {
  Pkce._(this.verifier, this.challenge, this.state);

  /// A new verifier (never leaves the app), its S256 challenge (goes into the browser url) and a state.
  factory Pkce.create([Random? random]) {
    final rnd = random ?? Random.secure();
    final verifier = _randomUrlSafe(rnd, 32);
    return Pkce._(verifier, challengeOf(verifier), _randomUrlSafe(rnd, 24));
  }

  final String verifier;
  final String challenge;
  final String state;

  static String challengeOf(String verifier) => base64Url
      .encode(sha256.convert(ascii.encode(verifier)).bytes)
      .replaceAll('=', '');

  static String _randomUrlSafe(Random random, int bytes) => base64Url
      .encode(List<int>.generate(bytes, (_) => random.nextInt(256)))
      .replaceAll('=', '');
}
