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

/// What the app shows of the server outside of its pages: the badge of the chat tab, the bell of the
/// notifications and the compatibility warning. Taken from the user status (`UserStatusRest.Result`, `GET /rs/userStatus`), the json map is kept.
class UserStatus {
  UserStatus(this.json);

  /// The feature id of the chat (`MenuItemDefId.CHAT`) in `disabledFeatures`.
  static const chatFeatureId = 'CHAT';

  final Map<String, dynamic> json;

  /// The unread messages in all chat rooms and direct messages (`chatUnread`).
  int get chatUnread => (json['chatUnread'] as num?)?.toInt() ?? 0;

  /// False if the chat is switched off on the server (`disabledFeatures`): the app shows no chat tab.
  bool get chatEnabled =>
      !(json['disabledFeatures'] as List<dynamic>? ?? const []).contains(
        chatFeatureId,
      );

  Map<String, dynamic> get _notifications =>
      json['notifications'] as Map<String, dynamic>? ?? const {};

  /// The notifications visible to the user (`NotificationDao.Summary.openCount`).
  int get openNotifications =>
      (_notifications['openCount'] as num?)?.toInt() ?? 0;

  /// The most severe of them: `INFO`, `IMPORTANT`, `URGENT`, null without any.
  String? get maxSeverity => _notifications['maxSeverity'] as String?;

  /// The id of the newest one: with [openNotifications] it tells when the list has to be loaded again.
  int? get latestNotificationId =>
      (_notifications['latestId'] as num?)?.toInt();

  /// The message of the administrators (e.g. maintenance, `alertMessage`), null without one.
  String? get alertMessage {
    final message = (json['alertMessage'] as String?)?.trim();
    return message == null || message.isEmpty ? null : message;
  }

  /// The user's theme of the web pages (`UISettingsRest`): `light`, `dark` or `system`, null from an older server.
  String? get theme => json['theme'] as String?;

  Map<String, dynamic> get _systemData =>
      json['systemData'] as Map<String, dynamic>? ?? const {};

  /// The version of the server (`SystemStatusRest.SystemData.version`), e.g. `9.2.4` or `9.2.4-SNAPSHOT`.
  String? get serverVersion => _systemData['version'] as String?;

  /// The oldest app version the server works with (`SystemData.minAppVersion`), null from an older server.
  String? get minAppVersion => _systemData['minAppVersion'] as String?;
}
