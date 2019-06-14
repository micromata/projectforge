/////////////////////////////////////////////////////////////////////////////
//
// Project ProjectForge Community Edition
//         www.projectforge.org
//
// Copyright (C) 2001-2019 Micromata GmbH, Germany (www.micromata.com)
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

import java.util.List;

import org.projectforge.framework.persistence.user.entities.PFUserDO;


/**
 * All methods of this interface should be implemented by every DOAccessControl class needed by every DO class in access-type="restricted"
 * application mode (productive mode).
 * @author Kai Reinhard (k.reinhard@micromata.de)
 */
public interface AccessControlInterface
{

  /**
   * Checks the access of the given user to get the given list. It's on the developer to throw an AccessException and/or remove some or all
   * entries of the given list, if the user has no access. <br>
   * Please note: The list is already read from the database, so access checking may depend on the object content!
   * @param list List of found database objects.
   * @param user The user which has requested the list.
   * @returns List of filtered objects for which the user has access to.
   * @throws AccessException
   */
  @SuppressWarnings("unchecked")
  public List checkFind(List list, PFUserDO user) throws AccessException;

  /**
   * Checks the access of the given user to get the load object.
   * @param obj Object requested and load from the database.
   * @param user The user which has requested the object.
   * @throws AccessException
   */
  public void checkLoad(Object obj, PFUserDO user) throws AccessException;

  /**
   * Checks the access of the given user to save the given object. This method will be called before saving this object to the database.
   * It's on the developer to throw an AccessException or maybe change some object properties (owner etc.) to satisfy the specific access
   * rules.
   * @param obj Object requested to save to the database.
   * @param user The user which has requested the object to save.
   * @throws AccessException
   */
  public void checkSave(Object obj, PFUserDO user) throws AccessException;

  /**
   * Checks the access of the given user to update the given object in the database. This method will be called before updating this object
   * in the database. It's on the developer to throw an AccessException or maybe change some object properties (owner etc.) to satisfy the
   * specific access rules.
   * @param obj Object requested to update in the database.
   * @param user The user which has requested the object to save.
   * @throws AccessException
   */
  public void checkUpdate(Object obj, PFUserDO user) throws AccessException;

  /**
   * Checks the access of the given user to delete the given object from the database. This method will be called before deleting this
   * object from the database. If the user should not have the access to delete the given object an AccessException should be thrown.
   * @param obj Object requested to delete from the database.
   * @param user The user which has requested the object to delete.
   * @throws AccessException
   */
  public void checkDestroy(Object obj, PFUserDO user) throws AccessException;
}
