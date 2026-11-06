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

// Port of projectforge-flutter/test/features/web/web_download_test.dart.
import { fakeHttp } from '../../../testing/fakeHttp';
import { blobFile, fileName, isWalletPass, MAX_BLOB_BYTES, WebDownload, WebDownloadRefused } from '../webDownload';
import { WebNavigation } from '../webNavigation';

const fileUrl = new URL('https://pf.example.org/rs/chat/conversation/1/file/abc?download=true');

test('a Wallet pass is recognized by its type or its extension', () => {
  expect(isWalletPass('application/vnd.apple.pkpass')).toBe(true);
  expect(isWalletPass(null, 'Jane Doe.pkpass')).toBe(true);
  expect(isWalletPass('text/vcard', 'a.vcf')).toBe(false);
  expect(isWalletPass()).toBe(false);
});

describe('fileName', () => {
  test('Content-Disposition first, the RFC 5987 name before the plain one', () => {
    expect(
      fileName({
        contentDisposition: "attachment; filename=\"Bericht.pdf\"; filename*=UTF-8''Bericht%20%C3%A4.pdf",
        suggestedFilename: 'other.pdf',
        url: fileUrl,
      }),
    ).toBe('Bericht ä.pdf');
    expect(fileName({ contentDisposition: 'attachment; filename="Report.pdf"', url: fileUrl })).toBe('Report.pdf');
  });

  test('then the suggested name, the url and a default', () => {
    expect(fileName({ suggestedFilename: 'a.txt', url: fileUrl })).toBe('a.txt');
    expect(fileName({ url: fileUrl })).toBe('abc');
    expect(fileName({ url: new URL('https://pf.example.org/') })).toBe('download');
  });

  test('no path leaves the download directory', () => {
    expect(fileName({ contentDisposition: 'attachment; filename="../../etc/passwd"', url: fileUrl })).toBe('passwd');
    expect(
      fileName({ contentDisposition: 'attachment; filename=".."', suggestedFilename: '..\\x.txt', url: fileUrl }),
    ).toBe('x.txt');
  });
});

describe('load', () => {
  const navigation = new WebNavigation('https://pf.example.org');

  test('loads with the session cookie of the WebView, not the app token, without following redirects', async () => {
    const { http, requests } = fakeHttp(() => ({ body: 'content', headers: { 'content-type': 'text/plain' } }));
    const file = await new WebDownload(navigation, async () => 'JSESSIONID=web', http).load(fileUrl.href, 'f.txt');
    expect(requests[0].headers.Cookie).toBe('JSESSIONID=web');
    expect(requests[0].headers.Authorization).toBeUndefined();
    expect(requests[0].init.redirect).toBe('manual');
    expect(file.name).toBe('f.txt');
    expect(new TextDecoder().decode(file.bytes)).toBe('content');
  });

  test('a redirect is no download', async () => {
    const { http } = fakeHttp(() => ({ status: 302, headers: { location: 'https://evil.example.org/' } }));
    await expect(new WebDownload(navigation, async () => null, http).load(fileUrl.href)).rejects.toThrow('HTTP 302');
  });

  test.each(['https://other.example.org/file', 'http://pf.example.org/file', 'data:text/plain,x'])(
    'urls of another server are refused: %s',
    async (url) => {
      const { http, requests } = fakeHttp(() => ({ body: 'x' }));
      await expect(new WebDownload(navigation, async () => null, http).load(url)).rejects.toBeInstanceOf(
        WebDownloadRefused,
      );
      expect(requests).toHaveLength(0);
    },
  );
});

test('a blob of the pages becomes a file, its name without a path', () => {
  const file = blobFile({ name: '../Aufträge.xlsx', mime: 'application/vnd.ms-excel', data: btoa('PK\x03\x04') });
  expect(file?.name).toBe('Aufträge.xlsx');
  expect(Array.from(file!.bytes)).toEqual([0x50, 0x4b, 3, 4]);
  expect(file?.mimeType).toBe('application/vnd.ms-excel');
  expect(blobFile({ data: btoa('x') })?.name).toBe('download');
  expect(blobFile({ name: 'x' })).toBeNull();
  expect(blobFile({ data: 'x'.repeat(MAX_BLOB_BYTES * 2) })).toBeNull();
});
