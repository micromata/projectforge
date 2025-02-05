/////////////////////////////////////////////////////////////////////////////
//
// Project ProjectForge Community Edition
//         www.projectforge.org
//
// Copyright (C) 2001-2025 Micromata GmbH, Germany (www.micromata.com)
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

import jakarta.persistence.Tuple
import org.projectforge.business.task.TaskDO
import org.projectforge.common.extensions.abbreviate
import org.projectforge.framework.persistence.database.TupleUtils.getBigDecimal
import org.projectforge.framework.persistence.database.TupleUtils.getBoolean
import org.projectforge.framework.persistence.database.TupleUtils.getDate
import org.projectforge.framework.persistence.database.TupleUtils.getInt
import org.projectforge.framework.persistence.database.TupleUtils.getLocalDate
import org.projectforge.framework.persistence.database.TupleUtils.getLong
import org.projectforge.framework.persistence.database.TupleUtils.getShort
import org.projectforge.framework.persistence.database.TupleUtils.getString
import org.projectforge.framework.persistence.jpa.PfPersistenceService
import org.projectforge.framework.persistence.user.entities.PFUserDO
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Service

/**
 * Uses tuple query for faster access to the database.
 */
@Service
class AuftragsCacheService {
    @Autowired
    private lateinit var persistenceService: PfPersistenceService

    fun selectAuftragsList(): List<AuftragDO> {
        return persistenceService.runIsolatedReadOnly { context ->
            val em = context.em
            val tuples = em.createQuery(SELECT_ORDERS, Tuple::class.java).resultList
            tuples.map { tuple ->
                AuftragDO().also { order ->
                    order.id = getLong(tuple, "id")
                    order.deleted = getBoolean(tuple, "deleted")!!
                    order.created = getDate(tuple, "created")
                    order.titel = getString(tuple, "titel")
                    order.status = tuple.get("status", AuftragsStatus::class.java)
                    order.nummer = getInt(tuple, "nummer")
                    order.angebotsDatum = getLocalDate(tuple, "angebotsDatum")
                    order.erfassungsDatum = getLocalDate(tuple, "erfassungsDatum")
                    order.entscheidungsDatum = getLocalDate(tuple, "entscheidungsDatum")
                    order.probabilityOfOccurrence = getInt(tuple, "probabilityOfOccurrence")
                    order.forecastType = tuple.get("forecastType", AuftragForecastType::class.java)
                    order.bemerkung = getString(tuple, "bemerkung")
                    order.periodOfPerformanceBegin = getLocalDate(tuple, "periodOfPerformanceBegin")
                    order.periodOfPerformanceEnd = getLocalDate(tuple, "periodOfPerformanceEnd")
                    order.kundeText = getString(tuple, "kundeText")
                    getLong(tuple, "kundeId")?.let { kundeId ->
                        order.kunde = em.getReference(KundeDO::class.java, kundeId)
                    }
                    getLong(tuple, "projektId")?.let { projektId ->
                        order.projekt = em.getReference(ProjektDO::class.java, projektId)
                    }
                    getLong(tuple, "contactPersonId")?.let { userId ->
                        order.contactPerson = em.getReference(PFUserDO::class.java, userId)
                    }
                }
            }
        }
    }

    fun selectNonDeletedAuftragsPositions(): List<AuftragsPositionDO> {
        return persistenceService.runIsolatedReadOnly { context ->
            val em = context.em
            val tuples = em.createQuery(SELECT_POSITIONS, Tuple::class.java).resultList
            tuples.map { tuple ->
                AuftragsPositionDO().also { pos ->
                    pos.id = getLong(tuple, "id")
                    getLong(tuple, "auftragId")?.let { auftragId ->
                        pos.auftrag = em.getReference(AuftragDO::class.java, auftragId)
                    }
                    pos.number = getShort(tuple, "number")!!
                    pos.deleted = getBoolean(tuple, "deleted")!!
                    pos.titel = getString(tuple, "titel")
                    pos.status = tuple.get("status", AuftragsStatus::class.java)
                    pos.paymentType = tuple.get("paymentType", AuftragsPositionsPaymentType::class.java)
                    pos.forecastType = tuple.get("forecastType", AuftragForecastType::class.java)
                    pos.art = tuple.get("art", AuftragsPositionsArt::class.java)
                    pos.personDays = getBigDecimal(tuple, "personDays")
                    pos.nettoSumme = getBigDecimal(tuple, "nettoSumme")
                    pos.vollstaendigFakturiert = getBoolean(tuple, "vollstaendigFakturiert")
                    pos.periodOfPerformanceType =
                        tuple.get("periodOfPerformanceType", PeriodOfPerformanceType::class.java)
                    pos.periodOfPerformanceBegin = getLocalDate(tuple, "periodOfPerformanceBegin")
                    pos.periodOfPerformanceEnd = getLocalDate(tuple, "periodOfPerformanceEnd")
                    getLong(tuple, "taskId")?.let { taskId -> pos.task = TaskDO().also { it.id = taskId } }
                    pos.bemerkung = getString(tuple, "bemerkung")?.abbreviate(30)
                }
            }
        }
    }

    fun selectNonDeletedPaymentSchedules(): List<PaymentScheduleDO> {
        return persistenceService.runIsolatedReadOnly { context ->
            val em = context.em
            val tuples = em.createQuery(SELECT_PAYMENTS_SCHEDULES, Tuple::class.java).resultList
            tuples.map { tuple ->
                PaymentScheduleDO().also { schedule ->
                    schedule.id = getLong(tuple, "id")
                    schedule.number = getShort(tuple, "number")!!
                    schedule.deleted = getBoolean(tuple, "deleted")!!
                    schedule.created = getDate(tuple, "created")
                    getLong(tuple, "auftragId")?.let { auftragId ->
                        schedule.auftrag = AuftragDO().also { it.id = auftragId }
                    }
                    schedule.amount = getBigDecimal(tuple, "amount")
                    schedule.reached = getBoolean(tuple, "reached")!!
                    schedule.vollstaendigFakturiert = getBoolean(tuple, "vollstaendigFakturiert")!!
                    schedule.scheduleDate = getLocalDate(tuple, "scheduleDate")
                    schedule.positionNumber = getShort(tuple, "positionNumber")
                }
            }
        }
    }

    companion object {
        private val SELECT_ORDERS = """
            SELECT a.id as id,a.deleted as deleted,a.created as created,a.titel as titel,a.status as status,
                   a.nummer as nummer,a.angebotsDatum as angebotsDatum,a.erfassungsDatum as erfassungsDatum,
                   a.entscheidungsDatum as entscheidungsDatum,a.bemerkung as bemerkung,
                   a.probabilityOfOccurrence as probabilityOfOccurrence,
                   a.forecastType as forecastType,
                   a.periodOfPerformanceBegin as periodOfPerformanceBegin, a.periodOfPerformanceEnd as periodOfPerformanceEnd,
                   a.contactPerson.id as contactPersonId, a.kunde.id as kundeId, a.projekt.id as projektId, a.kundeText as kundeText
            FROM ${AuftragDO::class.simpleName} a
        """.trimIndent()
        private val SELECT_POSITIONS = """
            SELECT p.id as id,p.auftrag.id as auftragId,p.task.id as taskId,p.number as number,p.deleted as deleted,p.titel as titel,
                   p.status as status,p.paymentType as paymentType,p.forecastType as forecastType,p.art as art,p.personDays as personDays,
                   p.nettoSumme as nettoSumme,p.vollstaendigFakturiert as vollstaendigFakturiert,p.bemerkung as bemerkung,
                   p.periodOfPerformanceType as periodOfPerformanceType,
                   p.periodOfPerformanceBegin as periodOfPerformanceBegin,p.periodOfPerformanceEnd as periodOfPerformanceEnd
            FROM ${AuftragsPositionDO::class.simpleName} p
            WHERE deleted = false
        """.trimIndent()
        private val SELECT_PAYMENTS_SCHEDULES = """
            SELECT s.id as id,s.deleted as deleted,s.created as created,s.number as number,s.auftrag.id as auftragId,
            s.amount as amount,s.reached as reached,s.vollstaendigFakturiert as vollstaendigFakturiert,
            s.scheduleDate as scheduleDate,s.positionNumber as positionNumber
            FROM ${PaymentScheduleDO::class.simpleName} s
            WHERE deleted = false
        """.trimIndent()
    }
}
