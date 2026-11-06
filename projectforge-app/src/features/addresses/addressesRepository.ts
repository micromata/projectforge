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

import type { AppAddressList } from '@projectforge/shared/rs/address';

import type { ApiClient } from '../../core/api/apiClient';
import { ApiException } from '../../core/api/apiException';
import { AddressBook, addressBook, mergeAddressBook } from './address';

/** The part of JsonCache the repository uses. */
export interface BookCache {
  read<T>(name: string): Promise<T | null>;
  write(name: string, json: unknown): void;
}

/** The images of the addresses on the device, by file name (see [imageName]). */
export interface ImageFiles {
  /** The `file://` url of the stored image, null if there is none. */
  uri(name: string): string | null;
  write(name: string, bytes: Uint8Array): string;
  names(): string[];
  delete(name: string): void;
}

/** The name of the stored image of an address in a version, the thumbnail or ([full]) the image itself. */
export const imageName = (id: number, version: number, full = false) => `${id}-${version}${full ? '-full' : ''}.img`;

/** Not reachable at all (no network, wrong host), unlike an answer of the server. */
const isOffline = (error: unknown) => !(error instanceof ApiException) || error.statusCode == null;

/**
 * The address book (AddressEntityRest, `/rs/address/appList`). All addresses the user may see are kept on the device,
 * with their thumbnails, so an address can also be used where there is no network; the server only delivers what
 * changed since the last sync. Port of the Flutter app's AddressesRepository.
 */
export class AddressesRepository {
  private static readonly PATH = '/rs/address';
  private static readonly CACHE_NAME = 'addresses';

  constructor(
    private readonly api: Pick<ApiClient, 'get' | 'download'>,
    private readonly cache: BookCache,
    private readonly images: ImageFiles,
  ) {}

  /** The stored address book, null if there is none (yet). Not marked as offline: the server isn't asked. */
  async loadCached(): Promise<AddressBook | null> {
    const cached = await this.cache.read<AddressBook>(AddressesRepository.CACHE_NAME);
    return cached == null ? null : { ...cached, fromCache: false };
  }

  /** Brings [stored] up to date. Without a connection it stays as it is (marked as such); another failure is thrown. */
  async sync(stored: AddressBook | null): Promise<AddressBook> {
    try {
      const book = (stored == null ? null : await this.syncChanges(stored)) ?? (await this.loadAll());
      this.cache.write(AddressesRepository.CACHE_NAME, { ...book, fromCache: undefined });
      this.deleteObsoleteImages(book);
      return book;
    } catch (e) {
      if (!isOffline(e) || stored == null) throw e;
      return { ...stored, fromCache: true };
    }
  }

  private async syncChanges(stored: AddressBook): Promise<AddressBook | null> {
    const list = await this.api.get<AppAddressList>(
      `${AddressesRepository.PATH}/appList?modifiedSince=${stored.timestamp}`,
    );
    return mergeAddressBook(stored, list.timestamp, list.ids, list.addresses, list.favoriteIds ?? []);
  }

  private async loadAll(): Promise<AddressBook> {
    const list = await this.api.get<AppAddressList>(`${AddressesRepository.PATH}/appList`);
    return addressBook(list.timestamp, list.addresses, list.favoriteIds ?? []);
  }

  /**
   * The thumbnail of the address ([full]: the image itself) as `file://` url, from the device or (once) from the
   * server. Null if there is none, or it isn't reachable now.
   */
  async image(id: number, version: number, full = false): Promise<string | null> {
    const name = imageName(id, version, full);
    const stored = this.images.uri(name);
    if (stored != null) return stored;
    try {
      const bytes = await this.api.download(`${AddressesRepository.PATH}/${full ? 'image' : 'imagePreview'}/${id}`, {}, {
        renewLogin: false,
      });
      return this.images.write(name, bytes);
    } catch {
      return null;
    }
  }

  /** Deletes the images of removed addresses and older versions. */
  private deleteObsoleteImages(book: AddressBook) {
    const current = new Set(
      book.addresses
        .filter((it) => it.hasImage)
        .flatMap((it) => [imageName(it.id, it.lastUpdate), imageName(it.id, it.lastUpdate, true)]),
    );
    for (const name of this.images.names()) {
      if (!current.has(name)) this.images.delete(name);
    }
  }
}
