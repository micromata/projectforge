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

package org.projectforge.business.fibu

import com.thoughtworks.xstream.annotations.XStreamAlias
import org.projectforge.framework.persistence.api.BaseSearchFilter
import org.projectforge.framework.persistence.user.entities.PFUserDO
import java.io.Serializable
import java.math.BigDecimal
import java.time.LocalDate

/**
 * @author Kai Reinhard (k.reinhard@micromata.de)
 */
@XStreamAlias("AuftragFilter")
class AuftragFilter @JvmOverloads constructor(filter: BaseSearchFilter? = null) : BaseSearchFilter(filter),
    Serializable, SearchFilterWithPeriodOfPerformance {

    var user: PFUserDO? = null

    var startDate: LocalDate? = null

    var endDate: LocalDate? = null

    override var periodOfPerformanceStartDate: LocalDate? = null

    override var periodOfPerformanceEndDate: LocalDate? = null

    val auftragsStatuses = mutableListOf<AuftragsStatus>()

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

    var projectList: Collection<ProjektDO>? = null

    fun match(auftrag: AuftragDO): Boolean {
        if (!matchUser(auftrag)) {
            return false
        }
        auftrag.erfassungsDatum?.let { erfassungsDatum ->
            startDate?.let {
                if (erfassungsDatum < it) {
                    return false
                }
            }
            endDate?.let {
                if (erfassungsDatum > it) {
                    return false
                }
            }
        }
        periodOfPerformanceStartDate?.let {
            auftrag.periodOfPerformanceEnd?.let { periodOfPerformanceEnd ->
                if (periodOfPerformanceEnd.isBefore(it)) {
                    return false
                }
            }
        }
        periodOfPerformanceEndDate?.let {
            auftrag.periodOfPerformanceBegin?.let { periodOfPerformanceBegin ->
                if (periodOfPerformanceBegin.isAfter(it)) {
                    return false
                }
            }
        }
        if (!matchProjectList(auftrag)) {
            return false
        }
        if (!matchAuftragsStatus(auftrag)) {
            return false
        }
        if (!matchAuftragsPositionsArten(auftrag)) {
            return false
        }
        return true
    }

    /**
     * @return true if the given order matches the [user] of the filter.
     *              If this filter setting is empty, true is returned.
     */
    fun matchUser(auftrag: AuftragDO): Boolean {
        val uid = user?.id ?: return true
        return (auftrag.projectManager?.id == uid ||
                auftrag.headOfBusinessManager?.id == uid ||
                auftrag.contactPerson?.id == uid ||
                auftrag.salesManager?.id == uid
                )
    }

    /**
     * @return true if the given order matches the [projectList] of the filter.
     *              If this filter setting is empty, true is returned.
     */
    fun matchProjectList(auftrag: AuftragDO): Boolean {
        val list = projectList
        if (list.isNullOrEmpty()) {
            return true
        }
        return auftrag.projekt?.id?.let { projektId ->
            list.any{ projektId == it.id }
        } ?: false
    }

    /**
     * @return true if the given order or any non-deleted order position matches the [auftragsStatuses] of the filter.
     *              If this filter setting is empty, true is returned.
     */
    fun matchAuftragsStatus(auftrag: AuftragDO): Boolean {
        if (auftragsStatuses.isEmpty()) {
            return true
        }
        auftrag.status?.let { status ->
            if (auftragsStatuses.contains(status)) {
                return true
            }
        }
        return auftrag.positionenExcludingDeleted.any { pos -> auftragsStatuses.contains(pos.status) }
    }

    /**
     * @return true if any given non-deleted order position matches the [auftragsPositionsArten] of the filter.
     *              If this filter setting is empty, true is returned.
     */
    fun matchAuftragsPositionsArten(auftrag: AuftragDO): Boolean {
        return auftragsPositionsArten.isEmpty() || auftrag.positionenExcludingDeleted.any { pos ->
            auftragsPositionsArten.contains(
                pos.art
            )
        }
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
        val orderInfo = AuftragsCache.instance.getOrderInfo(auftrag)
        if (auftragFakturiertFilterStatus == AuftragFakturiertFilterStatus.ZU_FAKTURIEREN) {
            return orderInfo.toBeInvoiced
        }
        if (auftragFakturiertFilterStatus == AuftragFakturiertFilterStatus.FAKTURIERT) {
            return orderInfo.isVollstaendigFakturiert
        }
        if (auftragFakturiertFilterStatus == AuftragFakturiertFilterStatus.NICHT_FAKTURIERT) {
            return orderInfo.notYetInvoicedSum > BigDecimal.ZERO
        }
        return true
    }
}
