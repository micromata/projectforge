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

import PfNative from '../../../modules/pf-native/src/PfNativeModule';
import { KeyValueStore, LegacyKeychain, SessionStore } from './sessionStore';

const OPTIONS: SecureStore.SecureStoreOptions = {
  // Not in backups or on another device, readable after the first unlock (push, background): the token belongs here.
  keychainAccessible: SecureStore.AFTER_FIRST_UNLOCK_THIS_DEVICE_ONLY,
};

/** The key chain (iOS) or the keystore backed storage (Android). */
export const secureStorage: KeyValueStore = {
  get: (key) => SecureStore.getItemAsync(key, OPTIONS),
  set: (key, value) => SecureStore.setItemAsync(key, value, OPTIONS),
  delete: (key) => SecureStore.deleteItemAsync(key, OPTIONS),
};

/** The service of flutter_secure_storage's items (its default IOSOptions). */
const FLUTTER_SERVICE = 'flutter_secure_storage_service';

const flutterKeychain: LegacyKeychain = {
  read: (key) => PfNative.readLegacyKeychain(FLUTTER_SERVICE, key),
  delete: (key) => PfNative.deleteLegacyKeychain(FLUTTER_SERVICE, key),
};

export const sessionStore = new SessionStore(secureStorage, flutterKeychain);
