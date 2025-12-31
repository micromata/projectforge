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

import org.projectforge.framework.persistence.api.impl.CustomResultFilter
import org.projectforge.framework.persistence.history.HistoryEntryDO
import org.projectforge.framework.persistence.jpa.PfPersistenceService

/**
 * Filters campaign values to show only entries where the current user has made modifications.
 * This is determined by checking the history table for entries modified by the user.
 */
class AddressCampaignValueMyEntriesResultFilter(
    persistenceService: PfPersistenceService,
    userId: Long
) : CustomResultFilter<AddressCampaignValueDO> {

    private val modifiedEntityIds: Set<Long>

    init {
        // Query history table to find all campaign value IDs that were modified by this user
        modifiedEntityIds = persistenceService.runReadOnly { context ->
            val query = """
                SELECT DISTINCT h.entityId
                FROM HistoryEntryDO h
                WHERE h.entityName = :entityName
                AND h.modifiedBy = :userId
                AND h.entityId IS NOT NULL
            """.trimIndent()

            context.em.createQuery(query, Long::class.java)
                .setParameter("entityName", AddressCampaignValueDO::class.java.name)
                .setParameter("userId", userId.toString())
                .resultList
                .toSet()
        }
    }

    override fun match(list: MutableList<AddressCampaignValueDO>, element: AddressCampaignValueDO): Boolean {
        return modifiedEntityIds.contains(element.id)
    }
}
