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

import 'package:flutter_riverpod/flutter_riverpod.dart';
import 'package:flutter_test/flutter_test.dart';
import 'package:projectforge_app/core/settings/app_settings.dart';
import 'package:projectforge_app/core/web/native_page.dart';

void main() {
  late Directory dir;

  setUp(() async => dir = await Directory.systemTemp.createTemp('pf_settings'));
  tearDown(() => dir.delete(recursive: true));

  test('everything opens in the app by default', () async {
    final settings = await AppSettingsStore(() async => dir).load();
    for (final page in NativePage.values) {
      expect(settings.opensInApp(page), isTrue, reason: page.name);
    }
  });

  test('a page kept in the web version is stored', () async {
    final container = ProviderContainer(
      overrides: [
        appSettingsStoreProvider.overrideWithValue(
          AppSettingsStore(() async => dir),
        ),
      ],
    );
    addTearDown(container.dispose);
    await container.read(appSettingsProvider.future);
    await container
        .read(appSettingsProvider.notifier)
        .setOpensInApp(NativePage.businessCards, false);
    final stored = await AppSettingsStore(() async => dir).load();
    expect(stored.opensInApp(NativePage.businessCards), isFalse);
    expect(stored.opensInApp(NativePage.chat), isTrue);
  });

  test('unknown or broken settings give the defaults', () async {
    expect(
      AppSettings.fromJson({
        'webVersion': ['chat', 'gone'],
      }).webVersion,
      {NativePage.chat},
    );
    await File('${dir.path}/settings.json').writeAsString('{');
    expect(
      (await AppSettingsStore(() async => dir).load()).webVersion,
      isEmpty,
    );
  });
}
