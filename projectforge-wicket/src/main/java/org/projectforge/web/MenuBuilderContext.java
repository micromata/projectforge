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

import org.projectforge.framework.access.AccessChecker;
import org.projectforge.framework.persistence.api.UserRightService;
import org.projectforge.framework.persistence.user.entities.PFUserDO;

/**
 * Context while building a menu for a logged-in-user (e. g. after login).
 */
public class MenuBuilderContext
{
  final static String VAR_PROJECT_MENU_ENTRY_EXISTS = "projectMenuEntryExists";

  final static String VAR_ORDER_BOOK_ENTRY_EXISTS = "orderBookMenuEntryExists";

  private final AccessChecker accessChecker;

  private final UserRightService userRights;

  private final PFUserDO loggedInUser;

  private final Menu menu;

  private final boolean mobileMenu;

  private Map<String, Object> variables;

  MenuBuilderContext(final Menu menu, final PFUserDO loggedInUser, final boolean mobileMenu,
      AccessChecker accessChecker, UserRightService userRights)
  {
    this.menu = menu;
    this.accessChecker = accessChecker;
    this.userRights = userRights;
    this.loggedInUser = loggedInUser;
    this.mobileMenu = mobileMenu;
  }

  public AccessChecker getAccessChecker()
  {
    return accessChecker;
  }

  public UserRightService getUserRights()
  {
    return userRights;
  }

  public PFUserDO getLoggedInUser()
  {
    return loggedInUser;
  }

  public Menu getMenu()
  {
    return menu;
  }

  /**
   * @return true, if this menu represents a mobile menu. A mobile menu is a different menu optimized for mobile
   *         devices.
   */
  public boolean isMobileMenu()
  {
    return mobileMenu;
  }

  /**
   * Sets a variable for using while building the tree. You can use this e. g. for settings some variables while
   * building a menu entry which is dependent from a state calculated while rending previous menu entries. Implemented
   * by a simple HashMap.
   * 
   * @param key
   * @param value
   * @return this for chaining.
   */
  public MenuBuilderContext setVariable(final String key, final Object value)
  {
    if (variables == null) {
      variables = new HashMap<String, Object>();
    }
    variables.put(key, value);
    return this;
  }

  /**
   * @param key
   * @return A variable's value set before via setVariable or null if doesn't exist.
   */
  public Object getVariable(final String key)
  {
    return variables != null ? variables.get(key) : null;
  }
}
