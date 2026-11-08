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

import { Directory, File, Paths } from 'expo-file-system';
import { useSyncExternalStore } from 'react';

import { NATIVE_PAGES, NativePage } from '../web/nativePage';

/** The settings of the app on this device (not of the user: they stay after a logout). */
export interface AppSettings {
  /**
   * The pages the web pages open in their own version, although the app has a tab for them (e.g. if the app's
   * version doesn't work with the server).
   */
  webVersion: NativePage[];
  /** The side of the navigation in landscape: left (default) or right, e.g. for the thumb of the right hand. */
  railSide: RailSide;
  /** The tab the app starts with: the one used last on this device. */
  lastTab: AppTab;
}

export type RailSide = 'left' | 'right';

/** The tabs of the logged-in app (the routes of `src/app/(tabs)/`). */
export const APP_TABS = ['cards', 'addresses', 'timesheets', 'pages', 'chat'] as const;
export type AppTab = (typeof APP_TABS)[number];

const DEFAULTS: AppSettings = { webVersion: [], railSide: 'left', lastTab: 'cards' };

/** True if a link of the web pages to [page] switches to the tab of the app. */
export const opensInApp = (settings: AppSettings, page: NativePage) => !settings.webVersion.includes(page);

/** The settings, parsed defensively: broken or of an older version, the defaults. */
export function settingsFromJson(json: unknown): AppSettings {
  const {
    webVersion: pages,
    railSide,
    lastTab,
  } = (json as { webVersion?: unknown; railSide?: unknown; lastTab?: unknown } | null) ?? {};
  return {
    webVersion: Array.isArray(pages) ? pages.filter((it): it is NativePage => NATIVE_PAGES.includes(it)) : [],
    railSide: railSide === 'right' ? 'right' : 'left',
    lastTab: APP_TABS.find((it) => it === lastTab) ?? 'cards',
  };
}

/** Keeps [AppSettings] in the app's documents, outside the cache deleted by a logout. */
class AppSettingsStore {
  private settings: AppSettings = DEFAULTS;
  private listeners = new Set<() => void>();

  load() {
    try {
      const file = this.file();
      if (file.exists) this.settings = settingsFromJson(JSON.parse(file.textSync()));
    } catch {
      this.settings = DEFAULTS;
    }
    this.emit();
  }

  get = () => this.settings;

  subscribe = (listener: () => void) => {
    this.listeners.add(listener);
    return () => {
      this.listeners.delete(listener);
    };
  };

  setOpensInApp(page: NativePage, inApp: boolean) {
    const others = this.settings.webVersion.filter((it) => it !== page);
    this.save({ ...this.settings, webVersion: inApp ? others : [...others, page] });
  }

  setRailSide(railSide: RailSide) {
    this.save({ ...this.settings, railSide });
  }

  setLastTab(lastTab: AppTab) {
    if (lastTab !== this.settings.lastTab) this.save({ ...this.settings, lastTab });
  }

  private save(settings: AppSettings) {
    this.settings = settings;
    const file = this.file();
    file.parentDirectory.create({ intermediates: true, idempotent: true });
    file.create({ overwrite: true });
    file.writeSync(JSON.stringify(this.settings));
    this.emit();
  }

  private file = () => new File(new Directory(Paths.document, 'pf-settings'), 'settings.json');

  private emit() {
    this.listeners.forEach((it) => it());
  }
}

export const appSettings = new AppSettingsStore();

export function useAppSettings(): AppSettings {
  return useSyncExternalStore(appSettings.subscribe, appSettings.get);
}
