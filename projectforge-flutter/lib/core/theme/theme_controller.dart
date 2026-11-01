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
import 'package:flutter/material.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';

import '../auth/auth_controller.dart';
import '../status/user_status_controller.dart';

final themeModeProvider = NotifierProvider<ThemeController, ThemeMode>(
  ThemeController.new,
);

/// The theme of the app's own screens: the one the user chose in the web pages (light, dark or the system's), so
/// the app looks like its pages. Taken from the user status; the last one is stored for a start without the server.
/// Logged out: the system's.
class ThemeController extends Notifier<ThemeMode> {
  static const _cacheName = 'theme';

  /// The user status answered: the stored theme is too old.
  bool _fromServer = false;

  @override
  ThemeMode build() {
    _fromServer = false;
    if (ref.watch(apiClientProvider) == null) return ThemeMode.system;
    ref.listen(userStatusProvider, (_, status) {
      final value = status.value;
      if (value == null) return;
      final theme = value.theme;
      if (theme == null) {
        _loadOfOlderServer();
      } else {
        _fromServer = true;
        _set(theme, store: true);
      }
    }, fireImmediately: true);
    _loadStored();
    return ThemeMode.system;
  }

  Future<void> _loadStored() async {
    final stored = await ref.read(jsonCacheProvider).read(_cacheName);
    final theme = stored?['theme'];
    if (!_fromServer && ref.mounted && theme is String) _set(theme);
  }

  /// A server whose user status has no theme yet: asked for by the endpoint of the web pages (`UISettingsRest`).
  Future<void> _loadOfOlderServer() async {
    try {
      final settings = await ref
          .read(apiClientProvider)
          ?.get<Map<String, dynamic>>(
            '/rs/uiSettings/theme',
            renewLogin: false,
          );
      final theme = settings?['theme'];
      if (!ref.mounted || theme is! String) return;
      _fromServer = true;
      _set(theme, store: true);
    } on Object {
      // Offline or refused: the stored theme stays.
    }
  }

  void _set(String theme, {bool store = false}) {
    final mode = parse(theme);
    if (mode == state) return;
    state = mode;
    if (store) {
      ref.read(jsonCacheProvider).write(_cacheName, {'theme': theme});
    }
  }

  static ThemeMode parse(String? theme) => switch (theme) {
    'light' => ThemeMode.light,
    'dark' => ThemeMode.dark,
    _ => ThemeMode.system,
  };
}
