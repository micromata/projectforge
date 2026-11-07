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

import type { ErrorMessage, ResponseAction, TwoFactorRequired, ValidationError } from '@projectforge/shared/rs/errors';

import { isRedirect } from './http';

export type { TwoFactorRequired, ValidationError };

/** The text of a refusal by an SSO proxy, see [isProxyRefusal]. */
export const PROXY_REFUSED_KEY = 'app.proxy.refused';

/**
 * An answer of an SSO proxy in front of the server, not of ProjectForge: a 401 without the json of the server
 * (RestAuthenticationUtils.sendError), or a redirect (to the proxy's login; the api itself never redirects). Told by
 * the content type, not by the parsed body.
 */
export function isProxyRefusal(response: Response | null | undefined): boolean {
  if (response == null) return false;
  if (isRedirect(response)) return true;
  const contentType = response.headers.get('content-type') ?? '';
  return response.status === 401 && !contentType.includes('json');
}

/** A failed call of the server (the Flutter app's ApiException). */
export class ApiException extends Error {
  constructor(
    message: string,
    readonly statusCode?: number,
    readonly validationErrors: ValidationError[] = [],
    readonly messageKey?: string,
  ) {
    super(message);
  }

  /** The server's answer: the validation errors of a 406, the message of the json error body, or its status. */
  static async fromResponse(response: Response): Promise<ApiException> {
    if (isProxyRefusal(response)) {
      return new ApiException(`HTTP ${response.status}`, response.status, [], PROXY_REFUSED_KEY);
    }
    let data: unknown = null;
    if ((response.headers.get('content-type') ?? '').includes('json')) {
      data = await response.json().catch(() => null);
    }
    if (data != null && typeof data === 'object') {
      const json = data as ResponseAction & ErrorMessage;
      const errors = json.validationErrors ?? [];
      const message = errors
        .map((it) => it.message)
        .filter((it): it is string => !!it)
        .join('\n');
      return new ApiException(message || json.message || `HTTP ${response.status}`, response.status, errors);
    }
    return new ApiException(`HTTP ${response.status}`, response.status);
  }
}
