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

import { Platform } from 'react-native';

import PfNative from '../../../modules/pf-native/src/PfNativeModule';
import { SharedFiles } from '../files/sharedFiles';

/**
 * Offers to add the contact of [vCard] (the bytes of a `.vcf` file) to the own contacts of the device: iOS shows the
 * prefilled form of a new contact, Android opens it with the contacts app. The file stays in the shared files (the
 * contacts app of Android reads it later), which the logout empties.
 */
export async function addToDeviceContacts(name: string, vCard: Uint8Array): Promise<void> {
  const file = SharedFiles.write(name, vCard);
  await PfNative.addContact(Platform.OS === 'android' ? file.contentUri : file.uri);
}
