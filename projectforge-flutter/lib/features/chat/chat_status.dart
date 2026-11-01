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

/// What the app shows of the chat outside of its pages: the badge of the tab. Taken from the user status
/// (`UserStatusRest.Result`, `GET /rs/userStatus`), the json map is kept.
class ChatStatus {
  ChatStatus(this.json);

  /// The feature id of the chat (`MenuItemDefId.CHAT`) in `disabledFeatures`.
  static const featureId = 'CHAT';

  final Map<String, dynamic> json;

  /// The unread messages in all chat rooms and direct messages (`chatUnread`).
  int get unread => (json['chatUnread'] as num?)?.toInt() ?? 0;

  /// False if the chat is switched off on the server (`disabledFeatures`): the app shows no chat tab.
  bool get enabled => !(json['disabledFeatures'] as List<dynamic>? ?? const [])
      .contains(featureId);
}
