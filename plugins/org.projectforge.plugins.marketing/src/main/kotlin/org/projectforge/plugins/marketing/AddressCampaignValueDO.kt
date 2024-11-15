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

package org.projectforge.plugins.marketing

import org.hibernate.search.mapper.pojo.mapping.definition.annotation.Indexed
import org.hibernate.search.mapper.pojo.mapping.definition.annotation.IndexedEmbedded
import org.projectforge.business.address.AddressDO
import org.projectforge.common.anots.PropertyInfo
import org.projectforge.Constants
import org.projectforge.framework.persistence.entities.DefaultBaseDO
import jakarta.persistence.*
import org.hibernate.search.mapper.pojo.automaticindexing.ReindexOnUpdate
import org.hibernate.search.mapper.pojo.mapping.definition.annotation.IndexingDependency

/**
 * A marketing campaign.
 *
 * @author Kai Reinhard (k.reinhard@micromata.de)
 */
@Entity
@Indexed
@Table(
  name = "T_PLUGIN_MARKETING_ADDRESS_CAMPAIGN_VALUE",
  uniqueConstraints = [UniqueConstraint(columnNames = ["address_fk", "address_campaign_fk"])],
  indexes = [Index(
    name = "idx_fk_t_plugin_marketing_address_campaign_value_address_campai",
    columnList = "address_campaign_fk"
  ), Index(name = "idx_fk_t_plugin_marketing_address_campaign_value_address_fk", columnList = "address_fk")]
)
@NamedQueries(
  NamedQuery(
    name = AddressCampaignValueDO.DELETE_BY_ADDRESS,
    query = "delete from AddressCampaignValueDO where address.id=:addressId"
  ),
  NamedQuery(
    name = AddressCampaignValueDO.FIND_BY_ADDRESS_AND_CAMPAIGN,
    query = "from AddressCampaignValueDO where address.id=:addressId and addressCampaign.id=:addressCampaignId"
  ),
  NamedQuery(
    name = AddressCampaignValueDO.FIND_BY_CAMPAIGN,
    query = "from AddressCampaignValueDO where addressCampaign.id=:addressCampaignId and deleted=false"
  )
)
open class AddressCampaignValueDO : DefaultBaseDO() {

  @IndexedEmbedded(includeDepth = 1)
  @IndexingDependency(reindexOnUpdate = ReindexOnUpdate.SHALLOW)
  @get:ManyToOne(fetch = FetchType.LAZY)
  @get:JoinColumn(name = "address_campaign_fk", nullable = false)
  open var addressCampaign: AddressCampaignDO? = null

  @IndexedEmbedded(includeDepth = 1)
  @IndexingDependency(reindexOnUpdate = ReindexOnUpdate.SHALLOW)
  @get:ManyToOne(fetch = FetchType.LAZY)
  @get:JoinColumn(name = "address_fk", nullable = false)
  open var address: AddressDO? = null

  @PropertyInfo(i18nKey = "value")
  @get:Column(length = AddressCampaignDO.MAX_VALUE_LENGTH)
  open var value: String? = null

  @PropertyInfo(i18nKey = "comment")
  @get:Column(length = Constants.LENGTH_COMMENT)
  open var comment: String? = null

  val addressCampaignId: Long?
    @Transient
    get() = if (addressCampaign != null) addressCampaign!!.id else null

  val addressId: Long?
    @Transient
    get() = if (this.address != null) this.address!!.id else null

  companion object {
    internal const val DELETE_BY_ADDRESS = "AddressCampaignValueDO_DeleteByAddress"
    internal const val FIND_BY_ADDRESS_AND_CAMPAIGN = "AddressCampaignValueDO_FindByAddressAndCampaign"
    internal const val FIND_BY_CAMPAIGN = "AddressCampaignValueDO_FindByCampaign"
  }
}
