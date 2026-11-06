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

// The URL parser of every decision about a url: WHATWG conformant, as the WebView parses. The global URL of React
// Native (Libraries/Blob/URL.js) is a set of regular expressions: `new URL('//evil.example.org/x', base)` gives
// `https://pf.example.org//evil.example.org/x` there, while the WebView loads evil.example.org. Never use the global
// one for anything that decides where a request or a page goes.
import { URL as WhatwgUrl, URLSearchParams as WhatwgUrlSearchParams } from 'react-native-url-polyfill';

export const SafeURL: typeof globalThis.URL = WhatwgUrl as unknown as typeof globalThis.URL;
export const SafeURLSearchParams: typeof globalThis.URLSearchParams =
  WhatwgUrlSearchParams as unknown as typeof globalThis.URLSearchParams;

/** Parses [value] (relative to [base]), null if it is no url. */
export function parseUrl(value: string, base?: string): URL | null {
  try {
    return new SafeURL(value, base);
  } catch {
    return null;
  }
}
