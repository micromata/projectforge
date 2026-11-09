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

import { JsonCache } from "../../core/cache/jsonCache";
import type { PageCache } from "./lastPage";

const NAME = "webMenuGroups";

/** The states of the categories of the main menu the user toggled: open (true) or closed (false), by their key. */
export type GroupStates = Readonly<Record<string, boolean>>;

/**
 * The categories of the main menu the user opened or closed, shown so again the next time (also after a restart). A
 * category never toggled follows the default of the screen (see WebMenu). Kept in the cache as the page shown last is:
 * data of the login, deleted on a logout.
 */
export class MenuGroups {
  constructor(private readonly cache: PageCache = JsonCache) {}

  remember(states: GroupStates) {
    this.cache.write(NAME, states);
  }

  /** The states remembered, none if there are none (or they are broken). */
  restore(): GroupStates {
    let json: unknown = null;
    try {
      json = this.cache.readSync<unknown>(NAME);
    } catch {
      // No cache to read: as if nothing was toggled.
    }
    if (json == null || typeof json !== "object" || Array.isArray(json))
      return {};
    return Object.fromEntries(
      Object.entries(json).filter(
        (it): it is [string, boolean] => typeof it[1] === "boolean",
      ),
    );
  }
}

export const menuGroups = new MenuGroups();
