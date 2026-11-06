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

import { createHash, randomBytes } from 'node:crypto';

import { HttpFetch } from '../core/api/http';
import { CryptoProvider } from '../core/auth/pkce';

export interface RecordedRequest {
  url: string;
  init: RequestInit;
  headers: Record<string, string>;
  body: unknown;
}

export interface FakeAnswer {
  status?: number;
  body?: unknown;
  headers?: Record<string, string>;
}

/** A fetch answering by [handler] and recording the requests (the Flutter tests' FakeAdapter). */
export function fakeHttp(handler: (request: RecordedRequest) => FakeAnswer) {
  const requests: RecordedRequest[] = [];
  const http: HttpFetch = async (url, init) => {
    const request: RecordedRequest = {
      url,
      init,
      headers: { ...(init.headers as Record<string, string> | undefined) },
      body: typeof init.body === 'string' ? JSON.parse(init.body) : init.body,
    };
    requests.push(request);
    const answer = handler(request);
    const isJson = answer.body !== undefined && typeof answer.body !== 'string';
    return new Response(isJson ? JSON.stringify(answer.body) : ((answer.body as string | undefined) ?? null), {
      status: answer.status ?? 200,
      headers: { ...(isJson ? { 'content-type': 'application/json' } : {}), ...answer.headers },
    });
  };
  return { http, requests };
}

export const nodeCrypto: CryptoProvider = {
  randomBytes: (count) => new Uint8Array(randomBytes(count)),
  sha256: async (data) => new Uint8Array(createHash('sha256').update(data).digest()),
};
