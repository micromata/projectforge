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

import type { AppAddress, AppPostalAddress } from '@projectforge/shared/rs/address';

export type { AppAddress, AppPostalAddress } from '@projectforge/shared/rs/address';

const filled = (value?: string | null): value is string => value != null && value.trim().length > 0;

/** The lines of a postal address as written on an envelope. */
export function postalLines(postal?: AppPostalAddress | null): string[] {
  if (postal == null) return [];
  return [
    postal.addressText,
    postal.addressText2,
    [postal.zipCode, postal.city].filter(filled).join(' '),
    [postal.state, postal.country].filter(filled).join(', '),
  ].filter(filled);
}

/** One phone number of an address with the i18n key of its kind. */
export interface PhoneNumber {
  kind: 'business' | 'mobile' | 'private' | 'privateMobile' | 'fax';
  number: string;
  /** A text message can be sent to it. */
  mobile: boolean;
}

const PHONE_FIELDS: [keyof AppAddress, PhoneNumber['kind'], boolean][] = [
  ['businessPhone', 'business', false],
  ['mobilePhone', 'mobile', true],
  ['privatePhone', 'private', false],
  ['privateMobilePhone', 'privateMobile', true],
  ['fax', 'fax', false],
];

export function phones(address: AppAddress): PhoneNumber[] {
  return PHONE_FIELDS.flatMap(([field, kind, mobile]) => {
    const value = address[field];
    return typeof value === 'string' && filled(value) ? [{ kind, number: value.trim(), mobile }] : [];
  });
}

/** The e-mail addresses, the private one marked. */
export function emails(address: AppAddress): { address: string; private: boolean }[] {
  return [
    ...(filled(address.email) ? [{ address: address.email.trim(), private: false }] : []),
    ...(filled(address.privateEmail) ? [{ address: address.privateEmail.trim(), private: true }] : []),
  ];
}

/** "Doe, Jane" by which the list is sorted (and shown): the last name, then the first name; else the organization. */
export function sortKey(address: AppAddress): string {
  const person = [address.name, address.firstName].filter(filled).join(', ');
  return person || (address.organization ?? '').trim();
}

/** "Dr. Jane Doe", or the organization if the address has no person. */
export function displayName(address: AppAddress): string {
  const person = [address.title, address.firstName, address.name].filter(filled).join(' ');
  return person || (address.organization ?? '').trim();
}

/** The second line of a list row: the organization of a person. */
export function subtitle(address: AppAddress): string | null {
  const person = [address.name, address.firstName].some(filled);
  return person && filled(address.organization) ? address.organization.trim() : null;
}

/** Two letters for the avatar. */
export function initials(address: AppAddress): string {
  const person = [address.firstName, address.name].filter(filled);
  const source = person.length > 0 ? person : [address.organization ?? ''];
  return source
    .map((it) => it.trim())
    .filter((it) => it.length > 0)
    .map((it) => it.substring(0, 1).toUpperCase())
    .slice(0, 2)
    .join('');
}

/** "A" to "Z" for the section of the list, "#" for everything else. */
export function sectionLetter(address: AppAddress): string {
  const key = sortKey(address);
  if (!key) return '#';
  const letter = foldText(key.substring(0, 1)).toUpperCase();
  return /^[A-Z]$/.test(letter) ? letter : '#';
}

/**
 * Every word of [query] is part of the name, the organization, an e-mail or a city; digits are also looked for in the
 * phone numbers (without spaces and signs).
 */
export function matches(address: AppAddress, query: string): boolean {
  const words = query
    .toLowerCase()
    .split(/\s+/)
    .filter((it) => it.length > 0);
  if (words.length === 0) return true;
  const text = foldText(
    [
      address.title,
      address.firstName,
      address.name,
      address.organization,
      address.division,
      address.positionText,
      address.email,
      address.privateEmail,
      address.business?.city,
      address.postal?.city,
      address.private?.city,
    ]
      .filter((it): it is string => it != null)
      .join(' '),
  );
  const digits = phones(address)
    .map((it) => it.number.replace(/[^0-9]/g, ''))
    .join(' ');
  return words.every((word) => {
    if (text.includes(foldText(word))) return true;
    const wordDigits = word.replace(/[^0-9]/g, '');
    return wordDigits.length > 0 && wordDigits.length === word.replace(/\+/g, '').length && digits.includes(wordDigits);
  });
}

const FOLDS: Record<string, string> = {
  ä: 'a', ö: 'o', ü: 'u', ß: 's', á: 'a', à: 'a', â: 'a', é: 'e', è: 'e', ê: 'e', ë: 'e', í: 'i', ì: 'i', î: 'i',
  ï: 'i', ó: 'o', ò: 'o', ô: 'o', ú: 'u', ù: 'u', û: 'u', ç: 'c', ñ: 'n', å: 'a', ø: 'o', ł: 'l', š: 's', ž: 'z',
  č: 'c', ř: 'r', ő: 'o', ű: 'u',
};

/** Lower case without umlauts and accents, so "Müller" is found by "muller". */
export function foldText(text: string): string {
  let result = '';
  for (const char of text.toLowerCase()) result += FOLDS[char] ?? char;
  return result;
}

/** The address book stored on the device: the addresses sorted for the list, up to date as of [timestamp]. */
export interface AddressBook {
  /** The `modifiedSince` of the next request. */
  timestamp: number;
  addresses: AppAddress[];
  favoriteIds: number[];
  /** The version of the stored form (see AddressesRepository.FORMAT): an older one is loaded anew. */
  format?: number;
  /** The server wasn't reachable: the addresses are the stored ones. */
  fromCache?: boolean;
}

/** "#" comes after "Z", as in the list of Apple. */
export function compareAddresses(a: AppAddress, b: AppAddress): number {
  const aLast = sectionLetter(a) === '#';
  const bLast = sectionLetter(b) === '#';
  if (aLast !== bLast) return aLast ? 1 : -1;
  const x = foldText(sortKey(a));
  const y = foldText(sortKey(b));
  return x < y ? -1 : x > y ? 1 : 0;
}

export function addressBook(timestamp: number, addresses: AppAddress[], favoriteIds: number[] = []): AddressBook {
  return { timestamp, addresses: [...addresses].sort(compareAddresses), favoriteIds };
}

/**
 * The book after a sync: the changed addresses replace the stored ones, the ones not in [ids] are gone. Null if
 * [ids] holds addresses the book doesn't have and that aren't among [changed] (a newly visible address that wasn't
 * modified): then everything has to be loaded.
 */
export function mergeAddressBook(
  book: AddressBook,
  timestamp: number,
  ids: number[],
  changed: AppAddress[],
  favoriteIds: number[] = [],
): AddressBook | null {
  const byId = new Map(book.addresses.map((it) => [it.id, it]));
  for (const it of changed) byId.set(it.id, it);
  if (ids.some((id) => !byId.has(id))) return null;
  const keep = new Set(ids);
  return addressBook(
    timestamp,
    [...byId.values()].filter((it) => keep.has(it.id)),
    favoriteIds,
  );
}

/** The two colours of the avatar's gradient (hsl): always the same for an address. */
export function gradientOf(address: AppAddress): [string, string] {
  const hue = (address.id * 47) % 360;
  return [`hsl(${hue}, 45%, 68%)`, `hsl(${(hue + 25) % 360}, 45%, 48%)`];
}

/** A list row (an address) or the header of a section (its letter). */
export type ListItem = { type: 'header'; letter: string } | { type: 'address'; address: AppAddress };

/** The letter of each section followed by its addresses (sorted already). */
export function listItems(addresses: AppAddress[]): ListItem[] {
  const items: ListItem[] = [];
  let letter: string | null = null;
  for (const address of addresses) {
    const current = sectionLetter(address);
    if (current !== letter) {
      letter = current;
      items.push({ type: 'header', letter });
    }
    items.push({ type: 'address', address });
  }
  return items;
}

/** The index of the first section at or after [letter] ("#" after "Z"), for the index bar; -1 if there is none. */
export function sectionIndex(items: ListItem[], letter: string): number {
  const rank = (it: string) => (it === '#' ? 1000 : it.charCodeAt(0));
  const found = items.findIndex((it) => it.type === 'header' && rank(it.letter) >= rank(letter));
  return found >= 0 ? found : items.length - 1;
}

/** The offset of every item in the list, whose headers and rows have fixed heights. */
export function itemOffsets(items: ListItem[], headerHeight: number, rowHeight: number): number[] {
  const offsets: number[] = [];
  let offset = 0;
  for (const it of items) {
    offsets.push(offset);
    offset += it.type === 'header' ? headerHeight : rowHeight;
  }
  return offsets;
}
