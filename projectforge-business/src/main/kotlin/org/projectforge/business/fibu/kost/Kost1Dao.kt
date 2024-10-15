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

package org.projectforge.business.fibu.kost

import org.projectforge.business.fibu.kost.KostHelper.parseKostString
import org.projectforge.business.user.UserRightId
import org.projectforge.common.i18n.UserException
import org.projectforge.framework.persistence.api.BaseDao
import org.projectforge.framework.persistence.api.BaseSearchFilter
import org.projectforge.framework.persistence.api.QueryFilter
import org.projectforge.framework.persistence.api.QueryFilter.Companion.eq
import org.projectforge.framework.persistence.api.QueryFilter.Companion.isNull
import org.projectforge.framework.persistence.api.QueryFilter.Companion.ne
import org.projectforge.framework.persistence.api.QueryFilter.Companion.or
import org.projectforge.framework.persistence.api.SortProperty.Companion.asc
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Service

@Service
class Kost1Dao : BaseDao<Kost1DO>(Kost1DO::class.java) {
    @Autowired
    private val kostCache: KostCache? = null

    init {
        userRightId = USER_RIGHT_ID
    }

    /**
     * @param kostString Format ######## or #.###.##.## is supported.
     * @see .getKost1
     */
    fun getKost1(kostString: String?): Kost1DO? {
        val kost = parseKostString(kostString) ?: return null
        return getKost1(kost[0], kost[1], kost[2], kost[3])
    }

    fun getKost1(
        nummernkreis: Int,
        bereich: Int,
        teilbereich: Int,
        endziffer: Int,
    ): Kost1DO? {
        return persistenceService.selectNamedSingleResult(
            Kost1DO.FIND_BY_NK_BEREICH_TEILBEREICH_ENDZIFFER,
            Kost1DO::class.java,
            Pair("nummernkreis", nummernkreis),
            Pair("bereich", bereich),
            Pair("teilbereich", teilbereich),
            Pair("endziffer", endziffer)
        )
    }

    override fun select(filter: BaseSearchFilter): List<Kost1DO> {
        val myFilter = if (filter is KostFilter) {
            filter
        } else {
            KostFilter(filter)
        }
        val queryFilter = QueryFilter(myFilter)
        if (myFilter.isActive) {
            queryFilter.add(eq("kostentraegerStatus", KostentraegerStatus.ACTIVE))
        } else if (myFilter.isNonActive) {
            queryFilter.add(eq("kostentraegerStatus", KostentraegerStatus.NONACTIVE))
        } else if (myFilter.isEnded) {
            queryFilter.add(eq("kostentraegerStatus", KostentraegerStatus.ENDED))
        } else if (myFilter.isNotEnded) {
            queryFilter.add(
                or(
                    ne("kostentraegerStatus", KostentraegerStatus.ENDED),
                    isNull("kostentraegerStatus")
                )
            )
        }
        queryFilter.addOrder(asc("nummernkreis")).addOrder(asc("bereich")).addOrder(asc("teilbereich"))
            .addOrder(asc("endziffer"))
        return select(queryFilter)
    }

    override fun onInsertOrModify(obj: Kost1DO) {
        verifyKost(obj)
        val other = if (obj.id == null) {
            // New entry
            getKost1(obj.nummernkreis, obj.bereich, obj.teilbereich, obj.endziffer)
        } else {
            // entry already exists. Check maybe changed:
            persistenceService.selectNamedSingleResult(
                Kost1DO.FIND_OTHER_BY_NK_BEREICH_TEILBEREICH_ENDZIFFER,
                Kost1DO::class.java,
                Pair("nummernkreis", obj.nummernkreis),
                Pair("bereich", obj.bereich),
                Pair("teilbereich", obj.teilbereich),
                Pair("endziffer", obj.endziffer),
                Pair("id", obj.id)
            )
        }
        if (other != null) {
            throw UserException("fibu.kost.error.collision")
        }
    }

    private fun verifyKost(obj: Kost1DO) {
        if (obj.nummernkreis < 0 || obj.nummernkreis > 9) throw UserException("fibu.kost.error.invalidKost")

        if (obj.bereich < 0 || obj.bereich > 999) throw UserException("fibu.kost.error.invalidKost")

        if (obj.teilbereich < 0 || obj.teilbereich > 99) throw UserException("fibu.kost.error.invalidKost")

        if (obj.endziffer < 0 || obj.endziffer > 99) throw UserException("fibu.kost.error.invalidKost")
    }

    override fun afterInsertOrModify(kost1: Kost1DO) {
        super.afterInsertOrModify(kost1)
        kostCache!!.updateKost1(kost1)
    }

    override fun newInstance(): Kost1DO {
        return Kost1DO()
    }

    companion object {
        val USER_RIGHT_ID: UserRightId = UserRightId.FIBU_COST_UNIT
    }
}
