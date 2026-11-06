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

import { parseUrl } from '../url';

export type LogLevel = 'debug' | 'info' | 'warn' | 'error';

export interface LogEntry {
  time: Date;
  level: LogLevel;
  area: string;
  message: string;
}

const MASK = '***';
const MAX_ENTRIES = 500;

/**
 * Keeps secrets out of the log (a reduced port of the Flutter app's LogSanitizer): urls show scheme, host, port and
 * path, the values of their query are masked; cookies only by name; `Authorization` values masked.
 */
export const LogSanitizer = {
  url(url: string | URL | null | undefined): string {
    if (url == null) return '-';
    const parsed = typeof url === 'string' ? parseUrl(url) : url;
    if (parsed == null) return MASK;
    if (parsed.protocol !== 'http:' && parsed.protocol !== 'https:') return `${parsed.protocol}…`;
    const names = [...parsed.searchParams.keys()];
    const query = names.length ? `?${names.map((it) => `${it}=${MASK}`).join('&')}` : '';
    // The packed login parameters in the path of /next/appLogin/<login>.
    const path = parsed.pathname.replace(/^(\/next\/appLogin\/).+/, `$1${MASK}`);
    return `${parsed.protocol}//${parsed.host}${path}${query}`;
  },

  cookieNames(names: Iterable<string>): string {
    const list = [...names];
    return list.length ? list.join(', ') : '-';
  },

  text(message: string): string {
    return message
      .replace(/(PFApp|Bearer)\s+\S+/g, `$1 ${MASK}`)
      .replace(/https?:\/\/[^\s"<>]+/g, (it) => LogSanitizer.url(it));
  },
};

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
