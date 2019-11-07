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

package org.projectforge.business.multitenancy;

import org.apache.commons.collections.CollectionUtils;
import org.projectforge.business.configuration.ConfigurationService;
import org.projectforge.framework.cache.AbstractCache;
import org.projectforge.framework.persistence.user.api.ThreadLocalUserContext;
import org.projectforge.framework.persistence.user.entities.PFUserDO;
import org.projectforge.framework.persistence.user.entities.TenantDO;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationContext;
import org.springframework.stereotype.Component;

import javax.annotation.PostConstruct;
import javax.persistence.EntityManager;
import java.util.*;

/**
 * Caches the tenants.
 *
 * @author Kai Reinhard (k.reinhard@micromata.de)
 */
@Component
public class TenantsCache extends AbstractCache {
  private static final long serialVersionUID = 8692234056373706543L;

  private static Logger log = LoggerFactory.getLogger(TenantsCache.class);

  @Autowired
  private ApplicationContext applicationContext;

  @Autowired
  private ConfigurationService configService;

  @Autowired
  private EntityManager em;

  /**
   * Collection of all tenants (without deleted ones).
   */
  private Collection<TenantDO> tenants;

  private TenantDO defaultTenant;

  /**
   * Key is the user id and the value is a set of the assigned tenants.
   */
  private Map<Integer, Set<TenantDO>> userTenantMap;

  @PostConstruct
  public void init() {
    TenantRegistryMap tenantRegistryMap = TenantRegistryMap.getInstance();
    tenantRegistryMap.setApplicationContext(applicationContext);
  }

  public boolean isEmpty() {
    checkRefresh();
    return CollectionUtils.isEmpty(tenants);
  }

  public String getLogName(final TenantDO tenant) {
    final TenantDO t = getTenant(tenant.getId());
    return "#" + t.getId() + " '" + t.getShortName() + "'";
  }

  /**
   * @return the defaultTenant
   */
  public TenantDO getDefaultTenant() {
    checkRefresh();
    return defaultTenant;
  }

  public TenantDO getTenant(final Integer id) {
    if (id == null) {
      return null;
    }
    checkRefresh();
    if (tenants == null) {
      return null;
    }
    for (final TenantDO tenant : tenants) {
      if (id.equals(tenant.getId())) {
        return tenant;
      }
    }
    return null;
  }

  /**
   * @return the tenants
   */
  public Collection<TenantDO> getTenants() {
    checkRefresh();
    return tenants;
  }

  public boolean hasTenants() {
    checkRefresh();
    return tenants != null && tenants.size() > 0;
  }

  /**
   * @return the tenants
   */
  public Collection<TenantDO> getTenantsOfLoggedInUser() {
    return getTenantsOfUser(ThreadLocalUserContext.getUserId());
  }

  /**
   * @param userId
   * @return the tenants
   */
  public Collection<TenantDO> getTenantsOfUser(final Integer userId) {
    checkRefresh();
    return userTenantMap.get(userId);
  }

  public boolean isUserAssignedToTenant(final Integer tenantId, final Integer userId) {
    if (tenantId == null || userId == null) {
      return false;
    }
    final TenantDO tenant = getTenant(tenantId);
    return isUserAssignedToTenant(tenant, userId);
  }

  public boolean isMultiTenancyAvailable() {
    return configService.isMultiTenancyConfigured()
            && hasTenants();
  }

  /**
   * @param tenant
   * @param userId
   * @return true if tenant is not null and not deleted and the given user is assigned to the given tenant. Otherwise
   * false.
   */
  public boolean isUserAssignedToTenant(final TenantDO tenant, final Integer userId) {
    if (tenant == null || tenant.getId() == null) {
      return false;
    }
    checkRefresh();
    final Set<TenantDO> assignedTenants = userTenantMap.get(userId);
    if (assignedTenants == null) {
      return false;
    }
    for (final TenantDO assignedTenant : assignedTenants) {
      if (tenant.getId().equals(assignedTenant.getId())) {
        return true;
      }
    }
    return false;
  }

  /**
   * This method will be called by CacheHelper and is synchronized via getData();
   */
  @Override
  @SuppressWarnings("unchecked")
  protected void refresh() {
    log.info("Initializing TenantsCache ...");
    // This method must not be synchronized because it works with a new copy of maps.
    final List<TenantDO> list = em.createQuery("from TenantDO as tenant left join fetch tenant.assignedUsers where tenant.deleted=false", TenantDO.class)
            .getResultList();
    final Map<Integer, Set<TenantDO>> map = new HashMap<>();
    final Collection<PFUserDO> users = TenantRegistryMap.getInstance().getTenantRegistry().getUserGroupCache()
            .getAllUsers();
    for (final PFUserDO user : users) {
      if (user.isDeleted()) {
        continue;
      }
      final boolean superAdmin = TenantChecker.isSuperAdmin(user);
      if (list != null) {
        final Set<TenantDO> set = new TreeSet<>(new TenantsComparator());
        for (final TenantDO tenant : list) {
          if (superAdmin) {
            set.add(tenant);
          }
          final Collection<PFUserDO> assignedUsers = tenant.getAssignedUsers();
          if (assignedUsers == null) {
            continue;
          }
          for (final PFUserDO assignedUser : assignedUsers) {
            if (user.getId().equals(assignedUser.getId())) {
              // User is assigned to the given tenant.
              set.add(tenant);
              continue;
            }
          }
        }
        if (!set.isEmpty()) {
          map.put(user.getId(), set);
        }
      }
    }
    if (list != null) {
      for (final TenantDO tenant : list) {
        if (tenant.isDefault()) {
          this.defaultTenant = tenant;
        }
      }
    }
    this.tenants = list;
    this.userTenantMap = map;
    log.info("Initializing of TenantsCache done.");
  }

}
