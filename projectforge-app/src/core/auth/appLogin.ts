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

import { bufferToBase64url, utf8 } from '@projectforge/shared/base64url';
import type { SystemData, TokenRequest, TokenResponse } from '@projectforge/shared/rs/app-auth';

import { ApiException } from '../api/apiException';
import { HTTP_DEFAULTS, HttpFetch, isRedirect } from '../api/http';
import { appLog, LogSanitizer } from '../log/appLog';
import { parseUrl, SafeURL } from '../url';
import { createPkce, CryptoProvider } from './pkce';
import { proxyCookiesFromJson, Session } from './session';

/** The user closed the browser or declined the app. */
export class AppLoginCancelled extends Error {
  constructor() {
    super('Login cancelled');
  }
}

export const CALLBACK_URL = 'projectforge://auth';

/**
 * Opens [url] in the system browser (ASWebAuthenticationSession, Custom Tabs) until it is redirected to
 * [CALLBACK_URL]; answers that url, null if the user cancelled.
 */
export type OpenBrowser = (url: string) => Promise<string | null>;

/**
 * The login of the app (see AppAuthService of the server), port of the Flutter app's `lib/core/auth/app_login.dart`:
 * the login page of the server opens in the system browser, so password, WebAuthn and OTP work as in the web. Its
 * callback (`projectforge://auth?code=…&state=…`) carries a one-time code, exchanged with the PKCE verifier for the
 * token of the app.
 */
export class AppLogin {
  constructor(
    private readonly http: HttpFetch,
    private readonly crypto: CryptoProvider,
    private readonly openBrowser: OpenBrowser,
  ) {}

  /**
   * @param deviceId The id of this app at the server if it is already registered (a new second factor): the server
   * keeps its device and only renews its token.
   * @param loginPath The server takes the packed parameters in the path too ([Session.appLoginPath]).
   */
  async login(opts: {
    serverUrl: string;
    deviceName: string;
    platform: string;
    deviceId?: number;
    loginPath?: boolean;
  }): Promise<Session> {
    const { serverUrl, deviceName, platform, deviceId, loginPath = false } = opts;
    const pkce = await createPkce(this.crypto);
    const loginParams: Record<string, string> = {
      challenge: pkce.challenge,
      state: pkce.state,
      device: deviceName,
      platform,
      ...(deviceId != null ? { deviceId: String(deviceId) } : {}),
    };
    const packed = packParams(loginParams);
    const url = new SafeURL(`${serverUrl}/next/appLogin${loginPath ? `/${packed}` : ''}`);
    url.searchParams.set('login', packed);
    for (const [key, value] of Object.entries(loginParams)) url.searchParams.set(key, value);
    appLog.info(
      'login',
      `Browser login${deviceId != null ? ` (renewal of device ${deviceId})` : ''}: ${LogSanitizer.url(url)}`,
    );
    const result = await this.openBrowser(url.href);
    if (result == null) {
      appLog.info('login', 'Browser login cancelled');
      throw new AppLoginCancelled();
    }
    const callback = parseUrl(result);
    appLog.info('login', `Callback: ${LogSanitizer.url(result)}`);
    const params = callback?.searchParams;
    if (params?.get('state') !== pkce.state) {
      appLog.warn('login', 'Callback with another state');
      throw new ApiException('Invalid state of the login callback.');
    }
    if (params.get('error') != null) {
      appLog.info('login', `Callback with error: ${params.get('error')}`);
      throw new AppLoginCancelled();
    }
    const code = params.get('code');
    if (code == null) {
      appLog.warn('login', 'Callback without code');
      throw new ApiException('No code in the login callback.');
    }
    const response = await this.request('Token', `${serverUrl}/rsPublic/appAuth/token`, {
      method: 'POST',
      headers: { 'Content-Type': 'application/json', Accept: 'application/json' },
      body: JSON.stringify({ code, codeVerifier: pkce.verifier } satisfies TokenRequest),
    });
    const json = (await response.json()) as TokenResponse;
    const proxyCookies = proxyCookiesFromJson(json.proxyCookies);
    appLog.info(
      'login',
      `Logged in: user ${json.userId}, device ${json.deviceId}, proxy cookies: ${LogSanitizer.cookieNames(Object.keys(proxyCookies))}`,
    );
    return {
      serverUrl,
      token: json.token,
      userId: json.userId,
      deviceId: json.deviceId,
      username: json.username ?? undefined,
      fullname: json.fullname ?? undefined,
      proxyCookies,
      appLoginPath: json.appLoginPath === true,
    };
  }

  /** The public system status of the server (`/rsPublic/systemStatus`). */
  async systemStatus(serverUrl: string): Promise<SystemData> {
    const response = await this.request('System status', `${serverUrl}/rsPublic/systemStatus`, {
      headers: { Accept: 'application/json' },
    });
    return (await response.json()) as SystemData;
  }

  /**
   * A public request of the login, logged with its answer or failure. A GET follows redirects within the server (a
   * canonical path, a load balancer), at most [MAX_REDIRECTS]; a redirect to another host is refused, as every
   * request of the app (HTTP_DEFAULTS).
   */
  private async request(what: string, url: string, init: RequestInit): Promise<Response> {
    const origin = parseUrl(url)?.origin;
    const redirects: string[] = [];
    let current = url;
    for (;;) {
      let response: Response;
      try {
        response = await this.http(current, { ...HTTP_DEFAULTS, ...init });
      } catch (e) {
        appLog.warn('login', `${what} failed: ${String(e)} (url: ${LogSanitizer.url(current)})`);
        throw e;
      }
      const location = response.headers.get('location');
      const next = location != null ? parseUrl(location, current) : null;
      const follow =
        isRedirect(response) &&
        (init.method ?? 'GET') === 'GET' &&
        next != null &&
        next.origin === origin &&
        redirects.length < MAX_REDIRECTS;
      if (follow) {
        redirects.push(`${response.status} ${LogSanitizer.url(next)}`);
        current = next.href;
        continue;
      }
      const after = redirects.length ? ` after redirects: ${redirects.join(' → ')}` : '';
      if (response.ok) {
        appLog.debug('login', `${what} → ${response.status}${after}`);
        return response;
      }
      appLog.warn(
        'login',
        `${what} → ${response.status}${after} (content-type: ${response.headers.get('content-type') ?? '-'}, ` +
          `url: ${LogSanitizer.url(current)}${location != null ? `, location: ${LogSanitizer.url(location)}` : ''})`,
      );
      throw await ApiException.fromResponse(response);
    }
  }
}

const MAX_REDIRECTS = 5;

/**
 * [params] packed into one query parameter (base64url of a json object, without padding), read by the `appLogin`
 * page of next (readAppLoginParams) before the single ones. It goes first: an SSO proxy redirecting the browser to
 * its login and back may keep only the first parameter (an unencoded `rd=…?a=1&b=2`).
 */
export function packParams(params: Record<string, string>): string {
  const json: Record<string, string | number> = { ...params };
  if (params.deviceId != null) json.deviceId = parseInt(params.deviceId, 10);
  return bufferToBase64url(utf8(JSON.stringify(json)));
}

/** `https://pf.example.com/next/…` or `pf.example.com` → `https://pf.example.com`; null if it is no url. */
export function normalizeServerUrl(input: string): string | null {
  let value = input.trim();
  if (!value) return null;
  if (!value.includes('://')) value = `https://${value}`;
  const url = parseUrl(value);
  if (url == null || !url.hostname || (url.protocol !== 'https:' && url.protocol !== 'http:')) return null;
  let path = url.pathname;
  for (const suffix of ['/next', '/react', '/wa']) {
    const index = path.indexOf(suffix);
    if (index >= 0) path = path.substring(0, index);
  }
  path = path.replace(/\/+$/, '');
  return `${url.protocol}//${url.host}${path}`;
}

/** Plain http is only for the development servers (debug builds): code, verifier and token would travel in clear. */
export function isInsecure(serverUrl: string, allowHttp: boolean = __DEV__): boolean {
  return !allowHttp && parseUrl(serverUrl)?.protocol === 'http:';
}
