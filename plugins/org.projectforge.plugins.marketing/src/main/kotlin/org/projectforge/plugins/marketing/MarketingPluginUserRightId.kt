/////////////////////////////////////////////////////////////////////////////
//
// Project ProjectForge Community Edition
//         www.projectforge.org
//
// Copyright (C) 2001-2026 Micromata GmbH, Germany (www.micromata.com)
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

import org.hibernate.search.mapper.pojo.bridge.mapping.annotation.TypeBinderRef
import org.hibernate.search.mapper.pojo.mapping.definition.annotation.Indexed
import org.hibernate.search.mapper.pojo.mapping.definition.annotation.TypeBinding
import org.projectforge.business.user.HibernateSearchUserRightIdTypeBinder
import org.projectforge.framework.persistence.api.IUserRightId
import org.projectforge.framework.persistence.api.RightRightIdProviderService
import java.util.*

@Indexed
@TypeBinding(binder = TypeBinderRef(type = HibernateSearchUserRightIdTypeBinder::class))
enum class MarketingPluginUserRightId
/**
 * @param id Must be unique (including all plugins).
 * @param orderString For displaying the rights in e. g. UserEditPage in the correct order.
 * @param i18nKey
 */(override val id: String, override val orderString: String,  override val i18nKey: String) :
    IUserRightId {
    PLUGIN_MARKETING_ADDRESS_CAMPAIGN(
        "PLUGIN_MARKETING_ADDRESS_CAMPAIGN", "unused",
        "unused"
    ),

    PLUGIN_MARKETING_ADDRESS_CAMPAIGN_VALUE(
        "PLUGIN_MARKETING_ADDRESS_CAMPAIGN_VALUE", "unused",
        "unused"
    );

    class ProviderService : RightRightIdProviderService {
        override fun getUserRightIds(): MutableCollection<IUserRightId> {
            return Arrays.asList(*entries.toTypedArray())
        }
    }

    override fun toString(): String {
        return id
    }

    override fun compareTo(o: IUserRightId?): Int {
        return this.compareTo(o)
    }
}
