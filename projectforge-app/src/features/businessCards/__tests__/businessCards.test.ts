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

import type { BusinessCardsData } from '@projectforge/shared/rs/business-card';

import { ApiException } from '../../../core/api/apiException';
import { BusinessCardsRepository, CardsCache, displayName, isOffline, vCardFileName } from '../businessCards';

const DATA: BusinessCardsData = {
  cards: [
    {
      card: { id: 'a1', cardName: 'Fair', title: 'Dr.', firstName: 'Jane', lastName: 'Doe', organization: 'ACME' },
      preview: { qrCodeUrl: 'data:image/png;base64,AAAA' },
    },
  ],
  walletConfigured: true,
};

function memoryCache(initial: Record<string, unknown> = {}): CardsCache & { entries: Record<string, unknown> } {
  const entries = { ...initial };
  return {
    entries,
    read: async <T,>(name: string) => (entries[name] as T) ?? null,
    write: (name: string, json: unknown) => {
      entries[name] = json;
    },
  };
}

function api(get: () => Promise<unknown>) {
  const download = jest.fn(async () => new Uint8Array([1, 2, 3]));
  return { get: get as <T>() => Promise<T>, download };
}

describe('BusinessCardsRepository', () => {
  it('loads the cards from the server and stores them', async () => {
    const cache = memoryCache();
    const cards = await new BusinessCardsRepository(api(async () => DATA), cache).load();
    expect(cards).toEqual({ data: DATA, fromCache: false });
    expect(cache.entries.businessCards).toEqual(DATA);
  });

  it('falls back to the stored cards without a connection', async () => {
    const cache = memoryCache({ businessCards: DATA });
    const failing = api(async () => {
      throw new TypeError('Network request failed');
    });
    expect(await new BusinessCardsRepository(failing, cache).load()).toEqual({ data: DATA, fromCache: true });
  });

  it('reports an answer of the server, not the stored cards', async () => {
    const cache = memoryCache({ businessCards: DATA });
    const refusing = api(async () => {
      throw new ApiException('HTTP 500', 500);
    });
    await expect(new BusinessCardsRepository(refusing, cache).load()).rejects.toThrow('HTTP 500');
  });

  it('reports a missing connection if nothing is stored', async () => {
    const failing = api(async () => {
      throw new TypeError('Network request failed');
    });
    await expect(new BusinessCardsRepository(failing, memoryCache()).load()).rejects.toThrow(TypeError);
  });

  it('offers the stored cards without asking the server', async () => {
    const get = jest.fn(async () => DATA);
    const repository = new BusinessCardsRepository(api(get), memoryCache({ businessCards: DATA }));
    expect(await repository.loadCached()).toEqual({ data: DATA, fromCache: false });
    expect(get).not.toHaveBeenCalled();
    expect(await new BusinessCardsRepository(api(get), memoryCache()).loadCached()).toBeNull();
  });

  it('downloads the vCard and the pass by the id of the card', async () => {
    const client = api(async () => DATA);
    const repository = new BusinessCardsRepository(client, memoryCache());
    await repository.vCard('a1');
    await repository.pass('a1');
    expect(client.download.mock.calls).toEqual([
      ['/rs/myBusinessCards/vcf', { id: 'a1' }],
      ['/rs/myBusinessCards/pkpass', { id: 'a1' }],
    ]);
  });
});

describe('business card helpers', () => {
  it('builds the display name of the given parts', () => {
    expect(displayName(DATA.cards[0].card)).toBe('Dr. Jane Doe');
    expect(displayName({ lastName: 'Doe' })).toBe('Doe');
  });

  it('names the vCard file safely', () => {
    expect(vCardFileName({ firstName: 'Jörg', lastName: "O'Neil/Smith" })).toBe('Jörg O_Neil_Smith.vcf');
    expect(vCardFileName({ cardName: 'Messe' })).toBe('Messe.vcf');
    expect(vCardFileName({})).toBe('card.vcf');
  });

  it('tells a missing connection from an answer of the server', () => {
    expect(isOffline(new TypeError('Network request failed'))).toBe(true);
    expect(isOffline(new ApiException('HTTP 404', 404))).toBe(false);
  });
});
