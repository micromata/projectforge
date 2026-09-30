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

import org.junit.jupiter.api.Assertions
import org.junit.jupiter.api.Test
import org.projectforge.common.i18n.UserException
import org.projectforge.framework.access.AccessException
import org.projectforge.business.test.AbstractTestBase
import org.springframework.beans.factory.annotation.Autowired
import java.io.Serializable
import java.math.BigDecimal
import java.time.LocalDate

class RechnungDaoTest : AbstractTestBase() {
    @Autowired
    private lateinit var rechnungDao: RechnungDao

    @Test
    fun testNextNumber() {
        persistenceService.runInTransaction { _ ->
            var dbNumber = rechnungDao.nextNumber
            logon(TEST_FINANCE_USER)
            val rechnung1 = RechnungDO()
            var number = rechnungDao.getNextNumber(rechnung1)
            rechnung1.datum = LocalDate.now()
            rechnung1.faelligkeit = LocalDate.now()
            rechnung1.projekt = initTestDB.addProjekt(null, 1, "foo")
            try {
                rechnungDao.insert(rechnung1)
                Assertions.fail("Exception with wrong number should be thrown (no number given).")
            } catch (ex: UserException) {
                // Expected
            }
            rechnung1.nummer = number
            rechnung1.addPosition(createPosition(1, "50.00", "0", "test"))
            var id: Serializable? = rechnungDao.insert(rechnung1)
            val rechnung1FromDb = rechnungDao.find(id, attached = true) // Attached is important, otherwise deadlock.
            Assertions.assertEquals(dbNumber++, rechnung1FromDb!!.nummer)

            val rechnung2 = RechnungDO()
            rechnung2.datum = LocalDate.now()
            rechnung2.nummer = number
            try {
                rechnungDao.insert(rechnung2)
                Assertions.fail<Any>("Exception with wrong number should be thrown (does already exists).")
            } catch (ex: UserException) {
                // Expected.
            }
            number = rechnungDao.getNextNumber(rechnung2)
            rechnung2.nummer = number + 1
            rechnung2.faelligkeit = LocalDate.now()
            rechnung2.projekt = initTestDB.addProjekt(null, 1, "foo")
            try {
                rechnungDao.insert(rechnung2)
                Assertions.fail<Any>("Exception with wrong number should be thrown (not continuously).")
            } catch (ex: UserException) {
                // OK
            }
            rechnung2.nummer = number
            rechnung2.addPosition(createPosition(1, "50.00", "0", "test"))
            id = rechnungDao.insert(rechnung2)
            val rechnung2FromDb = rechnungDao.find(id, attached = true) // Attached is important, otherwise deadlock.
            Assertions.assertEquals(dbNumber, rechnung2FromDb!!.nummer)

            val rechnung3 = RechnungDO()
            rechnung3.datum = LocalDate.now()
            rechnung3.typ = RechnungTyp.GUTSCHRIFTSANZEIGE_DURCH_KUNDEN
            rechnung3.addPosition(createPosition(1, "50.00", "0", "test"))
            rechnung3.faelligkeit = LocalDate.now()
            rechnung3.projekt = initTestDB.addProjekt(null, 1, "foo")
            id = rechnungDao.insert(rechnung3)
            val rechnung3FromDb = rechnungDao.find(id, attached = true) // Attached is important, otherwise deadlock.
            Assertions.assertNull(rechnung3FromDb!!.nummer)
        }
    }

    @Test
    fun testCancellation() {
        persistenceService.runInTransaction { _ ->
            logon(TEST_FINANCE_USER)
            val original = RechnungDO()
            original.datum = LocalDate.now()
            original.faelligkeit = LocalDate.now()
            original.projekt = initTestDB.addProjekt(null, 1, "foo")
            original.nummer = rechnungDao.getNextNumber(original)
            original.status = RechnungStatus.GESTELLT
            original.addPosition(createPosition(2, "50.00", "0.19", "test"))
            val originalId = rechnungDao.insert(original)
            val originalNummer = original.nummer!!
            Assertions.assertNull(rechnungDao.findCancellationOf(originalId))

            fun newCancellation(): RechnungDO {
                val cancellation = RechnungDO()
                cancellation.typ = RechnungTyp.CANCELLATION
                cancellation.datum = LocalDate.now()
                cancellation.faelligkeit = LocalDate.now()
                cancellation.projekt = original.projekt
                cancellation.originalRechnung = RechnungDO().also { it.id = originalId }
                cancellation.addPosition(createPosition(2, "-50.00", "0.19", "test"))
                return cancellation
            }
            assertUserException("fibu.rechnung.error.cancellation.noOwnNumber") {
                rechnungDao.insert(newCancellation().also { it.nummer = rechnungDao.nextNumber })
            }
            assertUserException("fibu.rechnung.error.cancellation.originalRequired") {
                rechnungDao.insert(newCancellation().also { it.originalRechnung = null })
            }
            assertUserException("fibu.rechnung.error.cancellation.mustBeNegative") {
                rechnungDao.insert(newCancellation().also { it.positionen!![0].einzelNetto = BigDecimal("50.00") })
            }

            val nextNumberBefore = rechnungDao.nextNumber
            val cancellationId = rechnungDao.insert(newCancellation())
            val cancellation = rechnungDao.find(cancellationId, attached = true)!!
            Assertions.assertNull(cancellation.nummer, "A cancellation has no number of its own.")
            Assertions.assertEquals("$originalNummer-S", cancellation.belegNummer)
            Assertions.assertEquals("$originalNummer-S", cancellation.displayName)
            Assertions.assertEquals(nextNumberBefore, rechnungDao.nextNumber, "No number is spent on a cancellation.")
            Assertions.assertEquals(cancellationId, rechnungDao.findCancellationOf(originalId)?.id)

            // An invoice is cancelled once at most, and a cancellation can't be cancelled.
            assertUserException("fibu.rechnung.error.cancellation.alreadyCancelled") {
                rechnungDao.insert(newCancellation())
            }
            assertUserException("fibu.rechnung.error.cancellation.originalInvalid") {
                rechnungDao.insert(newCancellation().also {
                    it.originalRechnung = RechnungDO().also { ref -> ref.id = cancellationId }
                })
            }

            // The original is cancelled by it.
            Assertions.assertEquals(RechnungStatus.STORNIERT, rechnungDao.find(originalId, attached = true)!!.status)
            Assertions.assertFalse(cancellation.isValid, "Left out where the STORNIERT original is left out.")

            // A stored cancellation keeps its type and its original.
            assertUserException("fibu.rechnung.error.cancellation.unchangeable") {
                rechnungDao.update(RechnungDO().also {
                    it.id = cancellationId
                    it.typ = RechnungTyp.RECHNUNG
                })
            }

            // Deleting the cancellation reverts the original.
            rechnungDao.markAsDeleted(cancellation)
            Assertions.assertEquals(RechnungStatus.GESTELLT, rechnungDao.find(originalId, attached = true)!!.status)
            Assertions.assertNull(rechnungDao.findCancellationOf(originalId))
        }
    }

    private fun assertUserException(i18nKey: String, block: () -> Unit) {
        val ex = Assertions.assertThrows(UserException::class.java) { block() }
        Assertions.assertEquals(i18nKey, ex.i18nKey)
    }

    @Test
    fun checkAccess() {
        lateinit var rechnung: RechnungDO
        lateinit var id: Serializable
        persistenceService.runInTransaction<Any?> { _ ->
            logon(TEST_FINANCE_USER)
            rechnung = RechnungDO()
            val number = rechnungDao.getNextNumber(rechnung)
            rechnung.datum = LocalDate.now()
            rechnung.faelligkeit = LocalDate.now()
            rechnung.projekt = initTestDB.addProjekt(null, 1, "foo")
            rechnung.nummer = number

            rechnung.addPosition(createPosition(2, "100.50", "0.19", "test"))
            rechnung.addPosition(createPosition(1, "50.00", "0", "test"))
            id = rechnungDao.insert(rechnung)
            rechnung = rechnungDao.find(id, attached = true)!! // Attached is important, otherwise deadlock.
            Assertions.assertEquals("289.19", rechnung.info.grossSum.setScale(2).toString(),
                "info should be calculated correctly by RechnungDao.afterInsertOrModify.")
        }
        persistenceService.runInTransaction { _ ->
            logon(TEST_CONTROLLING_USER)
            rechnungDao.find(id, attached = true) // Attached is important, otherwise deadlock.
            checkNoWriteAccess(rechnung, "Controlling")

            logon(TEST_USER)
            checkNoAccess(id, rechnung, "Other")

            logon(TEST_PROJECT_MANAGER_USER)
            // The project manager has PM_ORDER_BOOK, so it may open the (per-row filtered) outgoing invoice
            // list - but not this invoice, which is linked to no order (see RechnungDaoOrderBookAccessTest).
            checkNoAccess(id, rechnung, "Project manager", mayOpenList = true)

            logon(TEST_ADMIN_USER)
            checkNoAccess(id, rechnung, "Admin ")
            null
        }
    }

    private fun checkNoAccess(id: Serializable, rechnung: RechnungDO, who: String, mayOpenList: Boolean = false) {
        val filter = RechnungFilter()
        if (mayOpenList) {
            // Order book users (PM_ORDER_BOOK) may open the per-row filtered list without an exception; the
            // per-row grant itself is asserted in RechnungDaoOrderBookAccessTest.
            rechnungDao.select(filter)
        } else {
            try {
                rechnungDao.select(filter)
                Assertions.fail<Any>("AccessException expected: $who users should not have select list access to invoices.")
            } catch (ex: AccessException) {
                // OK
            }
        }
        try {
            rechnungDao.find(id, attached = true) // Attached is important, otherwise deadlock.
            Assertions.fail<Any>("AccessException expected: $who users should not have select access to invoices.")
        } catch (ex: AccessException) {
            // OK
        }
        checkNoHistoryAccess(rechnung, who)
        checkNoWriteAccess(rechnung, who)
    }

    private fun checkNoHistoryAccess(rechnung: RechnungDO, who: String) {
        Assertions.assertFalse(
            rechnungDao.hasLoggedInUserHistoryAccess(false),
            "$who users should not have select access to history of invoices."
        )
        try {
            rechnungDao.hasLoggedInUserHistoryAccess(true)
            Assertions.fail<Any>("AccessException expected: $who users should not have select access to history of invoices.")
        } catch (ex: AccessException) {
            // OK
        }
        Assertions.assertFalse(
            rechnungDao.hasLoggedInUserHistoryAccess(rechnung, false),
            "$who users should not have select access to history of invoices."
        )
        try {
            rechnungDao.hasLoggedInUserHistoryAccess(rechnung, true)
            Assertions.fail<Any>("AccessException expected: $who users should not have select access to history of invoices.")
        } catch (ex: AccessException) {
            // OK
        }
    }

    private fun checkNoWriteAccess(rechnung: RechnungDO, who: String) {
        try {
            val re = RechnungDO()
            val number = rechnungDao.getNextNumber(re)
            re.datum = LocalDate.now()
            re.nummer = number
            rechnungDao.insert(re)
            Assertions.fail<Any>("AccessException expected: $who users should not have save access to invoices.")
        } catch (ex: AccessException) {
            // OK
        }
        try {
            rechnung.bemerkung = who
            rechnungDao.update(rechnung)
            Assertions.fail<Any>("AccessException expected: $who users should not have update access to invoices.")
        } catch (ex: AccessException) {
            // OK
        }
    }

    /**
     * [RechnungDao.find] overrides its base to initialize the lazy positions and cost assignments. It used
     * to dereference both with `!!`, so an id nobody has answered with an NPE - a 500 where the REST layer
     * would have answered a 404.
     */
    @Test
    fun `an unknown id answers null instead of throwing`() {
        logon(TEST_FINANCE_USER)
        persistenceService.runInTransaction<Any?> { _ ->
            Assertions.assertNull(rechnungDao.find(-1L, checkAccess = false))
            null
        }
    }

    @Test
    fun `the positions and their cost assignments are readable outside the transaction`() {
        lateinit var id: Serializable
        logon(TEST_FINANCE_USER)
        persistenceService.runInTransaction<Any?> { _ ->
            val rechnung = RechnungDO()
            rechnung.nummer = rechnungDao.getNextNumber(rechnung)
            rechnung.datum = LocalDate.now()
            rechnung.faelligkeit = LocalDate.now()
            rechnung.projekt = initTestDB.addProjekt(null, 42, "lazy init")
            rechnung.addPosition(createPosition(1, "50.00", "0", "test"))
            id = rechnungDao.insert(rechnung)
            null
        }
        // What the override is for: no LazyInitializationException here.
        val fromDb = rechnungDao.find(id)
        Assertions.assertEquals(1, fromDb!!.positionen!!.size)
        Assertions.assertNotNull(fromDb.positionen!![0].kostZuweisungen)
    }

    private fun createPosition(
        menge: Int, einzelNetto: String, vat: String,
        text: String
    ): RechnungsPositionDO {
        val pos = RechnungsPositionDO()
        pos.menge = BigDecimal(menge)
        pos.einzelNetto = BigDecimal(einzelNetto)
        pos.vat = BigDecimal(vat)
        pos.text = text
        return pos
    }
}
