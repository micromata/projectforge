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

import { HTTP_DEFAULTS, HttpFetch, isRedirect } from '../../core/api/http';
import { parseUrl } from '../../core/url';
import { WebNavigation } from './webNavigation';

/** The page asked for a download of another server. */
export class WebDownloadRefused extends Error {
  constructor(readonly url: string) {
    super(`Download of another server refused: ${url}`);
  }
}

export interface DownloadedFile {
  name: string;
  bytes: Uint8Array;
  mimeType?: string;
}

/**
 * A download started by the pages in the WebView (a chat file, …), port of the Flutter app's `web_download.dart`:
 * the app loads the file and offers it in the share sheet. The request carries the WebView's session cookie, never
 * the app token; only urls of the own server, redirects aren't followed, so the cookie goes nowhere else.
 */
export class WebDownload {
  constructor(
    private readonly navigation: WebNavigation,
    private readonly cookieHeader: (url: string) => Promise<string | null>,
    private readonly http: HttpFetch,
  ) {}

  async load(url: string, suggestedFilename?: string): Promise<DownloadedFile> {
    const target = parseUrl(url);
    if (target == null || !this.navigation.isSameOrigin(target)) throw new WebDownloadRefused(url);
    const cookie = await this.cookieHeader(target.href);
    const response = await this.http(target.href, {
      ...HTTP_DEFAULTS,
      headers: cookie ? { Cookie: cookie } : {},
    });
    if (isRedirect(response) || !response.ok) throw new Error(`HTTP ${response.status}`);
    const bytes = new Uint8Array(await response.arrayBuffer());
    return {
      name: fileName({
        contentDisposition: response.headers.get('content-disposition'),
        suggestedFilename,
        url: target,
      }),
      bytes,
      mimeType: response.headers.get('content-type') ?? undefined,
    };
  }
}

/** The download is an Apple Wallet pass, which is added to the Wallet rather than shared. */
export function isWalletPass(mimeType?: string | null, name?: string | null): boolean {
  return (
    mimeType?.toLowerCase().startsWith('application/vnd.apple.pkpass') === true ||
    name?.toLowerCase().endsWith('.pkpass') === true
  );
}

/**
 * The name of the file: `filename*` (RFC 5987) or `filename` of [contentDisposition], [suggestedFilename], the last
 * segment of [url], or `download`. Without any path, so it stays in the download directory.
 */
export function fileName(opts: {
  contentDisposition?: string | null;
  suggestedFilename?: string | null;
  url: URL;
}): string {
  const segments = opts.url.pathname.split('/').filter((it) => it.length > 0);
  let last: string | undefined = segments[segments.length - 1];
  try {
    last = last == null ? undefined : decodeURIComponent(last);
  } catch {
    // Kept as it is.
  }
  for (const candidate of [dispositionName(opts.contentDisposition), opts.suggestedFilename, last]) {
    const name = sanitize(candidate);
    if (name != null) return name;
  }
  return 'download';
}

function dispositionName(header?: string | null): string | null {
  if (header == null) return null;
  const extended = /filename\*\s*=\s*([^']*)'[^']*'([^;]+)/i.exec(header);
  if (extended) {
    try {
      return decodeURIComponent(extended[2].trim());
    } catch {
      // Falls back to the plain filename.
    }
  }
  const plain = /filename\s*=\s*(?:"([^"]*)"|([^;]+))/i.exec(header);
  return plain == null ? null : (plain[1] ?? plain[2])?.trim() ?? null;
}

function sanitize(name?: string | null): string | null {
  if (name == null) return null;
  const parts = name.split(/[/\\]/);
  const clean = parts[parts.length - 1].replace(/[\x00-\x1f\x7f:*?"<>|]/g, '_').trim();
  if (!clean || clean === '.' || clean === '..') return null;
  return clean;
}
