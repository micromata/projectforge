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

// Port of projectforge-flutter/test/core/session_store_test.dart, plus the takeover of the Flutter app's logins.
import { Session } from '../session';
import { LegacyKeychain, MAX_SERVERS, memoryStore, SessionStore } from '../sessionStore';

const session = (serverUrl: string, token = 't'): Session => ({
  serverUrl,
  token,
  userId: 7,
  deviceId: 3,
  proxyCookies: {},
  appLoginPath: false,
});

test('the active login, the logins at the other servers and the servers, most recent first', async () => {
  const store = new SessionStore(memoryStore());
  await store.save(session('https://a.example.org'));
  await store.save(session('https://b.example.org'));
  await store.save(session('https://a.example.org', 't2'));
  expect((await store.load())?.token).toBe('t2');
  expect((await store.sessions()).map((it) => it.serverUrl)).toEqual(['https://a.example.org', 'https://b.example.org']);
  expect(await store.servers()).toEqual(['https://a.example.org', 'https://b.example.org']);
});

test('clear forgets the active login only, the servers stay for the next login', async () => {
  const store = new SessionStore(memoryStore());
  await store.save(session('https://b.example.org'));
  await store.save(session('https://a.example.org'));
  await store.clear();
  expect(await store.load()).toBeNull();
  expect((await store.sessions()).map((it) => it.serverUrl)).toEqual(['https://b.example.org']);
  expect(await store.servers()).toEqual(['https://a.example.org', 'https://b.example.org']);
});

test('removing a server removes its stored login, but not the active one', async () => {
  const store = new SessionStore(memoryStore());
  await store.save(session('https://b.example.org'));
  await store.save(session('https://a.example.org'));
  await store.removeServer('https://b.example.org');
  await store.removeServer('https://a.example.org');
  expect(await store.servers()).toEqual([]);
  expect((await store.sessions()).map((it) => it.serverUrl)).toEqual(['https://a.example.org']);
  expect((await store.load())?.serverUrl).toBe('https://a.example.org');
});

test('at most MAX_SERVERS servers', async () => {
  const store = new SessionStore(memoryStore());
  for (let i = 0; i < MAX_SERVERS + 3; i++) await store.save(session(`https://s${i}.example.org`));
  expect(await store.servers()).toHaveLength(MAX_SERVERS);
  expect(await store.sessions()).toHaveLength(MAX_SERVERS);
});

test('an unreadable stored login is dropped: log in again', async () => {
  const store = new SessionStore(memoryStore({ session: '{broken' }));
  expect(await store.load()).toBeNull();
});

describe('takeover of the Flutter app', () => {
  const flutter = (items: Record<string, string>): LegacyKeychain & { items: Record<string, string> } => ({
    items,
    read: (key) => items[key] ?? null,
    delete: (key) => {
      delete items[key];
    },
  });

  test('its logins are copied once and deleted there', async () => {
    const legacy = flutter({
      session: JSON.stringify(session('https://a.example.org', 'flutterToken')),
      sessions: JSON.stringify([session('https://a.example.org', 'flutterToken'), session('https://b.example.org')]),
      servers: JSON.stringify(['https://a.example.org', 'https://b.example.org']),
    });
    const store = new SessionStore(memoryStore(), legacy);
    await store.migrate();
    expect((await store.load())?.token).toBe('flutterToken');
    expect((await store.sessions()).map((it) => it.serverUrl)).toEqual(['https://a.example.org', 'https://b.example.org']);
    expect(legacy.items).toEqual({});
    // A second start doesn't look again (nor overwrite a login of this app).
    legacy.items.session = JSON.stringify(session('https://c.example.org'));
    await store.migrate();
    expect((await store.load())?.serverUrl).toBe('https://a.example.org');
  });

  test('a login of this app is never replaced, a failure leaves a new login', async () => {
    const own = memoryStore({ session: JSON.stringify(session('https://own.example.org')) });
    const store = new SessionStore(own, flutter({ session: JSON.stringify(session('https://a.example.org')) }));
    await store.migrate();
    expect((await store.load())?.serverUrl).toBe('https://own.example.org');

    const failing = new SessionStore(memoryStore(), {
      read: () => {
        throw new Error('key chain');
      },
      delete: () => undefined,
    });
    await failing.migrate();
    expect(await failing.load()).toBeNull();
  });
});

test('a broken list of servers is no list', async () => {
  expect(await new SessionStore(memoryStore({ servers: '{' })).servers()).toEqual([]);
  expect(await new SessionStore(memoryStore({ sessions: '{' })).sessions()).toEqual([]);
});
