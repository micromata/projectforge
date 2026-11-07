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

import * as Sharing from 'expo-sharing';
import { Directory, File, Paths } from 'expo-file-system';

import { appLog } from '../log/appLog';

/**
 * The temporary folder of the files the app hands to the share sheet or the Wallet (downloads of the pages, vCards,
 * passes). Emptied by the logout: they may be confidential (chat files). Port of the Flutter app's SharedFiles.
 */
export const SharedFiles = {
  /** Writes [bytes] as [name] into the folder (replacing a file of the same name). */
  write(name: string, bytes: Uint8Array): File {
    const dir = sharedDir();
    dir.create({ intermediates: true, idempotent: true });
    const file = new File(dir, name);
    if (file.exists) file.delete();
    file.create();
    file.writeSync(bytes);
    return file;
  },

  /** Offers [bytes] as file [name] in the share sheet (mail, messenger, AirDrop, …). */
  async share(name: string, bytes: Uint8Array, mimeType?: string): Promise<void> {
    const file = SharedFiles.write(name, bytes);
    await Sharing.shareAsync(file.uri, { mimeType });
  },

  clearAll(): void {
    try {
      const dir = sharedDir();
      if (dir.exists) dir.delete();
    } catch (e) {
      appLog.warn('files', `Shared files not deleted: ${String(e)}`);
    }
  },
};

const sharedDir = () => new Directory(Paths.cache, 'shared');
