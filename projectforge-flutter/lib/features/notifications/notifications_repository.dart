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

import '../../core/api/api_client.dart';
import 'notification.dart';

/// The notifications of the user (`NotificationRest`, `/rs/notification`).
class NotificationsRepository {
  NotificationsRepository(this._api);

  static const _base = '/rs/notification';

  final ApiClient _api;

  /// The visible ones, newest first. Loaded in the background (when the status changes): a refusal never opens a
  /// login in the browser.
  Future<List<AppNotification>> loadOpen() async =>
      _list(await _api.get<List<dynamic>>('$_base/my', renewLogin: false));

  /// The ones no longer visible of the last 90 days.
  Future<List<AppNotification>> loadArchived() async =>
      _list(await _api.get<List<dynamic>>('$_base/my/archived'));

  /// Marks it as read (a banner stays until it is done).
  Future<void> acknowledge(int id) => _api.post<void>('$_base/$id/acknowledge');

  /// Marks it as done (only if [AppNotification.manualDone]).
  Future<void> done(int id) => _api.post<void>('$_base/$id/done');

  static List<AppNotification> _list(List<dynamic> json) => [
    for (final item in json) AppNotification(item as Map<String, dynamic>),
  ];
}
