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

import { LogSanitizer } from './logSanitizer';

export { LogSanitizer };

export type LogLevel = 'debug' | 'info' | 'warn' | 'error';

export interface LogEntry {
  time: Date;
  level: LogLevel;
  area: string;
  message: string;
}

const MAX_ENTRIES = 500;

/** The client log of the app (in memory, shown in the app, also on the console of the dev build). */
class AppLog {
  private entries: LogEntry[] = [];
  private listeners = new Set<() => void>();

  debug(area: string, message: string) {
    this.add('debug', area, message);
  }

  info(area: string, message: string) {
    this.add('info', area, message);
  }

  warn(area: string, message: string) {
    this.add('warn', area, message);
  }

  error(area: string, message: string, error?: unknown) {
    this.add('error', area, error == null ? message : `${message}: ${String(error)}`);
  }

  /** Empties the log (logout, another user): it names the server, the pages, the ids. */
  clear() {
    this.entries = [];
    this.listeners.forEach((it) => it());
  }

  all(): readonly LogEntry[] {
    return this.entries;
  }

  subscribe(listener: () => void): () => void {
    this.listeners.add(listener);
    return () => this.listeners.delete(listener);
  }

  private add(level: LogLevel, area: string, message: string) {
    const entry: LogEntry = { time: new Date(), level, area, message: LogSanitizer.text(message) };
    this.entries = [...this.entries.slice(-(MAX_ENTRIES - 1)), entry];
    if (process.env.NODE_ENV !== 'test') {
      console.log(`[${level}] ${area}: ${entry.message}`);
    }
    this.listeners.forEach((it) => it());
  }
}

export const appLog = new AppLog();
