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

import { ApiClient } from '../api/apiClient';
import { appLog } from '../log/appLog';
import { appVersion, deviceName, platformCrypto, platformHttp, platformName, platformOpenBrowser } from '../platform';
import { WebCookies } from '../web/webCookies';
import PfWebSupport from '../../../modules/pf-web-support/src/PfWebSupportModule';
import { WebSessionRepository } from '../../features/web/webSessionRepository';
import { AppLogin, AppLoginCancelled, isInsecure, normalizeServerUrl } from './appLogin';
import { Session } from './session';
import { SessionStore } from './sessionStore';

export type AuthStatus = 'loading' | 'loggedOut' | 'loggedIn';

export interface AuthState {
  status: AuthStatus;
  session: Session | null;
  /** Counts the logins: the tabs (WebViews) start anew for another one (another user). */
  generation: number;
  message?: string;
}

/**
 * The login of the app (the Flutter app's AuthController, reduced to one server): restores the stored session,
 * logs in in the browser, renews the login for a second factor too old or a refusal of the SSO proxy (concurrent
 * requests share one renewal), logs out.
 */
class AuthController {
  private state: AuthState = { status: 'loading', session: null, generation: 0 };
  private listeners = new Set<() => void>();
  private reauthentication: Promise<boolean> | null = null;
  private appLogin = new AppLogin(platformHttp, platformCrypto, platformOpenBrowser);

  readonly webCookies = new WebCookies(PfWebSupport);
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
      const session = await SessionStore.load();
      this.setSession(session, session ? 'loggedIn' : 'loggedOut');
    } catch (e) {
      appLog.error('auth', 'Stored session not readable', e);
      this.setSession(null, 'loggedOut');
    }
  }

  /** Logs in at [serverInput] (as typed by the user) in the browser. */
  async login(serverInput: string) {
    const serverUrl = normalizeServerUrl(serverInput);
    if (serverUrl == null) throw new Error('Not a server url');
    if (isInsecure(serverUrl)) throw new Error('Plain http only for development builds');
    const status = await this.appLogin.systemStatus(serverUrl);
    appLog.info('auth', `Login at ${status.appname ?? '?'} ${status.version ?? ''} (app ${appVersion}, ${platformName})`);
    const session = await this.appLogin.login({
      serverUrl,
      deviceName: deviceName(),
      platform: platformName,
      loginPath: status.appLoginPath === true,
    });
    await SessionStore.save(session);
    this.setSession(session, 'loggedIn', true);
  }

  /** A new login in the browser (second factor too old, SSO proxy refused); false if cancelled or failed. */
  reauthenticate = (): Promise<boolean> =>
    (this.reauthentication ??= this.doReauthenticate().finally(() => (this.reauthentication = null)));

  private async doReauthenticate(): Promise<boolean> {
    const session = this.state.session;
    if (session == null) return false;
    appLog.info('auth', 'Renewal of the login');
    try {
      let loginPath = session.appLoginPath;
      if (!loginPath) {
        loginPath = await this.appLogin
          .systemStatus(session.serverUrl)
          .then((it) => it.appLoginPath === true)
          .catch(() => false);
      }
      const renewed = await this.appLogin.login({
        serverUrl: session.serverUrl,
        deviceName: deviceName(),
        platform: platformName,
        deviceId: session.deviceId,
        loginPath,
      });
      if (renewed.userId !== session.userId || renewed.deviceId !== session.deviceId) {
        appLog.warn(
          'auth',
          `Renewal logged in user ${renewed.userId}/device ${renewed.deviceId} instead of ${session.userId}/${session.deviceId}: switched`,
        );
        await this.api?.post('/rs/appAuth/logout', { renewLogin: false }).catch(() => undefined);
        await this.webCookies.clear();
        await SessionStore.save(renewed);
        this.setSession(renewed, 'loggedIn', true);
        return false;
      }
      await SessionStore.save(renewed);
      // Same api client and web session (they read the session), but the WebView needs the fresh proxy cookies.
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

  async logout() {
    appLog.info('auth', 'Logout');
    await Promise.race([
      this.api?.post('/rs/appAuth/logout', { renewLogin: false }).catch(() => undefined),
      new Promise((resolve) => setTimeout(resolve, 3000)),
    ]);
    await this.forget();
  }

  private onUnauthorized = () => {
    if (this.state.status !== 'loggedIn') return;
    void this.forget('The login of the app has expired.');
  };

  private async forget(message?: string) {
    await SessionStore.clear();
    await this.webCookies.clear();
    this.setSession(null, 'loggedOut', false, message);
  }

  private setSession(session: Session | null, status: AuthStatus, newLogin = false, message?: string) {
    if (session == null) {
      this.api = null;
      this.webSessions = null;
    } else if (newLogin || this.api == null || this.api.serverUrl !== session.serverUrl) {
      const api = new ApiClient(session.serverUrl, {
        http: platformHttp,
        appVersion,
        token: () => this.state.session?.token ?? null,
        proxyCookies: () => this.state.session?.proxyCookies ?? {},
        onUnauthorized: this.onUnauthorized,
        onReauthenticate: this.reauthenticate,
      });
      this.api = api;
      this.webSessions = new WebSessionRepository(api, this.webCookies, () => this.state.session?.proxyCookies ?? {});
    }
    this.state = {
      status,
      session,
      generation: this.state.generation + (newLogin ? 1 : 0),
      message,
    };
    this.emit();
  }

  private emit() {
    this.listeners.forEach((it) => it());
  }
}

export const authController = new AuthController();

export function useAuth(): AuthState {
  return useSyncExternalStore(authController.subscribe, authController.getState);
}
