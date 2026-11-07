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

import { bufferToBase64url, utf8 } from '@projectforge/shared/base64url';

/** Random bytes and SHA-256, given by the platform (expo-crypto in the app, node:crypto in the tests). */
export interface CryptoProvider {
  randomBytes(count: number): Uint8Array;
  sha256(data: Uint8Array): Promise<Uint8Array>;
}

/** Proof key for code exchange (RFC 7636) of the app login, see AppAuthService of the server. */
export interface Pkce {
  /** Never leaves the app. */
  verifier: string;
  /** S256 of the verifier, goes into the browser url. */
  challenge: string;
  state: string;
}

export async function challengeOf(verifier: string, crypto: CryptoProvider): Promise<string> {
  return bufferToBase64url(await crypto.sha256(utf8(verifier)));
}

export async function createPkce(crypto: CryptoProvider): Promise<Pkce> {
  const verifier = bufferToBase64url(crypto.randomBytes(32));
  return { verifier, challenge: await challengeOf(verifier, crypto), state: bufferToBase64url(crypto.randomBytes(24)) };
}
