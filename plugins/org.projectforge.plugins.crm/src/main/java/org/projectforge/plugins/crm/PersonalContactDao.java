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

import java.io.Serializable;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.commons.lang3.ObjectUtils;
import org.apache.commons.lang3.Validate;
import org.hibernate.LockMode;
import org.projectforge.business.user.UserDao;
import org.projectforge.framework.access.AccessChecker;
import org.projectforge.framework.access.AccessException;
import org.projectforge.framework.persistence.api.BaseDao;
import org.projectforge.framework.persistence.api.ModificationStatus;
import org.projectforge.framework.persistence.user.api.ThreadLocalUserContext;
import org.projectforge.framework.persistence.user.entities.PFUserDO;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.orm.hibernate5.HibernateTemplate;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Isolation;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

/**
 *
 * @author Kai Reinhard (k.reinhard@micromata.de)
 *
 */
@Repository
public class PersonalContactDao
{
  private static final org.slf4j.Logger log = org.slf4j.LoggerFactory.getLogger(PersonalContactDao.class);

  @Autowired
  private AccessChecker accessChecker;

  @Autowired
  private UserDao userDao;

  @Autowired
  HibernateTemplate hibernateTemplate;

  /**
   * @param personalContact
   * @param ownerId If null, then task will be set to null;
   * @see BaseDao#getOrLoad(Integer)
   */
  public void setOwner(final PersonalContactDO personalContact, final Integer ownerId)
  {
    final PFUserDO user = userDao.getOrLoad(ownerId);
    personalContact.setOwner(user);
  }

  /**
   * @param obj
   * @return the generated identifier.
   */
  @Transactional(readOnly = false, propagation = Propagation.REQUIRES_NEW, isolation = Isolation.REPEATABLE_READ)
  public Serializable saveOrUpdate(final PersonalContactDO obj)
  {
    if (internalUpdate(obj) == true) {
      return obj.getId();
    }
    return internalSave(obj);
  }

  private void checkAccess(final PersonalContactDO obj)
  {
    Validate.notNull(obj);
    Validate.notNull(obj.getOwnerId());
    Validate.notNull(obj.getContactId());
    final PFUserDO owner = ThreadLocalUserContext.getUser();
    if (owner == null || owner.getId().equals(obj.getOwnerId()) == false) {
      throw new AccessException("address.accessException.userIsNotOwnerOfPersonalAddress");
    }
    accessChecker.checkRestrictedOrDemoUser();
  }

  private Serializable internalSave(final PersonalContactDO obj)
  {
    if (isEmpty(obj) == true) {
      // No entry, so we do not need to save this entry.
      return null;
    }
    checkAccess(obj);
    obj.setCreated();
    obj.setLastUpdate();
    final Serializable id = hibernateTemplate.save(obj);
    log.info("New object added (" + id + "): " + obj.toString());
    return id;
  }

  private boolean isEmpty(final PersonalContactDO obj)
  {
    return (obj.isFavoriteCard() == false)
        && (obj.isFavoriteBusinessPhone() == false)
        && (obj.isFavoriteMobilePhone() == false)
        && (obj.isFavoriteFax() == false)
        && (obj.isFavoritePrivatePhone() == false)
        && (obj.isFavoritePrivateMobilePhone() == false);
  }

  /**
   * @param obj
   * @return true, if already existing entry was updated, otherwise false (e. g. if no entry exists for update).
   */
  private boolean internalUpdate(final PersonalContactDO obj)
  {
    PersonalContactDO dbObj = null;
    if (obj.getId() != null) {
      dbObj = hibernateTemplate.load(PersonalContactDO.class, obj.getId(), LockMode.PESSIMISTIC_WRITE);
    }
    if (dbObj == null) {
      dbObj = getByContactId(obj.getContactId());
    }
    if (dbObj == null) {
      return false;
    }
    checkAccess(dbObj);
    Validate.isTrue(ObjectUtils.equals(dbObj.getContactId(), obj.getContactId()));
    obj.setId(dbObj.getId());
    // Copy all values of modified user to database object.
    final ModificationStatus modified = dbObj.copyValuesFrom(obj, "owner", "address", "id");
    if (modified == ModificationStatus.MAJOR) {
      dbObj.setLastUpdate();
      log.info("Object updated: " + dbObj.toString());
    }
    return true;
  }

  /**
   * @return the PersonalContactDO entry assigned to the given address for the context user or null, if not exist.
   */
  @SuppressWarnings("unchecked")
  @Transactional(readOnly = true, propagation = Propagation.SUPPORTS)
  public PersonalContactDO getByContactId(final Integer contactId)
  {
    final PFUserDO owner = ThreadLocalUserContext.getUser();
    Validate.notNull(owner);
    Validate.notNull(owner.getId());
    final List<PersonalContactDO> list = (List<PersonalContactDO>) hibernateTemplate.find(
        "from " + PersonalContactDO.class.getSimpleName() + " t where t.owner.id = ? and t.contact.id = ?",
        new Object[] { owner.getId(), contactId });
    if (list != null) {
      if (list.size() == 0) {
        return null;
      }
      if (list.size() > 1) {
        log.error("Multiple personal address book entries for same user ("
            + owner.getId()
            + " and same address ("
            + contactId
            + "). Should not occur?!");
      }
      return list.get(0);
    }
    return null;
  }

  /**
   * @return the list of all PersonalContactDO entries for the context user.
   */
  @Transactional(readOnly = true, propagation = Propagation.SUPPORTS)
  public List<PersonalContactDO> getList()
  {
    final PFUserDO owner = ThreadLocalUserContext.getUser();
    Validate.notNull(owner);
    Validate.notNull(owner.getId());
    @SuppressWarnings("unchecked")
    final List<PersonalContactDO> list = (List<PersonalContactDO>) hibernateTemplate.find(
        "from "
            + PersonalContactDO.class.getSimpleName()
            + " t join fetch t.contact where t.owner.id=? and t.contact.deleted=false order by t.contact.name, t.contact.firstName",
        owner.getId());
    return list;
  }

  /**
   * @return the list of all address ids of personal address book for the context user (isFavorite() must be true).
   * @see PersonalContactDO#isFavorite()
   */
  @SuppressWarnings("unchecked")
  @Transactional(readOnly = true, propagation = Propagation.SUPPORTS)
  public Map<Integer, PersonalContactDO> getPersonalAddressByAddressId()
  {
    final PFUserDO owner = ThreadLocalUserContext.getUser();
    Validate.notNull(owner);
    Validate.notNull(owner.getId());
    final List<PersonalContactDO> list = (List<PersonalContactDO>) hibernateTemplate.find(
        "from " + PersonalContactDO.class.getSimpleName() + " t where t.owner.id = ?", owner.getId());
    final Map<Integer, PersonalContactDO> result = new HashMap<Integer, PersonalContactDO>();
    for (final PersonalContactDO entry : list) {
      if (entry.isFavorite() == true) {
        result.put(entry.getContactId(), entry);
      }
    }
    return result;
  }

  /**
   * @return the list of all address entries for the context user (isFavorite() must be true).
   * @see PersonalContactDO#isFavorite()
   */
  @SuppressWarnings("unchecked")
  @Transactional(readOnly = true, propagation = Propagation.SUPPORTS)
  public List<ContactDO> getMyAddresses()
  {
    final PFUserDO owner = ThreadLocalUserContext.getUser();
    Validate.notNull(owner);
    Validate.notNull(owner.getId());
    final List<PersonalContactDO> list = (List<PersonalContactDO>) hibernateTemplate.find(
        "from "
            + PersonalContactDO.class.getSimpleName()
            + " t join fetch t.contact where t.owner.id = ? and t.contact.deleted = false order by t.contact.name, t.contact.firstName",
        owner.getId());
    final List<ContactDO> result = new ArrayList<ContactDO>();
    for (final PersonalContactDO entry : list) {
      if (entry.isFavorite() == true) {
        result.add(entry.getContact());
      }
    }
    return result;
  }
}
