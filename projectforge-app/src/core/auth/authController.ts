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

import { useSyncExternalStore } from 'react';

import PfNative from '../../../modules/pf-native/src/PfNativeModule';
import { WebSessionRepository } from '../../features/web/webSessionRepository';
import { ApiClient } from '../api/apiClient';
import { HttpFetch } from '../api/http';
import { JsonCache } from '../cache/jsonCache';
import { compatibilityIssue } from '../compat/compatibility';
import { SharedFiles } from '../files/sharedFiles';
import { appLog, LogSanitizer } from '../log/appLog';
import { appVersion, deviceName, platformCrypto, platformHttp, platformName, platformOpenBrowser } from '../platform';
import { userStatus } from '../status/userStatus';
import { WebCookies, WebCookieStore } from '../web/webCookies';
import { AppLogin, AppLoginCancelled, isInsecure, normalizeServerUrl } from './appLogin';
import { sessionStore } from './secureStorage';
import { Session } from './session';
import type { SessionStore } from './sessionStore';

export type AuthStatus = 'restoring' | 'loggedOut' | 'loggedIn';

export interface AuthState {
  status: AuthStatus;
  session: Session | null;
  /** Counts the logins: the tabs (WebViews) start anew for another one (another server, another user). */
  generation: number;
  /** Why the app was logged out (an i18n key), shown on the login screen. */
  messageKey?: string;
}

/** The user typed something that is no server url. */
export class InvalidServerUrl extends Error {}

/** A server url with plain http in a release build: the token would travel in clear. */
export class InsecureServerUrl extends Error {}

/**
 * A client for the login [session], bound to it: once the app is switched to another server, a client of the old one
 * still in use (a request on its way, a retry) must not send the token or the SSO proxy cookies of the new login to
 * its server, and its refusal or renewal concerns the old login, not the new one.
 */
export function boundApiClient(
  session: Session,
  opts: {
    current: () => Session | null;
    onUnauthorized: () => void;
    onReauthenticate: () => Promise<boolean>;
    http?: typeof platformHttp;
  },
): ApiClient {
  const own = () => {
    const now = opts.current();
    return now?.serverUrl === session.serverUrl && now.deviceId === session.deviceId ? now : null;
  };
  return new ApiClient(session.serverUrl, {
    http: opts.http ?? platformHttp,
    appVersion,
    token: () => own()?.token ?? null,
    proxyCookies: () => own()?.proxyCookies ?? {},
    onUnauthorized: () => {
      if (own() != null) opts.onUnauthorized();
    },
    onReauthenticate: async () => own() != null && (await opts.onReauthenticate()),
  });
}

const loginKey = (session: Session) => `${session.serverUrl}#${session.userId}`;

/** What the controller works with on the device, replaced in the tests. */
export interface AuthDeps {
  http: HttpFetch;
  appLogin: AppLogin;
  sessionStore: SessionStore;
  cookieStore: WebCookieStore;
  /** The JsonCache and the SharedFiles: the user's data on the device. */
  cache: { clearAll(): void };
  files: { clearAll(): void };
  userStatus: { start(api: ApiClient): Promise<void>; stop(): void };
}

const platformDeps = (): AuthDeps => ({
  http: platformHttp,
  appLogin: new AppLogin(platformHttp, platformCrypto, platformOpenBrowser),
  sessionStore,
  cookieStore: PfNative,
  cache: JsonCache,
  files: SharedFiles,
  userStatus,
});

/**
 * Login, logout, the switch between the logins at several servers and the renewal of the login (a second factor too
 * old, a refusal of the SSO proxy) of the app. Port of the Flutter app's AuthController.
 */
export class AuthController {
  private state: AuthState = { status: 'restoring', session: null, generation: 0 };
  private listeners = new Set<() => void>();
  private reauthentication: Promise<boolean> | null = null;
  private readonly appLogin: AppLogin;

  readonly webCookies: WebCookies;

  constructor(private readonly deps: AuthDeps = platformDeps()) {
    this.appLogin = deps.appLogin;
    this.webCookies = new WebCookies(deps.cookieStore);
  }
  /** The client of the active login, null while logged out (rebuilt for another login, not for a renewed token). */
  api: ApiClient | null = null;
  webSessions: WebSessionRepository | null = null;

  getState = () => this.state;

  subscribe = (listener: () => void) => {
    this.listeners.add(listener);
    return () => {
      this.listeners.delete(listener);
    };
  };

  async restore() {
    try {
      await this.deps.sessionStore.migrate();
      const session = await this.deps.sessionStore.load();
      if (session) LogSanitizer.addSecretNames(Object.keys(session.proxyCookies));
      appLog.info(
        'auth',
        session == null
          ? 'No stored login'
          : `Stored login: user ${session.userId}, device ${session.deviceId}, proxy cookies: ${Object.keys(session.proxyCookies).length}`,
      );
      this.activate(session, true);
    } catch (e) {
      appLog.error('auth', 'Stored login not readable', e);
      this.activate(null, true);
    }
  }

  /** The servers of the last logins, most recent first (offered by the login). */
  servers = () => this.deps.sessionStore.servers();

  /** The stored logins, the active one among them, most recent first. */
  sessions = () => this.deps.sessionStore.sessions();

  /**
   * Logs in at [serverInput] (as typed by the user) in the browser.
   *
   * Throws InvalidServerUrl, InsecureServerUrl, AppLoginCancelled if the user cancelled, ApiException if the server
   * isn't a reachable ProjectForge or refused the code.
   */
  async login(serverInput: string) {
    const serverUrl = normalizeServerUrl(serverInput);
    if (serverUrl == null) throw new InvalidServerUrl();
    if (isInsecure(serverUrl)) throw new InsecureServerUrl();
    let session: Session;
    try {
      const status = await this.appLogin.systemStatus(serverUrl);
      appLog.info('auth', `Login at ${status.appname ?? '?'} ${status.version ?? ''} (app ${appVersion}, ${platformName})`);
      // Only the version the server needs is public, its own is checked after the login (the compatibility warning).
      const issue = compatibilityIssue({ appVersion, minAppVersion: status.minAppVersion });
      if (issue) appLog.warn('auth', `The server needs app version ${issue.minVersion}`);
      session = await this.appLogin.login({
        serverUrl,
        deviceName: deviceName(),
        platform: platformName,
        loginPath: status.appLoginPath === true,
      });
    } catch (e) {
      if (!(e instanceof AppLoginCancelled)) appLog.error('auth', 'Login failed', e);
      throw e;
    }
    // Another server added while logged in: the current login stays stored.
    await this.leave();
    const expired = this.expiredLogin;
    this.expiredLogin = null;
    if (expired != null && expired !== loginKey(session)) {
      this.clearLog('Logged in as another user: log of the expired login cleared');
    }
    LogSanitizer.addSecretNames(Object.keys(session.proxyCookies));
    await this.deps.sessionStore.save(session);
    this.activate(session, true);
  }

  /** Makes the stored login [target] (of another server) the active one, without the browser. */
  async switchTo(target: Session) {
    if (target.serverUrl === this.state.session?.serverUrl) return;
    appLog.info('auth', 'Switch to another server');
    await this.leave();
    LogSanitizer.addSecretNames(Object.keys(target.proxyCookies));
    await this.deps.sessionStore.save(target);
    this.activate(target, true);
  }

  /** Removes the server from the list; a stored login there (not the active one) is logged out at that server too. */
  async removeServer(serverUrl: string) {
    const stored = (await this.sessions()).find((it) => it.serverUrl === serverUrl);
    if (stored && stored.serverUrl !== this.state.session?.serverUrl) {
      const client = boundApiClient(stored, {
        http: this.deps.http,
        current: () => stored,
        onUnauthorized: () => undefined,
        onReauthenticate: async () => false,
      });
      await withTimeout(client.post('/rs/appAuth/logout', { renewLogin: false }));
    }
    await this.deps.sessionStore.removeServer(serverUrl);
    this.emit();
  }

  /** A new login in the browser (second factor too old, SSO proxy refused); false if cancelled or failed. */
  reauthenticate = (): Promise<boolean> =>
    (this.reauthentication ??= this.doReauthenticate().finally(() => (this.reauthentication = null)));

  private async doReauthenticate(): Promise<boolean> {
    const session = this.state.session;
    if (session == null) return false;
    appLog.info('auth', 'Renewal of the login');
    try {
      const loginPath =
        session.appLoginPath ||
        (await this.appLogin
          .systemStatus(session.serverUrl)
          .then((it) => it.appLoginPath === true)
          .catch(() => false));
      const renewed = await this.appLogin.login({
        serverUrl: session.serverUrl,
        deviceName: deviceName(),
        platform: platformName,
        deviceId: session.deviceId,
        loginPath,
      });
      LogSanitizer.addSecretNames(Object.keys(renewed.proxyCookies));
      if (renewed.userId !== session.userId || renewed.deviceId !== session.deviceId) {
        appLog.warn(
          'auth',
          `Renewal logged in user ${renewed.userId}/device ${renewed.deviceId} instead of ${session.userId}/${session.deviceId}: switched`,
        );
        await this.switchUser(renewed);
        return false;
      }
      await this.deps.sessionStore.save(renewed);
      // The same client and web session (they read the session per request), but the WebView needs the fresh cookies
      // of an SSO proxy.
      this.state = { ...this.state, session: renewed };
      this.emit();
      await this.webCookies.setProxyCookies(renewed.serverUrl, renewed.proxyCookies);
      appLog.info('auth', 'Login renewed');
      return true;
    } catch (e) {
      if (e instanceof AppLoginCancelled) appLog.info('auth', 'Renewal cancelled');
      else appLog.error('auth', 'Renewal failed', e);
      return false;
    }
  }

  /**
   * The browser was logged in as another user, for whom the server registered this app anew: the old user is logged
   * out (at the server too, best effort), and nothing of them stays (cache, cookies, tabs, log).
   */
  private async switchUser(renewed: Session) {
    await withTimeout(this.api?.post('/rs/appAuth/logout', { renewLogin: false }));
    await this.clearLocalData();
    this.clearLog('Logged in as another user: log of the previous user cleared');
    await this.deps.sessionStore.save(renewed);
    this.activate(renewed, true);
  }

  /**
   * Logs the app out at the server (its token is deleted) and here. The server gets a few seconds and whatever fails:
   * the app is logged out here in any case.
   */
  async logout() {
    appLog.info('auth', 'Logout');
    await withTimeout(this.api?.post('/rs/appAuth/pushToken/remove', { renewLogin: false }));
    await withTimeout(this.api?.post('/rs/appAuth/logout', { renewLogin: false }));
    await this.forget();
    this.clearLog('Logged out: log of the login cleared');
  }

  /**
   * The login whose expiry left its log ([onUnauthorized]): it tells why, and the same user logs in again; cleared once
   * somebody else logs in.
   */
  private expiredLogin: string | null = null;

  /** The server refused the token (logged out in the web, expired, password changed). */
  private onUnauthorized = () => {
    const session = this.state.session;
    if (this.state.status !== 'loggedIn' || session == null) return;
    this.expiredLogin = loginKey(session);
    void this.forget('app.session.expired');
  };

  /**
   * Before another login becomes the active one; the current one stays stored. Its server stops pushing (only the
   * server shown pushes), and its cache and shared files leave the device (not kept per server). The WebView keeps its
   * data, which is per server anyway.
   */
  private async leave() {
    if (this.state.status !== 'loggedIn') return;
    await withTimeout(this.api?.post('/rs/appAuth/pushToken/remove', { renewLogin: false }));
    this.deps.cache.clearAll();
    this.deps.files.clearAll();
  }

  private async forget(messageKey?: string) {
    await this.deps.sessionStore.clear();
    await this.clearLocalData();
    this.activate(null, true, messageKey);
  }

  /** The user's data on the device: the cache, the shared files and the web session and storage of the WebView. */
  private async clearLocalData() {
    this.deps.cache.clearAll();
    this.deps.files.clearAll();
    await this.webCookies.clear().catch((e) => appLog.warn('auth', `WebView data not cleared: ${String(e)}`));
  }

  private clearLog(reason: string) {
    appLog.clear();
    appLog.info('auth', reason);
  }

  /** [session] becomes the active login (null: logged out) with a new client, web session and status. */
  private activate(session: Session | null, newLogin: boolean, messageKey?: string) {
    if (session == null) {
      this.api = null;
      this.webSessions = null;
      this.deps.userStatus.stop();
    } else if (newLogin || this.api == null) {
      const api = boundApiClient(session, {
        http: this.deps.http,
        current: () => this.state.session,
        onUnauthorized: this.onUnauthorized,
        onReauthenticate: this.reauthenticate,
      });
      this.api = api;
      this.webSessions = new WebSessionRepository(api, this.webCookies, () => this.ownProxyCookies(session));
    }
    this.state = {
      status: session == null ? 'loggedOut' : 'loggedIn',
      session,
      generation: this.state.generation + (newLogin ? 1 : 0),
      messageKey,
    };
    this.emit();
    if (session != null && this.api != null && newLogin) void this.deps.userStatus.start(this.api);
  }

  /** The proxy cookies of the current login, if it is still the one of [session]. */
  private ownProxyCookies(session: Session): Record<string, string> {
    const now = this.state.session;
    return now?.serverUrl === session.serverUrl && now.deviceId === session.deviceId ? now.proxyCookies : {};
  }

  private emit() {
    this.listeners.forEach((it) => it());
  }
}

/** A call to the server that may fail or hang (logout, push token): a few seconds at most, errors ignored. */
async function withTimeout(call: Promise<unknown> | undefined, millis = 3000): Promise<void> {
  if (call == null) return;
  let timer: ReturnType<typeof setTimeout> | undefined;
  await Promise.race([call.catch(() => undefined), new Promise((resolve) => (timer = setTimeout(resolve, millis)))]);
  clearTimeout(timer);
}

export const authController = new AuthController();

export function useAuth(): AuthState {
  return useSyncExternalStore(authController.subscribe, authController.getState);
}
