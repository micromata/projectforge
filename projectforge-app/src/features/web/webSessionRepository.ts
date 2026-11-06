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

import type { WebSessionResponse } from '@projectforge/shared/rs/app-auth';

import { ApiClient } from '../../core/api/apiClient';
import { setCookieHeaders } from '../../core/api/http';
import { appLog, LogSanitizer } from '../../core/log/appLog';
import { WebCookies } from '../../core/web/webCookies';
import { WebNavigation } from './webNavigation';

/**
 * The web session of the WebView (AppAuthRest.webSession, `POST /rs/appAuth/webSession`): the server opens it for
 * the app token and sends its cookie, which goes into the WebView. All WebViews share one cookie jar and so one
 * session: a tab takes the opened one ([ensure]), a renewal is done once for all of them ([renew]).
 */
export class WebSessionRepository {
  private session: Promise<string> | null = null;
  private pending: Promise<string> | null = null;

  constructor(
    private readonly api: ApiClient,
    private readonly cookies: WebCookies,
    private readonly proxyCookies: () => Record<string, string>,
  ) {}

  /** The opened web session, or a new one. Answers the path of the start page. */
  ensure(): Promise<string> {
    return (this.session ??= this.open());
  }

  /** A new web session (the old one is gone). Concurrent renewals share one request. */
  renew(renewLogin = true): Promise<string> {
    if (this.pending) return this.pending;
    const renewal = this.open(renewLogin);
    this.pending = renewal;
    this.session = renewal;
    return renewal.finally(() => (this.pending = null));
  }

  private async open(renewLogin = true): Promise<string> {
    try {
      const response = await this.api.postForResponse('/rs/appAuth/webSession', { renewLogin });
      const headers = setCookieHeaders(response);
      await this.cookies.setProxyCookies(this.api.serverUrl, this.proxyCookies());
      const names = await this.cookies.setCookies(this.api.serverUrl, headers);
      const json = (await response.json().catch(() => ({}))) as Partial<WebSessionResponse>;
      const startUrl = json.startUrl ?? WebNavigation.defaultPath;
      appLog.info(
        'web',
        `Web session opened, cookies: ${LogSanitizer.cookieNames(names)} (${headers.length} set-cookie), ` +
          `proxy cookies: ${LogSanitizer.cookieNames(Object.keys(this.proxyCookies()))}, start: ${LogSanitizer.url(startUrl)}`,
      );
      if (!names.length) appLog.warn('web', 'Web session without a cookie: the WebView will show the login page');
      return startUrl;
    } catch (e) {
      appLog.warn('web', `Web session not opened: ${String(e)}`);
      this.session = null;
      throw e;
    }
  }
}
