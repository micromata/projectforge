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
import org.projectforge.business.fibu.ProjektDO
import org.projectforge.business.fibu.ProjektDao
import org.projectforge.business.fibu.RechnungDO
import org.projectforge.business.fibu.RechnungDao
import org.projectforge.business.fibu.RechnungStatus
import org.projectforge.business.fibu.RechnungTyp
import org.projectforge.business.fibu.RechnungsPositionDO
import org.projectforge.business.fibu.kost.Kost1DO
import org.projectforge.business.fibu.kost.Kost1Dao
import org.projectforge.business.fibu.kost.Kost2ArtDO
import org.projectforge.business.fibu.kost.Kost2ArtDao
import org.projectforge.business.fibu.kost.Kost2DO
import org.projectforge.business.fibu.kost.Kost2Dao
import org.projectforge.business.fibu.kost.KostZuweisungDO
import org.projectforge.business.test.AbstractTestBase
import org.projectforge.rest.core.SessionCsrfService
import org.projectforge.rest.dto.PostData
import org.projectforge.rest.dto.Rechnung
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.mock.web.MockHttpServletRequest
import org.springframework.mock.web.MockHttpSession
import java.math.BigDecimal
import java.time.LocalDate

/**
 * Saving an outgoing invoice whose positions carry cost assignments, the way the edit form of
 * projectforge-next posts it: the loaded DTO, unchanged, back through [OutgoingInvoiceEntityRest].
 *
 * `T_FIBU_KOST_ZUWEISUNG.index` used to be the order column of `RechnungsPositionDO.kostZuweisungen`: every
 * save rewrote it (0 -> 1, 0/1 -> 2/3), and the gap left a `null` hole in the loaded list, so the invoice
 * could neither be opened nor exported any more.
 */
class OutgoingInvoiceKostZuweisungSaveTest : AbstractTestBase() {
    @Autowired
    private lateinit var outgoingInvoiceEntityRest: OutgoingInvoiceEntityRest

    @Autowired
    private lateinit var rechnungDao: RechnungDao

    @Autowired
    private lateinit var projektDao: ProjektDao

    @Autowired
    private lateinit var kost1Dao: Kost1Dao

    @Autowired
    private lateinit var kost2ArtDao: Kost2ArtDao

    @Autowired
    private lateinit var kost2Dao: Kost2Dao

    @Autowired
    private lateinit var sessionCsrfService: SessionCsrfService

    @Test
    fun `saving an unchanged invoice keeps the index of its cost assignments`() {
        logon(TEST_FINANCE_USER)
        val id = insertInvoice()
        assertEquals(listOf(listOf<Short>(0), listOf<Short>(0, 1)), storedIndices(id))

        val dto = outgoingInvoiceEntityRest.transformFromDB(rechnungDao.find(id)!!, editMode = true)
        save(dto)
        assertEquals(listOf(listOf<Short>(0), listOf<Short>(0, 1)), storedIndices(id))
        // And it can still be loaded as a whole, which is what failed with the null hole.
        outgoingInvoiceEntityRest.transformFromDB(rechnungDao.find(id)!!, editMode = true)
    }

    @Test
    fun `an invoice whose indices have gaps can still be loaded and saved`() {
        logon(TEST_FINANCE_USER)
        val id = insertInvoice()
        // The state the order column left behind in production (0 -> 1, 0/1 -> 2/3).
        persistenceService.runInTransaction { context ->
            context.em.createNativeQuery(
                "update t_fibu_kost_zuweisung set index = index + 1 + (select count(*) - 1 from t_fibu_kost_zuweisung o" +
                        " where o.rechnungs_pos_fk = t_fibu_kost_zuweisung.rechnungs_pos_fk)" +
                        " where rechnungs_pos_fk in (select pk from t_fibu_rechnung_position where rechnung_fk = :id)"
            ).setParameter("id", id).executeUpdate()
        }
        assertEquals(listOf(listOf<Short>(1), listOf<Short>(2, 3)), storedIndices(id))

        val dto = outgoingInvoiceEntityRest.transformFromDB(rechnungDao.find(id)!!, editMode = true)
        assertEquals(listOf(1, 2), dto.positionen!!.map { it.kostZuweisungen!!.size })
        save(dto)
        assertEquals(listOf(listOf<Short>(1), listOf<Short>(2, 3)), storedIndices(id))
    }

    private fun save(dto: Rechnung) {
        val request = MockHttpServletRequest().also { it.setSession(MockHttpSession()) }
        val postData = PostData(
            data = dto,
            watchFieldsTriggered = null,
            serverData = sessionCsrfService.createServerData(request),
        )
        val response = outgoingInvoiceEntityRest.saveOrUpdate(request, postData)
        assertEquals(200, response.statusCode.value(), "Save refused: ${response.body}")
    }

    /** The stored indices per position, read directly from the table, ordered by position number and index. */
    private fun storedIndices(invoiceId: Long): List<List<Short>> {
        return persistenceService.runReadOnly { context ->
            @Suppress("UNCHECKED_CAST")
            val rows = context.em.createNativeQuery(
                "select p.number, kz.index from t_fibu_kost_zuweisung kz" +
                        " join t_fibu_rechnung_position p on p.pk = kz.rechnungs_pos_fk" +
                        " where p.rechnung_fk = :id order by p.number, kz.index"
            ).setParameter("id", invoiceId).resultList as List<Array<Any>>
            rows.groupBy({ (it[0] as Number).toShort() }, { (it[1] as Number).toShort() }).values.toList()
        }
    }

    private fun insertInvoice(): Long {
        val n = fixtureCounter++
        val project = ProjektDO().also {
            it.name = "Project $n of OutgoingInvoiceKostZuweisungSaveTest"
            it.internKost2_4 = 102
            it.nummer = 79 + n
            projektDao.insert(it, checkAccess = false)
        }
        val kost1 = Kost1DO().also {
            it.nummernkreis = 4
            it.bereich = 102
            it.teilbereich = 2
            it.endziffer = 2 + n
            kost1Dao.insert(it, checkAccess = false)
        }
        val kost2List = listOf(81L + 2 * n, 82L + 2 * n).map { nummer ->
            val art = Kost2ArtDO().also {
                it.id = nummer
                it.name = "Kost2Art $nummer of OutgoingInvoiceKostZuweisungSaveTest"
                kost2ArtDao.insert(it, checkAccess = false)
            }
            Kost2DO().also {
                it.projekt = project
                it.nummernkreis = project.nummernkreis
                it.bereich = project.bereich!!
                it.teilbereich = project.nummer
                it.kost2Art = art
                kost2Dao.insert(it, checkAccess = false)
            }
        }
        val invoice = RechnungDO()
        invoice.status = RechnungStatus.GESTELLT
        invoice.typ = RechnungTyp.RECHNUNG
        invoice.nummer = rechnungDao.nextNumber
        invoice.datum = LocalDate.of(2026, 3, 2)
        invoice.faelligkeit = LocalDate.of(2026, 4, 1)
        invoice.betreff = "Cost assignments"
        invoice.kundeText = "Customer"
        invoice.periodOfPerformanceBegin = LocalDate.of(2026, 2, 1)
        invoice.periodOfPerformanceEnd = LocalDate.of(2026, 2, 28)
        // One position with a single assignment, one split across two cost units.
        listOf(1, 2).forEach { count ->
            invoice.addPosition(RechnungsPositionDO().also { pos ->
                pos.menge = BigDecimal.ONE
                pos.einzelNetto = BigDecimal("100.00")
                pos.vat = BigDecimal("0.19")
                kost2List.take(count).forEach { kost2 ->
                    pos.addKostZuweisung(KostZuweisungDO().also {
                        it.kost1 = kost1
                        it.kost2 = kost2
                        it.netto = BigDecimal("100.00").divide(BigDecimal(count))
                    })
                }
            })
        }
        return rechnungDao.insert(invoice)!!
    }

    companion object {
        /**
         * Each case inserts cost units of its own: the database isn't cleared between the cases of a class, and
         * JUnit creates a new instance per case, so the counter has to live here.
         */
        private var fixtureCounter = 0
    }
}
