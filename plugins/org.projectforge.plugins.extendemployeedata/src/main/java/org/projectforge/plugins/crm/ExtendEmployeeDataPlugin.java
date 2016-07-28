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

package org.projectforge.plugins.crm;

import org.projectforge.framework.persistence.user.api.UserPrefArea;
import org.projectforge.plugins.core.AbstractPlugin;

/**
 * @author Florian Blumenstein
 */
public class ExtendEmployeeDataPlugin extends AbstractPlugin
{
  public static final String ID = "extendemployeedata";

  public static final String RESOURCE_BUNDLE_NAME = "ExtendEmployeeDataI18nResources";

  static UserPrefArea USER_PREF_AREA;

  // The order of the entities is important for xml dump and imports as well as for test cases (order for deleting objects at the end of
  // each test).
  // The entities are inserted in ascending order and deleted in descending order.
  private static final Class<?>[] PERSISTENT_ENTITIES = new Class<?>[] {};

  /**
   * @see org.projectforge.plugins.core.AbstractPlugin#initialize()
   */
  @Override
  protected void initialize()
  {

  }

}
