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

package org.projectforge.business.fibu

import com.thoughtworks.xstream.annotations.XStreamAlias
import org.projectforge.framework.persistence.api.BaseSearchFilter
import org.projectforge.framework.persistence.user.entities.PFUserDO

import java.io.Serializable
import java.util.ArrayList
import java.util.Date

/**
 * @author Kai Reinhard (k.reinhard@micromata.de)
 */
@XStreamAlias("AuftragFilter")
class AuftragFilter : BaseSearchFilter, Serializable, SearchFilterWithPeriodOfPerformance {

    var user: PFUserDO? = null

    var startDate: Date? = null

    var endDate: Date? = null

    private var periodOfPerformanceStartDate: Date? = null

    private var periodOfPerformanceEndDate: Date? = null

    private val auftragsStatuses = ArrayList<AuftragsStatus>()

    private val auftragsPositionsArten = ArrayList<AuftragsPositionsArt>()

    var auftragFakturiertFilterStatus: AuftragFakturiertFilterStatus? = null
        get() {
            if (field == null) {
                this.auftragFakturiertFilterStatus = AuftragFakturiertFilterStatus.ALL
            }
            return field
        }

    /**
     * null represents all.
     */
    var auftragsPositionsPaymentType: AuftragsPositionsPaymentType? = null

    constructor() {}

    constructor(filter: BaseSearchFilter) : super(filter) {}

    override fun getPeriodOfPerformanceStartDate(): Date? {
        return periodOfPerformanceStartDate
    }

    fun setPeriodOfPerformanceStartDate(periodOfPerformanceStartDate: Date) {
        this.periodOfPerformanceStartDate = periodOfPerformanceStartDate
    }

    override fun getPeriodOfPerformanceEndDate(): Date? {
        return periodOfPerformanceEndDate
    }

    fun setPeriodOfPerformanceEndDate(periodOfPerformanceEndDate: Date) {
        this.periodOfPerformanceEndDate = periodOfPerformanceEndDate
    }

    /**
     * empty collection represents all.
     */
    fun getAuftragsStatuses(): Collection<AuftragsStatus> {
        return auftragsStatuses
    }

    /**
     * empty collection represents all.
     */
    fun getAuftragsPositionsArten(): Collection<AuftragsPositionsArt> {
        return auftragsPositionsArten
    }

    override fun reset(): AuftragFilter {
        searchString = ""
        startDate = null
        endDate = null
        periodOfPerformanceStartDate = null
        periodOfPerformanceEndDate = null
        user = null
        auftragsStatuses.clear()
        auftragsPositionsArten.clear()
        auftragFakturiertFilterStatus = null
        auftragsPositionsPaymentType = null
        return this
    }

    companion object {
        private const val serialVersionUID = 3456000966109255447L
    }
}
