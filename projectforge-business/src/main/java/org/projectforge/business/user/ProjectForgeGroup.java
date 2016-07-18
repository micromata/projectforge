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

package org.projectforge.business.user;

/**
 * Special ProjectForge user groups, such as Administrators and Finance. Some system functionality is only available for user's which are
 * member of the required group.
 * @author Kai Reinhard (k.reinhard@micromata.de)
 * 
 */
public enum ProjectForgeGroup
{
  /**
   * IT system administrators of ProjectForge. They do not see all functionalities (such as finances, order book or the project manager's
   * view).
   */
  ADMIN_GROUP("PF_Admin"), /** Users for having read access to the companies finances. */
  CONTROLLING_GROUP("PF_Controlling"), /** Users for having full access to the companies finances. */
  FINANCE_GROUP("PF_Finance"), /** Marketing users can download all addresses in excel format. */
  MARKETING_GROUP("PF_Marketing"), /** Orgateam users have access to Posteingang and Postausgang. */
  ORGA_TEAM("PF_Organization"), /** Users having access to the order book (for assigned orders). */
  PROJECT_ASSISTANT("PF_ProjectAssistant"), /**
   * Users having access to all time sheets (without details) and order book (for assigned
   * orders).
   */
  PROJECT_MANAGER("PF_ProjectManager");

  private String key;

  public String getKey()
  {
    return key;
  }

  /**
   * Same as {@link #getKey()}. This is the group name.
   */
  public String getName()
  {
    return key;
  }

  public boolean equals(final String groupName)
  {
    return key.equals(groupName);
  }

  /**
   * The key will be used e. g. for i18n.
   * @return
   */
  @Override
  public String toString()
  {
    return key;
  }

  ProjectForgeGroup(final String key)
  {
    this.key = key;
  }

}
