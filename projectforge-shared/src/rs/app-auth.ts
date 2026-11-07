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

// The login of the app and its web session (AppAuthPublicRest, AppAuthRest, SystemStatusRest).
//
// Every interface of src/rs names its Kotlin class with @server (an answer) or @request (sent by the app):
// AppApiContractTest (projectforge-application) checks each field against it. A field the app reads defensively
// (with a fallback, e.g. of a newer server) is marked @optionalRead and may be missing on the server.

/** @request org.projectforge.rest.app.AppAuthPublicRest$TokenRequest */
export interface TokenRequest {
  code: string;
  codeVerifier: string;
}

/** @server org.projectforge.rest.app.AppAuthPublicRest$TokenResponse */
export interface TokenResponse {
  userId: number;
  username?: string | null;
  fullname?: string | null;
  deviceId: number;
  /** Sent as `Authorization: PFApp <token>` with every request. */
  token: string;
  /** The cookies of an SSO proxy in front of the server (name to value), empty without one. */
  proxyCookies?: Record<string, string> | null;
  /** The server takes the login parameters in the path too (`/next/appLogin/<login>`). */
  appLoginPath?: boolean;
}

/** @server org.projectforge.rest.pub.SystemStatusRest$SystemData */
export interface SystemData {
  appname?: string;
  version?: string;
  /** The oldest app version the server still works with (AppCompatibility.MIN_APP_VERSION). */
  minAppVersion?: string;
  appLoginPath?: boolean;
}

/** @request org.projectforge.rest.app.AppAuthRest$PushTokenRequest */
export interface PushTokenRequest {
  /** The APNs device token (hex). */
  token: string;
  environment: 'SANDBOX' | 'PRODUCTION';
}

/** @server org.projectforge.rest.app.AppAuthRest$PushTokenResponse */
export interface PushTokenResponse {
  /**
   * The pushes name the device (`device` beside `aps`): the app tells by it which login a push belongs to. Missing for
   * an older server, which answers nothing: such a server is let push only while it is shown.
   * @optionalRead
   */
  deviceTagged?: boolean;
}

/** @server org.projectforge.rest.app.AppAuthRest$WebSessionResponse */
export interface WebSessionResponse {
  startUrl: string;
}
