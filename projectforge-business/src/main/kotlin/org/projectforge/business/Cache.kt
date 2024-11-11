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

package org.projectforge.business

import jakarta.annotation.PostConstruct
import org.projectforge.business.fibu.EmployeeCache
import org.projectforge.business.fibu.EmployeeDO
import org.projectforge.business.fibu.KundeDO
import org.projectforge.business.fibu.ProjektDO
import org.projectforge.business.fibu.kost.Kost2DO
import org.projectforge.business.fibu.kost.KostCache
import org.projectforge.business.fibu.kost.KundeCache
import org.projectforge.business.fibu.kost.ProjektCache
import org.projectforge.business.timesheet.TimesheetDO
import org.projectforge.business.user.UserGroupCache
import org.projectforge.framework.persistence.user.entities.PFUserDO
import org.springframework.stereotype.Service

/**
 * Helper cache for avoiding lazy loading of entities. For convenient access to most caches.
 * Ideal for usage by scripts.
 */
@Service
class Cache {
    @PostConstruct
    private fun init() {
        instance = this
    }

    /**
     * Fills the user, kost2, project and customer of the given timesheet.
     * @param timesheet The timesheet to fill.
     * @return The filled timesheet for chaining.
     */
    fun populate(timesheet: TimesheetDO): TimesheetDO {
        timesheet.user = getUser(timesheet.userId)
        val kost2 = getKost2(timesheet.kost2Id)
        val projekt = getProjektByKost2(timesheet.kost2Id)
        val kunde = getKunde(projekt?.kunde?.nummer)
        projekt?.let { it.kunde = kunde }
        kost2?.let { it.projekt = projekt }
        timesheet.kost2 = kost2
        return timesheet
    }

    fun getUser(userId: Long?): PFUserDO? {
        return UserGroupCache.getInstance().getUser(userId)
    }

    fun getEmployeeByUserId(userId: Long?): EmployeeDO? {
        return EmployeeCache.instance.getEmployeeByUserId(userId)
    }

    fun getUserByEmployeeId(employeeId: Long?): PFUserDO? {
        return EmployeeCache.instance.getUserByEmployee(employeeId)
    }

    fun getKost2(kost2Id: Long?): Kost2DO? {
        return KostCache.instance.getKost2(kost2Id)
    }

    fun getProjekt(projektId: Long?): ProjektDO? {
        return ProjektCache.instance.getProjekt(projektId)
    }

    fun getProjektByKost2(kost2Id: Long?): ProjektDO? {
        val projektId = getKost2(kost2Id)?.projekt?.id ?: return null
        return getProjekt(projektId)
    }

    fun getKunde(kundeId: Long?): KundeDO? {
        return KundeCache.instance.getKunde(kundeId)
    }

    fun getKundeByKost2(kost2Id: Long?): KundeDO? {
        val projekt = getProjektByKost2(kost2Id) ?: return null
        return getKunde(projekt.kunde?.nummer)
    }

    companion object {
        @JvmStatic
        lateinit var instance: Cache
            private set
    }
}
