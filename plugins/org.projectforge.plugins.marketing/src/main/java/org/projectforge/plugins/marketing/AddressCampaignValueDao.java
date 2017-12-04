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

package org.projectforge.plugins.marketing;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.commons.lang3.StringUtils;
import org.hibernate.Session;
import org.hibernate.criterion.Restrictions;
import org.projectforge.business.address.AddressDO;
import org.projectforge.business.address.AddressDao;
import org.projectforge.business.user.UserGroupCache;
import org.projectforge.framework.i18n.UserException;
import org.projectforge.framework.persistence.api.BaseDao;
import org.projectforge.framework.persistence.api.BaseSearchFilter;
import org.projectforge.framework.persistence.api.QueryFilter;
import org.projectforge.framework.persistence.history.DisplayHistoryEntry;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.CollectionUtils;

import de.micromata.genome.db.jpa.history.api.DiffEntry;
import de.micromata.genome.db.jpa.history.api.HistProp;
import de.micromata.genome.db.jpa.history.api.HistoryEntry;

/**
 * @author Kai Reinhard (k.reinhard@micromata.de)
 */
@Repository
public class AddressCampaignValueDao extends BaseDao<AddressCampaignValueDO>
{
  private static final org.slf4j.Logger log = org.slf4j.LoggerFactory.getLogger(AddressCampaignValueDao.class);

  @Autowired
  private AddressDao addressDao;

  public AddressCampaignValueDao()
  {
    super(AddressCampaignValueDO.class);
    userRightId = MarketingPluginUserRightId.PLUGIN_MARKETING_ADDRESS_CAMPAIGN_VALUE;
  }

  @SuppressWarnings("unchecked")
  public AddressCampaignValueDO get(final Integer addressId, final Integer addressCampaignId)
  {
    final List<AddressCampaignValueDO> list = (List<AddressCampaignValueDO>) getHibernateTemplate().find(
        "from AddressCampaignValueDO a where a.address.id = ? and a.addressCampaign.id = ?",
        new Object[] { addressId, addressCampaignId });
    if (CollectionUtils.isEmpty(list) == true) {
      return null;
    }
    return list.get(0);
  }

  @Override
  public List<AddressCampaignValueDO> getList(final BaseSearchFilter filter)
  {
    final AddressCampaignValueFilter myFilter;
    if (filter instanceof AddressCampaignValueFilter) {
      myFilter = (AddressCampaignValueFilter) filter;
    } else {
      myFilter = new AddressCampaignValueFilter(filter);
    }
    final QueryFilter queryFilter = new QueryFilter(myFilter);
    if (myFilter.getAddressCampaign() != null) {
      queryFilter.add(Restrictions.eq("address_campaign_fk", myFilter.getAddressCampaign().getId()));
    }
    if (myFilter.getAddressCampaignValue() != null) {
      queryFilter.add(Restrictions.eq("value", myFilter.getAddressCampaign().getId()));
    }
    return getList(queryFilter);
  }

  /**
   * @param address
   * @param taskId  If null, then task will be set to null;
   * @see BaseDao#getOrLoad(Integer)
   */
  public void setAddress(final AddressCampaignValueDO addressCampaignValue, final Integer addressId)
  {
    final AddressDO address = addressDao.getOrLoad(addressId);
    addressCampaignValue.setAddress(address);
  }

  @Transactional(readOnly = false, propagation = Propagation.REQUIRES_NEW)
  public void massUpdate(final List<AddressDO> list, final AddressCampaignDO addressCampaign, final String value,
      final String comment)
  {
    if (list == null || list.size() == 0) {
      // No entries to update.
      return;
    }
    if (list.size() > MAX_MASS_UPDATE) {
      throw new UserException(MAX_MASS_UPDATE_EXCEEDED_EXCEPTION_I18N, new Object[] { MAX_MASS_UPDATE });
    }
    for (final AddressDO address : list) {
      AddressCampaignValueDO addressCampaignValue = get(address.getId(), addressCampaign.getId());
      if (addressCampaignValue == null) {
        addressCampaignValue = new AddressCampaignValueDO();
        setAddress(addressCampaignValue, address.getId());
        addressCampaignValue.setAddressCampaign(addressCampaign);
      }
      if (value != null) {
        addressCampaignValue.setValue(value);
      }
      if (StringUtils.isEmpty(comment) == false) {
        addressCampaignValue.setComment(comment);
      }
      if (addressCampaignValue.getId() != null) {
        try {
          addressCampaignValue.setDeleted(false);
          update(addressCampaignValue);
        } catch (final Exception ex) {
          log.info("Exception occured while updating entry inside mass update: " + addressCampaignValue);
        }
      } else {
        try {
          save(addressCampaignValue);
        } catch (final Exception ex) {
          log.info("Exception occured while inserting entry inside mass update: " + addressCampaignValue);
        }
      }
    }
  }

  @Override
  public AddressCampaignValueDO newInstance()
  {
    return new AddressCampaignValueDO();
  }

  public Map<Integer, AddressCampaignValueDO> getAddressCampaignValuesByAddressId(
      final AddressCampaignValueFilter searchFilter)
  {
    final HashMap<Integer, AddressCampaignValueDO> map = new HashMap<Integer, AddressCampaignValueDO>();
    return getAddressCampaignValuesByAddressId(map, searchFilter);
  }

  public Map<Integer, AddressCampaignValueDO> getAddressCampaignValuesByAddressId(
      final Map<Integer, AddressCampaignValueDO> map,
      final AddressCampaignValueFilter searchFilter)
  {
    map.clear();
    final Integer addressCampaignId = searchFilter.getAddressCampaignId();
    if (addressCampaignId == null) {
      return map;
    }
    @SuppressWarnings("unchecked")
    final List<AddressCampaignValueDO> list = (List<AddressCampaignValueDO>) getHibernateTemplate().find(
        "from AddressCampaignValueDO a where a.addressCampaign.id = ? and deleted = false",
        searchFilter.getAddressCampaignId());
    if (CollectionUtils.isEmpty(list) == true) {
      return map;
    }
    for (final AddressCampaignValueDO addressCampaignValue : list) {
      map.put(addressCampaignValue.getAddressId(), addressCampaignValue);
    }
    return map;
  }

  @Override
  public List<DisplayHistoryEntry> convert(final HistoryEntry<?> entry, final Session session)
  {
    if (entry.getDiffEntries().isEmpty() == true) {
      final DisplayHistoryEntry se = new DisplayHistoryEntry(getUserGroupCache(), entry);
      return Collections.singletonList(se);
    }
    List<DisplayHistoryEntry> result = new ArrayList<>();
    for (DiffEntry prop : entry.getDiffEntries()) {
      DisplayHistoryEntry se = new DisplayHistoryEntry(getUserGroupCache(), entry, prop, session)
      {
        @Override
        protected Object getObjectValue(UserGroupCache userGroupCache, Session session, HistProp prop)
        {
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

          return super.getObjectValue(userGroupCache, session, prop);
        }
      };
      result.add(se);
    }

    return result;
  }

  public void setAddressDao(final AddressDao addressDao)
  {
    this.addressDao = addressDao;
  }
}
