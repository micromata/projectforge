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

/** The login of the app at a server (the Flutter app's `lib/core/auth/session.dart`). */
export interface Session {
  serverUrl: string;
  token: string;
  userId: number;
  deviceId: number;
  username?: string;
  fullname?: string;
  /**
   * The cookies of an SSO proxy in front of the server (e.g. Authentik), taken from the browser login: sent with
   * each request and put into the WebView, otherwise the proxy refuses the app. Empty without such a proxy.
   */
  proxyCookies: Record<string, string>;
  /** The server takes the login parameters in the path too (`/next/appLogin/<login>`, from 9.2.4 on). */
  appLoginPath: boolean;
}

/** The `proxyCookies` of the server's answer or of a stored session (absent before this field existed). */
export function proxyCookiesFromJson(json: unknown): Record<string, string> {
  if (json == null || typeof json !== 'object' || Array.isArray(json)) return {};
  return Object.fromEntries(Object.entries(json).map(([key, value]) => [key, String(value)]));
}

export function sessionFromJson(json: Record<string, unknown>): Session {
  return {
    serverUrl: String(json.serverUrl),
    token: String(json.token),
    userId: Number(json.userId),
    deviceId: Number(json.deviceId),
    username: json.username as string | undefined,
    fullname: json.fullname as string | undefined,
    proxyCookies: proxyCookiesFromJson(json.proxyCookies),
    appLoginPath: json.appLoginPath === true,
  };
}

export function cookieHeaderOf(cookies: Record<string, string>): string | undefined {
  const entries = Object.entries(cookies);
  return entries.length ? entries.map(([name, value]) => `${name}=${value}`).join('; ') : undefined;
}
