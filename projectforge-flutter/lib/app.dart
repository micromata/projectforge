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

import 'core/auth/auth_controller.dart';
import 'core/i18n/translations.dart';
import 'features/home/home_screen.dart';
import 'features/login/login_screen.dart';

class ProjectForgeApp extends StatelessWidget {
  const ProjectForgeApp({super.key, required this.translations});

  final Translations translations;

  @override
  Widget build(BuildContext context) {
    // ProjectForge blue, as the logo of the web app.
    const seed = Color(0xFF1F5E9E);
    return TranslationsScope(
      translations: translations,
      child: MaterialApp(
        title: 'ProjectForge',
        theme: ThemeData(colorSchemeSeed: seed, useMaterial3: true),
        darkTheme: ThemeData(
          colorSchemeSeed: seed,
          brightness: Brightness.dark,
          useMaterial3: true,
        ),
        home: const _AuthGate(),
      ),
    );
  }
}

/// The login while logged out, the app afterwards.
class _AuthGate extends ConsumerWidget {
  const _AuthGate();

  @override
  Widget build(BuildContext context, WidgetRef ref) {
    final auth = ref.watch(authControllerProvider);
    return switch (auth.status) {
      AuthStatus.restoring => const Scaffold(
        body: Center(child: CircularProgressIndicator()),
      ),
      AuthStatus.loggedOut => LoginScreen(messageKey: auth.messageKey),
      // Keyed by the device: another user (a renewal in the browser as someone else) starts with fresh tabs.
      AuthStatus.loggedIn => HomeScreen(key: ValueKey(auth.session?.deviceId)),
    };
  }
}
