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

import Constants from 'expo-constants';
import * as Crypto from 'expo-crypto';
import { fetch as expoFetch } from 'expo/fetch';
import * as WebBrowser from 'expo-web-browser';
import { AppState, Platform } from 'react-native';

import { HttpFetch, withTimeout } from './api/http';
import { CALLBACK_URL, OpenBrowser } from './auth/appLogin';
import { CryptoProvider } from './auth/pkce';
import { appLog } from './log/appLog';

/** The platform services of the app, kept apart so the logic can be tested without them. */
export const platformCrypto: CryptoProvider = {
  randomBytes: (count) => Crypto.getRandomBytes(count),
  sha256: async (data) =>
    new Uint8Array(await Crypto.digest(Crypto.CryptoDigestAlgorithm.SHA256, data as unknown as BufferSource)),
};

/** expo/fetch (see src/core/api/http.ts), the answer within 30 s. */
export const platformHttp: HttpFetch = withTimeout(
  (url, init) => expoFetch(url, init as Parameters<typeof expoFetch>[1]) as unknown as Promise<Response>,
  30_000,
);

/**
 * The login page in the system browser: ASWebAuthenticationSession on iOS (shares the cookies of Safari, e.g. the
 * session of an SSO proxy), Custom Tabs on Android. Not ephemeral: the SSO session of the browser is the point.
 */
export const platformOpenBrowser: OpenBrowser = async (url) => {
  const open = () => WebBrowser.openAuthSessionAsync(url, CALLBACK_URL, { preferEphemeralSession: false });
  let result;
  try {
    result = await open();
  } catch (e) {
    // "The authentication session could not be started": the app isn't active yet (just back from the background) or
    // the previous session is still closing. Once more, when the app is active.
    appLog.warn('login', `Browser session not started: ${String(e)}, once more`);
    await untilActive(3_000);
    await new Promise((resolve) => setTimeout(resolve, 700));
    result = await open();
  }
  appLog.debug('login', `Browser session: ${result.type}`);
  return result.type === 'success' ? result.url : null;
};

/** Resolves when the app is active, at the latest after [maxMillis]. */
function untilActive(maxMillis: number): Promise<void> {
  if (AppState.currentState === 'active') return Promise.resolve();
  return new Promise((resolve) => {
    const done = () => {
      clearTimeout(timer);
      subscription.remove();
      resolve();
    };
    const timer = setTimeout(done, maxMillis);
    const subscription = AppState.addEventListener('change', (next) => next === 'active' && done());
  });
}

/** The version of the app as `version+build`, e.g. `1.1.0+6` (as the Flutter app: header, user agent, menu). */
export const appVersion: string = versionWithBuild(
  Constants.expoConfig?.version,
  Platform.OS === 'ios' ? Constants.expoConfig?.ios?.buildNumber : Constants.expoConfig?.android?.versionCode,
);

export function versionWithBuild(version?: string | null, build?: string | number | null): string {
  return build != null && `${build}` !== '' ? `${version ?? '0'}+${build}` : (version ?? '0');
}

export const platformName: string = Platform.OS;

export function deviceName(): string {
  return Constants.deviceName ?? (Platform.OS === 'ios' ? 'iPhone' : 'Android');
}
