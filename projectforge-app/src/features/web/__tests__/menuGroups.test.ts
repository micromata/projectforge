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

import type { PageCache } from "../lastPage";
import { MenuGroups } from "../menuGroups";

function memoryCache(): PageCache & { files: Map<string, unknown> } {
  const files = new Map<string, unknown>();
  return {
    files,
    readSync<T>(name: string) {
      return (files.get(name) as T) ?? null;
    },
    write(name, json) {
      files.set(name, json);
    },
  };
}

describe("the categories of the main menu the user toggled", () => {
  test("come back the next time", () => {
    const cache = memoryCache();
    expect(new MenuGroups(cache).restore()).toEqual({});
    new MenuGroups(cache).remember({ FIBU: false, COMMON: true });
    expect(new MenuGroups(cache).restore()).toEqual({
      FIBU: false,
      COMMON: true,
    });
  });

  test("broken states are dropped", () => {
    const cache = memoryCache();
    const groups = new MenuGroups(cache);
    cache.files.set("webMenuGroups", "broken");
    expect(groups.restore()).toEqual({});
    cache.files.set("webMenuGroups", ["FIBU"]);
    expect(groups.restore()).toEqual({});
    cache.files.set("webMenuGroups", { FIBU: "yes", COMMON: false });
    expect(groups.restore()).toEqual({ COMMON: false });
  });
});
