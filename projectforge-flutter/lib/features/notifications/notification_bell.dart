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
import '../../core/status/user_status_controller.dart';
import 'notifications_screen.dart';

/// The bell in the bar: the count of the visible notifications, in the colour of the most severe; opens the list.
class NotificationBell extends ConsumerWidget {
  const NotificationBell({super.key, required this.onOpenLink});

  /// Shows the page of a link (a path of the server, see [webPathOfLink]).
  final void Function(String path) onOpenLink;

  @override
  Widget build(BuildContext context, WidgetRef ref) {
    final status = ref.watch(userStatusProvider).value;
    final count = status?.openNotifications ?? 0;
    return IconButton(
      tooltip: context.t('notification.center'),
      icon: Badge(
        label: Text('$count'),
        isLabelVisible: count > 0,
        backgroundColor: severityColor(context, status?.maxSeverity),
        child: Icon(count > 0 ? Icons.notifications : Icons.notifications_none),
      ),
      onPressed: () => Navigator.of(context).push(
        MaterialPageRoute<void>(
          builder: (_) => NotificationsScreen(onOpenLink: onOpenLink),
        ),
      ),
    );
  }
}
