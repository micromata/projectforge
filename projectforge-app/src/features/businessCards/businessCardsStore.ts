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

import { File } from 'expo-file-system';
import { useEffect, useSyncExternalStore } from 'react';

import type { ApiClient } from '../../core/api/apiClient';
import { authController } from '../../core/auth/authController';
import { JsonCache } from '../../core/cache/jsonCache';
import { BusinessCards, BusinessCardsRepository, PhotoFiles } from './businessCards';

export type BusinessCardsState =
  | { status: 'loading' }
  | { status: 'error'; error: unknown }
  | { status: 'data'; cards: BusinessCards };

const photoDir = () => JsonCache.directory('businessCardPhotos');

/** The photos of the cards in the cache (deleted with it on logout). */
const photoFiles: PhotoFiles = {
  uri(name) {
    const file = new File(photoDir(), name);
    return file.exists ? file.uri : null;
  },
  write(name, bytes) {
    const file = new File(photoDir(), name);
    if (file.exists) file.delete();
    file.create();
    file.writeSync(bytes);
    return file.uri;
  },
  delete(name) {
    const file = new File(photoDir(), name);
    if (file.exists) file.delete();
  },
};

/**
 * The list of the own business cards (created and edited in the web version), one per login. The stored cards are
 * shown at once (also without network) and replaced by the ones of the server when it answers; if it doesn't, the
 * stored ones stay. The photos of the cards are loaded with them and stored as well. Port of the Flutter app's
 * BusinessCardsController.
 */
class BusinessCardsStore {
  private state: BusinessCardsState = { status: 'loading' };
  private api: ApiClient | null = null;
  /** The uri of the photo by address id, null if there is none; missing while it is loaded. */
  private photos = new Map<number, string | null>();
  private listeners = new Set<() => void>();

  getState = () => this.state;

  subscribe = (listener: () => void) => {
    this.listeners.add(listener);
    return () => {
      this.listeners.delete(listener);
    };
  };

  photoOf = (addressId: number) => this.photos.get(addressId);

  /** The repository of the active login (null while logged out). */
  repository(): BusinessCardsRepository | null {
    const api = authController.api;
    return api == null ? null : new BusinessCardsRepository(api, JsonCache, photoFiles);
  }

  /** Loads the cards of the active login, once per login (the stored ones first). */
  ensureLoaded() {
    const api = authController.api;
    if (api === this.api) return;
    this.api = api;
    this.photos = new Map();
    this.set({ status: 'loading' });
    if (api != null) void this.start(api);
  }

  private async start(api: ApiClient) {
    const repository = new BusinessCardsRepository(api, JsonCache, photoFiles);
    const cached = await repository.loadCached().catch(() => null);
    if (this.api !== api) return;
    if (cached == null) {
      await this.refresh();
      return;
    }
    this.set({ status: 'data', cards: cached });
    void this.loadPhotos(api, repository, cached, false, false);
    // Silently: the stored cards stay if the server doesn't answer.
    void repository
      .load()
      .then((cards) => {
        if (this.api !== api) return;
        this.set({ status: 'data', cards });
        void this.loadPhotos(api, repository, cards, !cards.fromCache);
      })
      .catch(() => void this.loadPhotos(api, repository, cached, false));
  }

  /**
   * Loads the cards from the server. Without a connection the stored cards stay (marked as such); another failure
   * keeps the shown cards too and is returned for the caller to report.
   */
  async refresh(): Promise<unknown> {
    const api = this.api;
    const repository = this.repository();
    if (api == null || repository == null) return null;
    const shown = this.state.status === 'data';
    try {
      const cards = await repository.load();
      if (this.api === api) {
        this.set({ status: 'data', cards });
        void this.loadPhotos(api, repository, cards, !cards.fromCache);
      }
      return null;
    } catch (e) {
      if (!shown && this.api === api) this.set({ status: 'error', error: e });
      return e;
    }
  }

  /**
   * The photos of the [cards] that have one, from the server if [online], else the stored ones. Unless [final], a
   * photo not stored stays missing (loading): the server is still asked.
   */
  private async loadPhotos(
    api: ApiClient,
    repository: BusinessCardsRepository,
    cards: BusinessCards,
    online: boolean,
    final = true,
  ) {
    const ids = new Set<number>();
    for (const { card, preview } of cards.data.cards) {
      if (preview.withPhoto && card.addressId != null) ids.add(card.addressId);
    }
    for (const id of ids) {
      const uri = await repository.photo(id, online).catch(() => null);
      if (this.api !== api) return;
      if (uri == null && !final) continue;
      this.photos.set(id, uri);
      this.listeners.forEach((it) => it());
    }
  }

  private set(state: BusinessCardsState) {
    this.state = state;
    this.listeners.forEach((it) => it());
  }
}

export const businessCards = new BusinessCardsStore();

/** The uri of the photo of the address [addressId] of a card: null if there is none, undefined while it is loaded. */
export function useCardPhoto(addressId: number): string | null | undefined {
  return useSyncExternalStore(businessCards.subscribe, () => businessCards.photoOf(addressId));
}

/** The cards of the active login, loaded on first use. */
export function useBusinessCards(): BusinessCardsState {
  useEffect(() => businessCards.ensureLoaded());
  return useSyncExternalStore(businessCards.subscribe, businessCards.getState);
}
