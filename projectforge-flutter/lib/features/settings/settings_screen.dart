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

import '../../core/i18n/translations.dart';
import '../../core/settings/app_settings.dart';
import '../../core/web/native_page.dart';

/// The settings of the app on this device.
class SettingsScreen extends ConsumerWidget {
  const SettingsScreen({super.key});

  @override
  Widget build(BuildContext context, WidgetRef ref) {
    final settings =
        ref.watch(appSettingsProvider).value ?? const AppSettings();
    return Scaffold(
      appBar: AppBar(title: Text(context.t('settings'))),
      body: ListView(
        children: [
          ListTile(
            title: Text(context.t('app.settings.openInApp')),
            subtitle: Text(context.t('app.settings.openInApp.info')),
          ),
          for (final page in switchableNativePages)
            SwitchListTile(
              title: Text(_label(context, page)),
              value: settings.opensInApp(page),
              onChanged: (inApp) => ref
                  .read(appSettingsProvider.notifier)
                  .setOpensInApp(page, inApp),
            ),
        ],
      ),
    );
  }

  static String _label(BuildContext context, NativePage page) => switch (page) {
    NativePage.chat => context.t('chat.title'),
    NativePage.businessCards => context.t('menu.myBusinessCards'),
    NativePage.notifications => context.t('notification.center'),
    NativePage.addresses => context.t('menu.addressList'),
    NativePage.timesheets => context.t('timesheet.title.heading'),
  };
}
