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

/**
 * The oldest server version this app works with. Raise it in the commit that needs a feature of a newer server;
 * prefer a feature flag of the system status (e.g. `appLoginPath`) where the app can fall back.
 *
 * The other direction, the app version a server needs, is declared by the server (`AppCompatibility.MIN_APP_VERSION`,
 * `systemData.minAppVersion`): only the newer side knows about a break.
 */
export const MIN_SERVER_VERSION = '9.2.5';

/** Why the app and the server may not work together. Only a warning: nothing is refused. */
export type CompatibilityIssue =
  /** The server needs a newer app: the user should update it. */
  | { kind: 'appTooOld'; minVersion: string }
  /** The app needs a newer server: some functions may fail, only the administrator can help. */
  | { kind: 'serverTooOld'; minVersion: string };

/**
 * The issue between [appVersion] (`1.1.0`) and a server of [serverVersion] (`9.2.4-SNAPSHOT`) that asks for
 * [minAppVersion]. Null if they fit, or if a version is unknown or unparseable: no false alarm.
 */
export function compatibilityIssue(opts: {
  appVersion?: string | null;
  serverVersion?: string | null;
  minAppVersion?: string | null;
}): CompatibilityIssue | null {
  if (isOlder(opts.appVersion, opts.minAppVersion)) return { kind: 'appTooOld', minVersion: opts.minAppVersion! };
  if (isOlder(opts.serverVersion, MIN_SERVER_VERSION)) return { kind: 'serverTooOld', minVersion: MIN_SERVER_VERSION };
  return null;
}

function isOlder(version?: string | null, min?: string | null): boolean {
  const a = parseVersion(version);
  const b = parseVersion(min);
  return a != null && b != null && compareVersions(a, b) < 0;
}

/**
 * The numeric segments of `1.0.1`, `1.0.1+3` or `9.2.4-SNAPSHOT` (a snapshot counts as its release, so a development
 * server doesn't warn); null if it doesn't start with a number.
 */
export function parseVersion(version?: string | null): number[] | null {
  const match = /^\d{1,9}(\.\d{1,9})*/.exec(version?.trim() ?? '');
  return match ? match[0].split('.').map(Number) : null;
}

/** Compares the segments, a missing one counts as 0 (`1.0` == `1.0.0`). */
export function compareVersions(a: number[], b: number[]): number {
  for (let i = 0; i < a.length || i < b.length; i++) {
    const diff = (a[i] ?? 0) - (b[i] ?? 0);
    if (diff !== 0) return Math.sign(diff);
  }
  return 0;
}
