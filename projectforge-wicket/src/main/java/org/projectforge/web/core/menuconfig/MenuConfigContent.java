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

package org.projectforge.web.core.menuconfig;

import org.apache.wicket.AttributeModifier;
import org.apache.wicket.markup.html.WebMarkupContainer;
import org.apache.wicket.markup.html.basic.Label;
import org.apache.wicket.markup.html.link.AbstractLink;
import org.apache.wicket.markup.html.panel.Panel;
import org.apache.wicket.markup.repeater.RepeatingView;
import org.apache.wicket.model.ResourceModel;
import org.projectforge.web.Menu;
import org.projectforge.web.MenuEntry;
import org.projectforge.web.core.NavAbstractPanel;

/**
 * @author Dennis Hilpmann (d.hilpmann.extern@micromata.de)
 * @author Kai Reinhard (k.reinhard@micromata.de)
 * @author Johannes Unterstein (j.unterstein@micromata.de)
 */
public class MenuConfigContent extends Panel
{
  private static final long serialVersionUID = 7330216552642637129L;

  private static final org.apache.log4j.Logger log = org.apache.log4j.Logger.getLogger(MenuConfigContent.class);

  public MenuConfigContent(final String id, final Menu menu)
  {
    super(id);
    final RepeatingView mainMenuRepeater = new RepeatingView("mainMenuItem");
    add(mainMenuRepeater);
    if (menu == null) {
      mainMenuRepeater.setVisible(false);
      log.error("Oups, menu is null. Configuration of favorite menu not possible.");
      return;
    }
    int counter = 0;
    if (menu.getMenuEntries() == null) {
      // Should only occur in maintenance mode!
      return;
    }
    for (final MenuEntry mainMenuEntry : menu.getMenuEntries()) {
      if (mainMenuEntry.hasSubMenuEntries() == false) {
        continue;
      }
      final WebMarkupContainer mainMenuContainer = new WebMarkupContainer(mainMenuRepeater.newChildId());
      mainMenuRepeater.add(mainMenuContainer);
      if (counter++ % 5 == 0) {
        mainMenuContainer.add(AttributeModifier.append("class", "mm_clear"));
      }
      mainMenuContainer.add(new Label("label", new ResourceModel(mainMenuEntry.getI18nKey())));
      final RepeatingView subMenuRepeater = new RepeatingView("menuItem");
      mainMenuContainer.add(subMenuRepeater);
      for (final MenuEntry subMenuEntry : mainMenuEntry.getSubMenuEntries()) {
        final WebMarkupContainer subMenuContainer = new WebMarkupContainer(subMenuRepeater.newChildId());
        subMenuRepeater.add(subMenuContainer);
        final AbstractLink link = NavAbstractPanel.getMenuEntryLink(subMenuEntry, false);
        if (link != null) {
          subMenuContainer.add(link);
        } else {
          subMenuContainer.setVisible(false);
        }
      }
    }
  }
}
