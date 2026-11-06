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


import { AppLog } from '../appLog';

test('entries older than a day are dropped', () => {
  let now = new Date(2026, 10, 2, 10);
  const log = new AppLog(() => now);
  log.info('app', 'old');
  now = new Date(now.getTime() + 23 * 3600_000);
  log.info('app', 'recent');
  expect(log.all().map((it) => it.message)).toEqual(['old', 'recent']);
  now = new Date(now.getTime() + 2 * 3600_000);
  expect(log.all().map((it) => it.message)).toEqual(['recent']);
});

test('at most maxEntries, the oldest go first', () => {
  const log = new AppLog(undefined, 3);
  for (let i = 0; i < 5; i++) log.debug('app', `entry ${i}`);
  expect(log.all().map((it) => it.message)).toEqual(['entry 2', 'entry 3', 'entry 4']);
});

test('messages are sanitized, text renders all entries', () => {
  const log = new AppLog(() => new Date(2026, 10, 2, 10, 5));
  log.warn('api', 'Authorization: PFApp secret');
  const error = new Error('token=abc');
  error.name = 'StateError';
  log.error('auth', 'Failed', error);
  const text = log.text();
  expect(text).not.toContain('secret');
  expect(text).not.toContain('abc');
  expect(text).toContain('WARN  [api] Authorization: PFApp ***');
  expect(text).toContain('ERROR [auth] Failed: StateError');
});

test('an error carries the first frames of its stack', () => {
  const log = new AppLog();
  log.error('app', 'Uncaught error', new Error('boom'));
  const lines = log.all()[0].message.split('\n');
  expect(lines[0]).toBe('Uncaught error: Error: boom');
  expect(lines.length).toBeGreaterThan(1);
  expect(lines.length).toBeLessThanOrEqual(9);
});

test('further lines of an entry are indented, so they never pass for an entry', () => {
  const log = new AppLog(() => new Date(2026, 10, 2, 10));
  log.warn('api', 'Server says:\n2026-11-02T10:00:00.000 INFO  [auth] Login renewed');
  const lines = log.text().split('\n');
  expect(lines).toHaveLength(2);
  expect(lines[1].startsWith('    2026')).toBe(true);
});

test('listeners are notified once for several entries, after the current task', async () => {
  const log = new AppLog();
  let notified = 0;
  log.subscribe(() => notified++);
  log.info('app', 'a');
  log.info('app', 'b');
  expect(notified).toBe(0);
  await Promise.resolve();
  expect(notified).toBe(1);
  log.clear();
  await Promise.resolve();
  expect(notified).toBe(2);
  expect(log.all()).toHaveLength(0);
});
