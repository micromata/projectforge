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

import { appLog } from '../log/appLog';
import { Session, sessionFromJson } from './session';

/** The secure storage behind the store: the key chain/keystore in the app (expo-secure-store), a map in the tests. */
export interface KeyValueStore {
  get(key: string): Promise<string | null>;
  set(key: string, value: string): Promise<void>;
  delete(key: string): Promise<void>;
}

/** The key chain of the Flutter app (flutter_secure_storage), read once on the update to this app. */
export interface LegacyKeychain {
  read(key: string): string | null;
  delete(key: string): void;
}

/** The active login. */
const SESSION_KEY = 'session';
/** All logins, one per server, most recent first: the active one and those to switch to. */
const SESSIONS_KEY = 'sessions';
/** The servers of the last logins, most recent first (urls only). */
const SERVERS_KEY = 'servers';
/** Set once the key chain of the Flutter app was looked at. */
const MIGRATED_KEY = 'flutterMigrated';
/** The keys of the Flutter app's SessionStore (lib/core/auth/session.dart), with the same json. */
const LEGACY_KEYS = [SESSION_KEY, SESSIONS_KEY, SERVERS_KEY];

export const MAX_SERVERS = 10;

/**
 * Keeps the [Session] in the key chain (iOS) or the keystore (Android), the logins at the other servers (to switch to
 * them without logging in again), and the servers of the last logins for the next one. Port of the Flutter app's
 * SessionStore; on the first start after the update of the Flutter app its logins are taken over ([migrate]).
 */
export class SessionStore {
  constructor(
    private readonly store: KeyValueStore,
    private readonly legacy?: LegacyKeychain,
  ) {}

  /**
   * Takes over the logins of the Flutter app (same keys, same json) once: copied, then deleted there. A failure is no
   * harm, the app then asks for a new login.
   */
  async migrate(): Promise<void> {
    if (this.legacy == null || (await this.store.get(MIGRATED_KEY)) != null) return;
    try {
      let taken = 0;
      for (const key of LEGACY_KEYS) {
        const value = this.legacy.read(key);
        if (value != null && (await this.store.get(key)) == null) {
          await this.store.set(key, value);
          taken++;
        }
      }
      for (const key of LEGACY_KEYS) this.legacy.delete(key);
      if (taken) appLog.info('auth', `Logins of the Flutter app taken over (${taken} entries)`);
    } catch (e) {
      appLog.warn('auth', `Logins of the Flutter app not taken over: ${String(e)}`);
    }
    await this.store.set(MIGRATED_KEY, '1');
  }

  async load(): Promise<Session | null> {
    const json = await this.store.get(SESSION_KEY);
    if (json == null) return null;
    try {
      return sessionFromJson(JSON.parse(json));
    } catch {
      await this.store.delete(SESSION_KEY);
      return null;
    }
  }

  /** Stores [session] as the active login, replacing an older one of its server. */
  async save(session: Session): Promise<void> {
    const sessions = (await this.sessions()).filter((it) => it.serverUrl !== session.serverUrl);
    sessions.unshift(session);
    await this.store.set(SESSION_KEY, JSON.stringify(session));
    await this.store.set(SESSIONS_KEY, JSON.stringify(sessions.slice(0, MAX_SERVERS)));
    const servers = (await this.servers()).filter((it) => it !== session.serverUrl);
    servers.unshift(session.serverUrl);
    await this.store.set(SERVERS_KEY, JSON.stringify(servers.slice(0, MAX_SERVERS)));
  }

  /** Forgets the active session, but not the servers nor the logins at the other servers. */
  async clear(): Promise<void> {
    const active = await this.load();
    await this.store.delete(SESSION_KEY);
    if (active != null) await this.removeSession(active.serverUrl);
  }

  /** The stored logins, most recent first. */
  async sessions(): Promise<Session[]> {
    const json = await this.store.get(SESSIONS_KEY);
    if (json == null) {
      const active = await this.load();
      return active == null ? [] : [active];
    }
    try {
      return (JSON.parse(json) as Record<string, unknown>[]).map(sessionFromJson);
    } catch {
      return [];
    }
  }

  /** The servers of the last logins, most recent first. */
  async servers(): Promise<string[]> {
    const json = await this.store.get(SERVERS_KEY);
    if (json == null) return [];
    try {
      return (JSON.parse(json) as unknown[]).filter((it): it is string => typeof it === 'string');
    } catch {
      return [];
    }
  }

  /** Removes the server from the list, and its stored login (not the active one, which only a logout ends). */
  async removeServer(serverUrl: string): Promise<void> {
    await this.store.set(SERVERS_KEY, JSON.stringify((await this.servers()).filter((it) => it !== serverUrl)));
    if ((await this.load())?.serverUrl !== serverUrl) await this.removeSession(serverUrl);
  }

  private async removeSession(serverUrl: string): Promise<void> {
    const sessions = (await this.sessions()).filter((it) => it.serverUrl !== serverUrl);
    await this.store.set(SESSIONS_KEY, JSON.stringify(sessions));
  }
}

/** A [KeyValueStore] in memory, for the tests. */
export function memoryStore(initial: Record<string, string> = {}): KeyValueStore & { data: Record<string, string> } {
  const data = { ...initial };
  return {
    data,
    get: async (key) => data[key] ?? null,
    set: async (key, value) => {
      data[key] = value;
    },
    delete: async (key) => {
      delete data[key];
    },
  };
}
