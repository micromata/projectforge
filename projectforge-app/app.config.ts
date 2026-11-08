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
// The Expo account and project of the store builds (eas init) aren't part of the repository: locally in .env.local
// (gitignored, see README), on EAS as environment variables of the project. Without them local builds work as before.
const EXPO_OWNER = process.env.EXPO_OWNER || undefined;
const EAS_PROJECT_ID = process.env.EAS_PROJECT_ID || undefined;

export default ({ config }: ConfigContext): ExpoConfig => ({
  ...config,
  name: IS_DEV ? 'ProjectForge Dev' : 'ProjectForge',
  slug: 'projectforge-app',
  ...(EXPO_OWNER && { owner: EXPO_OWNER }),
  ...(EAS_PROJECT_ID && {
    extra: { ...config.extra, eas: { projectId: EAS_PROJECT_ID } },
    // EAS Update (OTA): an update reaches only builds of the same native code (runtimeVersion).
    updates: { url: `https://u.expo.dev/${EAS_PROJECT_ID}` },
  }),
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
    buildNumber: '7',
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
    versionCode: 7,
    allowBackup: false,
    predictiveBackGestureEnabled: false,
  },
  plugins: [
    'expo-router',
    'expo-web-browser',
    'expo-secure-store',
    'expo-sharing',
    'expo-localization',
    // APNs directly (ApnsPushSender of the server, no FCM). The entitlement `aps-environment` is development; Xcode
    // switches it to production for App Store and TestFlight builds.
    'expo-notifications',
  ],
});
