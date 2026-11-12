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

import type {
  PushSettings,
  PushSettingsRequest,
  UIThemeRequest,
  UITimeGridRequest,
  UITimeGridSettings,
  UIThemeSettings,
  UserStatusResult,
} from '@projectforge/shared/rs/user-status';
import { useSyncExternalStore } from 'react';
import { AppState, AppStateStatus, Appearance } from 'react-native';

import { parseTimeGrid } from '@projectforge/shared/booking-time';

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

/** The user's first day of the week (Monday 1 … Sunday 7), Monday if the server sends none or an invalid one. */
export const parseFirstDayOfWeek = (value?: number | null): number =>
  value != null && Number.isInteger(value) && value >= 1 && value <= 7 ? value : 1;

const INTERVAL = 60_000;
const RELOAD_DELAY = 3_000;
const THEME_CACHE = 'theme';
const FIRST_DAY_CACHE = 'firstDayOfWeek';
const TIME_GRID_CACHE = 'timeGrid';

interface State {
  status: UserStatusResult | null;
  /** The user's theme of the web pages, applied to the app's own screens too. */
  theme: ThemeMode;
  /** The user's first day of the week (Monday 1 … Sunday 7), stored for a start without the server. */
  firstDayOfWeek: number;
  /** The grid of the times of time sheets in minutes, 5 for a server that sends none; stored like the first day. */
  timeGrid: number;
  /** Loaded by the settings ([loadPushSettings]); null before, offline and for an older server without them. */
  pushSettings: PushSettings | null;
}

const INITIAL: State = { status: null, theme: 'system', firstDayOfWeek: 1, timeGrid: 5, pushSettings: null };

/**
 * The user status (`GET /rs/userStatus`) for the badge of the chat tab, the bell of the notifications, the message of
 * the administrators, the compatibility warning and the theme: reloaded every minute while the app is in the
 * foreground, on resuming it and on [refresh] (e.g. leaving the chat tab, a push). The app token has no session, so
 * the polling keeps nothing alive; it never opens a login in the browser (`renewLogin: false`). Port of the Flutter
 * app's UserStatusController and ThemeController.
 */
export class UserStatusStore {
  private state: State = INITIAL;
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
    const firstDay = await JsonCache.read<{ firstDayOfWeek?: number }>(FIRST_DAY_CACHE);
    if (this.api === api && this.state.status == null && firstDay?.firstDayOfWeek != null) {
      this.setFirstDayOfWeek(parseFirstDayOfWeek(firstDay.firstDayOfWeek));
    }
    const grid = await JsonCache.read<{ timeGrid?: number }>(TIME_GRID_CACHE);
    if (this.api === api && this.state.status == null && grid?.timeGrid != null) {
      this.setTimeGrid(parseTimeGrid(grid.timeGrid));
    }
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
    this.state = INITIAL;
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
      // An older server sends no first day of the week: the stored one stays.
      const firstDay = status.userData?.isoFirstDayOfWeekValue;
      if (firstDay != null) this.setFirstDayOfWeek(parseFirstDayOfWeek(firstDay), true);
      // An older server sends no grid: the stored one stays.
      const grid = status.userData?.timesheetTimeGridMinutes;
      if (grid != null) this.setTimeGrid(parseTimeGrid(grid), true);
      this.emit();
    } catch (e) {
      appLog.debug('status', `User status not loaded: ${String(e)}`);
    }
  };

  /**
   * The user chose [theme] in the app: shown at once and stored for the user as the web pages store it (the same
   * setting, so it follows them on other devices). A failure is thrown, the theme shown before is back then.
   */
  chooseTheme = async (theme: ThemeMode) => {
    const api = this.api;
    if (api == null) return;
    const before = this.state.theme;
    this.setTheme(theme, true);
    try {
      const body: UIThemeRequest = { theme };
      const answer = await api.post<UIThemeSettings | undefined>('/rs/uiSettings/theme', { body });
      if (this.api === api && answer?.theme) this.setTheme(parseTheme(answer.theme), true);
    } catch (e) {
      if (this.api === api) this.setTheme(before, true);
      throw e;
    }
  };

  /**
   * The user chose [minutes] as the grid of the timer in the app: shown at once and stored for the user as "My account"
   * of the web pages stores it. A failure is thrown, the grid shown before is back then.
   */
  chooseTimeGrid = async (minutes: number) => {
    const api = this.api;
    if (api == null) return;
    const before = this.state.timeGrid;
    this.setTimeGrid(parseTimeGrid(minutes), true);
    try {
      const body: UITimeGridRequest = { minutes };
      const answer = await api.post<UITimeGridSettings | undefined>('/rs/uiSettings/timesheetTimeGrid', { body });
      if (this.api === api && answer?.minutes != null) this.setTimeGrid(parseTimeGrid(answer.minutes), true);
    } catch (e) {
      if (this.api === api) this.setTimeGrid(before, true);
      throw e;
    }
  };

  /** The settings of the pushes (`PushSettingsRest`), for the settings screen; an older server has none. */
  loadPushSettings = async () => {
    const api = this.api;
    if (api == null) return;
    try {
      const pushSettings = await api.get<PushSettings>('/rs/pushSettings', { renewLogin: false });
      if (this.api !== api) return;
      this.state = { ...this.state, pushSettings };
      this.emit();
    } catch (e) {
      appLog.debug('status', `Push settings not loaded: ${String(e)}`);
    }
  };

  /**
   * The user chose whether pushes show the content: shown at once and stored for the user (the same setting as in "My
   * account"). A failure is thrown, the setting shown before is back then.
   */
  choosePushContent = async (content: boolean) => {
    const api = this.api;
    const before = this.state.pushSettings;
    if (api == null || before == null) return;
    this.state = { ...this.state, pushSettings: { ...before, content } };
    this.emit();
    try {
      const body: PushSettingsRequest = { content };
      const answer = await api.post<PushSettings | undefined>('/rs/pushSettings', { body });
      if (this.api === api && answer != null) {
        this.state = { ...this.state, pushSettings: answer };
        this.emit();
      }
    } catch (e) {
      if (this.api === api) {
        this.state = { ...this.state, pushSettings: before };
        this.emit();
      }
      throw e;
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

  private setFirstDayOfWeek(firstDayOfWeek: number, store = false) {
    if (store) JsonCache.write(FIRST_DAY_CACHE, { firstDayOfWeek });
    if (firstDayOfWeek === this.state.firstDayOfWeek) return;
    this.state = { ...this.state, firstDayOfWeek };
    this.emit();
  }

  private setTimeGrid(timeGrid: number, store = false) {
    if (store) JsonCache.write(TIME_GRID_CACHE, { timeGrid });
    if (timeGrid === this.state.timeGrid) return;
    this.state = { ...this.state, timeGrid };
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

/** The user's first day of the week (Monday 1 … Sunday 7). */
export function useFirstDayOfWeek(): number {
  return useSyncExternalStore(userStatus.subscribe, () => userStatus.get().firstDayOfWeek);
}

/** The grid of the times of time sheets in minutes (timer, pickers, presets), configured on the server. */
export function useTimeGrid(): number {
  return useSyncExternalStore(userStatus.subscribe, () => userStatus.get().timeGrid);
}
