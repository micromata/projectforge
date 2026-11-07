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

// The menus of the user (MenuRest, /rs/menu): the app's menu of the pages tab. See src/rs/app-auth.ts for @server.

/** The menus of the logged-in user, built for their rights. @server org.projectforge.rest.MenuRest$Menus */
export interface Menus {
  mainMenu: Menu;
  favoritesMenu: Menu;
  /** One item with the user's full name, its entries (my account, customize menu, …, logout) below it. */
  myAccountMenu: Menu;
  /** The entries opened last, most recent first. Newer than the other menus. @optionalRead */
  recentMenu?: Menu;
}

/** @server org.projectforge.menu.Menu */
export interface Menu {
  menuItems: MenuItem[];
}

/** An entry (a page, with url) or a folder of entries (subMenu). @server org.projectforge.menu.MenuItem */
export interface MenuItem {
  /** The key the server reports a use by (POST /rs/menu/recent): qualified in the main menu (`COMMON.ADDRESS_LIST`). */
  key?: string | null;
  title?: string | null;
  /** A path of the server without the leading slash (`next/address`, `wa/…`), or another site (`https://…`). */
  url?: string | null;
  /** `REDIRECT` (default), `MODAL`, `DOWNLOAD` or `RESTCALL` (the logout: no page). */
  type?: string | null;
  badge?: MenuBadge | null;
  subMenu?: MenuItem[] | null;
}

/** @server org.projectforge.menu.MenuBadge */
export interface MenuBadge {
  counter?: number | null;
}

/** @request org.projectforge.rest.MenuRest$MenuUsage */
export interface MenuUsage {
  key: string;
}
