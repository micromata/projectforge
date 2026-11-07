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

import type { WebScreenHandle, WebScreenState } from './WebScreen';

/** The tabs with the pages of the server in a WebView. */
export type WebTab = 'pages' | 'chat';

/** The start page of the chat tab (its rooms). */
export const CHAT_PATH = '/next/chat';

const EMPTY: WebScreenState = { canGoBack: false, canGoForward: false, hasPage: false };

/**
 * The WebView tabs for the navigation around them (the browser menu, a tab asked to show a page): their handles and
 * whether they can go back or forward. One per login: the tabs register again after a new one.
 */
class WebTabs {
  private handles: Partial<Record<WebTab, WebScreenHandle>> = {};
  /** A page asked for before the tab exists (it is created on first use): loaded by it as its start page. */
  private pending: Partial<Record<WebTab, string>> = {};
  private states: Record<WebTab, WebScreenState> = { pages: EMPTY, chat: EMPTY };
  /** The menu of the pages tab is shown over its page: at the start, until an entry or a page is opened. */
  private menu = true;
  /** The login generation of the last [reset], and what waits for a later one ([afterReset]). */
  private resetGeneration = 0;
  private waiting: { generation: number; run: () => void }[] = [];
  private listeners = new Set<() => void>();

  register(tab: WebTab, handle: WebScreenHandle | null) {
    if (handle) this.handles[tab] = handle;
    else delete this.handles[tab];
  }

  handle(tab: WebTab): WebScreenHandle | undefined {
    return this.handles[tab];
  }

  /** Shows [path] in [tab], as soon as it exists (the page, not the menu over it). */
  open(tab: WebTab, path: string) {
    if (tab === 'pages') this.setMenu(false);
    const handle = this.handles[tab];
    if (handle) handle.open(path);
    else this.pending[tab] = path;
  }

  /**
   * Runs [run] once the tabs of the login [generation] are reset (e.g. a page to show on the server switched to);
   * at once if they are already.
   */
  afterReset(generation: number, run: () => void) {
    if (generation <= this.resetGeneration) run();
    else this.waiting.push({ generation, run });
  }

  /** The page asked for before the tab existed, taken once. */
  takePending(tab: WebTab): string | undefined {
    const path = this.pending[tab];
    delete this.pending[tab];
    return path;
  }

  setState(tab: WebTab, state: WebScreenState) {
    const old = this.states[tab];
    if (
      old.canGoBack === state.canGoBack &&
      old.canGoForward === state.canGoForward &&
      old.hasPage === state.hasPage
    ) {
      return;
    }
    this.states = { ...this.states, [tab]: state };
    this.listeners.forEach((it) => it());
  }

  setMenu(open: boolean) {
    if (this.menu === open) return;
    this.menu = open;
    this.listeners.forEach((it) => it());
  }

  toggleMenu() {
    this.setMenu(!this.menu);
  }

  /**
   * Another login ([generation]): nothing of the former one's pages stays. The handles stay, the tabs (re)register and
   * unregister themselves (the new ones have done so already when this runs, in an effect of their parent).
   */
  reset(generation = this.resetGeneration + 1) {
    this.pending = {};
    this.states = { pages: EMPTY, chat: EMPTY };
    this.menu = true;
    this.resetGeneration = generation;
    this.listeners.forEach((it) => it());
    const due = this.waiting.filter((it) => it.generation <= generation);
    this.waiting = this.waiting.filter((it) => it.generation > generation);
    due.forEach((it) => it.run());
  }

  getStates = () => this.states;

  getMenu = () => this.menu;

  subscribe = (listener: () => void) => {
    this.listeners.add(listener);
    return () => {
      this.listeners.delete(listener);
    };
  };
}

export const webTabs = new WebTabs();

export function useWebTabStates(): Record<WebTab, WebScreenState> {
  return useSyncExternalStore(webTabs.subscribe, webTabs.getStates);
}

/** Whether the menu of the pages tab is shown over its page. */
export function useWebMenuOpen(): boolean {
  return useSyncExternalStore(webTabs.subscribe, webTabs.getMenu);
}
