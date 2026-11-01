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

import 'dart:io';

import 'package:device_info_plus/device_info_plus.dart';
import 'package:package_info_plus/package_info_plus.dart';

/// How the app names its device at the server (shown in "My account", apps), and its version.
class DeviceDescription {
  const DeviceDescription({
    required this.name,
    required this.platform,
    required this.appVersion,
  });

  final String name;
  final String platform;
  final String appVersion;

  static Future<DeviceDescription> load() async {
    final info = DeviceInfoPlugin();
    final package = await PackageInfo.fromPlatform();
    final version = '${package.version}+${package.buildNumber}';
    if (Platform.isIOS) {
      final ios = await info.iosInfo;
      return DeviceDescription(
        name: ios.name,
        platform: 'ios',
        appVersion: version,
      );
    }
    if (Platform.isAndroid) {
      final android = await info.androidInfo;
      return DeviceDescription(
        name: '${android.manufacturer} ${android.model}',
        platform: 'android',
        appVersion: version,
      );
    }
    return DeviceDescription(
      name: Platform.localHostname,
      platform: Platform.operatingSystem,
      appVersion: version,
    );
  }
}
