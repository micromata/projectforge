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

package org.projectforge.business.address;

import org.apache.commons.lang3.Validate;
import org.hibernate.LockMode;
import org.hibernate.SessionFactory;
import org.projectforge.business.user.UserDao;
import org.projectforge.business.user.UserRightId;
import org.projectforge.framework.access.AccessChecker;
import org.projectforge.framework.access.AccessException;
import org.projectforge.framework.persistence.api.BaseDao;
import org.projectforge.framework.persistence.api.ModificationStatus;
import org.projectforge.framework.persistence.api.UserRightService;
import org.projectforge.framework.persistence.user.api.ThreadLocalUserContext;
import org.projectforge.framework.persistence.user.entities.PFUserDO;
import org.projectforge.framework.persistence.utils.SQLHelper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.orm.hibernate5.HibernateTemplate;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Isolation;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.io.Serializable;
import java.util.*;
import java.util.stream.Collectors;

/**
 * @author Kai Reinhard (k.reinhard@micromata.de)
 */
@Repository
public class PersonalAddressDao {
  private static final org.slf4j.Logger log = org.slf4j.LoggerFactory.getLogger(PersonalAddressDao.class);

  @Autowired
  private AccessChecker accessChecker;

  @Autowired
  private UserDao userDao;

  @Autowired
  private HibernateTemplate hibernateTemplate;

  @Autowired
  private SessionFactory sessionFactory;

  @Autowired
  private AddressbookDao addressbookDao;

  @Autowired
  private UserRightService userRights;

  private transient AddressbookRight addressbookRight;

  /**
   * @param personalAddress
   * @param ownerId         If null, then task will be set to null;
   * @see BaseDao#getOrLoad(Integer)
   */
  public void setOwner(final PersonalAddressDO personalAddress, final Integer ownerId) {
    final PFUserDO user = userDao.getOrLoad(ownerId);
    personalAddress.setOwner(user);
  }

  /**
   * @param obj
   * @return the generated identifier.
   */
  @Transactional(readOnly = false, propagation = Propagation.REQUIRES_NEW, isolation = Isolation.REPEATABLE_READ)
  public Serializable saveOrUpdate(final PersonalAddressDO obj) {
    if (internalUpdate(obj) == true) {
      return obj.getId();
    }
    return internalSave(obj);
  }

  private boolean checkAccess(final PersonalAddressDO obj, boolean throwException) {
    Validate.notNull(obj);
    Validate.notNull(obj.getOwnerId());
    Validate.notNull(obj.getAddressId());
    final PFUserDO owner = ThreadLocalUserContext.getUser();
    if (owner == null || owner.getId().equals(obj.getOwnerId()) == false) {
      if (throwException) {
        throw new AccessException("address.accessException.userIsNotOwnerOfPersonalAddress");
      }
      return false;
    }
    Set<Integer> addressbookIDListForUser = getAddressbookIdsForUser(owner);
    Set<Integer> addressbookIDListFromAddress = obj.getAddress().getAddressbookList().stream().mapToInt(AddressbookDO::getId).boxed()
            .collect(Collectors.toSet());
    if (Collections.disjoint(addressbookIDListForUser, addressbookIDListFromAddress)) {
      if (throwException) {
        throw new AccessException("address.accessException.userHasNoRightForAddressbook");
      }
      return false;
    }
    accessChecker.checkRestrictedOrDemoUser();
    return true;
  }

  private boolean checkAccess(final PersonalAddressDO obj) {
    return checkAccess(obj, true);
  }

  private Set<Integer> getAddressbookIdsForUser(final PFUserDO user) {
    Set<Integer> abIdSet = new HashSet<>();
    //Get all addressbooks for user
    if (addressbookRight == null) {
      addressbookRight = (AddressbookRight) userRights.getRight(UserRightId.MISC_ADDRESSBOOK);
    }
    abIdSet.add(AddressbookDao.GLOBAL_ADDRESSBOOK_ID);
    for (AddressbookDO ab : addressbookDao.internalLoadAll()) {
      if (ab.isDeleted() == false && addressbookRight.hasSelectAccess(user, ab)) {
        abIdSet.add(ab.getId());
      }
    }
    return abIdSet;
  }

  private Serializable internalSave(final PersonalAddressDO obj) {
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

  private boolean isEmpty(final PersonalAddressDO obj) {
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
  private boolean internalUpdate(final PersonalAddressDO obj) {
    PersonalAddressDO dbObj = null;
    if (obj.getId() != null) {
      dbObj = hibernateTemplate.load(PersonalAddressDO.class, obj.getId(), LockMode.PESSIMISTIC_WRITE);
    }
    if (dbObj == null) {
      dbObj = getByAddressId(obj.getAddressId());
    }
    if (dbObj == null) {
      return false;
    }
    checkAccess(dbObj);
    Validate.isTrue(Objects.equals(dbObj.getAddressId(), obj.getAddressId()));
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
   * @return the PersonalAddressDO entry assigned to the given address for the context user or null, if not exist.
   */
  @SuppressWarnings("unchecked")
  @Transactional(readOnly = true, propagation = Propagation.SUPPORTS)
  public PersonalAddressDO getByAddressId(final Integer addressId) {
    final PFUserDO owner = ThreadLocalUserContext.getUser();
    Validate.notNull(owner);
    Validate.notNull(owner.getId());
    return SQLHelper.ensureUniqueResult(sessionFactory.getCurrentSession()
            .createNamedQuery(PersonalAddressDO.FIND_BY_OWNER_AND_ADDRESS_ID, PersonalAddressDO.class)
            .setParameter("ownerId", owner.getId())
            .setParameter("addressId", addressId),
            "Multiple personal address book entries for same user (" + owner.getId() + ") and same address ("
                                    + addressId + "). Should not occur?!");
  }

  /**
   * @return the list of all PersonalAddressDO entries for the context user.
   */
  @Transactional(readOnly = true, propagation = Propagation.SUPPORTS)
  public List<PersonalAddressDO> getList() {
    final PFUserDO owner = ThreadLocalUserContext.getUser();
    Validate.notNull(owner);
    Validate.notNull(owner.getId());
    final long start = System.currentTimeMillis();
    List<PersonalAddressDO> list = sessionFactory.getCurrentSession()
            .createNamedQuery(PersonalAddressDO.FIND_JOINED_BY_OWNER, PersonalAddressDO.class)
            .setParameter("ownerId", owner.getId())
            .list();
    log.info("PersonalDao.getList took " + (System.currentTimeMillis() - start) + "ms.");
    list = list.stream().filter(pa -> checkAccess(pa, false) == true).collect(Collectors.toList());
    return list;
  }

  /**
   * @return the list of all PersonalAddressDO entries for the context user without any check access (addresses might be also deleted).
   */
  @Transactional(readOnly = true, propagation = Propagation.SUPPORTS)
  public List<Integer> getIdList() {
    final PFUserDO owner = ThreadLocalUserContext.getUser();
    Validate.notNull(owner);
    Validate.notNull(owner.getId());
    List<Integer> list = sessionFactory.getCurrentSession()
            .createNamedQuery(PersonalAddressDO.FIND_IDS_BY_OWNER, Integer.class)
            .setParameter("ownerId", owner.getId())
            .list();
    return list;
  }

  /**
   * @return the list of all address ids of personal address book for the context user (isFavorite() must be true).
   * @see PersonalAddressDO#isFavorite()
   */
  @SuppressWarnings("unchecked")
  @Transactional(readOnly = true, propagation = Propagation.SUPPORTS)
  public Map<Integer, PersonalAddressDO> getPersonalAddressByAddressId() {
    final PFUserDO owner = ThreadLocalUserContext.getUser();
    Validate.notNull(owner);
    Validate.notNull(owner.getId());
    final List<PersonalAddressDO> list = sessionFactory.getCurrentSession()
            .createNamedQuery(PersonalAddressDO.FIND_BY_OWNER, PersonalAddressDO.class)
            .setParameter("ownerId", owner.getId())
            .list();
    final Map<Integer, PersonalAddressDO> result = new HashMap<Integer, PersonalAddressDO>();
    for (final PersonalAddressDO entry : list) {
      if (entry.isFavorite() == true && checkAccess(entry, false)) {
        result.put(entry.getAddressId(), entry);
      }
    }
    return result;
  }

  /**
   * @return the list of all address entries for the context user (isFavorite() must be true).
   * @see PersonalAddressDO#isFavorite()
   */
  @SuppressWarnings("unchecked")
  @Transactional(readOnly = true, propagation = Propagation.SUPPORTS)
  public List<AddressDO> getMyAddresses() {
    final PFUserDO owner = ThreadLocalUserContext.getUser();
    Validate.notNull(owner);
    Validate.notNull(owner.getId());
    final List<PersonalAddressDO> list = getList();
    final List<AddressDO> result = new ArrayList<AddressDO>();
    for (final PersonalAddressDO entry : list) {
      if (entry.isFavorite() == true && checkAccess(entry, false)) {
        result.add(entry.getAddress());
      }
    }
    return result;
  }
}
