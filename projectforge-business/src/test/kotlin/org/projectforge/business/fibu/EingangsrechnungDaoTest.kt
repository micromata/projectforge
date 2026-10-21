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

import org.hibernate.Hibernate
import org.junit.jupiter.api.Assertions
import org.junit.jupiter.api.Test
import org.projectforge.business.fibu.kost.KostZuweisungDO
import org.projectforge.common.i18n.UserException
import org.projectforge.framework.access.AccessException
import org.projectforge.business.test.AbstractTestBase
import org.springframework.beans.factory.annotation.Autowired
import java.io.Serializable
import java.math.BigDecimal
import java.time.LocalDate

class EingangsrechnungDaoTest : AbstractTestBase() {
    @Autowired
    private lateinit var eingangsrechnungDao: EingangsrechnungDao

    @Autowired
    private lateinit var kontoDao: KontoDao

    @Test
    fun checkAccess() {
        logon(TEST_FINANCE_USER)
        var eingangsrechnung = EingangsrechnungDO()
        eingangsrechnung.datum = LocalDate.now()
        eingangsrechnung.addPosition(EingangsrechnungsPositionDO())
        eingangsrechnung.faelligkeit = LocalDate.now()
        val id: Serializable = eingangsrechnungDao.insert(eingangsrechnung)
        eingangsrechnung = eingangsrechnungDao.find(id)!!

        logon(TEST_CONTROLLING_USER)
        eingangsrechnungDao.find(id)
        checkNoWriteAccess(id, eingangsrechnung, "Controlling")

        logon(TEST_USER)
        checkNoAccess(id, eingangsrechnung, "Other")

        logon(TEST_PROJECT_MANAGER_USER)
        checkNoAccess(id, eingangsrechnung, "Project manager")

        logon(TEST_ADMIN_USER)
        checkNoAccess(id, eingangsrechnung, "Admin ")
    }

    /**
     * A soft-deleted invoice with a far-future date must not widen the year range: the min/max-date query
     * behind [EingangsrechnungDao.years] used to include deleted rows, so a leftover e2e import row dated
     * 2099 (see `creditor-invoice-import.ts`) blew the 30-year guard in `SQLHelper.getYears` and crashed the
     * Wicket list page. Regression test for the `where deleted = false` fix.
     */
    @Test
    fun yearsIgnoresDeletedInvoices() {
        logon(TEST_FINANCE_USER)
        // A live invoice in the current year, so a min/max range actually exists.
        val current = EingangsrechnungDO()
        current.datum = LocalDate.now()
        current.addPosition(EingangsrechnungsPositionDO())
        eingangsrechnungDao.insert(current)

        // A far-future invoice, then deleted: it must not be counted any more.
        val farFuture = EingangsrechnungDO()
        farFuture.datum = LocalDate.of(2099, 12, 31)
        farFuture.addPosition(EingangsrechnungsPositionDO())
        val farFutureId = eingangsrechnungDao.insert(farFuture)
        eingangsrechnungDao.markAsDeleted(eingangsrechnungDao.find(farFutureId)!!)

        // Must not throw the "Paranoia Exception" and must not report the deleted far-future year.
        val years = eingangsrechnungDao.years
        Assertions.assertFalse(years.contains(2099), "Deleted far-future invoice must not appear in years.")
        Assertions.assertTrue(years.contains(LocalDate.now().year), "The current invoice's year must be listed.")
    }

    /**
     * A deleted position is only flagged and stays in the collection; an invoice whose positions are all deleted has
     * none left and is refused like one without any.
     */
    @Test
    fun onlyDeletedPositionsAreRefused() {
        logon(TEST_FINANCE_USER)
        fun newInvoice(vararg deleted: Boolean): EingangsrechnungDO {
            val invoice = EingangsrechnungDO()
            invoice.datum = LocalDate.now()
            invoice.faelligkeit = LocalDate.now()
            deleted.forEach { invoice.addPosition(EingangsrechnungsPositionDO().also { pos -> pos.deleted = it }) }
            return invoice
        }
        val ex = Assertions.assertThrows(UserException::class.java) { eingangsrechnungDao.insert(newInvoice(true)) }
        Assertions.assertEquals("fibu.rechnung.error.rechnungHatKeinePositionen", ex.i18nKey)

        val id = eingangsrechnungDao.insert(newInvoice(false, true))
        val edited = eingangsrechnungDao.find(id)!!
        edited.positionen!!.forEach { it.deleted = true }
        val updateEx = Assertions.assertThrows(UserException::class.java) { eingangsrechnungDao.update(edited) }
        Assertions.assertEquals("fibu.rechnung.error.rechnungHatKeinePositionen", updateEx.i18nKey)
    }

    /**
     * A deleted position stays in the collection (soft delete) and is posted with the invoice, but it is no
     * part of it any more: an invoice left with deleted positions only has none and must be refused, as one
     * without any is.
     */
    @Test
    fun refusesInvoiceWithDeletedPositionsOnly() {
        logon(TEST_FINANCE_USER)
        val invoice = EingangsrechnungDO()
        invoice.datum = LocalDate.now()
        invoice.addPosition(EingangsrechnungsPositionDO().also { it.deleted = true })
        val ex = Assertions.assertThrows(UserException::class.java) {
            suppressErrorLogs { eingangsrechnungDao.insert(invoice) }
        }
        Assertions.assertEquals("fibu.rechnung.error.rechnungHatKeinePositionen", ex.i18nKey)
        Assertions.assertEquals("positionen", ex.causedByField)
    }

    /**
     * The import reconciliation reads account, positions and cost assignments of all invoices of a date range: they must come
     * initialized (fetch joins), lazy loading cost two queries per invoice and one per position (n+1).
     */
    @Test
    fun getByDateRangeFetchesPositionsAndCostAssignments() {
        logon(TEST_FINANCE_USER)
        // A day of its own (no other test uses it), but near: a far-off year would break the year range guard of years.
        val datum = LocalDate.now().minusYears(2).withMonth(3).withDayOfMonth(15)
        val invoice = EingangsrechnungDO()
        invoice.datum = datum
        invoice.faelligkeit = datum
        invoice.konto = KontoDO().also {
            it.nummer = 47110815
            it.bezeichnung = "Test account of getByDateRange"
            kontoDao.insert(it, checkAccess = false)
        }
        repeat(2) { posIndex ->
            invoice.addPosition(EingangsrechnungsPositionDO().also { pos ->
                pos.menge = BigDecimal.ONE
                pos.einzelNetto = BigDecimal.TEN
                repeat(posIndex + 1) { pos.addKostZuweisung(KostZuweisungDO().also { it.netto = BigDecimal.ONE }) }
            })
        }
        val id = eingangsrechnungDao.insert(invoice)

        val result = eingangsrechnungDao.getByDateRange(datum, datum)
        Assertions.assertEquals(listOf(id), result.map { it.id }, "One invoice expected, not one per fetched position.")
        Assertions.assertTrue(Hibernate.isInitialized(result[0].konto), "Account must be fetched.")
        val positions = result[0].positionen
        Assertions.assertTrue(Hibernate.isInitialized(positions), "Positions must be fetched.")
        Assertions.assertEquals(listOf<Short>(1, 2), positions!!.map { it.number })
        positions.forEach { pos ->
            Assertions.assertTrue(Hibernate.isInitialized(pos.kostZuweisungen), "Cost assignments must be fetched.")
        }
        Assertions.assertEquals(listOf(1, 2), positions.map { it.kostZuweisungen!!.size })
    }

    private fun checkNoAccess(id: Serializable, eingangsrechnung: EingangsrechnungDO, who: String) {
        try {
            val filter = RechnungFilter()
            suppressErrorLogs {
                eingangsrechnungDao.select(filter)
            }
            Assertions.fail { "AccessException expected: $who users should not have select list access to invoices." }
        } catch (ex: AccessException) {
            // OK
        }
        try {
            suppressErrorLogs {
                eingangsrechnungDao.find(id)
            }
            Assertions.fail { "AccessException expected: $who users should not have select access to invoices." }
        } catch (ex: AccessException) {
            // OK
        }
        checkNoHistoryAccess(id, eingangsrechnung, who)
        checkNoWriteAccess(id, eingangsrechnung, who)
    }

    private fun checkNoHistoryAccess(id: Serializable, eingangsrechnung: EingangsrechnungDO, who: String) {
        Assertions.assertEquals(
            eingangsrechnungDao.hasLoggedInUserHistoryAccess(false), false,
            "$who users should not have select access to history of invoices."
        )
        try {
            suppressErrorLogs {
                eingangsrechnungDao.hasLoggedInUserHistoryAccess(true)
            }
            Assertions.fail { "AccessException expected: $who users should not have select access to history of invoices." }
        } catch (ex: AccessException) {
            // OK
        }
        Assertions.assertEquals(
            eingangsrechnungDao.hasLoggedInUserHistoryAccess(eingangsrechnung, false), false,
            "$who users should not have select access to history of invoices."
        )
        try {
            suppressErrorLogs {
                eingangsrechnungDao.hasLoggedInUserHistoryAccess(eingangsrechnung, true)
            }
            Assertions.fail { "AccessException expected: $who users should not have select access to history of invoices." }
        } catch (ex: AccessException) {
            // OK
        }
    }

    private fun checkNoWriteAccess(id: Serializable, eingangsrechnung: EingangsrechnungDO, who: String) {
        try {
            val re = EingangsrechnungDO()
            re.datum = LocalDate.now()
            suppressErrorLogs {
                eingangsrechnungDao.insert(re)
            }
            Assertions.fail { "AccessException expected: $who users should not have save access to invoices." }
        } catch (ex: AccessException) {
            // OK
        }
        try {
            eingangsrechnung.bemerkung = who
            suppressErrorLogs {
                eingangsrechnungDao.update(eingangsrechnung)
            }
            Assertions.fail { "AccessException expected: $who users should not have update access to invoices." }
        } catch (ex: AccessException) {
            // OK
        }
    }
}
