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

import org.apache.wicket.Page;
import org.apache.wicket.model.IModel;
import org.projectforge.menu.builder.MenuItemDef;
import org.projectforge.web.wicket.WicketUtils;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Collection;

/**
 * Represents a single menu entry (of the user's individual menu).
 */
public class MenuEntry implements Serializable {
  private static final org.slf4j.Logger log = org.slf4j.LoggerFactory.getLogger(MenuEntry.class);

  private static final long serialVersionUID = 7961498640193169174L;

  protected Collection<MenuEntry> subMenuEntries;

  protected String url;

  protected String id;

  protected String i18nKey;

  protected Class<? extends Page> pageClass;

  protected IModel<Integer> newCounterModel;

  protected TotalNewCounterModel totalNewCounterModel;

  protected boolean totalNewCounterModelEvaluated;

  protected String newCounterTooltip;

  protected String name;

  protected Menu menu;

  public IModel<Integer> getNewCounterModel() {
    return getNewCounterModel(0);
  }

  private IModel<Integer> getNewCounterModel(final int depth) {
    if (hasSubMenuEntries() == false || depth == 10) {
      // End less loop detection (depth == 10).
      return newCounterModel;
    }
    if (totalNewCounterModelEvaluated == true) {
      return totalNewCounterModel;
    }
    for (final MenuEntry subEntry : subMenuEntries) {
      final IModel<Integer> subSumModel = subEntry.getNewCounterModel(depth + 1);
      if (subSumModel == null) {
        continue;
      }
      if (totalNewCounterModel == null) {
        totalNewCounterModel = new TotalNewCounterModel();
      }
      totalNewCounterModel.add(subSumModel);
    }
    totalNewCounterModelEvaluated = true;
    return totalNewCounterModel;
  }

  public void setNewCounterTooltip(final String newCounterTooltip) {
    this.newCounterTooltip = newCounterTooltip;
  }

  public String getNewCounterTooltip() {
    return newCounterTooltip;
  }

  /**
   * Needed as marker for modified css.
   */
  public boolean isFirst() {
    return menu.isFirst(this);
  }

  /**
   * Root menu entry.
   */
  MenuEntry() {
  }

  public void setMenu(final Menu menu) {
    this.menu = menu;
  }

  /**
   * @return the name Only given for customized menu entries if the user renamed the menu.
   */
  public String getName() {
    return name;
  }

  /**
   * @param name the name to set
   * @return this for chaining.
   */
  public MenuEntry setName(final String name) {
    this.name = name;
    return this;
  }

  public void addMenuEntry(final MenuEntry subMenuEntry) {
    if (subMenuEntries == null) {
      subMenuEntries = new ArrayList<MenuEntry>();
    }
    subMenuEntries.add(subMenuEntry);
  }

  public MenuEntry findById(final String id) {
    if (id.equals(id) == true) {
      return this;
    }
    if (this.subMenuEntries == null) {
      return null;
    }
    for (final MenuEntry subMenuEntry : this.subMenuEntries) {
      final MenuEntry found = subMenuEntry.findById(id);
      if (found != null) {
        return found;
      }
    }
    return null;
  }

  public boolean hasSubMenuEntries() {
    return (this.subMenuEntries != null && subMenuEntries.size() > 0);
  }

  public boolean isWicketPage() {
    return pageClass != null;
  }

  public Class<? extends Page> getPageClass() {
    return pageClass;
  }

  /**
   * @return
   */
  public Collection<MenuEntry> getSubMenuEntries() {
    return subMenuEntries;
  }

  public String getI18nKey() {
    return i18nKey;
  }

  /**
   * @return
   */
  public String getUrl() {
    return url;
  }

  public void setNewCounterModel(final IModel<Integer> newCounterModel) {
    this.newCounterModel = newCounterModel;
  }

  public boolean isLink() {
    return pageClass != null || url != null;
  }

  public String getId() {
    return id;
  }
}
