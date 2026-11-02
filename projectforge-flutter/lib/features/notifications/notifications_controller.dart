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

import 'package:flutter_riverpod/flutter_riverpod.dart';

import '../../core/api/api_exception.dart';
import '../../core/auth/auth_controller.dart';
import '../../core/status/user_status_controller.dart';
import 'notification.dart';
import 'notifications_repository.dart';

final notificationsRepositoryProvider = Provider<NotificationsRepository?>((
  ref,
) {
  final api = ref.watch(apiClientProvider);
  return api == null ? null : NotificationsRepository(api);
});

final notificationsProvider =
    AsyncNotifierProvider<NotificationsController, List<AppNotification>>(
      NotificationsController.new,
    );

/// The archived ones, loaded when the list is opened.
final archivedNotificationsProvider =
    FutureProvider.autoDispose<List<AppNotification>>((ref) {
      return ref.watch(notificationsRepositoryProvider)!.loadArchived();
    });

/// The visible notifications. They are loaded again whenever the summary of the user status changes (count or newest
/// id), so a poll or a push that finds something new shows it without a timer of its own.
class NotificationsController extends AsyncNotifier<List<AppNotification>> {
  @override
  Future<List<AppNotification>> build() {
    final repository = ref.watch(notificationsRepositoryProvider)!;
    // Rebuilds only when the summary changes, not with every poll of the status.
    ref.watch(
      userStatusProvider.select(
        (s) => '${s.value?.openNotifications}/${s.value?.latestNotificationId}',
      ),
    );
    final status = ref.read(userStatusProvider).value;
    if (status != null && status.openNotifications == 0) {
      return Future.value(const []);
    }
    return repository.loadOpen();
  }

  /// Loads the list and the status again; offline (or on an error) the shown list stays.
  Future<ApiException?> refresh() async {
    final repository = ref.read(notificationsRepositoryProvider);
    if (repository == null) return null;
    try {
      final list = await repository.loadOpen();
      if (ref.mounted) state = AsyncData(list);
      await ref.read(userStatusProvider.notifier).refresh();
      return null;
    } on ApiException catch (e) {
      return e;
    }
  }

  /// Marks it as read. The server answers with the updated notification, the list is loaded again.
  Future<ApiException?> acknowledge(int id) =>
      _act((repository) => repository.acknowledge(id));

  Future<ApiException?> done(int id) =>
      _act((repository) => repository.done(id));

  Future<ApiException?> _act(
    Future<void> Function(NotificationsRepository) action,
  ) async {
    final repository = ref.read(notificationsRepositoryProvider);
    if (repository == null) return null;
    try {
      await action(repository);
    } on ApiException catch (e) {
      return e;
    }
    ref.invalidate(archivedNotificationsProvider);
    return refresh();
  }
}
