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

import * as SecureStore from 'expo-secure-store';

import { Session, sessionFromJson } from './session';

const KEY = 'pf.session';

/**
 * The login of the app, in the key chain (only this device, not in backups, readable after the first unlock) or the
 * Android keystore. One server only in this spike.
 */
export const SessionStore = {
  async load(): Promise<Session | null> {
    const json = await SecureStore.getItemAsync(KEY, options);
    return json ? sessionFromJson(JSON.parse(json)) : null;
  },

  save(session: Session): Promise<void> {
    return SecureStore.setItemAsync(KEY, JSON.stringify(session), options);
  },

  clear(): Promise<void> {
    return SecureStore.deleteItemAsync(KEY, options);
  },
};

const options: SecureStore.SecureStoreOptions = {
  keychainAccessible: SecureStore.AFTER_FIRST_UNLOCK_THIS_DEVICE_ONLY,
};
