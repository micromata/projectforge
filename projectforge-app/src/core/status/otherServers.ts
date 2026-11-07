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

import type { UserStatusResult } from '@projectforge/shared/rs/user-status';
import { useSyncExternalStore } from 'react';
import { AppState } from 'react-native';

import { ApiClient } from '../api/apiClient';
import { Session } from '../auth/session';
import { appLog } from '../log/appLog';

/** The open notifications of a stored login that isn't the active one. */
export interface OtherServer {
  session: Session;
  openCount: number;
  maxSeverity: string | null;
}

const INTERVAL = 5 * 60_000;

const key = (session: Session) => `${session.serverUrl}#${session.userId}`;

/**
 * The open notifications of the other stored logins (their servers push too, see PushService): shown at the "more"
 * menu, in the server list and below the notifications. Loaded on the start of a login, on resuming the app, when the
 * server list or the notifications are opened and every few minutes in the foreground (not each minute as the active
 * login: such a request goes to its server for nothing most of the time), and taken from a push of such a server (its
 * badge).
 */
export class OtherServersStore {
  private servers: OtherServer[] = [];
  private listeners = new Set<() => void>();
  private deps: { sessions: () => Promise<Session[]>; clientFor: (session: Session) => ApiClient } | null = null;
  private appState: { remove(): void } | null = null;
  private timer: ReturnType<typeof setInterval> | null = null;
  private generation = 0;

  get = () => this.servers;

  subscribe = (listener: () => void) => {
    this.listeners.add(listener);
    return () => {
      this.listeners.delete(listener);
    };
  };

  /** For the active login: [sessions] are the other stored logins. */
  start(deps: { sessions: () => Promise<Session[]>; clientFor: (session: Session) => ApiClient }) {
    this.stop();
    this.deps = deps;
    this.appState = AppState.addEventListener('change', (next) => {
      if (next === 'active') {
        void this.refresh();
        this.startTimer();
      } else {
        this.stopTimer();
      }
    });
    this.startTimer();
    void this.refresh();
  }

  stop() {
    this.stopTimer();
    this.appState?.remove();
    this.appState = null;
    this.deps = null;
    this.generation++;
    this.set([]);
  }

  /** Loads the count of each other login; one not answering keeps its last count. */
  refresh = async () => {
    const deps = this.deps;
    if (deps == null) return;
    const generation = this.generation;
    const sessions = await deps.sessions();
    const loaded = await Promise.all(
      sessions.map(async (session): Promise<OtherServer | null> => {
        try {
          const status = await deps.clientFor(session).get<UserStatusResult>('/rs/userStatus', { renewLogin: false });
          appLog.debug('status', `Another server: ${status.notifications?.openCount ?? 0} open notifications`);
          return {
            session,
            openCount: status.notifications?.openCount ?? 0,
            maxSeverity: status.notifications?.maxSeverity ?? null,
          };
        } catch (e) {
          appLog.debug('status', `Status of another server not loaded: ${String(e)}`);
          return this.servers.find((it) => key(it.session) === key(session)) ?? null;
        }
      }),
    );
    if (generation !== this.generation) return;
    this.set(loaded.filter((it): it is OtherServer => it != null));
  };

  /** A push of the other login [session] arrived: its count at once ([badge], if it has one). */
  pushed(session: Session, badge: number | null) {
    if (badge == null) return;
    const known = this.servers.some((it) => key(it.session) === key(session));
    this.set(
      known
        ? this.servers.map((it) => (key(it.session) === key(session) ? { ...it, openCount: badge } : it))
        : [...this.servers, { session, openCount: badge, maxSeverity: null }],
    );
  }

  private startTimer() {
    this.stopTimer();
    this.timer = setInterval(() => void this.refresh(), INTERVAL);
  }

  private stopTimer() {
    if (this.timer) clearInterval(this.timer);
    this.timer = null;
  }

  /** The open notifications of all other logins. */
  total = () => this.servers.reduce((sum, it) => sum + it.openCount, 0);

  private set(servers: OtherServer[]) {
    this.servers = servers;
    this.listeners.forEach((it) => it());
  }
}

export const otherServers = new OtherServersStore();

export function useOtherServers(): OtherServer[] {
  return useSyncExternalStore(otherServers.subscribe, otherServers.get);
}
