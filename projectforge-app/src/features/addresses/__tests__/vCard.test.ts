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

import { imageTypeOf, vCardFileName, vCardWithPhoto } from '../vCard';

const VCARD = 'BEGIN:VCARD\r\nVERSION:3.0\r\nFN:Jane Doe\r\nEND:VCARD\r\n';
const JPEG = new Uint8Array([0xff, 0xd8, 0xff, 0xe0, ...new Array(200).fill(7)]);

describe('vCardWithPhoto', () => {
  it('embeds the photo before the end, folded into lines of 75 characters', () => {
    const result = vCardWithPhoto(VCARD, JPEG);
    const lines = result.split('\r\n');
    expect(lines.slice(0, 3)).toEqual(['BEGIN:VCARD', 'VERSION:3.0', 'FN:Jane Doe']);
    expect(lines[3]).toMatch(/^PHOTO;ENCODING=b;TYPE=JPEG:\/9j\/4/);
    const photoLines = lines.slice(3, lines.indexOf('END:VCARD'));
    expect(photoLines.length).toBeGreaterThan(1);
    expect(photoLines.every((it) => it.length <= 75)).toBe(true);
    expect(photoLines.slice(1).every((it) => it.startsWith(' '))).toBe(true);
    const unfolded = photoLines.map((it, i) => (i === 0 ? it : it.slice(1))).join('');
    expect(atob(unfolded.split(':')[1])).toHaveLength(JPEG.length);
    expect(result.endsWith('END:VCARD\r\n')).toBe(true);
  });

  it('keeps the vCard without a photo, with an unknown image or with a photo of its own', () => {
    expect(vCardWithPhoto(VCARD, null)).toBe(VCARD);
    expect(vCardWithPhoto(VCARD, new Uint8Array([1, 2, 3]))).toBe(VCARD);
    const withPhoto = VCARD.replace('END:VCARD', 'PHOTO;VALUE=uri:https://pf.example.com/p\r\nEND:VCARD');
    expect(vCardWithPhoto(withPhoto, JPEG)).toBe(withPhoto);
  });

  it('tells JPEG from PNG', () => {
    expect(imageTypeOf(JPEG)).toBe('JPEG');
    expect(imageTypeOf(new Uint8Array([0x89, 0x50, 0x4e, 0x47, 0x0d]))).toBe('PNG');
    expect(imageTypeOf(new Uint8Array([0x47, 0x49]))).toBeNull();
  });

  it('names the file safely', () => {
    expect(vCardFileName({ id: 1, firstName: 'Jörg', name: "O'Neil/Smith", lastUpdate: 1 })).toBe(
      'Jörg O_Neil_Smith.vcf',
    );
    expect(vCardFileName({ id: 1, lastUpdate: 1 })).toBe('contact.vcf');
  });
});
