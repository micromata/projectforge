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

import {
  addressBook,
  AppAddress,
  displayName,
  initials,
  listItems,
  matches,
  mergeAddressBook,
  phones,
  sectionIndex,
  sectionLetter,
  subtitle,
  withFavorite,
} from '../address';
import { AddressesRepository, ImageFiles, imageName } from '../addressesRepository';
import { ApiException } from '../../../core/api/apiException';

const address = (fields: Partial<AppAddress>): AppAddress => ({ id: 1, lastUpdate: 10, ...fields });

describe('address', () => {
  it('sorts the list by last name, umlauts as their letter, "#" at the end', () => {
    const book = addressBook(1, [
      address({ id: 1, name: 'Zander' }),
      address({ id: 2, name: 'Ärger', firstName: 'Anna' }),
      address({ id: 3, name: 'Abel' }),
      address({ id: 4, name: '1Plus' }),
      address({ id: 5, organization: 'Acme' }),
    ]);
    expect(book.addresses.map((it) => it.id)).toEqual([3, 5, 2, 1, 4]);
  });

  it('puts the address in the section of its first letter, "#" for anything else', () => {
    expect(sectionLetter(address({ name: 'Ärger' }))).toBe('A');
    expect(sectionLetter(address({ name: 'özil' }))).toBe('O');
    expect(sectionLetter(address({ name: '42' }))).toBe('#');
    expect(sectionLetter(address({}))).toBe('#');
    expect(sectionLetter(address({ organization: 'Acme' }))).toBe('A');
  });

  it('builds the name and the initials', () => {
    const a = address({ title: 'Dr.', firstName: 'Jane', name: 'Doe', organization: 'Acme' });
    expect(displayName(a)).toBe('Dr. Jane Doe');
    expect(initials(a)).toBe('JD');
    expect(subtitle(a)).toBe('Acme');
    const org = address({ organization: 'Acme' });
    expect(displayName(org)).toBe('Acme');
    expect(subtitle(org)).toBeNull();
  });

  it('finds words in any order, without umlauts, and numbers', () => {
    const a = address({
      firstName: 'Jürgen',
      name: 'Müller',
      organization: 'Acme',
      email: 'jm@example.org',
      mobilePhone: '+49 (170) 123-456',
    });
    expect(matches(a, '')).toBe(true);
    expect(matches(a, 'muller jurgen')).toBe(true);
    expect(matches(a, 'acme müller')).toBe(true);
    expect(matches(a, 'example.org')).toBe(true);
    expect(matches(a, '170123')).toBe(true);
    expect(matches(a, '999')).toBe(false);
    expect(matches(a, 'schmidt')).toBe(false);
  });

  it('keeps the kind of the phone numbers, only mobile ones take messages', () => {
    const a = address({ businessPhone: '111', mobilePhone: '222', fax: ' ' });
    expect(phones(a).map((it) => [it.number, it.mobile])).toEqual([
      ['111', false],
      ['222', true],
    ]);
  });

  it('replaces changed addresses on a sync and drops the removed ones', () => {
    const stored = addressBook(1, [address({ id: 1, name: 'Alt' }), address({ id: 2, name: 'Weg' }), address({ id: 3, name: 'Bleibt' })]);
    const merged = mergeAddressBook(stored, 5, [1, 3, 4], [address({ id: 1, name: 'Neu' }), address({ id: 4, name: 'Dazu' })]);
    expect(merged?.timestamp).toBe(5);
    expect(merged?.addresses.map((it) => it.name)).toEqual(['Bleibt', 'Dazu', 'Neu']);
  });

  it('needs a full load for an address that became visible unchanged', () => {
    expect(mergeAddressBook(addressBook(1, [address({ id: 1 })]), 5, [1, 9], [])).toBeNull();
  });

  it('lists the sections and jumps to the next existing one', () => {
    const items = listItems(addressBook(1, [address({ id: 1, name: 'Abel' }), address({ id: 2, name: 'Cohen' })]).addresses);
    expect(items.map((it) => (it.type === 'header' ? it.letter : it.address.id))).toEqual(['A', 1, 'C', 2]);
    expect(sectionIndex(items, 'B')).toBe(2);
    expect(sectionIndex(items, 'Z')).toBe(3);
  });
});

describe('withFavorite', () => {
  it('adds a favorite once and removes it', () => {
    const book = addressBook(1, [], [1]);
    expect(withFavorite(book, 2, true).favoriteIds).toEqual([1, 2]);
    expect(withFavorite(book, 1, true).favoriteIds).toEqual([1]);
    expect(withFavorite(book, 1, false).favoriteIds).toEqual([]);
  });
});

describe('AddressesRepository', () => {
  const files = (): ImageFiles & { stored: Map<string, Uint8Array> } => {
    const stored = new Map<string, Uint8Array>();
    return {
      stored,
      uri: (name) => (stored.has(name) ? `file:///${name}` : null),
      write: (name, bytes) => {
        stored.set(name, bytes);
        return `file:///${name}`;
      },
      bytes: async (name) => stored.get(name) ?? null,
      names: () => [...stored.keys()],
      delete: (name) => void stored.delete(name),
    };
  };
  const cache = () => {
    const entries: Record<string, unknown> = {};
    return { entries, read: async <T,>(name: string) => (entries[name] as T) ?? null, write: (name: string, json: unknown) => void (entries[name] = json) };
  };

  it('loads everything first, then only the changes', async () => {
    const get = jest
      .fn()
      .mockResolvedValueOnce({ timestamp: 1, ids: [1], favoriteIds: [1], addresses: [address({ id: 1, name: 'Abel' })] })
      .mockResolvedValueOnce({ timestamp: 2, ids: [1, 2], addresses: [address({ id: 2, name: 'Cohen' })] });
    const repository = new AddressesRepository({ post: jest.fn(), get, download: jest.fn() }, cache(), files());
    const first = await repository.sync(null);
    expect(first.favoriteIds).toEqual([1]);
    const second = await repository.sync(first);
    expect(get.mock.calls.map((it) => it[0])).toEqual(['/rs/address/appList', '/rs/address/appList?modifiedSince=1']);
    expect(second.addresses.map((it) => it.id)).toEqual([1, 2]);
  });

  it('keeps the stored book without a connection, but reports an answer of the server', async () => {
    const stored = addressBook(1, [address({ id: 1 })]);
    const offline = new AddressesRepository({ post: jest.fn(), get: jest.fn().mockRejectedValue(new TypeError('Network request failed')), download: jest.fn() }, cache(), files());
    expect(await offline.sync(stored)).toEqual({ ...stored, fromCache: true });
    const refused = new AddressesRepository({ post: jest.fn(), get: jest.fn().mockRejectedValue(new ApiException('HTTP 500', 500)), download: jest.fn() }, cache(), files());
    await expect(refused.sync(stored)).rejects.toThrow('HTTP 500');
  });

  it('loads everything once when the server starts to send the vCards, else only the changes', async () => {
    const list = { timestamp: 2, ids: [1], addresses: [address({ id: 1, vCard: 'BEGIN:VCARD' })], withVCards: true };
    const get = jest.fn().mockResolvedValue(list);
    const repository = new AddressesRepository({ post: jest.fn(), get, download: jest.fn() }, cache(), files());
    const book = await repository.sync(addressBook(1, [address({ id: 1 })]));
    expect(book.withVCards).toBe(true);
    await repository.sync(book);
    expect(get.mock.calls.map((it) => it[0])).toEqual([
      '/rs/address/appList?modifiedSince=1',
      '/rs/address/appList',
      '/rs/address/appList?modifiedSince=2',
    ]);
    const old = jest.fn().mockResolvedValue({ ...list, withVCards: undefined });
    await new AddressesRepository({ post: jest.fn(), get: old, download: jest.fn() }, cache(), files()).sync(addressBook(1, [address({ id: 1 })]));
    expect(old.mock.calls.map((it) => it[0])).toEqual(['/rs/address/appList?modifiedSince=1']);
  });

  it('builds the vCard with the stored photo, or loads it from a server that sends none', async () => {
    const images = files();
    images.write(imageName(1, 10), new Uint8Array([0xff, 0xd8, 1]));
    const download = jest.fn().mockResolvedValue(new Uint8Array([9]));
    const repository = new AddressesRepository({ post: jest.fn(), get: jest.fn(), download }, cache(), images);
    const vCard = 'BEGIN:VCARD\r\nFN:Jane\r\nEND:VCARD\r\n';
    const text = new TextDecoder().decode(await repository.vCard(address({ id: 1, lastUpdate: 10, hasImage: true, vCard })));
    expect(text).toBe('BEGIN:VCARD\r\nFN:Jane\r\nPHOTO;ENCODING=b;TYPE=JPEG:/9gB\r\nEND:VCARD\r\n');
    expect(download).not.toHaveBeenCalled();
    expect(await repository.vCard(address({ id: 2 }))).toEqual(new Uint8Array([9]));
    expect(download.mock.calls[0][0]).toBe('/rs/address/exportVCard/2');
  });

  it('loads an image once and deletes the ones of older versions', async () => {
    const images = files();
    const download = jest.fn().mockResolvedValue(new Uint8Array([1]));
    images.write(imageName(1, 5), new Uint8Array([0]));
    const get = jest.fn().mockResolvedValue({ timestamp: 2, ids: [1], addresses: [address({ id: 1, lastUpdate: 10, hasImage: true })] });
    const repository = new AddressesRepository({ post: jest.fn(), get, download }, cache(), images);
    await repository.sync(null);
    expect(images.names()).toEqual([]);
    expect(await repository.image(1, 10)).toBe('file:///1-10.img');
    expect(await repository.image(1, 10)).toBe('file:///1-10.img');
    expect(download).toHaveBeenCalledTimes(1);
    expect(download.mock.calls[0][0]).toBe('/rs/address/imagePreview/1');
  });

  it('marks a favorite at the server and stores the book', async () => {
    const post = jest.fn().mockResolvedValue({ isFavoriteCard: true });
    const stored = cache();
    const repository = new AddressesRepository({ post, get: jest.fn(), download: jest.fn() }, stored, files());
    await repository.setFavorite(7, true);
    await repository.setFavorite(7, false);
    expect(post.mock.calls.map((it) => it[0])).toEqual([
      '/rs/address/favorite/7?favorite=true',
      '/rs/address/favorite/7?favorite=false',
    ]);
    repository.store({ ...addressBook(1, [], [7]), fromCache: true });
    expect(await repository.loadCached()).toEqual({ ...addressBook(1, [], [7]), fromCache: false });
  });
});
