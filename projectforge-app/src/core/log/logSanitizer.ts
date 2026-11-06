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

import { parseUrl } from '../url';

const MASK = '***';

/** `Authorization: PFApp …`, `Bearer …`. */
const AUTHORIZATION = /\b(PFApp|Bearer|Basic)\s+[^\s,;]+/gi;

/** A JSON Web Token (three base64url parts). */
const JWT = /\beyJ[A-Za-z0-9_-]+\.[A-Za-z0-9_-]+\.[A-Za-z0-9_-]*/g;

/** The names of secret parameters, queries, cookies and json fields. */
const SECRET_NAME =
  '(?:[A-Za-z_]*(?:token|password|passwd|secret|cookie)[A-Za-z0-9_]*|code|codeVerifier|code_verifier|verifier|challenge|state|otp|nonce|ticket|api_?key|authentik[A-Za-z0-9_]*|JSESSIONID|SESSION)';

/** `name=value` of a query or cookie. */
const SECRET_PARAMETER = new RegExp(`\\b(${SECRET_NAME})=([^\\s&;,]+)`, 'gi');

/** `"name": "value"` of json. */
const SECRET_JSON = new RegExp(`"(${SECRET_NAME})"(\\s*:\\s*)("[^"]*"|[^\\s,}]+)`, 'gi');

/**
 * Control characters other than the line break, further line breaks (NEL, line and paragraph separator) and the marks
 * of the text direction: a text of the server mustn't move the cursor, fake entries or reorder the text.
 */
const CONTROL = /[\x00-\x09\x0b-\x1f\x7f-\x9f\u200e\u200f\u2028\u2029\u202a-\u202e\u2066-\u2069]/g;

/** An url within a text (e.g. of an exception): passed through [url], so the values of its query are masked too. */
const URL_IN_TEXT = /https?:\/\/[^\s"<>]+/g;

/** A mail address (personal data). */
const MAIL = /[\w.+-]+@[\w-]+(\.[\w-]+)+/g;

/** The cookies of a `Cookie` header (`a=1; b=2`): `a=***; b=***`. */
const COOKIE_PAIR = /(^|;\s*)([^=;\s]+)=[^;]*/g;

/** A random token (e.g. `/next/public/datatransfer/<token>`): 20+ characters of base64url with letters and digits. */
const TOKEN_WORD_SOURCE =
  '(?<![A-Za-z0-9_-])(?=[A-Za-z0-9_-]*[A-Za-z])(?=[A-Za-z0-9_-]*[0-9])[A-Za-z0-9_-]{20,}(?![A-Za-z0-9_-])';
const TOKEN_WORD = new RegExp(TOKEN_WORD_SOURCE, 'g');
const TOKEN_SEGMENT = new RegExp(`^${TOKEN_WORD_SOURCE}$`);

/** The external access of an area (`/public/datatransfer/<token>/…`): everything after the area is its access. */
const PUBLIC_ACCESS = /(\/public\/[^/\s?#]+\/)[^\s?#&]+/g;

/** Urls that are personal data as a whole (the address, the number): only their scheme is logged. */
const PERSONAL_SCHEMES = new Set(['mailto:', 'tel:', 'sms:']);

const escapeRegExp = (value: string) => value.replace(/[.*+?^${}()|[\]\\]/g, '\\$&');

let extraNames = new Set<string>();
let extraParameter: RegExp | null = null;
let extraJson: RegExp | null = null;

/** A token, a segment after `public/<area>/`, or the packed parameters after `appLogin/`. */
function isSecretSegment(segments: string[], index: number): boolean {
  const segment = segments[index];
  if (!segment) return false;
  const pub = segments.indexOf('public');
  return (pub >= 0 && index >= pub + 2) || (index > 0 && segments[index - 1] === 'appLogin') || TOKEN_SEGMENT.test(segment);
}

/**
 * Keeps secrets out of the client log: no token, cookie value, code, verifier or password is written. Port of the
 * Flutter app's LogSanitizer. The log points pass urls through [url] and cookies through [cookieNames]; [text] is
 * applied to every message as a safety net (appLog does it).
 */
export const LogSanitizer = {
  MASK,

  /** The values of [names] are masked from now on (the cookies of the SSO proxy, named by the server). */
  addSecretNames(names: Iterable<string>) {
    const before = extraNames.size;
    extraNames = new Set([...extraNames, ...[...names].filter((it) => it.length > 0)]);
    if (extraNames.size === before) return;
    const alternatives = [...extraNames].map(escapeRegExp).join('|');
    extraParameter = new RegExp(`(?<![\\w-])(${alternatives})=([^\\s&;,]+)`, 'g');
    extraJson = new RegExp(`"(${alternatives})"(\\s*:\\s*)("[^"]*"|[^\\s,}]+)`, 'g');
  },

  /** [message] without anything that looks like a secret. */
  text(message: string): string {
    let result = message
      .replace(CONTROL, ' ')
      .replace(URL_IN_TEXT, (it) => (parseUrl(it) == null ? MASK : LogSanitizer.url(it)))
      .replace(AUTHORIZATION, (_, scheme: string) => `${scheme} ${MASK}`)
      .replace(JWT, MASK)
      .replace(MAIL, MASK)
      .replace(SECRET_PARAMETER, (_, name: string) => `${name}=${MASK}`)
      .replace(SECRET_JSON, (_, name: string, colon: string) => `"${name}"${colon}${MASK}`)
      .replace(PUBLIC_ACCESS, (_, area: string) => `${area}${MASK}`)
      .replace(TOKEN_WORD, MASK);
    if (extraParameter && extraJson) {
      result = result
        .replace(extraParameter, (_, name: string) => `${name}=${MASK}`)
        .replace(extraJson, (_, name: string, colon: string) => `"${name}"${colon}${MASK}`);
    }
    return result;
  },

  /**
   * Scheme, host, port and path of [url]; the values of its query are masked (their names show what arrived), user
   * info and fragment are dropped, tokens in the path masked.
   */
  url(value: string | URL | null | undefined): string {
    if (value == null) return '-';
    const url = typeof value === 'string' ? parseUrl(value) : value;
    if (url == null) return LogSanitizer.text(String(value));
    if (PERSONAL_SCHEMES.has(url.protocol)) return `${url.protocol}${MASK}`;
    const segments = url.pathname.split('/');
    const path = segments.map((it, i) => (isSecretSegment(segments, i) ? MASK : it)).join('/');
    const names = [...new Set(url.searchParams.keys())];
    const query = names.length ? `?${names.map((it) => `${it}=${MASK}`).join('&')}` : '';
    const authority = url.host ? `//${url.host}` : url.href.startsWith(`${url.protocol}//`) ? '//' : '';
    return `${url.protocol}${authority}${path}${query}`;
  },

  /** The names of cookies, never the values. */
  cookieNames(names: Iterable<string>): string {
    const list = [...names].sort();
    return list.length ? list.join(', ') : '-';
  },

  /** The names of the cookies of a `Cookie` header. */
  cookieHeader(header: string | null | undefined): string {
    return header == null ? '-' : header.replace(COOKIE_PAIR, (_, sep: string, name: string) => `${sep}${name}=${MASK}`);
  },
};
