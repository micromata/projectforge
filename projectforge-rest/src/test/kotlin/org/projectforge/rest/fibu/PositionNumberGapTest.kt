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

package org.projectforge.rest.fibu

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import org.projectforge.business.fibu.AuftragDO
import org.projectforge.business.fibu.AuftragDao
import org.projectforge.business.fibu.AuftragsPositionDO
import org.projectforge.business.fibu.AuftragsStatus
import org.projectforge.business.fibu.EingangsrechnungDO
import org.projectforge.business.fibu.EingangsrechnungDao
import org.projectforge.business.fibu.EingangsrechnungsPositionDO
import org.projectforge.business.fibu.PaymentScheduleDO
import org.projectforge.business.fibu.RechnungDO
import org.projectforge.business.fibu.RechnungDao
import org.projectforge.business.fibu.RechnungStatus
import org.projectforge.business.fibu.RechnungTyp
import org.projectforge.business.fibu.RechnungsPositionDO
import org.projectforge.business.test.AbstractTestBase
import org.projectforge.framework.persistence.api.ExtendedBaseDO
import org.projectforge.rest.core.AbstractDTOEntityRest
import org.projectforge.rest.core.SessionCsrfService
import org.projectforge.rest.dto.BaseDTO
import org.projectforge.rest.dto.PostData
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.mock.web.MockHttpServletRequest
import org.springframework.mock.web.MockHttpSession
import java.math.BigDecimal
import java.time.LocalDate

/**
 * Positions (and payment schedules) whose numbers have a gap, loaded and saved through the edit forms of
 * projectforge-next.
 *
 * The collections used to be `@OrderColumn(name = "number")` with `@ListIndexBase(1)`: numbers 1, 3, 5 loaded as
 * `[1, null, 3, null, 5]`, and the order or invoice could neither be opened nor saved any more (NPE in the DTO
 * mapping). Sorted by the number instead, a gap is just a gap.
 */
class PositionNumberGapTest : AbstractTestBase() {
    @Autowired
    private lateinit var orderEntityRest: OrderEntityRest

    @Autowired
    private lateinit var outgoingInvoiceEntityRest: OutgoingInvoiceEntityRest

    @Autowired
    private lateinit var incomingInvoiceEntityRest: IncomingInvoiceEntityRest

    @Autowired
    private lateinit var auftragDao: AuftragDao

    @Autowired
    private lateinit var rechnungDao: RechnungDao

    @Autowired
    private lateinit var eingangsrechnungDao: EingangsrechnungDao

    @Autowired
    private lateinit var sessionCsrfService: SessionCsrfService

    @Test
    fun `an order whose position and payment schedule numbers have gaps can be loaded and saved`() {
        logon(TEST_FINANCE_USER)
        val order = AuftragDO().also {
            it.nummer = auftragDao.nextNumber
            it.status = AuftragsStatus.GELEGT
            it.angebotsDatum = LocalDate.of(2026, 3, 1)
            it.titel = "Order with gaps"
            it.periodOfPerformanceBegin = LocalDate.of(2026, 3, 1)
            it.periodOfPerformanceEnd = LocalDate.of(2026, 3, 31)
            (1..3).forEach { i ->
                it.addPosition(AuftragsPositionDO().also { pos ->
                    pos.titel = "Position $i"
                    pos.nettoSumme = BigDecimal(100)
                    pos.status = AuftragsStatus.GELEGT
                })
                it.addPaymentSchedule(PaymentScheduleDO().also { schedule ->
                    // Equal amounts: the gaps renumber the positions (2 becomes 3), but not the schedules referring to them.
                    schedule.amount = BigDecimal(100)
                    schedule.scheduleDate = LocalDate.of(2026, 4, i)
                    schedule.positionNumber = i.toShort()
                })
            }
        }
        val id = auftragDao.insert(order, checkAccess = false)
        openGaps("t_fibu_auftrag_position", "auftrag_fk", id)
        openGaps("t_fibu_payment_schedule", "auftrag_id", id)

        val loaded = auftragDao.find(id)!!
        assertEquals(GAPS, loaded.positionen!!.map { it.number })
        assertEquals(GAPS, loaded.paymentSchedules!!.map { it.number })
        save(orderEntityRest, orderEntityRest.transformFromDB(loaded, editMode = true))
        assertEquals(GAPS, storedNumbers("t_fibu_auftrag_position", "auftrag_fk", id))
        assertEquals(GAPS, storedNumbers("t_fibu_payment_schedule", "auftrag_id", id))
    }

    @Test
    fun `an outgoing invoice whose position numbers have a gap can be loaded and saved`() {
        logon(TEST_FINANCE_USER)
        val invoice = RechnungDO().also {
            it.status = RechnungStatus.GESTELLT
            it.typ = RechnungTyp.RECHNUNG
            it.nummer = rechnungDao.nextNumber
            it.datum = LocalDate.of(2026, 3, 2)
            it.faelligkeit = LocalDate.of(2026, 4, 1)
            it.betreff = "Invoice with gaps"
            it.kundeText = "Customer"
            it.periodOfPerformanceBegin = LocalDate.of(2026, 2, 1)
            it.periodOfPerformanceEnd = LocalDate.of(2026, 2, 28)
            repeat(3) { _ ->
                it.addPosition(RechnungsPositionDO().also { pos ->
                    pos.menge = BigDecimal.ONE
                    pos.einzelNetto = BigDecimal("100.00")
                    pos.vat = BigDecimal("0.19")
                })
            }
        }
        val id = rechnungDao.insert(invoice)
        openGaps("t_fibu_rechnung_position", "rechnung_fk", id)

        val loaded = rechnungDao.find(id)!!
        assertEquals(GAPS, loaded.positionen!!.map { it.number })
        save(outgoingInvoiceEntityRest, outgoingInvoiceEntityRest.transformFromDB(loaded, editMode = true))
        assertEquals(GAPS, storedNumbers("t_fibu_rechnung_position", "rechnung_fk", id))
    }

    @Test
    fun `an incoming invoice whose position numbers have a gap can be loaded and saved`() {
        logon(TEST_FINANCE_USER)
        val invoice = EingangsrechnungDO().also {
            it.kreditor = "Creditor with gaps"
            it.datum = LocalDate.of(2026, 8, 18)
            it.faelligkeit = LocalDate.of(2026, 9, 18)
            it.betreff = "Incoming invoice with gaps"
            repeat(3) { _ ->
                it.addPosition(EingangsrechnungsPositionDO().also { pos ->
                    pos.menge = BigDecimal.ONE
                    pos.einzelNetto = BigDecimal("100.00")
                    pos.vat = BigDecimal("0.19")
                })
            }
        }
        val id = eingangsrechnungDao.insert(invoice)
        openGaps("t_fibu_eingangsrechnung_position", "eingangsrechnung_fk", id)

        val loaded = eingangsrechnungDao.find(id)!!
        assertEquals(GAPS, loaded.positionen!!.map { it.number })
        save(incomingInvoiceEntityRest, incomingInvoiceEntityRest.transformFromDB(loaded, editMode = true))
        assertEquals(GAPS, storedNumbers("t_fibu_eingangsrechnung_position", "eingangsrechnung_fk", id))
    }

    /** Renumbers 1, 2, 3 to 1, 3, 5 directly in the table, as no form would. */
    private fun openGaps(table: String, parentColumn: String, parentId: Long) {
        persistenceService.runInTransaction { context ->
            context.em.createNativeQuery("update $table set number = number * 2 - 1 where $parentColumn = :id")
                .setParameter("id", parentId).executeUpdate()
        }
        assertEquals(GAPS, storedNumbers(table, parentColumn, parentId))
    }

    private fun storedNumbers(table: String, parentColumn: String, parentId: Long): List<Short> {
        return persistenceService.runReadOnly { context ->
            context.em.createNativeQuery("select number from $table where $parentColumn = :id order by number")
                .setParameter("id", parentId).resultList.map { (it as Number).toShort() }
        }
    }

    private fun <O : ExtendedBaseDO<Long>, D : BaseDTO<O>> save(rest: AbstractDTOEntityRest<O, D, *>, dto: D) {
        val request = MockHttpServletRequest().also { it.setSession(MockHttpSession()) }
        val postData = PostData(
            data = dto,
            watchFieldsTriggered = null,
            serverData = sessionCsrfService.createServerData(request),
        )
        val response = rest.saveOrUpdate(request, postData)
        assertEquals(200, response.statusCode.value(), "Save refused: ${response.body?.validationErrors?.map { "${it.fieldId}: ${it.message}" } ?: response.body}")
    }

    companion object {
        private val GAPS = listOf<Short>(1, 3, 5)
    }
}
