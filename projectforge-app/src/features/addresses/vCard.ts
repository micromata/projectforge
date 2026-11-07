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

import { AppAddress, displayName } from './address';

/** The longest line of a vCard (RFC 2426 §2.6): longer ones continue on the next line after a space. */
const LINE_LENGTH = 75;

/** The type of an image for its PHOTO line, by its first bytes; null if it is no JPEG or PNG. */
export function imageTypeOf(bytes: Uint8Array): 'JPEG' | 'PNG' | null {
  if (bytes[0] === 0xff && bytes[1] === 0xd8) return 'JPEG';
  if (bytes[0] === 0x89 && bytes[1] === 0x50 && bytes[2] === 0x4e && bytes[3] === 0x47) return 'PNG';
  return null;
}

function base64(bytes: Uint8Array): string {
  let raw = '';
  // In parts: String.fromCharCode with all bytes of a photo at once would exceed the arguments of a call.
  for (let i = 0; i < bytes.length; i += 0x8000) {
    raw += String.fromCharCode(...bytes.subarray(i, i + 0x8000));
  }
  return btoa(raw);
}

/**
 * [vCard] (3.0, as the server sends it without a photo) with [photo] as embedded PHOTO, the way the server's export
 * embeds it. Without a photo, of an unknown type, or if the vCard has one already, [vCard] as is.
 */
export function vCardWithPhoto(vCard: string, photo: Uint8Array | null): string {
  const type = photo ? imageTypeOf(photo) : null;
  if (photo == null || type == null || /^PHOTO[;:]/m.test(vCard)) return vCard;
  const line = `PHOTO;ENCODING=b;TYPE=${type}:${base64(photo)}`;
  const folded = [line.slice(0, LINE_LENGTH)];
  for (let i = LINE_LENGTH; i < line.length; i += LINE_LENGTH - 1)
    folded.push(` ${line.slice(i, i + LINE_LENGTH - 1)}`);
  const end = vCard.lastIndexOf('END:VCARD');
  if (end < 0) return vCard;
  return `${vCard.slice(0, end)}${folded.join('\r\n')}\r\n${vCard.slice(end)}`;
}

/** The name of the shared vCard file of [address], without characters a file system or a mail program would refuse. */
export function vCardFileName(address: AppAddress): string {
  const name = displayName(address) || 'contact';
  return `${name.replace(/[^\p{L}\p{N}_\- ]+/gu, '_')}.vcf`;
}
