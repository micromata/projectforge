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
import org.projectforge.common.DatabaseDialect
import org.projectforge.framework.persistence.api.BaseSearchFilter
import org.projectforge.framework.persistence.api.HibernateUtils
import org.projectforge.framework.persistence.user.entities.PFUserDO
import java.io.Serializable
import java.util.*

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

    val auftragsStatuses = mutableListOf<AuftragsStatus>()

    val auftragsPositionStatuses: List<AuftragsPositionsStatus>
        get() = auftragsStatuses.map { it.asAuftragsPositionStatus() }

    val auftragsPositionsArten = ArrayList<AuftragsPositionsArt>()

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

    @JvmOverloads
    constructor(filter: BaseSearchFilter? = null) : super(filter) {
    }

    override fun getPeriodOfPerformanceStartDate(): Date? {
        return periodOfPerformanceStartDate
    }

    fun setPeriodOfPerformanceStartDate(periodOfPerformanceStartDate: Date?) {
        this.periodOfPerformanceStartDate = periodOfPerformanceStartDate
    }

    override fun getPeriodOfPerformanceEndDate(): Date? {
        return periodOfPerformanceEndDate
    }

    fun setPeriodOfPerformanceEndDate(periodOfPerformanceEndDate: Date?) {
        this.periodOfPerformanceEndDate = periodOfPerformanceEndDate
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

    fun filterFakturiert(list: List<AuftragDO>): List<AuftragDO> {
        if (auftragFakturiertFilterStatus == null || auftragFakturiertFilterStatus == AuftragFakturiertFilterStatus.ALL) {
            // do not filter
            return list
        }
        return list.filter { checkFakturiert(it) }
    }

    private fun checkFakturiert(auftrag: AuftragDO): Boolean {
        val orderIsCompletelyInvoiced = auftrag.isVollstaendigFakturiert

        val vollstaendigFakturiert = AuftragFakturiertFilterStatus.FAKTURIERT == auftragFakturiertFilterStatus
        // special case
        if (HibernateUtils.getDialect() != DatabaseDialect.HSQL &&
                !vollstaendigFakturiert && auftragsStatuses.contains(AuftragsStatus.ABGESCHLOSSEN)) {

            // if order is completed and not all positions are completely invoiced
            if (auftrag.auftragsStatus == AuftragsStatus.ABGESCHLOSSEN && !orderIsCompletelyInvoiced) {
                return true
            }
            // if order is completed and not completely invoiced
            for (pos in auftrag.positionenExcludingDeleted) {
                if (pos.isAbgeschlossenUndNichtVollstaendigFakturiert) {
                    return true
                }
            }
            if (auftrag.paymentSchedules != null) {
                for (schedule in auftrag.paymentSchedules!!) {
                    if (schedule.reached && !schedule.vollstaendigFakturiert) {
                        return true
                    }
                }
            }
            return false
        }
        return orderIsCompletelyInvoiced == vollstaendigFakturiert
    }
}
