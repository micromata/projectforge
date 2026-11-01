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

import 'package:flutter_secure_storage/flutter_secure_storage.dart';
import 'package:flutter_test/flutter_test.dart';
import 'package:projectforge_app/core/auth/session.dart';

void main() {
  TestWidgetsFlutterBinding.ensureInitialized();

  Session session(String serverUrl) =>
      Session(serverUrl: serverUrl, token: 't', userId: 1, deviceId: 1);

  test(
    'the servers of the logins, most recent first, without duplicates',
    () async {
      FlutterSecureStorage.setMockInitialValues({});
      final store = SessionStore();
      await store.save(session('https://a'));
      await store.save(session('https://b'));
      await store.save(session('https://a'));
      expect(await store.servers(), ['https://a', 'https://b']);
    },
  );

  test('at most maxServers servers are kept', () async {
    FlutterSecureStorage.setMockInitialValues({});
    final store = SessionStore();
    for (var i = 0; i < SessionStore.maxServers + 2; i++) {
      await store.save(session('https://s$i'));
    }
    final servers = await store.servers();
    expect(servers, hasLength(SessionStore.maxServers));
    expect(servers.first, 'https://s${SessionStore.maxServers + 1}');
  });

  test('the logout keeps the servers, a server can be removed', () async {
    FlutterSecureStorage.setMockInitialValues({});
    final store = SessionStore();
    await store.save(session('https://a'));
    await store.save(session('https://b'));
    await store.clear();
    expect(await store.load(), isNull);
    expect(await store.servers(), ['https://b', 'https://a']);
    await store.removeServer('https://b');
    expect(await store.servers(), ['https://a']);
  });

  test('the last server of an older version is taken over', () async {
    FlutterSecureStorage.setMockInitialValues({'serverUrl': 'https://old'});
    final store = SessionStore();
    expect(await store.servers(), ['https://old']);
    await store.save(session('https://new'));
    expect(await store.servers(), ['https://new', 'https://old']);
  });

  test('a broken list is no list', () async {
    FlutterSecureStorage.setMockInitialValues({'servers': '{'});
    expect(await SessionStore().servers(), isEmpty);
  });
}
