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
  /** The area: `api`, `login`, `auth`, `web`, `app`, … */
  area: string;
  message: string;
}

/** An entry as a line of the copied log; further lines (a stack trace, a text of the server) are indented. */
export function logLine(entry: LogEntry): string {
  return `${entry.time.toISOString()} ${entry.level.toUpperCase().padEnd(5)} [${entry.area}] ${entry.message.replace(/\n/g, '\n    ')}`;
}

/**
 * The log of the app (login, requests, WebView navigation), shown and copied in the app to find out why something
 * fails, e.g. behind an SSO proxy.
 * - Only in memory, never written to a file; at most [MAX_AGE] old and at most [maxEntries] entries.
 * - Every message goes through LogSanitizer.text: no tokens, cookie values, codes or passwords.
 * - On the console of a dev build only: the system log of a release build is readable by others.
 */
export class AppLog {
  static readonly MAX_AGE = 24 * 60 * 60 * 1000;

  private entries: LogEntry[] = [];
  private listeners = new Set<() => void>();
  private notifying = false;

  constructor(
    private readonly clock: () => Date = () => new Date(),
    private readonly maxEntries = 2000,
    private readonly print = typeof __DEV__ !== 'undefined' && __DEV__ && process.env.NODE_ENV !== 'test',
  ) {}

  debug(area: string, message: string) {
    this.add('debug', area, message);
  }

  info(area: string, message: string) {
    this.add('info', area, message);
  }

  warn(area: string, message: string) {
    this.add('warn', area, message);
  }

  /** With the type of [error] and the first frames of its stack, enough to locate it. */
  error(area: string, message: string, error?: unknown) {
    let details = message;
    if (error != null) {
      details += error instanceof Error ? `: ${error.name}: ${error.message}` : `: ${String(error)}`;
      const stack = error instanceof Error ? error.stack : undefined;
      if (stack) {
        const frames = stack.split('\n').filter((it) => /^\s*at /.test(it) || /@/.test(it));
        if (frames.length) details += '\n' + frames.slice(0, 8).map((it) => it.trim()).join('\n');
      }
    }
    this.add('error', area, details);
  }

  /** Empties the log (logout, another user): it names the server, the pages, the ids. */
  clear() {
    this.entries = [];
    this.notify();
  }

  /** The entries, oldest first (the same array until the log changes). */
  all(): readonly LogEntry[] {
    this.prune();
    return this.entries;
  }

  /** All entries as text, for copying. */
  text(): string {
    return this.all().map(logLine).join('\n');
  }

  subscribe(listener: () => void): () => void {
    this.listeners.add(listener);
    return () => this.listeners.delete(listener);
  }

  private add(level: LogLevel, area: string, message: string) {
    const entry: LogEntry = { time: this.clock(), level, area, message: LogSanitizer.text(message) };
    this.entries = [...this.entries, entry];
    this.prune();
    if (this.print) console.log(`PF ${logLine(entry)}`);
    this.notify();
  }

  /**
   * Deferred: an entry may be written while a component renders, the log screen must not update then. Several entries
   * in a row notify once.
   */
  private notify() {
    if (this.notifying) return;
    this.notifying = true;
    queueMicrotask(() => {
      this.notifying = false;
      this.listeners.forEach((it) => it());
    });
  }

  private prune() {
    const oldest = this.clock().getTime() - AppLog.MAX_AGE;
    let kept = this.entries.filter((it) => it.time.getTime() >= oldest);
    if (kept.length > this.maxEntries) kept = kept.slice(kept.length - this.maxEntries);
    if (kept.length !== this.entries.length) this.entries = kept;
  }
}

export const appLog = new AppLog();
