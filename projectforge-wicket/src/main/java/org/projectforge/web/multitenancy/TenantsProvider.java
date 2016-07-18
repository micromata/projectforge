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

package org.projectforge.web.multitenancy;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.TreeSet;

import org.apache.commons.lang.StringUtils;
import org.projectforge.business.multitenancy.TenantService;
import org.projectforge.business.multitenancy.TenantsComparator;
import org.projectforge.common.StringHelper;
import org.projectforge.framework.persistence.user.api.ThreadLocalUserContext;
import org.projectforge.framework.persistence.user.entities.PFUserDO;
import org.projectforge.framework.persistence.user.entities.TenantDO;
import org.projectforge.framework.utils.NumberHelper;

import com.vaynberg.wicket.select2.Response;
import com.vaynberg.wicket.select2.TextChoiceProvider;

public class TenantsProvider extends TextChoiceProvider<TenantDO>
{
  private static final org.apache.log4j.Logger log = org.apache.log4j.Logger.getLogger(TenantsProvider.class);

  private static final long serialVersionUID = 6228672635966093252L;

  private TenantService tenantService;

  private int pageSize = 20;

  private final TenantsComparator tenantsComparator = new TenantsComparator();

  private Collection<TenantDO> sortedTenants;

  public TenantsProvider(TenantService tenantService)
  {
    this.tenantService = tenantService;
  }

  /**
   * @param tenantIds
   * @return
   */
  public List<String> getTenantNames(final String tenantIds)
  {
    if (StringUtils.isEmpty(tenantIds) == true) {
      return null;
    }
    final int[] ids = StringHelper.splitToInts(tenantIds, ",", false);
    final List<String> list = new ArrayList<String>();
    for (final int id : ids) {
      final TenantDO tenant = tenantService.getTenant(id);
      if (tenant != null) {
        list.add(tenant.getName());
      } else {
        log.warn("Tenant with id '" + id + "' not found in UserTenantCache. tenantIds string was: " + tenantIds);
      }
    }
    return list;
  }

  /**
   * 
   * @param tenantIds
   * @return
   */
  public Collection<TenantDO> getSortedTenants(final String tenantIds)
  {
    if (StringUtils.isEmpty(tenantIds) == true) {
      return null;
    }
    sortedTenants = new TreeSet<TenantDO>(tenantsComparator);
    final int[] ids = StringHelper.splitToInts(tenantIds, ",", false);
    for (final int id : ids) {
      final TenantDO tenant = tenantService.getTenant(id);
      if (tenant != null) {
        sortedTenants.add(tenant);
      } else {
        log.warn("Tenant with id '" + id + "' not found in UserTenantCache. tenantIds string was: " + tenantIds);
      }
    }
    return sortedTenants;
  }

  public String getTenantIds(final Collection<TenantDO> tenants)
  {
    final StringBuffer buf = new StringBuffer();
    boolean first = true;
    for (final TenantDO tenant : tenants) {
      if (tenant.getId() != null) {
        first = StringHelper.append(buf, first, String.valueOf(tenant.getId()), ",");
      }
    }
    return buf.toString();
  }

  public Collection<TenantDO> getSortedTenants()
  {
    if (sortedTenants == null) {
      final Collection<TenantDO> allTenants = tenantService.getAllTenants();
      sortedTenants = new TreeSet<TenantDO>(tenantsComparator);
      final PFUserDO loggedInUser = ThreadLocalUserContext.getUser();
      if (allTenants != null) {
        for (final TenantDO tenant : allTenants) {
          if (tenant.isDeleted() == false && tenantService.hasSelectAccess(loggedInUser, tenant, false) == true) {
            sortedTenants.add(tenant);
          }
        }
      }
    }
    return sortedTenants;
  }

  /**
   * @param pageSize the pageSize to set
   * @return this for chaining.
   */
  public TenantsProvider setPageSize(final int pageSize)
  {
    this.pageSize = pageSize;
    return this;
  }

  /**
   * @see com.vaynberg.wicket.select2.TextChoiceProvider#getDisplayText(java.lang.Object)
   */
  @Override
  protected String getDisplayText(final TenantDO choice)
  {
    return choice.getName();
  }

  /**
   * @see com.vaynberg.wicket.select2.TextChoiceProvider#getId(java.lang.Object)
   */
  @Override
  protected Object getId(final TenantDO choice)
  {
    return choice.getId();
  }

  /**
   * @see com.vaynberg.wicket.select2.ChoiceProvider#query(java.lang.String, int, com.vaynberg.wicket.select2.Response)
   */
  @Override
  public void query(String term, final int page, final Response<TenantDO> response)
  {
    final Collection<TenantDO> sortedTenants = getSortedTenants();
    final List<TenantDO> result = new ArrayList<TenantDO>();
    term = term.toLowerCase();

    final int offset = page * pageSize;

    int matched = 0;
    boolean hasMore = false;
    for (final TenantDO tenant : sortedTenants) {
      if (result.size() == pageSize) {
        hasMore = true;
        break;
      }
      if (tenant.getName().toLowerCase().contains(term) == true) {
        matched++;
        if (matched > offset) {
          result.add(tenant);
        }
      }
    }
    response.addAll(result);
    response.setHasMore(hasMore);
  }

  /**
   * @see com.vaynberg.wicket.select2.ChoiceProvider#toChoices(java.util.Collection)
   */
  @Override
  public Collection<TenantDO> toChoices(final Collection<String> ids)
  {
    final List<TenantDO> list = new ArrayList<TenantDO>();
    if (ids == null) {
      return list;
    }
    for (final String str : ids) {
      final Integer tenantId = NumberHelper.parseInteger(str);
      if (tenantId == null) {
        continue;
      }
      final TenantDO tenant = tenantService.getTenant(tenantId);
      if (tenant != null) {
        list.add(tenant);
      }
    }
    return list;
  }

}
