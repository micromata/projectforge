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

import 'package:flutter/material.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';

import '../../core/auth/auth_controller.dart';
import '../../core/auth/session.dart';
import '../../core/i18n/translations.dart';
import '../login/login_screen.dart';
import '../timesheets/timesheets_controller.dart';

/// The choice of the sheet to add a server rather than switching to a stored login.
const _addServer = 'add';

/// The stored logins to switch to without the browser, and the way to log in at another server. The data of the
/// current server is removed from the device by a switch, so a running timer is discarded (asked first).
Future<void> showServerSwitch(BuildContext context, WidgetRef ref) async {
  final auth = ref.read(authControllerProvider.notifier);
  final sessions = await auth.sessions();
  if (!context.mounted) return;
  final active = ref.read(authControllerProvider).session;
  final choice = await showModalBottomSheet<Object>(
    context: context,
    showDragHandle: true,
    useSafeArea: true,
    isScrollControlled: true,
    builder: (context) => SingleChildScrollView(
      child: Column(
        mainAxisSize: MainAxisSize.min,
        crossAxisAlignment: CrossAxisAlignment.stretch,
        children: [
          Padding(
            padding: const EdgeInsets.fromLTRB(16, 0, 16, 8),
            child: Text(
              context.t('app.server.switch'),
              style: Theme.of(context).textTheme.titleMedium,
            ),
          ),
          for (final it in sessions)
            ListTile(
              leading: const Icon(Icons.dns_outlined),
              title: Text(it.fullname ?? it.username ?? it.serverUrl),
              subtitle: Text(it.serverUrl),
              trailing: it.serverUrl == active?.serverUrl
                  ? const Icon(Icons.check)
                  : null,
              onTap: () => Navigator.pop(context, it),
            ),
          const Divider(),
          ListTile(
            leading: const Icon(Icons.add),
            title: Text(context.t('app.server.add')),
            onTap: () => Navigator.pop(context, _addServer),
          ),
        ],
      ),
    ),
  );
  if (!context.mounted) return;
  if (choice == _addServer) {
    if (!await _timerMayBeDiscarded(context, ref) || !context.mounted) return;
    await Navigator.of(context).push(
      MaterialPageRoute<void>(
        builder: (_) => const LoginScreen(addServer: true),
      ),
    );
  } else if (choice is Session && choice.serverUrl != active?.serverUrl) {
    if (!await _timerMayBeDiscarded(context, ref)) return;
    await auth.switchTo(choice);
  }
}

/// True without a running timer, otherwise if the user agrees to discard it.
Future<bool> _timerMayBeDiscarded(BuildContext context, WidgetRef ref) async {
  if (ref.read(timerProvider).value == null) return true;
  final confirmed = await showDialog<bool>(
    context: context,
    builder: (context) => AlertDialog(
      content: Text(context.t('app.server.switch.timer')),
      actions: [
        TextButton(
          onPressed: () => Navigator.pop(context, false),
          child: Text(context.t('cancel')),
        ),
        FilledButton(
          onPressed: () => Navigator.pop(context, true),
          child: Text(context.t('app.server.switch')),
        ),
      ],
    ),
  );
  return confirmed == true;
}
