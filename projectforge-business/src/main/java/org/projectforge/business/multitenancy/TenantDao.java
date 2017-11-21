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

package org.projectforge.business.multitenancy;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Set;

import org.apache.commons.lang3.ObjectUtils;
import org.hibernate.LockMode;
import org.projectforge.business.user.UserRightId;
import org.projectforge.continuousdb.Table;
import org.projectforge.framework.access.AccessException;
import org.projectforge.framework.i18n.UserException;
import org.projectforge.framework.persistence.api.BaseDao;
import org.projectforge.framework.persistence.api.BaseSearchFilter;
import org.projectforge.framework.persistence.api.QueryFilter;
import org.projectforge.framework.persistence.database.InitDatabaseDao;
import org.projectforge.framework.persistence.jpa.PfEmgrFactory;
import org.projectforge.framework.persistence.user.api.ThreadLocalUserContext;
import org.projectforge.framework.persistence.user.entities.PFUserDO;
import org.projectforge.framework.persistence.user.entities.TenantDO;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
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
public class TenantDao extends BaseDao<TenantDO>
{
  private static final org.apache.log4j.Logger log = org.apache.log4j.Logger.getLogger(TenantDao.class);

  public static final UserRightId USER_RIGHT_ID = UserRightId.ADMIN_TENANT;

  private static final String[] ADDITIONAL_SEARCH_FIELDS = new String[] { "assignedUsers.username",
      "assignedUsers.firstname",
      "assignedUsers.lastname" };

  @Autowired
  private InitDatabaseDao initDatabaseDao;

  @Autowired
  private JdbcTemplate jdbc;

  private static Boolean tenantTableExists = null;

  public TenantDao()
  {
    super(TenantDO.class);
    this.supportAfterUpdate = true;
    userRightId = USER_RIGHT_ID;
  }

  public TenantDO getDefaultTenant()
  {
    @SuppressWarnings("unchecked")
    final List<TenantDO> list = (List<TenantDO>) getHibernateTemplate()
        .find("from TenantDO t where t.defaultTenant = true");
    if (list != null && list.isEmpty() == true) {
      return null;
    }
    if (list.size() > 1) {
      log.warn(
          "There are more than one tenent object declared as default! No or only one tenant should be defined as default!");
    }
    return list.get(0);
  }

  /**
   * @see org.projectforge.framework.persistence.api.BaseDao#onSaveOrModify(org.projectforge.core.ExtendedBaseDO)
   */
  @Override
  protected void onSaveOrModify(final TenantDO obj)
  {
    if (obj.isDefault() == false) {
      return;
    }
    final TenantDO defaultTenant = getDefaultTenant();
    if (defaultTenant == null) {
      return;
    }
    if (obj.getId() == null || ObjectUtils.equals(defaultTenant.getId(), obj.getId()) == false) {
      throw new UserException("multitenancy.error.maxOnlyOneTenantShouldBeDefault");
    }
  }

  /**
   * @see org.projectforge.framework.persistence.api.BaseDao#createQueryFilter(org.projectforge.framework.persistence.api.BaseSearchFilter)
   */
  @Override
  protected QueryFilter createQueryFilter(final BaseSearchFilter filter)
  {
    final boolean superAdmin = TenantChecker.isSuperAdmin(ThreadLocalUserContext.getUser()) == true;
    if (superAdmin == false) {
      return super.createQueryFilter(filter);
    }
    return new QueryFilter(filter, true);
  }

  /**
   * Please note: Any existing assigned user in tenant object is ignored!
   * 
   * @param tenant
   * @param assignedUsers Full list of all users which have to assigned to this tenant.
   * @return
   */
  public void setAssignedUsers(final TenantDO tenant, final Collection<PFUserDO> assignedUsers) throws AccessException
  {
    final Set<PFUserDO> origAssignedUsers = tenant.getAssignedUsers();
    if (origAssignedUsers != null) {
      final Iterator<PFUserDO> it = origAssignedUsers.iterator();
      while (it.hasNext() == true) {
        final PFUserDO user = it.next();
        if (assignedUsers.contains(user) == false) {
          it.remove();
        }
      }
    }
    for (final PFUserDO user : assignedUsers) {
      if (origAssignedUsers == null || origAssignedUsers.contains(user) == false) {
        tenant.addUser(user);
      }
    }
  }

  /**
   * Creates ProjectForge's system groups for the new tenant.<br/>
   * Creates for every user an history entry if the user is part of this new tenant.
   * 
   * @param tenant
   * @see org.projectforge.framework.persistence.api.BaseDao#afterSave(org.projectforge.core.ExtendedBaseDO)
   */
  @Override
  public void afterSave(final TenantDO tenant)
  {
    final PFUserDO adminUser = ThreadLocalUserContext.getUser();
    if (tenant.isDefault() == false) {
      // The groups do already exist for the default tenant.
      initDatabaseDao.internalCreateProjectForgeGroups(tenant, adminUser);
    } else {
      // Clear the dummy entry:
      TenantRegistryMap.getInstance().clear();
    }
    final Collection<TenantDO> tenantList = new ArrayList<TenantDO>();
    tenantList.add(tenant);
    if (tenant.getAssignedUsers() != null) {
      // Create history entry of PFUserDO for all assigned users:
      for (final PFUserDO user : tenant.getAssignedUsers()) {
        createHistoryEntry(user, null, tenantList);
      }
    }
  }

  /**
   * Creates for every user an history if the user is assigned or unassigned from this updated tenant.
   * 
   * @param tenant
   * @param dbTenant
   * @see org.projectforge.framework.persistence.api.BaseDao#afterUpdate(TenantDO, TenantDO)
   */
  @Override
  protected void afterUpdate(final TenantDO tenant, final TenantDO dbTenant)
  {
    final Set<PFUserDO> origAssignedUsers = dbTenant.getAssignedUsers();
    final Set<PFUserDO> assignedUsers = tenant.getAssignedUsers();
    final Collection<PFUserDO> assignedList = new ArrayList<PFUserDO>(); // List of new assigned users.
    final Collection<PFUserDO> unassignedList = new ArrayList<PFUserDO>(); // List of unassigned users.
    for (final PFUserDO user : tenant.getAssignedUsers()) {
      if (origAssignedUsers.contains(user) == false) {
        assignedList.add(user);
      }
    }
    for (final PFUserDO user : dbTenant.getAssignedUsers()) {
      if (assignedUsers.contains(user) == false) {
        unassignedList.add(user);
      }
    }
    final Collection<TenantDO> tenantList = new ArrayList<TenantDO>();
    tenantList.add(tenant);
    // Create history entry of PFUserDO for all new assigned users:
    for (final PFUserDO user : assignedList) {
      createHistoryEntry(user, null, tenantList);
    }
    // Create history entry of PFUserDO for all unassigned users:
    for (final PFUserDO user : unassignedList) {
      createHistoryEntry(user, tenantList, null);
    }
  }

  /**
   * Assigns tenants to and unassigns tenants from given user.
   * 
   * @param user
   * @param tenantsToAssign Tenants to assign (nullable).
   * @param tenantsToUnassign Tenants to unassign (nullable).
   * @throws AccessException
   */
  @Transactional(readOnly = false, propagation = Propagation.REQUIRES_NEW, isolation = Isolation.REPEATABLE_READ)
  public void assignTenants(final PFUserDO user, final Set<TenantDO> tenantsToAssign,
      final Set<TenantDO> tenantsToUnassign)
  {
    internalAssignTenants(user, tenantsToAssign, tenantsToUnassign, true, true);
  }

  @Transactional(readOnly = false, propagation = Propagation.REQUIRES_NEW, isolation = Isolation.REPEATABLE_READ)
  public void internalAssignTenants(final PFUserDO user, final Set<TenantDO> tenantsToAssign,
      final Set<TenantDO> tenantsToUnassign, boolean checkAccess, boolean createHistoryEntry)
      throws AccessException
  {
    getHibernateTemplate().refresh(user, LockMode.READ);
    if (checkAccess) {
      if (TenantChecker.isSuperAdmin(ThreadLocalUserContext.getUser()) == false) {
        log.warn("User has now access right to change assigned users of a tenant! Skipping assignment.");
        return;
      }
    }
    final List<TenantDO> assignedTenants = new ArrayList<TenantDO>();
    if (tenantsToAssign != null) {
      for (final TenantDO tenant : tenantsToAssign) {
        final TenantDO dbTenant = getHibernateTemplate().get(clazz, tenant.getId(), LockMode.PESSIMISTIC_WRITE);
        Set<PFUserDO> assignedUsers = dbTenant.getAssignedUsers();
        if (assignedUsers == null) {
          assignedUsers = new HashSet<PFUserDO>();
          dbTenant.setAssignedUsers(assignedUsers);
        }
        if (assignedUsers.contains(user) == false) {
          log.info("Assigning user '" + user.getUsername() + "' to tenant '" + dbTenant.getName() + "'.");
          assignedUsers.add(user);
          assignedTenants.add(dbTenant);
          dbTenant.setLastUpdate(); // Needed, otherwise TenantDO is not detected for hibernate history!
        } else {
          log.info("User '" + user.getUsername() + "' already assigned to tenant '" + dbTenant.getName() + "'.");
        }
      }
    }
    final List<TenantDO> unassignedTenants = new ArrayList<TenantDO>();
    if (tenantsToUnassign != null) {
      for (final TenantDO tenant : tenantsToUnassign) {
        final TenantDO dbTenant = getHibernateTemplate().get(clazz, tenant.getId(), LockMode.PESSIMISTIC_WRITE);
        final Set<PFUserDO> assignedUsers = dbTenant.getAssignedUsers();
        if (assignedUsers != null && assignedUsers.contains(user) == true) {
          log.info("Unassigning user '" + user.getUsername() + "' from tenant '" + dbTenant.getName() + "'.");
          assignedUsers.remove(user);
          unassignedTenants.add(dbTenant);
          dbTenant.setLastUpdate(); // Needed, otherwise TenantDO is not detected for hibernate history!
        } else {
          log.info("User '" + user.getUsername() + "' is not assigned to tenant '" + dbTenant.getName()
              + "' (can't unassign).");
        }
      }
    }
    flushSession();
    if (createHistoryEntry) {
      createHistoryEntry(user, unassignedTenants, assignedTenants);
    }
  }

  private void createHistoryEntry(final PFUserDO user, Collection<TenantDO> unassignedList,
      Collection<TenantDO> assignedList)
  {
    if (unassignedList != null && unassignedList.size() == 0) {
      unassignedList = null;
    }
    if (assignedList != null && assignedList.size() == 0) {
      assignedList = null;
    }
    if (unassignedList == null && assignedList == null) {
      return;
    }
    createHistoryEntry(user, user.getId(), "assignedTenants", TenantDO.class, unassignedList, assignedList);
  }

  @Override
  protected String[] getAdditionalSearchFields()
  {
    return ADDITIONAL_SEARCH_FIELDS;
  }

  @Override
  public TenantDO newInstance()
  {
    return new TenantDO();
  }

  /**
   * @see org.projectforge.framework.persistence.api.BaseDao#useOwnCriteriaCacheRegion()
   */
  @Override
  protected boolean useOwnCriteriaCacheRegion()
  {
    return true;
  }

  @Override
  public List<TenantDO> internalLoadAll()
  {
    return findAll();
  }

  public List<TenantDO> findAll()
  {
    return PfEmgrFactory.get().runRoTrans(emgr -> emgr.selectAllAttached(TenantDO.class));
  }

  public boolean hasTenants()
  {
    List<TenantDO> allTenants = findAll();
    return allTenants != null && allTenants.size() > 0;
  }

  @Transactional(readOnly = true, propagation = Propagation.SUPPORTS)
  public boolean tenantTableExists()
  {
    if (tenantTableExists == null) {
      final Table tenantTable = new Table(TenantDO.class);
      try {
        jdbc.queryForObject("SELECT COUNT(*) FROM " + tenantTable.getName(), Integer.class);
        tenantTableExists = true;
      } catch (final Exception ex) {
        log.info(
            "Exception while checking count from table: " + tenantTable.getName() + " Exception: " + ex.getMessage()
                + "*** OK, if database is empty or needs to migrate ***");
        tenantTableExists = false;
      }
    }
    return tenantTableExists;
  }

  public boolean hasAssignedTenants(PFUserDO user)
  {
    for (TenantDO tenant : findAll()) {
      for (PFUserDO tenantUser : tenant.getAssignedUsers()) {
        if (tenantUser.getId().equals(user.getId())) {
          return true;
        }
      }
    }
    return false;
  }

  public void resetTenantTableStatus()
  {
    tenantTableExists = null;
  }

}
