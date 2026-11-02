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

import '../auth/app_version.dart';
import '../status/user_status_controller.dart';

/// The oldest server version this app works with. Raise it in the commit that needs a feature of a newer server;
/// prefer a feature flag of the system status (e.g. `appLoginPath`) where the app can fall back.
///
/// The other direction, the app version a server needs, is declared by the server (`AppCompatibility.MIN_APP_VERSION`,
/// `systemData.minAppVersion`): only the newer side knows about a break.
const minServerVersion = '9.2.5';

/// Why the app and the server may not work together. Only a warning: nothing is refused.
sealed class CompatibilityIssue {
  const CompatibilityIssue(this.minVersion);

  /// The version the other side needs.
  final String minVersion;
}

/// The server needs a newer app ([minVersion]): the user should update it.
class AppTooOld extends CompatibilityIssue {
  const AppTooOld(super.minVersion);
}

/// The app needs a newer server ([minVersion]): some functions may fail, only the administrator can help.
class ServerTooOld extends CompatibilityIssue {
  const ServerTooOld(super.minVersion);
}

/// The issue between [appVersion] (`1.0.1+3`) and a server of [serverVersion] (`9.2.4-SNAPSHOT`) that asks for
/// [minAppVersion]. Null if they fit, or if a version is unknown or unparseable (an older server sends no
/// [minAppVersion]): no false alarm.
CompatibilityIssue? compatibilityIssue({
  required String? appVersion,
  required String? serverVersion,
  required String? minAppVersion,
}) {
  if (_isOlder(appVersion, minAppVersion)) return AppTooOld(minAppVersion!);
  if (_isOlder(serverVersion, minServerVersion)) {
    return const ServerTooOld(minServerVersion);
  }
  return null;
}

bool _isOlder(String? version, String? min) {
  final a = parseVersion(version);
  final b = parseVersion(min);
  return a != null && b != null && compareVersions(a, b) < 0;
}

final _versionPattern = RegExp(r'^\d{1,9}(\.\d{1,9})*');

/// The numeric segments of `1.0.1`, `1.0.1+3` or `9.2.4-SNAPSHOT` (a snapshot counts as its release, so a
/// development server doesn't warn); null if it doesn't start with a number.
List<int>? parseVersion(String? version) {
  final match = _versionPattern.firstMatch(version?.trim() ?? '');
  return match?.group(0)!.split('.').map(int.parse).toList();
}

/// Compares the segments, a missing one counts as 0 (`1.0` == `1.0.0`).
int compareVersions(List<int> a, List<int> b) {
  for (var i = 0; i < a.length || i < b.length; i++) {
    final diff = (i < a.length ? a[i] : 0).compareTo(i < b.length ? b[i] : 0);
    if (diff != 0) return diff;
  }
  return 0;
}

/// The issue with the current server, from its user status (`systemData`); null as long as it isn't loaded.
final compatibilityIssueProvider = Provider<CompatibilityIssue?>((ref) {
  final appVersion = ref.watch(appVersionProvider).value;
  final status = ref.watch(userStatusProvider).value;
  if (appVersion == null || status == null) return null;
  return compatibilityIssue(
    appVersion: appVersion,
    serverVersion: status.serverVersion,
    minAppVersion: status.minAppVersion,
  );
});
