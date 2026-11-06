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

import type { BusinessCard, BusinessCardsData } from '@projectforge/shared/rs/business-card';

import type { ApiClient } from '../../core/api/apiClient';
import { ApiException } from '../../core/api/apiException';

export type { BusinessCard, BusinessCardInfo } from '@projectforge/shared/rs/business-card';

/** The cards of the server, plus whether they come from the cache (server not reachable). */
export interface BusinessCards {
  data: BusinessCardsData;
  fromCache: boolean;
}

/** The part of JsonCache the repository uses. */
export interface CardsCache {
  read<T>(name: string): Promise<T | null>;
  write(name: string, json: unknown): void;
}

/** "Dr. Jane Doe", for the list. */
export function displayName(card: BusinessCard): string {
  return [card.title, card.firstName, card.lastName].filter((it): it is string => !!it).join(' ');
}

/** The name of the shared vCard file of [card], without characters a file system or a mail program would refuse. */
export function vCardFileName(card: BusinessCard): string {
  const name = displayName(card) || card.cardName || 'card';
  return `${name.replace(/[^\p{L}\p{N}_\- ]+/gu, '_')}.vcf`;
}

/** Not reachable at all (no network, wrong host), unlike an answer of the server: cached data may stand in. */
export function isOffline(error: unknown): boolean {
  return !(error instanceof ApiException) || error.statusCode == null;
}

/**
 * The own business cards of the user (BusinessCardRest, `/rs/myBusinessCards`). The list is cached: a card has to be
 * shown where there is no network. Port of the Flutter app's BusinessCardsRepository.
 */
export class BusinessCardsRepository {
  static readonly BASE = '/rs/myBusinessCards';
  private static readonly CACHE_NAME = 'businessCards';

  constructor(
    private readonly api: Pick<ApiClient, 'get' | 'download'>,
    private readonly cache: CardsCache,
  ) {}

  /** The cards of the last load, null if there are none (yet). Not marked as offline: the server isn't asked. */
  async loadCached(): Promise<BusinessCards | null> {
    const cached = await this.cache.read<BusinessCardsData>(BusinessCardsRepository.CACHE_NAME);
    return cached == null ? null : { data: cached, fromCache: false };
  }

  /** The cards from the server, or the cached ones if it isn't reachable. */
  async load(): Promise<BusinessCards> {
    try {
      const data = await this.api.get<BusinessCardsData>(BusinessCardsRepository.BASE);
      this.cache.write(BusinessCardsRepository.CACHE_NAME, data);
      return { data, fromCache: false };
    } catch (e) {
      if (!isOffline(e)) throw e;
      const cached = await this.cache.read<BusinessCardsData>(BusinessCardsRepository.CACHE_NAME);
      if (cached == null) throw e;
      return { data: cached, fromCache: true };
    }
  }

  /** The card as vCard file. */
  vCard(id: string): Promise<Uint8Array> {
    return this.api.download(`${BusinessCardsRepository.BASE}/vcf`, { id });
  }

  /** The card as Apple Wallet pass (`.pkpass`). */
  pass(id: string): Promise<Uint8Array> {
    return this.api.download(`${BusinessCardsRepository.BASE}/pkpass`, { id });
  }
}
