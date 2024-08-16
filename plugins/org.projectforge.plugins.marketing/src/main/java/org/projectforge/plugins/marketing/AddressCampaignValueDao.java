/////////////////////////////////////////////////////////////////////////////
//
// Project ProjectForge Community Edition
//         www.projectforge.org
//
// Copyright (C) 2001-2024 Micromata GmbH, Germany (www.micromata.com)
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

package org.projectforge.plugins.marketing;

import de.micromata.genome.db.jpa.history.api.DiffEntry;
import de.micromata.genome.db.jpa.history.api.HistProp;
import de.micromata.genome.db.jpa.history.api.HistoryEntry;
import org.apache.commons.lang3.StringUtils;
import org.projectforge.business.address.AddressDO;
import org.projectforge.business.address.AddressDao;
import org.projectforge.business.user.UserGroupCache;
import org.projectforge.common.i18n.UserException;
import org.projectforge.framework.persistence.api.BaseDao;
import org.projectforge.framework.persistence.api.BaseSearchFilter;
import org.projectforge.framework.persistence.api.QueryFilter;
import org.projectforge.framework.persistence.history.DisplayHistoryEntry;
import org.projectforge.framework.persistence.utils.SQLHelper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Repository;
import org.springframework.util.CollectionUtils;

import jakarta.persistence.EntityManager;
import java.util.*;

/**
 * @author Kai Reinhard (k.reinhard@micromata.de)
 */
@Repository
public class AddressCampaignValueDao extends BaseDao<AddressCampaignValueDO> {
  private static final org.slf4j.Logger log = org.slf4j.LoggerFactory.getLogger(AddressCampaignValueDao.class);

  @Autowired
  private AddressDao addressDao;

  public AddressCampaignValueDao() {
    super(AddressCampaignValueDO.class);
    userRightId = MarketingPluginUserRightId.PLUGIN_MARKETING_ADDRESS_CAMPAIGN_VALUE;
  }

  public AddressCampaignValueDO get(final Integer addressId, final Integer addressCampaignId) {
    return SQLHelper.ensureUniqueResult(em
            .createNamedQuery(AddressCampaignValueDO.FIND_BY_ADDRESS_AND_CAMPAIGN, AddressCampaignValueDO.class)
            .setParameter("addressId", addressId)
            .setParameter("addressCampaignId", addressCampaignId));
  }

  @Override
  public List<AddressCampaignValueDO> getList(final BaseSearchFilter filter) {
    final AddressCampaignValueFilter myFilter;
    if (filter instanceof AddressCampaignValueFilter) {
      myFilter = (AddressCampaignValueFilter) filter;
    } else {
      myFilter = new AddressCampaignValueFilter(filter);
    }
    final QueryFilter queryFilter = new QueryFilter(myFilter);
    if (myFilter.getAddressCampaign() != null) {
      queryFilter.add(QueryFilter.eq("address_campaign_fk", myFilter.getAddressCampaign().getId()));
    }
    if (myFilter.getAddressCampaignValue() != null) {
      queryFilter.add(QueryFilter.eq("value", myFilter.getAddressCampaign().getId()));
    }
    return getList(queryFilter);
  }

  /**
   * @see BaseDao#getOrLoad(Integer)
   */
  public void setAddress(final AddressCampaignValueDO addressCampaignValue, final Integer addressId) {
    final AddressDO address = addressDao.getOrLoad(addressId);
    addressCampaignValue.setAddress(address);
  }

  @Override
  public AddressCampaignValueDO newInstance() {
    return new AddressCampaignValueDO();
  }

  public Map<Integer, AddressCampaignValueDO> getAddressCampaignValuesByAddressId(
          final AddressCampaignValueFilter searchFilter) {
    final HashMap<Integer, AddressCampaignValueDO> map = new HashMap<>();
    return getAddressCampaignValuesByAddressId(map, searchFilter);
  }

  public Map<Integer, AddressCampaignValueDO> getAddressCampaignValuesByAddressId(
          final Map<Integer, AddressCampaignValueDO> map,
          final AddressCampaignValueFilter searchFilter) {
    map.clear();
    final Integer addressCampaignId = searchFilter.getAddressCampaignId();
    if (addressCampaignId == null) {
      return map;
    }
    final List<AddressCampaignValueDO> list = em
            .createNamedQuery(AddressCampaignValueDO.FIND_BY_CAMPAIGN, AddressCampaignValueDO.class)
            .setParameter("addressCampaignId", searchFilter.getAddressCampaignId())
            .getResultList();
    if (CollectionUtils.isEmpty(list)) {
      return map;
    }
    for (final AddressCampaignValueDO addressCampaignValue : list) {
      map.put(addressCampaignValue.getAddressId(), addressCampaignValue);
    }
    return map;
  }

  public Map<Integer, AddressCampaignValueDO> getAddressCampaignValuesByAddressId(
      final Map<Integer, AddressCampaignValueDO> map,
      final Integer addressCampaignId) {
    map.clear();
    if (addressCampaignId == null) {
      return map;
    }
    final List<AddressCampaignValueDO> list = em
        .createNamedQuery(AddressCampaignValueDO.FIND_BY_CAMPAIGN, AddressCampaignValueDO.class)
        .setParameter("addressCampaignId", addressCampaignId)
        .getResultList();
    if (CollectionUtils.isEmpty(list)) {
      return map;
    }
    for (final AddressCampaignValueDO addressCampaignValue : list) {
      map.put(addressCampaignValue.getAddressId(), addressCampaignValue);
    }
    return map;
  }

  @Override
  public List<DisplayHistoryEntry> convert(final HistoryEntry<?> entry, final EntityManager em) {
    if (entry.getDiffEntries().isEmpty()) {
      final DisplayHistoryEntry se = new DisplayHistoryEntry(userGroupCache, entry);
      return Collections.singletonList(se);
    }
    List<DisplayHistoryEntry> result = new ArrayList<>();
    for (DiffEntry prop : entry.getDiffEntries()) {
      DisplayHistoryEntry se = new DisplayHistoryEntry(userGroupCache, entry, prop, em) {
        @Override
        protected Object getObjectValue(UserGroupCache userGroupCache, EntityManager em, HistProp prop) {
          if (prop == null) {
            return null;
          }

          String type = prop.getType();

          if (AddressDO.class.getName().equals(type)) {
            return prop.getValue();
          }
          if (AddressCampaignDO.class.getName().equals(type)) {
            return prop.getValue();
          }

          return super.getObjectValue(userGroupCache, em, prop);
        }
      };
      result.add(se);
    }

    return result;
  }

  public void setAddressDao(final AddressDao addressDao) {
    this.addressDao = addressDao;
  }
}
