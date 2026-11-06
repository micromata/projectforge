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

import { NativeModule, requireNativeModule } from 'expo';

/**
 * The native parts of the app (ios/PfNativeModule.swift, android/…/PfNativeModule.kt): the cookie jar and website
 * data of the WebViews, the Apple Wallet, the key chain items of the Flutter app, the APNs environment.
 */
declare class PfNativeModule extends NativeModule<Record<string, never>> {
  /** Sets the cookies of `Set-Cookie` header values for `url` in the WebViews; answers the names set. */
  setCookies(url: string, setCookieHeaders: string[]): Promise<string[]>;

  /** The `Cookie` header the WebViews would send to `url`, null without cookies. */
  cookieHeader(url: string): Promise<string | null>;

  /** Removes all cookies and web storage of the WebViews. */
  clearAll(): Promise<void>;

  /**
   * Shows the system sheet of iOS to add the pass (a `.pkpass` file at the `file://` url [fileUrl]) to the Wallet.
   * Rejects with the reason if iOS refuses it (an invalid signature, already in the Wallet); always on Android.
   */
  addPass(fileUrl: string): Promise<void>;

  /** A key chain item of the Flutter app (flutter_secure_storage), null if there is none (always on Android). */
  readLegacyKeychain(service: string, key: string): string | null;

  /** Deletes a key chain item of the Flutter app. */
  deleteLegacyKeychain(service: string, key: string): void;

  /**
   * The APNs environment of the signing (the `aps-environment` of the provisioning profile), null without a profile
   * (store, TestFlight: production) and on Android.
   */
  apsEnvironment(): 'SANDBOX' | 'PRODUCTION' | null;

  /** Shows the find navigator of WebKit for the WebView on screen (iOS); false if there is none (always on Android). */
  presentFindNavigator(): Promise<boolean>;
}

export default requireNativeModule<PfNativeModule>('PfNative');
