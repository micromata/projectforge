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

import org.apache.commons.lang3.StringUtils;
import org.apache.wicket.Page;
import org.apache.wicket.model.IModel;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Collection;

/**
 * Represents a single menu entry (of the user's individual menu).
 */
public class WicketMenuEntry implements Serializable {
  private static final org.slf4j.Logger log = org.slf4j.LoggerFactory.getLogger(WicketMenuEntry.class);

  private static final long serialVersionUID = 7961498640193169174L;

  protected Collection<WicketMenuEntry> subMenuEntries;

  protected String url;

  protected String id;

  protected String i18nKey;

  protected Class<? extends Page> pageClass;

  protected IModel<Integer> badgeCounter;

  protected String badgeCounterTooltip;

  protected String name;

  /**
   * Root menu entry.
   */
  WicketMenuEntry() {
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
  public WicketMenuEntry setName(final String name) {
    this.name = name;
    return this;
  }

  public void addMenuEntry(final WicketMenuEntry subMenuEntry) {
    if (subMenuEntries == null) {
      subMenuEntries = new ArrayList<WicketMenuEntry>();
    }
    subMenuEntries.add(subMenuEntry);
  }

  public WicketMenuEntry findById(final String id) {
    if (this.id != null) {
      if (this.id.equals(id)) {
        return this;
      }
      int pos = this.id.indexOf('.');
      if (pos >= 0 && pos + 1 < this.id.length() && StringUtils.equals(this.id.substring(pos + 1), id)) {
        return this;
      }
    }
    if (this.subMenuEntries == null) {
      return null;
    }
    for (final WicketMenuEntry subMenuEntry : this.subMenuEntries) {
      final WicketMenuEntry found = subMenuEntry.findById(id);
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
  public Collection<WicketMenuEntry> getSubMenuEntries() {
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

  public boolean isLink() {
    return pageClass != null || url != null;
  }

  public String getId() {
    return id;
  }

  public IModel<Integer> getBadgeCounter() {
    return badgeCounter;
  }

  public void setBadgeCounter(IModel<Integer> badgeCounter) {
    this.badgeCounter = badgeCounter;
  }

  public String getBadgeCounterTooltip() {
    return badgeCounterTooltip;
  }

  public void setBadgeCounterTooltip(String badgeCounterTooltip) {
    this.badgeCounterTooltip = badgeCounterTooltip;
  }
}
