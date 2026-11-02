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
import 'dart:io';

import 'package:dio/dio.dart';
import 'package:flutter/material.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';
import 'package:flutter_test/flutter_test.dart';
import 'package:projectforge_app/core/api/api_client.dart';
import 'package:projectforge_app/core/auth/auth_controller.dart';
import 'package:projectforge_app/core/cache/json_cache.dart';
import 'package:projectforge_app/core/theme/theme_controller.dart';

import '../fake_adapter.dart';

void main() {
  TestWidgetsFlutterBinding.ensureInitialized();

  late Directory dir;

  setUp(() async => dir = await Directory.systemTemp.createTemp('pf_theme'));
  tearDown(() => dir.delete(recursive: true));

  ProviderContainer container(String? theme, {String? fallback}) {
    final api = ApiClient(
      serverUrl: 'https://pf.example.com',
      token: () => 'token',
      appVersion: '1',
      onUnauthorized: () {},
      onReauthenticate: () async => false,
      dio: Dio()
        ..httpClientAdapter = FakeAdapter(
          (options) => options.path == '/rs/uiSettings/theme'
              ? (status: 200, body: {'theme': fallback})
              : (status: 200, body: {'theme': ?theme}),
        ),
    );
    final container = ProviderContainer(
      overrides: [
        apiClientProvider.overrideWithValue(api),
        jsonCacheProvider.overrideWithValue(JsonCache(() async => dir)),
      ],
    );
    addTearDown(container.dispose);
    return container;
  }

  Future<ThemeMode> settled(ProviderContainer c) async {
    c.listen(themeModeProvider, (_, _) {});
    for (var i = 0; i < 10; i++) {
      await Future<void>.delayed(const Duration(milliseconds: 10));
    }
    return c.read(themeModeProvider);
  }

  test('the theme of the web pages', () {
    expect(ThemeController.parse('dark'), ThemeMode.dark);
    expect(ThemeController.parse('light'), ThemeMode.light);
    expect(ThemeController.parse('system'), ThemeMode.system);
    expect(ThemeController.parse(null), ThemeMode.system);
  });

  test('the theme of the server is shown and stored', () async {
    expect(await settled(container('dark')), ThemeMode.dark);
    expect(await JsonCache(() async => dir).read('theme'), {'theme': 'dark'});
  });

  test('an older server is asked by the endpoint of the web pages', () async {
    expect(await settled(container(null, fallback: 'dark')), ThemeMode.dark);
  });

  test('without the theme of the server the stored one is shown', () async {
    await JsonCache(() async => dir).write('theme', {'theme': 'dark'});
    expect(await settled(container(null)), ThemeMode.dark);
  });

  test('logged out the system decides', () {
    final c = ProviderContainer(
      overrides: [apiClientProvider.overrideWithValue(null)],
    );
    addTearDown(c.dispose);
    expect(c.read(themeModeProvider), ThemeMode.system);
  });
}
