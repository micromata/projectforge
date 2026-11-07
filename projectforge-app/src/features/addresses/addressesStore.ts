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

import { useEffect, useState, useSyncExternalStore } from 'react';

import type { ApiClient } from '../../core/api/apiClient';
import { authController } from '../../core/auth/authController';
import { JsonCache } from '../../core/cache/jsonCache';
import { AddressBook, AppAddress } from './address';
import { addressImageFiles } from './addressImages';
import { AddressesRepository, imageName } from './addressesRepository';

export type AddressesState =
  | { status: 'loading' }
  | { status: 'error'; error: unknown }
  | { status: 'data'; book: AddressBook };

/** A silent sync when the tab is opened, at most this often. */
const STALE_MILLIS = 3 * 60_000;

/**
 * The address book of the app, one per login: the stored addresses are shown at once (also without network) and
 * replaced by the synced ones when the server answers; if it doesn't, the stored ones stay. Port of the Flutter app's
 * AddressesController.
 */
class AddressesStore {
  private state: AddressesState = { status: 'loading' };
  private api: ApiClient | null = null;
  private lastSync = 0;
  private listeners = new Set<() => void>();
  /** The images being loaded or loaded, by file name. */
  private images = new Map<string, Promise<string | null>>();

  getState = () => this.state;

  subscribe = (listener: () => void) => {
    this.listeners.add(listener);
    return () => {
      this.listeners.delete(listener);
    };
  };

  private repository(api: ApiClient) {
    return new AddressesRepository(api, JsonCache, addressImageFiles);
  }

  /** Loads the addresses of the active login, once per login (the stored ones first). */
  ensureLoaded() {
    const api = authController.api;
    if (api === this.api) return;
    this.api = api;
    this.images.clear();
    this.lastSync = 0;
    this.set({ status: 'loading' });
    if (api != null) void this.start(api);
  }

  private async start(api: ApiClient) {
    const repository = this.repository(api);
    const cached = await repository.loadCached().catch(() => null);
    if (this.api !== api) return;
    if (cached == null) {
      await this.refresh();
      return;
    }
    this.set({ status: 'data', book: cached });
    await this.syncSilently(api, cached);
  }

  private async syncSilently(api: ApiClient, stored: AddressBook) {
    this.lastSync = Date.now();
    try {
      const book = await this.repository(api).sync(stored);
      if (this.api === api) this.set({ status: 'data', book });
    } catch {
      // The stored addresses stay.
    }
  }

  /**
   * Asks the server for changes. Without a connection the stored addresses stay (marked as such); another failure
   * keeps the shown addresses too and is returned for the caller to report.
   */
  async refresh(): Promise<unknown> {
    const api = this.api;
    if (api == null) return null;
    const shown = this.state.status === 'data' ? this.state.book : null;
    this.lastSync = Date.now();
    try {
      const book = await this.repository(api).sync(shown);
      if (this.api === api) this.set({ status: 'data', book });
      return null;
    } catch (e) {
      if (shown == null && this.api === api) this.set({ status: 'error', error: e });
      return e;
    }
  }

  /** A silent [refresh], e.g. when the tab is opened; at most every few minutes. */
  refreshIfStale() {
    const api = this.api;
    if (api == null || this.state.status !== 'data' || Date.now() - this.lastSync < STALE_MILLIS) return;
    void this.syncSilently(api, this.state.book);
  }

  /** The `file://` url of the image of [address] (thumbnail or [full]), null without one. */
  image(address: AppAddress, full: boolean): Promise<string | null> {
    const api = this.api;
    if (api == null || !address.hasImage) return Promise.resolve(null);
    const name = imageName(address.id, address.lastUpdate, full);
    let image = this.images.get(name);
    if (image == null) {
      image = this.repository(api).image(address.id, address.lastUpdate, full);
      this.images.set(name, image);
      // Not reachable now: asked again next time.
      void image.then((uri) => uri == null && this.images.delete(name));
    }
    return image;
  }

  /** The vCard of [address] for sharing or the contacts of the device (see AddressesRepository.vCard). */
  vCard(address: AppAddress): Promise<Uint8Array> {
    const api = this.api;
    if (api == null) return Promise.reject(new Error('Not logged in.'));
    return this.repository(api).vCard(address);
  }

  private set(state: AddressesState) {
    this.state = state;
    this.listeners.forEach((it) => it());
  }
}

export const addresses = new AddressesStore();

/** The address book of the active login, loaded on first use. */
export function useAddresses(): AddressesState {
  useEffect(() => addresses.ensureLoaded());
  return useSyncExternalStore(addresses.subscribe, addresses.getState);
}

/** The image of [address] (thumbnail, or [full] the image itself with the thumbnail until it is there). */
export function useAddressImage(address: AppAddress, full = false): string | null {
  const [uri, setUri] = useState<{ key: string; uri: string | null } | null>(null);
  const key = `${address.id}-${address.lastUpdate}-${full}`;
  useEffect(() => {
    let cancelled = false;
    const show = (value: string | null) => {
      if (!cancelled && value != null) setUri({ key, uri: value });
    };
    void addresses.image(address, false).then((thumbnail) => {
      show(thumbnail);
      if (full) void addresses.image(address, true).then(show);
    });
    return () => {
      cancelled = true;
    };
  }, [address, full, key]);
  return uri?.key === key ? uri.uri : null;
}
