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

import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

import org.apache.commons.lang.Validate;
import org.projectforge.framework.cache.AbstractCache;
import org.projectforge.framework.persistence.api.BaseDO;
import org.projectforge.framework.persistence.user.api.ThreadLocalUserContext;
import org.projectforge.framework.persistence.user.entities.PFUserDO;
import org.projectforge.framework.persistence.user.entities.TenantDO;
import org.springframework.context.ApplicationContext;

/**
 * Holds TenantCachesHolder element and detaches them if not used for some time to save memory.
 *
 * @author Kai Reinhard (k.reinhard@micromata.de)
 */
public class TenantRegistryMap extends AbstractCache
{
  private static final long serialVersionUID = -7062742437818230657L;

  private static final org.apache.log4j.Logger log = org.apache.log4j.Logger.getLogger(TenantRegistryMap.class);

  private static final long EXPIRE_TIME = AbstractCache.TICKS_PER_DAY;

  private final Map<Integer, TenantRegistry> tenantRegistryMap = new HashMap<Integer, TenantRegistry>();

  private TenantRegistry singleTenantRegistry;

  private TenantRegistry dummyTenantRegistry;

  private static TenantRegistryMap instance = new TenantRegistryMap();

  private ApplicationContext applicationContext;

  private TenantChecker tenantChecker;

  private TenantService tenantService;

  public static TenantRegistryMap getInstance()
  {
    return instance;
  }

  private TenantRegistryMap()
  {
    super(EXPIRE_TIME);
  }

  public TenantRegistry getTenantRegistry(final BaseDO<?> obj)
  {
    if (obj == null) {
      return getTenantRegistry();
    }
    return getTenantRegistry(obj.getTenant());
  }

  public TenantRegistry getTenantRegistry(TenantDO tenant)
  {
    checkRefresh();
    if (tenantService.isMultiTenancyAvailable() == false) {
      if (tenant != null && !tenant.isDefault()) {
        log.warn("Oups, why call getTenantRegistry with tenant " + tenant.getId()
            + " if ProjectForge is running in single tenant mode?");
      }
      return getSingleTenantRegistry();
    }
    if (tenant == null) {
      final TenantDO defaultTenant = tenantService.getDefaultTenant();
      if (defaultTenant != null) {
        final PFUserDO user = ThreadLocalUserContext.getUser();
        if (tenantChecker.isPartOfTenant(defaultTenant, user)) {
          tenant = defaultTenant;
        }
      }
    }
    if (tenant == null) {
      final PFUserDO user = ThreadLocalUserContext.getUser();
      if (user == null) {
        return getDummyTenantRegistry();
      }
      throw new IllegalArgumentException("No default tenant found for user: " + user.getUsername());
      // if (UserRights.getAccessChecker().isUserMemberOfAdminGroup(tenant, user) == true) {
      // throw new AccessException("multitenancy.accessException.noTenant.adminUser");
      // }
      // throw new AccessException("multitenancy.accessException.noTenant.nonAdminUser");
    }
    Validate.notNull(tenant);
    synchronized (this) {
      TenantRegistry registry = tenantRegistryMap.get(tenant.getId());
      if (registry == null) {
        registry = new TenantRegistry(tenant, applicationContext);
        tenantRegistryMap.put(tenant.getId(), registry);
      }
      return registry;
    }
  }

  public TenantRegistry getTenantRegistry()
  {
    if (tenantService.isMultiTenancyAvailable() == false) {
      return getSingleTenantRegistry();
    }
    final TenantDO tenant = tenantChecker.getCurrentTenant();
    return getTenantRegistry(tenant);
  }

  private TenantRegistry getSingleTenantRegistry()
  {
    if (singleTenantRegistry == null) {
      singleTenantRegistry = createSingleTenantRegistry();
    }
    return singleTenantRegistry;
  }

  private TenantRegistry createSingleTenantRegistry()
  {
    synchronized (this) {
      return new TenantRegistry(tenantService.getDefaultTenant(), applicationContext);
    }
  }

  /**
   * The dummy tenant registry is implemented only for misconfigured systems, meaning the administrator has configured
   * tenants but no default tenant and the users try to login in, but no tenant will be found (error messages will
   * occur). The super admin is the onliest user to fix this issue (because the system is available due to this dummy
   * tenant).
   *
   * @return
   */
  private TenantRegistry getDummyTenantRegistry()
  {
    if (dummyTenantRegistry == null) {
      createDummyTenantRegistry();
    }
    return dummyTenantRegistry;
  }

  private void createDummyTenantRegistry()
  {
    synchronized (this) {
      final TenantDO dummyTenant = new TenantDO().setName("Dummy tenant").setShortName("Dummy tenant")
          .setDescription("This tenant is only a technical tenant, if no default tenant is given.");
      dummyTenant.setId(-1);
      dummyTenantRegistry = new TenantRegistry(dummyTenant, applicationContext);
    }
  }

  public void clear()
  {
    synchronized (this) {
      singleTenantRegistry = null;
      dummyTenantRegistry = null;
      tenantRegistryMap.clear();
    }
  }

  /**
   * @see org.projectforge.framework.cache.AbstractCache#refresh()
   */
  @Override
  protected void refresh()
  {
    log.info("Refreshing " + TenantRegistry.class.getName() + "...");
    final Iterator<Map.Entry<Integer, TenantRegistry>> it = tenantRegistryMap.entrySet().iterator();
    while (it.hasNext() == true) {
      final Map.Entry<Integer, TenantRegistry> entry = it.next();
      final TenantRegistry registry = entry.getValue();
      if (registry.isOutdated() == true) {
        final TenantDO tenant = registry.getTenant();
        log.info("Detaching caches of tenant '"
            + (tenant != null ? tenant.getShortName() : "null")
            + "' with id "
            + (tenant != null ? tenant.getId() : "null"));
        it.remove();
      }
    }
    log.info("Refreshing of " + TenantRegistry.class.getName() + " done.");
  }

  public void setAllUserGroupCachesAsExpired()
  {
    for (final TenantRegistry registry : tenantRegistryMap.values()) {
      registry.getUserGroupCache().setExpired();
    }
  }

  public void setApplicationContext(ApplicationContext applicationContext)
  {
    this.applicationContext = applicationContext;
    this.tenantChecker = applicationContext.getBean(TenantChecker.class);
    this.tenantService = applicationContext.getBean(TenantService.class);
  }

}
