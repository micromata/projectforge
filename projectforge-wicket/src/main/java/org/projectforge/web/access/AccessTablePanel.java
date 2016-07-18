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

package org.projectforge.web.access;

import java.util.List;

import org.apache.wicket.Component;
import org.apache.wicket.markup.html.WebMarkupContainer;
import org.apache.wicket.markup.html.basic.Label;
import org.apache.wicket.markup.html.panel.Panel;
import org.apache.wicket.markup.repeater.RepeatingView;
import org.projectforge.framework.access.AccessEntryDO;
import org.projectforge.framework.persistence.user.api.ThreadLocalUserContext;
import org.projectforge.web.wicket.flowlayout.IconPanel;
import org.projectforge.web.wicket.flowlayout.IconType;

/**
 * Rows of access rights (without header).
 * @author Kai Reinhard (k.reinhard@micromata.de)
 * 
 */
public class AccessTablePanel extends Panel
{
  private static final long serialVersionUID = -7347864057331989812L;

  private final List<AccessEntryDO> accessEntries;

  private boolean drawHeader;

  public AccessTablePanel(final String id, final List<AccessEntryDO> accessEntries)
  {
    super(id);
    this.accessEntries = accessEntries;
  }

  public AccessTablePanel init()
  {
    final RepeatingView rowRepeater = new RepeatingView("row");
    add(rowRepeater);
    if (drawHeader == true) {
      final WebMarkupContainer row = new WebMarkupContainer(rowRepeater.newChildId());
      rowRepeater.add(row);
      row.add(new Label("area", ""));
      row.add(createHeadCell("selectAccess", IconType.SELECT, "access.tooltip.selectAccess").setRenderBodyOnly(false));
      row.add(createHeadCell("insertAccess", IconType.INSERT, "access.tooltip.insertAccess").setRenderBodyOnly(false));
      row.add(createHeadCell("updateAccess", IconType.UPDATE, "access.tooltip.updateAccess").setRenderBodyOnly(false));
      row.add(createHeadCell("deleteAccess", IconType.DELETE, "access.tooltip.deleteAccess").setRenderBodyOnly(false));
    }
    for (final AccessEntryDO accessEntry : accessEntries) {
      final WebMarkupContainer row = new WebMarkupContainer(rowRepeater.newChildId());
      rowRepeater.add(row);
      row.add(new Label("area", getLocalizedString(accessEntry.getAccessType().getI18nKey())));
      row.add(createTicker("selectAccess", accessEntry.getAccessSelect()));
      row.add(createTicker("insertAccess", accessEntry.getAccessInsert()));
      row.add(createTicker("updateAccess", accessEntry.getAccessUpdate()));
      row.add(createTicker("deleteAccess", accessEntry.getAccessDelete()));
    }
    return this;
  }

  public AccessTablePanel setDrawHeader(final boolean drawHeader)
  {
    this.drawHeader = drawHeader;
    return this;
  }

  private Component createHeadCell(final String componentId, final IconType icon, final String tooltip)
  {
    return new IconPanel(componentId, icon, getLocalizedString(tooltip));
  }

  private Component createTicker(final String componentId, final boolean value)
  {
    if (value == true) {
      return new IconPanel(componentId, IconType.ACCEPT);
    } else {
      return new IconPanel(componentId, IconType.DENY);
    }
  }

  /**
   * Avoid warnings (AccessTablePanel is added in AccessListPage but Wicket logs warnings?).
   * @param key
   * @return
   */
  private String getLocalizedString(final String key) {
    //return super.getString(key);
    return ThreadLocalUserContext.getLocalizedString(key);
  }
}
