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

package org.projectforge.rest.core

import jakarta.servlet.http.HttpServletRequest
import org.projectforge.framework.persistence.api.BaseDao
import org.projectforge.framework.persistence.api.ExtendedBaseDO
import org.projectforge.framework.persistence.api.MagicFilter
import org.projectforge.rest.dto.BaseDTO

/**
 * The layout free base class of an entity page whose data travels as its own DTO, see
 * [AbstractEntityRest].
 *
 * The counterpart of [AbstractDTOPagesRest], whose body this repeats: Kotlin has no multiple
 * inheritance, so the DTO half cannot be shared between the layout free hierarchy and the deprecated
 * one. The duplication ends with [AbstractPagesRest].
 *
 * @author Kai Reinhard
 */
abstract class AbstractDTOEntityRest<
        O : ExtendedBaseDO<Long>,
        DTO : BaseDTO<O>,
        B : BaseDao<O>>
@JvmOverloads
constructor(
    baseDaoClazz: Class<B>,
    i18nKeyPrefix: String,
    cloneSupport: CloneSupport = CloneSupport.NONE,
) : AbstractEntityRest<O, DTO, B>(baseDaoClazz, i18nKeyPrefix, cloneSupport) {

    /**
     * @return New result set of dto's, transformed from database objects.
     */
    override fun postProcessResultSet(
        resultSet: ResultSet<O>,
        request: HttpServletRequest,
        magicFilter: MagicFilter,
    ): ResultSet<*> {
        // A page may offer a lean row for the hand built next list, see createListRow.
        val leanRows = useListRow(request)
        val newList = resultSet.resultSet.map {
            if (leanRows) createListRow(it) else transformFromDB(it, false)
        }
        val result = ResultSet(
            newList,
            resultSet,
            // For a server-side paged result totalSize is the size of the whole result, not of this page;
            // keep it (and the paging fields below), which the DTO transform would otherwise drop.
            totalSize = resultSet.totalSize ?: newList.size,
            selectedEntityIds = resultSet.selectedEntityIds,
            magicFilter = magicFilter,
            offset = resultSet.offset,
            limit = resultSet.limit,
            totalSizeExact = resultSet.totalSizeExact,
        )
        result.statistics = resultSet.statistics
        resultSet.resultInfo?.let { info ->
            if (info.isNotBlank()) {
                result.resultInfo = info
            }
        }
        return result
    }

    /**
     * An empty DTO, for a page that fills one itself. Null unless the page names its DTO's constructor, which
     * is all [createListRow] needs of it — Kotlin cannot reach `DTO` at runtime (type erasure), and resolving
     * it from the generic supertype breaks as soon as a page inherits through an intermediate class.
     */
    protected open fun newDTO(): DTO? {
        return null
    }

    /**
     * The lean row of the hand built next list, built by the DTO itself
     * ([org.projectforge.rest.dto.BaseDTO.copyFrom4ListRow]) — so what a row of an entity consists of is
     * declared once, next to its full copy, rather than in this rest class.
     *
     * Falls back to the full DTO for a page that names no [newDTO], which is every page whose rows are small
     * enough already.
     */
    override fun createListRow(obj: O): DTO {
        val dto = newDTO() ?: return transformFromDB(obj, false)
        dto.copyFrom4ListRow(obj)
        return dto
    }

    /**
     * @param dto Expected as DTO
     */
    override fun getId(dto: Any): Long? {
        @Suppress("UNCHECKED_CAST")
        return (dto as DTO).id
    }

    /**
     * @param dto Expected as DTO
     */
    override fun isDeleted(dto: Any): Boolean {
        @Suppress("UNCHECKED_CAST")
        return (dto as DTO).deleted
    }

    override fun isHistorizable(): Boolean {
        return isHistorizable(baseDao.doClass)
    }
}
