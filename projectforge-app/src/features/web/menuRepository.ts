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

import type { Menus, MenuUsage } from '@projectforge/shared/rs/menu';

import type { ApiClient } from '../../core/api/apiClient';
import { appLog } from '../../core/log/appLog';

/** The menus of the user (MenuRest `/rs/menu`), for the menu of the pages tab. */
export class MenuRepository {
  private static readonly PATH = '/rs/menu';

  constructor(private readonly api: Pick<ApiClient, 'get' | 'post'>) {}

  load(): Promise<Menus> {
    return this.api.get<Menus>(MenuRepository.PATH);
  }

  /** Reports an entry as opened (the entries opened last, shared with the web pages); a failure is only logged. */
  reportUsage(key: string): void {
    const body: MenuUsage = { key };
    this.api
      .post(`${MenuRepository.PATH}/recent`, { body })
      .catch((e: unknown) => appLog.warn('menu', `Use of a menu entry not reported: ${String(e)}`));
  }
}
