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

/** The page of next where passkeys are registered (`{"pf":"passkeySetup"}` of the page opens it in the browser). */
const PASSKEY_SETUP_PATH = '/next/2FASetup';

/**
 * The address of the passkey setup at [server]. The WebView can't register passkeys (the relying party is the domain of
 * the server, which a store app has no association with), so the system browser shows it: the user logs in there.
 */
export function passkeySetupUrl(server: URL): string {
  return `${server.toString().replace(/\/+$/, '')}${PASSKEY_SETUP_PATH}`;
}
