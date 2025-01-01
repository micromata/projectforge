/////////////////////////////////////////////////////////////////////////////
//
// Project ProjectForge Community Edition
//         www.projectforge.org
//
// Copyright (C) 2001-2025 Micromata GmbH, Germany (www.micromata.com)
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

package org.projectforge.framework.access;

import org.projectforge.framework.persistence.api.BaseSearchFilter;

import java.io.Serializable;

/**
 *
 * @author Kai Reinhard (k.reinhard@micromata.de)
 *
 */
public class AccessFilter extends BaseSearchFilter implements Serializable
{
  private static final long serialVersionUID = -1358813343070331969L;

  private Long taskId, groupId, userId;

  private boolean includeAncestorTasks, includeDescendentTasks, inherit;

  public AccessFilter()
  {
  }

  public AccessFilter(final BaseSearchFilter filter)
  {
    super(filter);
  }

  public Long getTaskId()
  {
    return taskId;
  }

  /**
   * @param taskId
   * @return this for chaining.
   */
  public AccessFilter setTaskId(final Long taskId)
  {
    this.taskId = taskId;
    return this;
  }

  public boolean isIncludeAncestorTasks()
  {
    return includeAncestorTasks;
  }

  /**
   * @param includeAncestorTasks
   * @return this for chaining.
   */
  public AccessFilter setIncludeAncestorTasks(final boolean includeAncestorTasks)
  {
    this.includeAncestorTasks = includeAncestorTasks;
    return this;
  }

  /**
   * @return the includeDescendentTasks
   */
  public boolean isIncludeDescendentTasks()
  {
    return includeDescendentTasks;
  }

  /**
   * @param includeDescendentTasks the includeDescendentTasks to set
   * @return this for chaining.
   */
  public AccessFilter setIncludeDescendentTasks(final boolean includeDescendentTasks)
  {
    this.includeDescendentTasks = includeDescendentTasks;
    return this;
  }

  public boolean isInherit()
  {
    return inherit;
  }

  /**
   * @param inherit
   * @return this for chaining.
   */
  public AccessFilter setInherit(final boolean inherit)
  {
    this.inherit = inherit;
    return this;
  }

  public Long getGroupId()
  {
    return groupId;
  }

  /**
   * @param groupId
   * @return this for chaining.
   */
  public AccessFilter setGroupId(final Long groupId)
  {
    this.groupId = groupId;
    return this;
  }

  /**
   * For checking the access rights for an user.
   */
  public Long getUserId()
  {
    return userId;
  }

  /**
   * @param userId
   * @return this for chaining.
   */
  public AccessFilter setUserId(final Long userId)
  {
    this.userId = userId;
    return this;
  }
}
