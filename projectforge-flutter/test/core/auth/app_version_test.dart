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
import 'package:flutter_test/flutter_test.dart';
import 'package:package_info_plus/package_info_plus.dart';
import 'package:projectforge_app/core/auth/app_version.dart';

void main() {
  test('the version is shown as version+build', () async {
    PackageInfo.setMockInitialValues(
      appName: 'ProjectForge',
      packageName: 'de.projectforge.mobileapp',
      version: '1.0.1',
      buildNumber: '3',
      buildSignature: '',
    );
    final container = ProviderContainer();
    addTearDown(container.dispose);
    expect(await container.read(appVersionProvider.future), '1.0.1+3');
  });
}
