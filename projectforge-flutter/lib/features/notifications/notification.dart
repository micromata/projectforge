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

/// A notification of the user (`Notification`, `GET /rs/notification/my`), the json map is kept.
class AppNotification {
  AppNotification(this.json);

  final Map<String, dynamic> json;

  int get id => (json['id'] as num).toInt();

  /// `INFO`, `IMPORTANT` or `URGENT`.
  String get severity => json['severity'] as String? ?? 'INFO';

  /// `TOAST`, `TOAST_CONFIRM` or `BANNER` (stays until it is done).
  String get display => json['display'] as String? ?? 'TOAST';

  /// `OPEN`, `ACKNOWLEDGED`, `DONE`, `RESOLVED` or `EXPIRED`.
  String get status => json['status'] as String? ?? 'OPEN';

  /// True if the user may mark it as done (otherwise a rule resolves it).
  bool get manualDone => json['manualDone'] as bool? ?? false;

  String get title => json['title'] as String? ?? '';

  /// The text as sanitized html, see [htmlToText].
  String get body => json['body'] as String? ?? '';

  /// A path of the next app (`/chat/5?message=7`, `/timesheet`), see [webPathOfLink].
  String? get link => json['link'] as String?;

  DateTime? get created => _date('created');

  bool get isBanner => display == 'BANNER';

  /// As in the web: "confirm" while it is open, "done" for a banner the user may end.
  bool get canAcknowledge => status == 'OPEN';

  bool get canDone => isBanner && manualDone;

  DateTime? _date(String key) {
    final value = json[key];
    if (value is num) return DateTime.fromMillisecondsSinceEpoch(value.toInt());
    if (value is String) return DateTime.tryParse(value)?.toLocal();
    return null;
  }
}
