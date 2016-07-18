package org.projectforge.business.multitenancy;

import java.util.Collection;

import org.projectforge.framework.persistence.user.entities.PFUserDO;
import org.projectforge.framework.persistence.user.entities.TenantDO;

public interface TenantService
{

  TenantDO getDefaultTenant();

  boolean isMultiTenancyAvailable();

  boolean hasTenants();

  String getUsernameCommaList(TenantDO tenant);

  TenantDO getTenant(Integer id);

  Collection<TenantDO> getTenantsOfUser(Integer userId);

  Collection<TenantDO> getTenantsOfLoggedInUser();

  boolean isUserAssignedToTenant(Integer tenantId, Integer userId);

  boolean isUserAssignedToTenant(TenantDO tenant, Integer userId);

  Collection<TenantDO> getAllTenants();

  boolean hasSelectAccess(PFUserDO loggedInUser, TenantDO tenant, boolean b);

  String getLogName(TenantDO tenant);

  String getTenantShortNames(Collection<TenantDO> tenants);

  void resetTenantTableStatus();

}
