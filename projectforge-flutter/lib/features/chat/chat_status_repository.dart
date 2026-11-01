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
import 'chat_status.dart';

/// The chat's part of the user status (`UserStatusRest`, `GET /rs/userStatus`).
class ChatStatusRepository {
  ChatStatusRepository(this._api);

  final ApiClient _api;

  /// Polled in the background: a refusal never opens a login in the browser (see ApiClient).
  Future<ChatStatus> load() async => ChatStatus(
    await _api.get<Map<String, dynamic>>('/rs/userStatus', renewLogin: false),
  );
}
