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

package org.projectforge.web.core;

import org.apache.wicket.markup.html.panel.Panel;
import org.projectforge.web.wicket.components.ContentMenuEntryPanel;
import org.projectforge.web.wicket.flowlayout.IconType;
import org.projectforge.web.wicket.flowlayout.MyComponentsRepeater;

/**
 * Display a drop-down menu
 * @author Kai Reinhard (k.reinhard@micromata.de)
 */
public class MenuBarPanel extends Panel
{
  private static final long serialVersionUID = -8693164616993515262L;

  private ContentMenuEntryPanel extendedMenuEntry;

  /**
   * List to create content menu in the desired order before creating the RepeatingView.
   */
  protected MyComponentsRepeater<ContentMenuEntryPanel> contentMenu;

  public MenuBarPanel(final String id)
  {
    super(id);
    contentMenu = new MyComponentsRepeater<ContentMenuEntryPanel>("repeater");
    add(contentMenu.getRepeatingView());
  }

  @Override
  protected void onBeforeRender()
  {
    contentMenu.render();
    super.onBeforeRender();
  }

  public String newChildId()
  {
    return contentMenu.newChildId();
  }

  public MenuBarPanel addMenuEntry(final ContentMenuEntryPanel menuEntry)
  {
    if (this.extendedMenuEntry != null) {
      // Don't append entry after extended menu (should be the last entry).
      final int size = contentMenu.size();
      contentMenu.add(size - 1, menuEntry);
    } else {
      contentMenu.add(menuEntry);
    }
    return this;
  }

  public ContentMenuEntryPanel ensureAndAddExtendetMenuEntry()
  {
    if (extendedMenuEntry == null) {
      extendedMenuEntry = new ContentMenuEntryPanel(newChildId(), IconType.COG);
      contentMenu.add(extendedMenuEntry);
    }
    return extendedMenuEntry;
  }

  /**
   * @see org.apache.wicket.Component#isVisible()
   */
  @Override
  public boolean isVisible()
  {
    return contentMenu.hasEntries();
  }
}
