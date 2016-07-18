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

import org.apache.commons.lang.Validate;
import org.projectforge.framework.access.AccessType;
import org.projectforge.framework.access.OperationType;
import org.projectforge.framework.persistence.api.BaseDao;
import org.projectforge.framework.persistence.user.entities.PFUserDO;

/**
 * @author Werner Feder (werner.feder@t-online.de)
 * TODO: Historisierung
 */
public class ContactEntryDao extends BaseDao<ContactEntryDO>
{

  private ContactDao contactDao;

  public ContactEntryDao()
  {
    super(ContactEntryDO.class);
  }

  public ContactEntryDao setContactDao(final ContactDao contactDao)
  {
    this.contactDao = contactDao;
    return this;
  }

  /**
   * @param contactEntry
   * @param contactId If null, then address will be set to null;
   * @see BaseDao#getOrLoad(Integer)
   */
  public void setContact(final ContactEntryDO contactEntry, final Integer contactId)
  {
    final ContactDO contact = contactDao.getOrLoad(contactId);
    contactEntry.setContact(contact);
  }

  /**
   * return Always true, no generic select access needed for address objects.
   * @see org.projectforge.framework.persistence.api.BaseDao#hasSelectAccess()
   */
  @Override
  public boolean hasSelectAccess(final PFUserDO user, final boolean throwException)
  {
    return true;
  }


  /**
   * @see org.projectforge.framework.persistence.api.BaseDao#hasAccess(Object, OperationType)
   */
  @Override
  public boolean hasAccess(final PFUserDO user, final ContactEntryDO obj, final ContactEntryDO oldObj, final OperationType operationType,
      final boolean throwException)
  {
    return accessChecker.hasPermission(user, obj.getContactId(), AccessType.TASKS, operationType, throwException);
  }

  /**
   * @see org.projectforge.framework.persistence.api.BaseDao#hasUpdateAccess(Object, Object)
   */
  @Override
  public boolean hasUpdateAccess(final PFUserDO user, final ContactEntryDO obj, final ContactEntryDO dbObj, final boolean throwException)
  {
    Validate.notNull(dbObj);
    Validate.notNull(obj);
    Validate.notNull(dbObj.getContactId());
    Validate.notNull(obj.getContactId());
    if (accessChecker.hasPermission(user, obj.getContactId(), AccessType.TASKS, OperationType.UPDATE, throwException) == false) {
      return false;
    }
    //    if (dbObj.getAddressId().equals(obj.getAddressId()) == false) {
    //      // User moves the object to another task:
    //      if (accessChecker.hasPermission(user, obj.getTaskId(), AccessType.TASKS, OperationType.INSERT, throwException) == false) {
    //        // Inserting of object under new task not allowed.
    //        return false;
    //      }
    //      if (accessChecker.hasPermission(user, dbObj.getTaskId(), AccessType.TASKS, OperationType.DELETE, throwException) == false) {
    //        // Deleting of object under old task not allowed.
    //        return false;
    //      }
    //    }
    return true;
  }

  /**
   * @see org.projectforge.framework.persistence.api.BaseDao#newInstance()
   */
  @Override
  public ContactEntryDO newInstance()
  {
    return new ContactEntryDO();
  }

}
