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

/** The stored photos of the cards, by name (a file of the cache); [uri] and [write] give its uri for an Image. */
export interface PhotoFiles {
  uri(name: string): string | null;
  write(name: string, bytes: Uint8Array): string;
  delete(name: string): void;
}

/** The fields of a card in the order of the web version, with the key of their label. */
export const CARD_FIELDS = [
  { field: 'title', labelKey: 'businessCard.title' },
  { field: 'firstName', labelKey: 'businessCard.firstName' },
  { field: 'lastName', labelKey: 'businessCard.lastName' },
  { field: 'organization', labelKey: 'businessCard.organization' },
  { field: 'division', labelKey: 'businessCard.division' },
  { field: 'positionText', labelKey: 'businessCard.positionText' },
  { field: 'email', labelKey: 'businessCard.email' },
  { field: 'businessPhone', labelKey: 'businessCard.businessPhone' },
  { field: 'mobilePhone', labelKey: 'businessCard.mobilePhone' },
  { field: 'fax', labelKey: 'businessCard.fax' },
  { field: 'website', labelKey: 'businessCard.website' },
  { field: 'street', labelKey: 'businessCard.street' },
  { field: 'zipCode', labelKey: 'businessCard.zipCode' },
  { field: 'city', labelKey: 'businessCard.city' },
  { field: 'country', labelKey: 'businessCard.country' },
  { field: 'comment', labelKey: 'businessCard.comment' },
  { field: 'privateEmail', labelKey: 'businessCard.privateEmail' },
  { field: 'privatePhone', labelKey: 'businessCard.privatePhone' },
  { field: 'privateMobilePhone', labelKey: 'businessCard.privateMobilePhone' },
  { field: 'privateStreet', labelKey: 'businessCard.privateStreet' },
  { field: 'privateZipCode', labelKey: 'businessCard.privateZipCode' },
  { field: 'privateCity', labelKey: 'businessCard.privateCity' },
  { field: 'privateCountry', labelKey: 'businessCard.privateCountry' },
  { field: 'birthday', labelKey: 'businessCard.birthday' },
] as const;

export type CardField = (typeof CARD_FIELDS)[number]['field'];

/** The filled fields of [card] (what its QR code contains), in the order of [CARD_FIELDS]. */
export function filledFields(card: BusinessCard): { field: CardField; labelKey: string; value: string }[] {
  return CARD_FIELDS.flatMap(({ field, labelKey }) => {
    const value = card[field]?.trim();
    return value ? [{ field, labelKey, value }] : [];
  });
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
    private readonly photos?: PhotoFiles,
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

  /**
   * The uri of the photo of the address [addressId] as its public url delivers it, i.e. what a scanner of the QR code
   * gets; stored for the use without network. With [online] false, or if the server isn't reachable, the stored one.
   * Null if there is none: the server can't deliver it (an answer like 404), or nothing is stored.
   */
  async photo(addressId: number, online: boolean): Promise<string | null> {
    const name = `${addressId}`;
    const stored = this.photos?.uri(name) ?? null;
    if (!online || this.photos == null) return stored;
    try {
      const bytes = await this.api.download(`${BusinessCardsRepository.BASE}/photo`, { addressId: name });
      return this.photos.write(name, bytes);
    } catch (e) {
      if (isOffline(e)) return stored;
      this.photos.delete(name);
      return null;
    }
  }
}
