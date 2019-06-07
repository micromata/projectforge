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

package org.projectforge.plugins.marketing;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.FetchType;
import javax.persistence.JoinColumn;
import javax.persistence.ManyToOne;
import javax.persistence.Table;
import javax.persistence.Transient;
import javax.persistence.UniqueConstraint;

import org.hibernate.search.annotations.Indexed;
import org.hibernate.search.annotations.IndexedEmbedded;
import org.projectforge.business.address.AddressDO;
import org.projectforge.framework.persistence.api.Constants;
import org.projectforge.framework.persistence.entities.DefaultBaseDO;

/**
 * A marketing campaign.
 * 
 * @author Kai Reinhard (k.reinhard@micromata.de)
 * 
 */
@Entity
@Indexed
@Table(name = "T_PLUGIN_MARKETING_ADDRESS_CAMPAIGN_VALUE",
    uniqueConstraints = {
        @UniqueConstraint(columnNames = { "address_fk", "address_campaign_fk" })
    },
    indexes = {
        @javax.persistence.Index(name = "idx_fk_t_plugin_marketing_address_campaign_value_address_campai",
            columnList = "address_campaign_fk"),
        @javax.persistence.Index(name = "idx_fk_t_plugin_marketing_address_campaign_value_address_fk",
            columnList = "address_fk"),
        @javax.persistence.Index(name = "idx_fk_t_plugin_marketing_address_campaign_value_tenant_id",
            columnList = "tenant_id")
    })
public class AddressCampaignValueDO extends DefaultBaseDO
{
  private static final long serialVersionUID = 4414457700384141088L;

  @IndexedEmbedded(depth = 1)
  private AddressCampaignDO addressCampaign;

  @IndexedEmbedded(depth = 1)
  private AddressDO address;

  private String value;

  private String comment;

  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "address_campaign_fk", nullable = false)
  public AddressCampaignDO getAddressCampaign()
  {
    return addressCampaign;
  }

  public void setAddressCampaign(final AddressCampaignDO addressCampaign)
  {
    this.addressCampaign = addressCampaign;
  }

  @Transient
  public Integer getAddressCampaignId()
  {
    return addressCampaign != null ? addressCampaign.getId() : null;
  }

  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "address_fk", nullable = false)
  public AddressDO getAddress()
  {
    return address;
  }

  public void setAddress(final AddressDO address)
  {
    this.address = address;
  }

  @Transient
  public Integer getAddressId()
  {
    return this.address != null ? this.address.getId() : null;
  }

  @Column(length = AddressCampaignDO.MAX_VALUE_LENGTH)
  public String getValue()
  {
    return value;
  }

  public void setValue(final String value)
  {
    this.value = value;
  }

  @Column(length = Constants.LENGTH_COMMENT)
  public String getComment()
  {
    return comment;
  }

  public void setComment(final String comment)
  {
    this.comment = comment;
  }
}
