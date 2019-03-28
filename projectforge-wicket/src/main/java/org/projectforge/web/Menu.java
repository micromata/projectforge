/////////////////////////////////////////////////////////////////////////////
//
// Project ProjectForge Community Edition
//         www.projectforge.org
//
// Copyright (C) 2001-2014 Kai Reinhard (k.reinhard@micromata.de)
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

package org.projectforge.web;

import java.io.Serializable;
import java.util.Collection;

/**
 * Helper for the web menu.
 */
public class Menu implements Serializable {
  private static final org.slf4j.Logger log = org.slf4j.LoggerFactory.getLogger(Menu.class);

  private static final long serialVersionUID = -4954464926815538198L;

  private final MenuEntry rootMenuEntry = new MenuEntry();

  public Menu() {
  }

  public Collection<MenuEntry> getMenuEntries() {
    return rootMenuEntry.getSubMenuEntries();
  }

  public MenuEntry findById(final String id) {
    return rootMenuEntry.findById(id);
  }

  public boolean isFirst(final MenuEntry entry) {
    return (rootMenuEntry.subMenuEntries != null && rootMenuEntry.subMenuEntries.size() > 0 && rootMenuEntry.subMenuEntries.iterator()
            .next() == entry);
  }

  public void addMenuEntry(MenuEntry entry) {
    rootMenuEntry.addMenuEntry(entry);
  }
}
