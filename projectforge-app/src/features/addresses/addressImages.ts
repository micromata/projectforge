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

import { File } from 'expo-file-system';

import { JsonCache } from '../../core/cache/jsonCache';
import type { ImageFiles } from './addressesRepository';

const dir = () => JsonCache.directory('addressImages');

/** The images of the addresses in the cache (deleted with it on logout). */
export const addressImageFiles: ImageFiles = {
  uri(name) {
    const file = new File(dir(), name);
    return file.exists ? file.uri : null;
  },
  write(name, bytes) {
    const file = new File(dir(), name);
    if (file.exists) file.delete();
    file.create();
    file.write(bytes);
    return file.uri;
  },
  names() {
    return dir()
      .list()
      .filter((it): it is File => it instanceof File)
      .map((it) => it.name);
  },
  delete(name) {
    const file = new File(dir(), name);
    if (file.exists) file.delete();
  },
};
