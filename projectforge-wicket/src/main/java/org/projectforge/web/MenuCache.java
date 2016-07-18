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

import java.util.HashMap;
import java.util.Map;

import org.projectforge.framework.cache.AbstractCache;

/**
 * Caches the user menus. Expire time is one hour.
 * @author Kai Reinhard (k.reinhard@micromata.de)
 * 
 */
public class MenuCache extends AbstractCache
{
  private Map<Integer, Menu> menuMap;

  private Map<Integer, Menu> mobileMenuMap;

  public MenuCache()
  {
    super(AbstractCache.TICKS_PER_HOUR); // Expires every hour.
  }

  public Menu getMenu(final Integer userId)
  {
    return getMenuMap().get(userId);
  }

  public Menu getMobileMenu(final Integer userId)
  {
    return getMobileMenuMap().get(userId);
  }

  public void putMenu(final Integer userId, final Menu menu)
  {
    getMenuMap().put(userId, menu);
  }

  public void putMobileMenu(final Integer userId, final Menu menu)
  {
    getMobileMenuMap().put(userId, menu);
  }

  public void removeMenu(final Integer userId)
  {
    getMenuMap().remove(userId);
  }

  public void removeMobileMenu(final Integer userId)
  {
    getMobileMenuMap().remove(userId);
  }

  private Map<Integer, Menu> getMenuMap()
  {
    checkRefresh();
    return menuMap;
  }

  private Map<Integer, Menu> getMobileMenuMap()
  {
    checkRefresh();
    return mobileMenuMap;
  }

  @Override
  protected void refresh()
  {
    menuMap = new HashMap<Integer, Menu>();
    mobileMenuMap = new HashMap<Integer, Menu>();
  }
}
