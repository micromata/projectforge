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

import { cookieHeaderOf } from '../auth/session';
import { appLog, LogSanitizer } from '../log/appLog';
import { ApiException, isProxyRefusal, TwoFactorRequired } from './apiException';
import { HTTP_DEFAULTS, HttpFetch } from './http';

export interface RequestOptions {
  body?: unknown;
  /** False: a refusal doesn't start a new login in the browser (polling in the background, the logout). */
  renewLogin?: boolean;
}

/**
 * The calls of the app to its ProjectForge server (`/rs`, authenticated by the app token), port of the Flutter
 * earlier Flutter app's api client:
 *
 * - Every request carries `Authorization: PFApp <token>`: no session, no cookies (no cookie jar at all, see
 *   [HTTP_DEFAULTS]), hence no CSRF token. Redirects aren't followed: the header mustn't go elsewhere.
 * - A 401 of the server (json body) means the token is gone: [onUnauthorized].
 * - A 403 with `twoFactorRequired`: the app logs in again in the browser ([onReauthenticate]) and repeats once.
 * - An SSO proxy in front of the server gets its cookies ([proxyCookies]). A refusal of the proxy (a 401 without
 *   json, a redirect) means they expired: new browser login, which brings fresh ones, and one repetition.
 */
export class ApiClient {
  /**
   * The proxy cookies a new login in the browser brought without satisfying the proxy: while the app has just these,
   * the proxy's refusals aren't answered by a new browser login again.
   */
  private uselessProxyCookies: string | null = null;

  constructor(
    readonly serverUrl: string,
    private readonly opts: {
      http: HttpFetch;
      appVersion: string;
      token: () => string | null;
      proxyCookies: () => Record<string, string>;
      onUnauthorized: () => void;
      onReauthenticate: () => Promise<boolean>;
    },
  ) {}

  get<T>(path: string, options?: RequestOptions): Promise<T> {
    return this.call<T>('GET', path, options);
  }

  post<T>(path: string, options?: RequestOptions): Promise<T> {
    return this.call<T>('POST', path, options);
  }

  put<T>(path: string, options?: RequestOptions): Promise<T> {
    return this.call<T>('PUT', path, options);
  }

  delete<T>(path: string, options?: RequestOptions): Promise<T> {
    return this.call<T>('DELETE', path, options);
  }

  /** The bytes of a file (a vCard, a Wallet pass), e.g. `download('/rs/myBusinessCards/vcf', { id })`. */
  async download(path: string, query: Record<string, string> = {}, options: RequestOptions = {}): Promise<Uint8Array> {
    const params = Object.entries(query)
      .map(([key, value]) => `${encodeURIComponent(key)}=${encodeURIComponent(value)}`)
      .join('&');
    const response = await this.send('GET', params ? `${path}?${params}` : path, options, false, '*/*');
    if (!response.ok) throw await ApiException.fromResponse(response);
    return new Uint8Array(await response.arrayBuffer());
  }

  /** As [post], with the whole response (its headers, e.g. `set-cookie`). */
  async postForResponse(path: string, options: RequestOptions = {}): Promise<Response> {
    const response = await this.send('POST', path, options, false);
    if (!response.ok) throw await ApiException.fromResponse(response);
    return response;
  }

  /**
   * Whether the SSO proxy refuses the pages right now: tells its login from any other site the WebView is
   * redirected to (e.g. an open redirect of the server). Asks for `/next/` (static, never redirected by the server)
   * with the proxy cookies only, no token and no session, without following a redirect.
   */
  async proxyRefuses(): Promise<boolean> {
    const cookie = cookieHeaderOf(this.opts.proxyCookies());
    try {
      const response = await this.opts.http(`${this.serverUrl}/next/`, {
        ...HTTP_DEFAULTS,
        headers: cookie ? { Cookie: cookie } : {},
      });
      const refused = isProxyRefusal(response);
      appLog.info(
        'api',
        `Probe of the SSO proxy: GET /next/ → ${response.status}${refused ? ' (refused)' : ''}`,
      );
      return refused;
    } catch (e) {
      appLog.warn('api', `Probe of the SSO proxy failed: ${String(e)}`);
      return false;
    }
  }

  private async call<T>(method: string, path: string, options: RequestOptions = {}): Promise<T> {
    const response = await this.send(method, path, options, false);
    if (!response.ok) throw await ApiException.fromResponse(response);
    const text = await response.text();
    return (text ? JSON.parse(text) : undefined) as T;
  }

  private async send(
    method: string,
    path: string,
    options: RequestOptions,
    retried: boolean,
    accept = 'application/json',
  ): Promise<Response> {
    const describe = `${method} ${LogSanitizer.url(this.serverUrl + path)}${retried ? ' (retry)' : ''}`;
    const start = Date.now();
    let response: Response;
    try {
      response = await this.fetch(method, path, options.body, accept);
    } catch (e) {
      appLog.warn('api', `${describe} failed after ${Date.now() - start} ms: ${String(e)}`);
      throw e;
    }
    const duration = `${Date.now() - start} ms`;
    if (response.ok) {
      if (options.renewLogin !== false) appLog.debug('api', `${describe} → ${response.status} (${duration})`);
      return response;
    }
    const proxyRefused = isProxyRefusal(response);
    const location = response.headers.get('location');
    const server = response.headers.get('server');
    const details = [
      `content-type: ${response.headers.get('content-type') ?? '-'}`,
      location != null ? `location: ${LogSanitizer.url(location)}` : null,
      server != null ? `server: ${server}` : null,
      `proxy cookies sent: ${LogSanitizer.cookieNames(Object.keys(this.opts.proxyCookies()))}`,
      proxyRefused ? 'refused by the SSO proxy' : null,
    ].filter(Boolean);
    appLog.warn('api', `${describe} → ${response.status} in ${duration} (${details.join(', ')})`);
    if (response.status === 401 && !proxyRefused) {
      appLog.warn('api', 'Token refused by the server: logged out');
      this.opts.onUnauthorized();
      return response;
    }
    const proxyRenewalUseless =
      this.uselessProxyCookies != null && this.uselessProxyCookies === this.proxyCookieKey();
    const twoFactor = response.status === 403 && (await isTwoFactorRequired(response.clone()));
    if (((proxyRefused && !proxyRenewalUseless) || twoFactor) && !retried && options.renewLogin !== false) {
      appLog.info(
        'api',
        proxyRefused
          ? 'Refused by the SSO proxy: new login in the browser'
          : 'Second factor required: new login in the browser',
      );
      if (await this.opts.onReauthenticate()) {
        const retry = await this.send(method, path, options, true, accept);
        if (proxyRefused && isProxyRefusal(retry)) {
          appLog.warn('api', 'Still refused by the SSO proxy after the new login: no further logins for this session');
          this.uselessProxyCookies = this.proxyCookieKey();
        }
        return retry;
      }
      appLog.info('api', 'New login cancelled or failed');
    } else if (proxyRefused && proxyRenewalUseless) {
      appLog.debug('api', "No new login: a login didn't satisfy the SSO proxy");
    }
    return response;
  }

  private fetch(method: string, path: string, body: unknown, accept: string): Promise<Response> {
    const token = this.opts.token();
    const cookie = cookieHeaderOf(this.opts.proxyCookies());
    const headers: Record<string, string> = {
      Accept: accept,
      'X-PF-Frontend': 'next',
      'X-PF-App-Version': this.opts.appVersion,
    };
    if (token) headers.Authorization = `PFApp ${token}`;
    if (cookie) headers.Cookie = cookie;
    if (body !== undefined) headers['Content-Type'] = 'application/json';
    return this.opts.http(this.serverUrl + path, {
      ...HTTP_DEFAULTS,
      method,
      headers,
      body: body === undefined ? undefined : JSON.stringify(body),
    });
  }

  private proxyCookieKey(): string {
    return JSON.stringify(Object.entries(this.opts.proxyCookies()).sort());
  }
}

async function isTwoFactorRequired(response: Response): Promise<boolean> {
  try {
    const data = (await response.json()) as Partial<TwoFactorRequired>;
    return data?.twoFactorRequired === true;
  } catch {
    return false;
  }
}
