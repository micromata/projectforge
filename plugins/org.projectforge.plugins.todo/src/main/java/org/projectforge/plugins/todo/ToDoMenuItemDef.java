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

package org.projectforge.plugins.todo;

import org.apache.wicket.Page;
import org.projectforge.web.MenuBuilderContext;
import org.projectforge.web.MenuEntry;
import org.projectforge.web.MenuItemDef;

/**
 * @author Kai Reinhard (k.reinhard@micromata.de)
 */
public class ToDoMenuItemDef extends MenuItemDef
{
  private static final long serialVersionUID = -7535509328303694730L;

  ToDoMenuItemDef(final MenuItemDef parentMenu, final String id, final int order, final String i18nKey,
      final Class<? extends Page> pageClass)
  {
    super(parentMenu, id, order, i18nKey, pageClass);
  }

  @Override
  protected void afterMenuEntryCreation(final MenuEntry createdMenuEntry, final MenuBuilderContext context)
  {
    createdMenuEntry.setNewCounterModel(new MenuCounterOpenToDos());
  }
}
