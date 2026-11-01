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
import 'dart:io';

import 'package:flutter_riverpod/flutter_riverpod.dart';
import 'package:path_provider/path_provider.dart';

import '../web/native_page.dart';

/// The settings of the app on this device (not of the user: they stay after a logout).
class AppSettings {
  const AppSettings({this.webVersion = const {}});

  factory AppSettings.fromJson(Map<String, dynamic> json) => AppSettings(
    webVersion: {
      for (final name in (json['webVersion'] as List?) ?? const [])
        ?NativePage.values.asNameMap()[name],
    },
  );

  /// The pages the web pages open in their own version, although the app has a tab for them (e.g. if the app's
  /// version doesn't work with the server).
  final Set<NativePage> webVersion;

  /// True if a link of the web pages to [page] switches to the tab of the app.
  bool opensInApp(NativePage page) => !webVersion.contains(page);

  AppSettings withOpensInApp(NativePage page, bool inApp) => AppSettings(
    webVersion: inApp ? ({...webVersion}..remove(page)) : {...webVersion, page},
  );

  Map<String, dynamic> toJson() => {
    'webVersion': [for (final page in webVersion) page.name],
  };
}

/// Keeps [AppSettings] in the app's support directory, outside the cache deleted by a logout.
class AppSettingsStore {
  AppSettingsStore([Future<Directory> Function()? baseDir])
    : _baseDir = baseDir ?? getApplicationSupportDirectory;

  final Future<Directory> Function() _baseDir;

  Future<AppSettings> load() async {
    final file = await _file();
    if (!await file.exists()) return const AppSettings();
    try {
      return AppSettings.fromJson(
        jsonDecode(await file.readAsString()) as Map<String, dynamic>,
      );
    } on Object {
      return const AppSettings(); // Broken or of an older version: the defaults.
    }
  }

  Future<void> save(AppSettings settings) async {
    final file = await _file();
    await file.parent.create(recursive: true);
    await file.writeAsString(jsonEncode(settings.toJson()), flush: true);
  }

  Future<File> _file() async =>
      File('${(await _baseDir()).path}/settings.json');
}

final appSettingsStoreProvider = Provider<AppSettingsStore>(
  (ref) => AppSettingsStore(),
);

final appSettingsProvider =
    AsyncNotifierProvider<AppSettingsController, AppSettings>(
      AppSettingsController.new,
    );

class AppSettingsController extends AsyncNotifier<AppSettings> {
  @override
  Future<AppSettings> build() => ref.read(appSettingsStoreProvider).load();

  Future<void> setOpensInApp(NativePage page, bool inApp) async {
    final settings = (state.value ?? const AppSettings()).withOpensInApp(
      page,
      inApp,
    );
    state = AsyncData(settings);
    await ref.read(appSettingsStoreProvider).save(settings);
  }
}
