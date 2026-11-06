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

import { Redirect } from 'expo-router';

import { useAuth } from '../core/auth/authController';

/**
 * The start: the tabs when logged in, otherwise the login. Explicit, because with the tabs protected the router would
 * otherwise fall back to the first route allowed, in the order of the files.
 */
export default function Start() {
  const auth = useAuth();
  return <Redirect href={auth.status === 'loggedIn' ? '/pages' : '/login'} />;
}
