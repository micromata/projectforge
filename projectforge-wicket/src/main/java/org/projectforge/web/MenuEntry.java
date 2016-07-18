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
import java.util.ArrayList;
import java.util.Collection;
import java.util.TreeSet;

import org.apache.commons.lang.Validate;
import org.apache.commons.lang.builder.ToStringBuilder;
import org.apache.wicket.Page;
import org.apache.wicket.model.IModel;
import org.projectforge.web.wicket.WicketUtils;

/**
 * Represents a single menu entry (of the user's individual menu).
 */
public class MenuEntry implements Serializable, Comparable<MenuEntry>
{
  private static final org.apache.log4j.Logger log = org.apache.log4j.Logger.getLogger(MenuEntry.class);

  private static final long serialVersionUID = 7961498640193169174L;

  protected Collection<MenuEntry> subMenuEntries;

  protected String url;

  protected boolean opened = false;

  protected IModel<Integer> newCounterModel;

  protected TotalNewCounterModel totalNewCounterModel;

  protected boolean totalNewCounterModelEvaluated;

  protected String newCounterTooltip;

  protected MenuEntry parent;

  protected Boolean visible;

  protected MenuItemDef menuItemDef;

  protected String name;

  protected Menu menu;

  private boolean mobileMenu;

  private boolean sorted = true;

  /**
   * @param sorted if true (default) the entries are sorted, otherwise the order is the order of adding.
   * @return this for chaining.
   */
  public MenuEntry setSorted(final boolean sorted)
  {
    this.sorted = sorted;
    return this;
  }

  public IModel<Integer> getNewCounterModel()
  {
    return getNewCounterModel(0);
  }

  private IModel<Integer> getNewCounterModel(final int depth)
  {
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

  public void setNewCounterTooltip(final String newCounterTooltip)
  {
    this.newCounterTooltip = newCounterTooltip;
  }

  public String getNewCounterTooltip()
  {
    return newCounterTooltip;
  }

  /**
   * Needed as marker for modified css.
   */
  public boolean isFirst()
  {
    return menu.isFirst(this);
  }

  public MenuEntry getParent()
  {
    return parent;
  }

  /**
   * @param menu Needed because MenuEntry is perhaps under construction and menu member isn't set yet.
   * @param id
   */
  public void setParent(final Menu menu, final String id)
  {
    final MenuEntry parentEntry = menu.findById(id);
    if (parentEntry == null) {
      log.error("Oups, menu entry '" + id + "' not found (ignoring setParent(...) of : " + getId());
    } else {
      setParent(parentEntry);
    }
  }

  void setParent(final MenuEntry parent)
  {
    this.parent = parent;
  }

  public boolean hasParent()
  {
    return this.parent != null;
  }

  /**
   * Root menu entry.
   */
  MenuEntry()
  {
  }

  public MenuEntry(final MenuItemDef menuItem, final MenuBuilderContext context)
  {
    this.menuItemDef = menuItem;
    if (context.isMobileMenu() == true) {
      mobileMenu = true;
      Validate.notNull(menuItem.getMobilePageClass());
      this.url = WicketUtils.getBookmarkablePageUrl(menuItem.getMobilePageClass());
    } else if (menuItem.isWicketPage() == true) {
      this.url = WicketUtils.getBookmarkablePageUrl(menuItem.getPageClass(), menuItem.getParams());
    } else if (menuItem.getUrl() != null) {
      this.url = "../secure/" + menuItem.getUrl();
    }
  }

  public void setMenu(final Menu menu)
  {
    this.menu = menu;
    menu.addMenuEntry(this);
  }

  /**
   * @return the name Only given for customized menu entries if the user renamed the menu.
   */
  public String getName()
  {
    return name;
  }

  /**
   * @param name the name to set
   * @return this for chaining.
   */
  public MenuEntry setName(final String name)
  {
    this.name = name;
    return this;
  }

  public void addMenuEntry(final MenuEntry subMenuEntry)
  {
    if (subMenuEntries == null) {
      if (sorted == true) {
        subMenuEntries = new TreeSet<MenuEntry>();
      } else {
        subMenuEntries = new ArrayList<MenuEntry>();
      }
    }
    subMenuEntries.add(subMenuEntry);
    subMenuEntry.setParent(this);
  }

  public MenuEntry findById(final String id)
  {
    if (menuItemDef != null && menuItemDef.getId().equals(id) == true) {
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

  public boolean hasSubMenuEntries()
  {
    return (this.subMenuEntries != null && subMenuEntries.size() > 0);
  }

  public boolean isOpened()
  {
    return this.opened;
  }

  public MenuItemDef getParentMenuItemDef()
  {
    if (this.mobileMenu == true) {
      return this.menuItemDef.getMobileParentMenu();
    } else {
      return this.menuItemDef.getParent();
    }
  }

  /**
   * Should the link open a separate window (named 'pforge2')?
   * 
   * @return
   */
  public boolean isNewWindow()
  {
    return menuItemDef != null && menuItemDef.isNewWindow();
  }

  public boolean isWicketPage()
  {
    return menuItemDef != null && menuItemDef.isWicketPage();
  }

  public Class<? extends Page> getPageClass()
  {
    return menuItemDef != null ? menuItemDef.getPageClass() : null;
  }

  public Class<? extends Page> getMobilePageClass()
  {
    return menuItemDef != null ? menuItemDef.getMobilePageClass() : null;
  }

  /**
   * @return
   */
  public Collection<MenuEntry> getSubMenuEntries()
  {
    return subMenuEntries;
  }

  public String getI18nKey()
  {
    return menuItemDef != null ? menuItemDef.getI18nKey() : null;
  }

  /**
   * @return
   */
  public String getUrl()
  {
    return url;
  }

  public String[] getParams()
  {
    return menuItemDef.getParams();
  }

  public void setNewCounterModel(final IModel<Integer> newCounterModel)
  {
    this.newCounterModel = newCounterModel;
  }

  public boolean isLink()
  {
    return menuItemDef.isWicketPage() == true || menuItemDef.hasUrl() == true;
  }

  /**
   * @return True or false if variable visible is set. True, if no sub menu entries exists in this entry has an link. If
   *         sub menu entries does exist then it's visible if any of the sub menu entries is visible. The variable
   *         visible is set automatically after the first call of this method.
   */
  public boolean isVisible()
  {
    if (visible != null) {
      return visible;
    }
    if (subMenuEntries == null || subMenuEntries.size() == 0) {
      visible = isLink();
    } else {
      for (final MenuEntry subMenuEntry : subMenuEntries) {
        if (subMenuEntry.isVisible() == true) {
          visible = true;
          break;
        }
      }
    }
    return visible;
  }

  public void setVisible(final boolean visible)
  {
    this.visible = visible;
  }

  public String getId()
  {
    return menuItemDef != null ? menuItemDef.getId() : null;
  }

  @Override
  public int compareTo(final MenuEntry o)
  {
    final int orderNumber = menuItemDef != null ? menuItemDef.getOrderNumber() : 10000;
    final int otherOrderNumber = o.menuItemDef != null ? o.menuItemDef.getOrderNumber() : 10000;
    if (orderNumber < otherOrderNumber) {
      return -1;
    } else if (orderNumber > otherOrderNumber) {
      return 1;
    }
    final String name = menuItemDef != null ? menuItemDef.getI18nKey() : getName();
    final String otherName = o.menuItemDef != null ? o.menuItemDef.getI18nKey() : o.getName();
    return name.compareTo(otherName);
  }

  /**
   * @see java.lang.Object#toString()
   */
  @Override
  public String toString()
  {
    final ToStringBuilder tos = new ToStringBuilder(this);
    if (menuItemDef != null)
      tos.append("menuItemDef", menuItemDef);
    if (name != null)
      tos.append("name", name);
    return tos.toString();
  }
}
