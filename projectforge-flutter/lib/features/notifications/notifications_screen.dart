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
import 'package:intl/intl.dart';

import '../../core/api/api_exception.dart';
import '../../core/i18n/translations.dart';
import '../../core/widgets/error_view.dart';
import 'notification.dart';
import 'notification_text.dart';
import 'notifications_controller.dart';

/// The colour of a severity (`INFO`, `IMPORTANT`, `URGENT`), null for none.
Color? severityColor(BuildContext context, String? severity) =>
    switch (severity) {
      'URGENT' => Colors.red.shade700,
      'IMPORTANT' => Colors.orange.shade800,
      'INFO' => Theme.of(context).colorScheme.primary,
      _ => null,
    };

/// The visible notifications (banners first, they stay until done) and the archived ones of the last 90 days.
class NotificationsScreen extends ConsumerWidget {
  const NotificationsScreen({super.key, required this.onOpenLink});

  /// Shows the page of a link (a path of the server, see [webPathOfLink]); the list is closed before.
  final void Function(String path) onOpenLink;

  @override
  Widget build(BuildContext context, WidgetRef ref) {
    final notifications = ref.watch(notificationsProvider);
    return Scaffold(
      appBar: AppBar(title: Text(context.t('notification.center'))),
      body: notifications.when(
        skipLoadingOnRefresh: true,
        loading: () => const Center(child: CircularProgressIndicator()),
        error: (error, _) => ErrorView(
          message: context.errorText(error),
          onRetry: () => ref.read(notificationsProvider.notifier).refresh(),
        ),
        data: (list) {
          final sorted = [...list]
            ..sort((a, b) => (b.isBanner ? 1 : 0) - (a.isBanner ? 1 : 0));
          return RefreshIndicator(
            onRefresh: () async {
              final error = await ref
                  .read(notificationsProvider.notifier)
                  .refresh();
              if (error != null && context.mounted) _report(context, error);
            },
            child: ListView(
              padding: const EdgeInsets.fromLTRB(12, 8, 12, 24),
              children: [
                if (sorted.isEmpty)
                  Padding(
                    padding: const EdgeInsets.symmetric(vertical: 24),
                    child: Center(
                      child: Text(context.t('notification.centerEmpty')),
                    ),
                  ),
                for (final notification in sorted)
                  _NotificationCard(
                    notification: notification,
                    onOpenLink: onOpenLink,
                  ),
                const _Archived(),
              ],
            ),
          );
        },
      ),
    );
  }
}

void _report(BuildContext context, ApiException error) =>
    ScaffoldMessenger.of(context)
        .showSnackBar(SnackBar(content: Text(context.errorText(error))));

class _Archived extends ConsumerWidget {
  const _Archived();

  @override
  Widget build(BuildContext context, WidgetRef ref) => ExpansionTile(
    title: Text(context.t('notification.centerArchived')),
    shape: const Border(),
    collapsedShape: const Border(),
    children: [
      // Built (and loaded) only once expanded.
      Consumer(
        builder: (context, ref, _) {
          final archived = ref.watch(archivedNotificationsProvider);
          return archived.when(
            loading: () => const Padding(
              padding: EdgeInsets.all(16),
              child: Center(child: CircularProgressIndicator()),
            ),
            error: (error, _) => ErrorView(
              message: context.errorText(error),
              onRetry: () => ref.invalidate(archivedNotificationsProvider),
            ),
            data: (list) => list.isEmpty
                ? Padding(
                    padding: const EdgeInsets.all(16),
                    child: Text(context.t('notification.centerArchivedEmpty')),
                  )
                : Column(
                    children: [
                      for (final notification in list)
                        _NotificationCard(
                          notification: notification,
                          archived: true,
                        ),
                    ],
                  ),
          );
        },
      ),
    ],
  );
}

class _NotificationCard extends ConsumerWidget {
  const _NotificationCard({
    required this.notification,
    this.onOpenLink,
    this.archived = false,
  });

  final AppNotification notification;
  final void Function(String path)? onOpenLink;
  final bool archived;

  @override
  Widget build(BuildContext context, WidgetRef ref) {
    final color = severityColor(context, notification.severity);
    final body = htmlToText(notification.body);
    final path = archived ? null : webPathOfLink(notification.link);
    final created = notification.created;
    final controller = ref.read(notificationsProvider.notifier);
    return Card(
      clipBehavior: Clip.antiAlias,
      child: Container(
        decoration: BoxDecoration(
          border: Border(
            left: BorderSide(color: color ?? Colors.transparent, width: 4),
          ),
        ),
        padding: const EdgeInsets.fromLTRB(12, 10, 8, 4),
        child: Column(
          crossAxisAlignment: CrossAxisAlignment.start,
          children: [
            Text(
              notification.title,
              style: Theme.of(context).textTheme.titleSmall?.copyWith(
                fontWeight: !archived && notification.canAcknowledge
                    ? FontWeight.bold
                    : null,
              ),
            ),
            if (body.isNotEmpty)
              Padding(
                padding: const EdgeInsets.only(top: 4),
                child: Text(body),
              ),
            if (created != null)
              Padding(
                padding: const EdgeInsets.only(top: 4),
                child: Text(
                  DateFormat.yMd().add_Hm().format(created),
                  style: Theme.of(context).textTheme.bodySmall,
                ),
              ),
            if (!archived)
              OverflowBar(
                alignment: MainAxisAlignment.end,
                children: [
                  if (path != null)
                    TextButton(
                      onPressed: () async {
                        // Reading it is following the link: the notification is not left open behind.
                        if (notification.canAcknowledge) {
                          await controller.acknowledge(notification.id);
                        }
                        if (!context.mounted) return;
                        Navigator.of(context).pop();
                        onOpenLink?.call(path);
                      },
                      child: Text(context.t('notification.open')),
                    ),
                  if (notification.canAcknowledge)
                    TextButton(
                      onPressed: () async {
                        final error = await controller.acknowledge(
                          notification.id,
                        );
                        if (error != null && context.mounted) {
                          _report(context, error);
                        }
                      },
                      child: Text(context.t('notification.acknowledge')),
                    ),
                  if (notification.canDone)
                    TextButton(
                      onPressed: () async {
                        final error = await controller.done(notification.id);
                        if (error != null && context.mounted) {
                          _report(context, error);
                        }
                      },
                      child: Text(context.t('notification.markDone')),
                    ),
                ],
              ),
          ],
        ),
      ),
    );
  }
}
