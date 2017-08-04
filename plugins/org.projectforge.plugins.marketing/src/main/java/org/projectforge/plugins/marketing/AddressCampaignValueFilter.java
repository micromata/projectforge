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

import org.projectforge.business.address.AddressFilter;
import org.projectforge.framework.persistence.api.BaseSearchFilter;

/**
 * @author Kai Reinhard (k.reinhard@micromata.de)
 */
public class AddressCampaignValueFilter extends AddressFilter
{
  private static final long serialVersionUID = 5731140604154434730L;

  private AddressCampaignDO addressCampaign;

  private String addressCampaignValue;

  public AddressCampaignValueFilter()
  {

  }

  public AddressCampaignValueFilter(final BaseSearchFilter filter)
  {
    super(filter);
    if (filter instanceof AddressCampaignValueFilter) {
      AddressCampaignValueFilter obj = (AddressCampaignValueFilter) filter;
      this.addressCampaign = obj.getAddressCampaign();
      this.addressCampaignValue = obj.getAddressCampaignValue();
    }
  }

  public AddressCampaignDO getAddressCampaign()
  {
    return addressCampaign;
  }

  public Integer getAddressCampaignId()
  {
    return addressCampaign != null ? addressCampaign.getId() : null;
  }

  /**
   * @param addressCampaign
   * @return this for chaining.
   */
  public AddressCampaignValueFilter setAddressCampaign(final AddressCampaignDO addressCampaign)
  {
    this.addressCampaign = addressCampaign;
    return this;
  }

  public String getAddressCampaignValue()
  {
    return addressCampaignValue;
  }

  public void setAddressCampaignValue(final String addressCampaignValue)
  {
    this.addressCampaignValue = addressCampaignValue;
  }
}
