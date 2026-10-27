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

package org.projectforge.business.fibu.kost.reallocation

import org.projectforge.common.i18n.UserException
import org.projectforge.framework.access.OperationType
import org.projectforge.framework.persistence.api.BaseSearchFilter
import org.projectforge.framework.persistence.api.QueryFilter
import org.projectforge.framework.persistence.api.SortProperty
import org.springframework.stereotype.Service

/**
 * The reallocation runs, finance only. Only a draft may be edited or deleted; the status is changed by the
 * [Kost2ReallocationService] only ([runChangingStatus]), never by an edit.
 */
@Service
open class Kost2ReallocationDao : AbstractKost2ReallocationDao<Kost2ReallocationDO>(Kost2ReallocationDO::class.java) {
    /** Set by [runChangingStatus] only: [onUpdate] keeps the stored status otherwise. */
    private val changingStatus = ThreadLocal<Boolean>()
    override val defaultSortProperties: Array<SortProperty>
        get() = DEFAULT_SORT_PROPERTIES

    override fun select(filter: BaseSearchFilter): List<Kost2ReallocationDO> {
        return select(QueryFilter(filter))
    }

    override fun newInstance(): Kost2ReallocationDO {
        return Kost2ReallocationDO()
    }

    override fun onInsertOrModify(obj: Kost2ReallocationDO, operationType: OperationType) {
        if (operationType == OperationType.INSERT) {
            obj.status = Kost2ReallocationStatus.DRAFT
            obj.appliedAt = null
        }
        val from = obj.fromYearMonth
        val to = obj.toYearMonth
        if (from == null || to == null || from > to || obj.fromMonth !in 1..12 || obj.toMonth !in 1..12) {
            throw UserException("fibu.kost2Reallocation.error.invalidPeriod")
        }
    }

    override fun onUpdate(obj: Kost2ReallocationDO, dbObj: Kost2ReallocationDO) {
        if (changingStatus.get() == true) {
            return
        }
        if (dbObj.status != Kost2ReallocationStatus.DRAFT) {
            throw UserException("fibu.kost2Reallocation.error.notDraft")
        }
        obj.status = dbObj.status
        obj.appliedAt = dbObj.appliedAt
    }

    override fun onDelete(obj: Kost2ReallocationDO) {
        if (obj.status == Kost2ReallocationStatus.APPLIED) {
            throw UserException("fibu.kost2Reallocation.error.deleteApplied")
        }
    }

    /** Runs the block changing the status of runs (by [Kost2ReallocationService] only). */
    open fun <T> runChangingStatus(block: () -> T): T {
        changingStatus.set(true)
        try {
            return block()
        } finally {
            changingStatus.remove()
        }
    }

    /** The applied runs, in the order of their application (without access check). */
    open fun selectApplied(): List<Kost2ReallocationDO> {
        return persistenceService.executeQuery(
            "from Kost2ReallocationDO r where r.deleted = false and r.status = :status order by r.appliedAt, r.id",
            Kost2ReallocationDO::class.java,
            Pair("status", Kost2ReallocationStatus.APPLIED),
        )
    }

    companion object {
        private val DEFAULT_SORT_PROPERTIES = arrayOf(SortProperty.desc("created"))
    }
}
