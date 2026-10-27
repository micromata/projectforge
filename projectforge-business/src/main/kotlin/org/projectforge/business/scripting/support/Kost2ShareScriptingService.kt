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

package org.projectforge.business.scripting.support

import org.projectforge.business.fibu.kost.BuchungssatzDO
import org.projectforge.business.fibu.kost.reallocation.BuchungssatzKost2ShareDO
import org.projectforge.business.fibu.kost.reallocation.BuchungssatzKost2ShareDao
import org.projectforge.business.fibu.kost.reallocation.BuchungssatzKost2ShareService

/**
 * The effective view of the accounting records for scripts (variable `kost2ShareService`), only for finance and
 * controlling members. A reallocated record counts with its kost2 shares instead of its own kost2:
 * ```
 * val records = kost2ShareService.expand(accountingRecordDao.select(filter))
 * records.map { AccountingRecord.create(it, debits, businessUnit, customerGroup) }
 * ```
 * A share is a transient copy of the record with the kost2 and the amount of the share; its `originalRecord` is the
 * stored record.
 */
class Kost2ShareScriptingService(
    private val shareService: BuchungssatzKost2ShareService,
    private val shareDao: BuchungssatzKost2ShareDao,
) {
    /** One copy per kost2 share of a reallocated record, the other records as they are. */
    fun expand(records: List<BuchungssatzDO>): List<BuchungssatzDO> {
        shareDao.hasLoggedInUserSelectAccess(true)
        return shareService.expand(records)
    }

    /** The active kost2 shares of the given record (empty, if it isn't reallocated). */
    fun getShares(record: BuchungssatzDO): List<BuchungssatzKost2ShareDO> {
        shareDao.hasLoggedInUserSelectAccess(true)
        return shareService.activeShares(record)
    }

    /** The effective kost2 of the record: its own one, if it isn't reallocated, otherwise null (several shares). */
    fun getEffectiveKost2Id(record: BuchungssatzDO): Long? {
        val shares = getShares(record)
        return if (shares.isEmpty()) record.kost2?.id else shares.map { it.kost2?.id }.distinct().singleOrNull()
    }
}
