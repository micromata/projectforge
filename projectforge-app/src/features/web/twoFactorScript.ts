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

/** The event next's 2FA dialog waits for (`APP_2FA_EVENT` of projectforge-next/lib/app-bridge.ts). */
const EVENT = 'pf-app-2fa';

/**
 * The answer to the page's `{"pf":"twoFactor"}`: [ok] if the second factor was confirmed in the browser and the new web
 * session is set, so the page repeats its refused request.
 */
export function twoFactorAnswerScript(ok: boolean): string {
  return `(function(){window.dispatchEvent(new CustomEvent(${JSON.stringify(EVENT)},{detail:{ok:${ok}}}));})();true;`;
}
