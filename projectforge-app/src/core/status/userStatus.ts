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

import type { UIThemeSettings, UserStatusResult } from '@projectforge/shared/rs/user-status';
import { useSyncExternalStore } from 'react';
import { AppState, AppStateStatus, Appearance } from 'react-native';

import { ApiClient } from '../api/apiClient';
import { JsonCache } from '../cache/jsonCache';
import { appLog } from '../log/appLog';

/** The feature id of the chat (`MenuItemDefId.CHAT`) in `disabledFeatures`. */
export const CHAT_FEATURE_ID = 'CHAT';

export const chatEnabled = (status: UserStatusResult | null) =>
  !(status?.disabledFeatures ?? []).includes(CHAT_FEATURE_ID);

export const alertMessage = (status: UserStatusResult | null) => status?.alertMessage?.trim() || null;

export type ThemeMode = 'light' | 'dark' | 'system';

export const parseTheme = (theme?: string | null): ThemeMode =>
  theme === 'light' || theme === 'dark' ? theme : 'system';

const INTERVAL = 60_000;
const RELOAD_DELAY = 3_000;
const THEME_CACHE = 'theme';

interface State {
  status: UserStatusResult | null;
  /** The user's theme of the web pages, applied to the app's own screens too. */
  theme: ThemeMode;
}

/**
 * The user status (`GET /rs/userStatus`) for the badge of the chat tab, the bell of the notifications, the message of
 * the administrators, the compatibility warning and the theme: reloaded every minute while the app is in the
 * foreground, on resuming it and on [refresh] (e.g. leaving the chat tab, a push). The app token has no session, so
 * the polling keeps nothing alive; it never opens a login in the browser (`renewLogin: false`). Port of the Flutter
 * app's UserStatusController and ThemeController.
 */
export class UserStatusStore {
  private state: State = { status: null, theme: 'system' };
  private listeners = new Set<() => void>();
  private api: ApiClient | null = null;
  private timer: ReturnType<typeof setInterval> | null = null;
  private reloadTimer: ReturnType<typeof setTimeout> | null = null;
  private appState: { remove(): void } | null = null;

  get = () => this.state;

  subscribe = (listener: () => void) => {
    this.listeners.add(listener);
    return () => {
      this.listeners.delete(listener);
    };
  };

  /** For the login of [api]: the stored theme first (a start without the server), then the server's. */
  async start(api: ApiClient) {
    this.stop();
    this.api = api;
    const stored = await JsonCache.read<{ theme?: string }>(THEME_CACHE);
    if (this.api === api && this.state.status == null && stored?.theme) this.setTheme(parseTheme(stored.theme));
    this.appState = AppState.addEventListener('change', this.onAppState);
    this.startTimer();
    await this.refresh();
  }

  /** Logged out or another login: nothing of the last one stays, the theme is the system's. */
  stop() {
    this.stopTimer();
    if (this.reloadTimer) clearTimeout(this.reloadTimer);
    this.reloadTimer = null;
    this.appState?.remove();
    this.appState = null;
    this.api = null;
    this.state = { status: null, theme: 'system' };
    this.applyTheme();
    this.emit();
  }

  /** Loads the status again; offline (or on an error) the last one stays. */
  refresh = async () => {
    const api = this.api;
    if (api == null) return;
    try {
      const status = await api.get<UserStatusResult>('/rs/userStatus', { renewLogin: false });
      if (this.api !== api) return;
      this.state = { ...this.state, status };
      if (status.theme != null) {
        this.setTheme(parseTheme(status.theme), true);
      } else {
        void this.loadThemeOfOlderServer(api);
      }
      this.emit();
    } catch (e) {
      appLog.debug('status', `User status not loaded: ${String(e)}`);
    }
  };

  /**
   * A push arrived while the app is open: the bell takes its count at once (the server's open count, [badge]), then
   * the status is loaded ([changed]).
   */
  pushed = (badge: number | null) => {
    const status = this.state.status;
    if (badge != null && status?.notifications != null && status.notifications.openCount !== badge) {
      this.state = {
        ...this.state,
        status: { ...status, notifications: { ...status.notifications, openCount: badge } },
      };
      this.emit();
    }
    this.changed();
  };

  /**
   * The status changed on the server (a push, a web page read a chat or closed a notification): loaded now and again a
   * moment later. A request answered just before a commit, or before the server resolved the notifications of a read
   * chat (on its own thread), would keep the old count until the next poll.
   */
  changed = () => {
    void this.refresh();
    if (this.reloadTimer) clearTimeout(this.reloadTimer);
    this.reloadTimer = setTimeout(() => {
      this.reloadTimer = null;
      void this.refresh();
    }, RELOAD_DELAY);
  };

  /** A server whose user status has no theme yet: the endpoint of the web pages (`UISettingsRest`). */
  private async loadThemeOfOlderServer(api: ApiClient) {
    try {
      const settings = await api.get<UIThemeSettings>('/rs/uiSettings/theme', { renewLogin: false });
      if (this.api === api && settings?.theme) this.setTheme(parseTheme(settings.theme), true);
    } catch {
      // Offline or refused: the stored theme stays.
    }
  }

  private setTheme(theme: ThemeMode, store = false) {
    if (store) JsonCache.write(THEME_CACHE, { theme });
    if (theme === this.state.theme) return;
    this.state = { ...this.state, theme };
    this.applyTheme();
    this.emit();
  }

  /** The app's own screens (useColorScheme) follow the theme of the web pages. */
  private applyTheme() {
    Appearance.setColorScheme(this.state.theme === 'system' ? 'unspecified' : this.state.theme);
  }

  private onAppState = (next: AppStateStatus) => {
    if (next === 'active') {
      void this.refresh();
      this.startTimer();
    } else {
      this.stopTimer();
    }
  };

  private startTimer() {
    this.stopTimer();
    this.timer = setInterval(() => void this.refresh(), INTERVAL);
  }

  private stopTimer() {
    if (this.timer) clearInterval(this.timer);
    this.timer = null;
  }

  private emit() {
    this.listeners.forEach((it) => it());
  }
}

export const userStatus = new UserStatusStore();

export function useUserStatus(): State {
  return useSyncExternalStore(userStatus.subscribe, userStatus.get);
}
