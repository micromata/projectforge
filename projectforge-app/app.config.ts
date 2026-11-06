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

import { ConfigContext, ExpoConfig } from 'expo/config';

/**
 * The app config (instead of app.json, for the variants). APP_VARIANT=development (the dev client, eas.json
 * profile development) gets its own bundle id and name, so it lives next to the store app on a device and doesn't
 * replace it; preview and production are the store app de.projectforge.mobileapp, the update of the Flutter app.
 */
const IS_DEV = process.env.APP_VARIANT === 'development';
const BUNDLE_ID = IS_DEV ? 'de.projectforge.mobileapp.dev' : 'de.projectforge.mobileapp';

export default ({ config }: ConfigContext): ExpoConfig => ({
  ...config,
  name: IS_DEV ? 'ProjectForge Dev' : 'ProjectForge',
  slug: 'projectforge-app',
  // The callback of the browser login (AppAuthService of the server builds projectforge://auth).
  scheme: 'projectforge',
  // Higher than the Flutter app's last 1.0.1+5. The build numbers go on from there.
  version: '1.1.0',
  orientation: 'default',
  icon: './assets/icon.png',
  userInterfaceStyle: 'automatic',
  runtimeVersion: { policy: 'fingerprint' },
  ios: {
    bundleIdentifier: BUNDLE_ID,
    buildNumber: '6',
    appleTeamId: 'E4Q65AC7LP',
    supportsTablet: true,
    infoPlist: {
      CFBundleDisplayName: IS_DEV ? 'PF Dev' : 'ProjectForge',
      ITSAppUsesNonExemptEncryption: false,
      NSCameraUsageDescription: 'Photos for the chat and other uploads of the pages.',
      NSPhotoLibraryUsageDescription: 'Images for the chat and other uploads of the pages.',
    },
  },
  android: {
    package: BUNDLE_ID,
    versionCode: 6,
    allowBackup: false,
    predictiveBackGestureEnabled: false,
  },
  plugins: ['expo-web-browser', 'expo-secure-store', 'expo-sharing'],
});
