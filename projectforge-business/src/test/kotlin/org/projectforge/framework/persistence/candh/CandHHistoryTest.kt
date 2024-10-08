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

package org.projectforge.framework.persistence.candh

import org.junit.jupiter.api.Assertions
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.fail
import org.projectforge.business.fibu.*
import org.projectforge.business.user.*
import org.projectforge.framework.persistence.history.EntityOpType
import org.projectforge.framework.persistence.history.HistoryEntryDO
import org.projectforge.framework.persistence.history.PropertyOpType
import org.projectforge.framework.persistence.user.entities.Gender
import org.projectforge.framework.persistence.user.entities.PFUserDO
import org.projectforge.framework.persistence.user.entities.UserRightDO
import org.projectforge.framework.time.PFDateTime
import org.projectforge.framework.time.PFDateTimeUtils
import org.projectforge.framework.time.TimeNotation
import org.projectforge.test.AbstractTestBase
import org.projectforge.test.HistoryTester
import org.projectforge.test.HistoryTester.Companion.assertHistoryEntry
import org.springframework.beans.factory.annotation.Autowired
import java.math.BigDecimal
import java.time.LocalDate
import java.time.Month
import java.util.*

class CandHHistoryTest : AbstractTestBase() {
    @Autowired
    private lateinit var userDao: UserDao

    @Autowired
    private lateinit var userRightDao: UserRightDao

    @Autowired
    private lateinit var rechnungDao: RechnungDao

    @Autowired
    private lateinit var kundeDao: KundeDao

    @Test
    fun baseTests() {
        logon(ADMIN_USER)
        val user = PFUserDO()
        user.username = "$PREFIX.test"
        user.email = "hs@gmail.private"
        user.timeZone = TimeZone.getTimeZone("Europe/Berlin")
        user.gender = Gender.MALE
        user.timeNotation = TimeNotation.H24
        user.restrictedUser = false
        user.firstDayOfWeekValue = 1
        user.locale = Locale.GERMAN
        user.lastPasswordChange =
            PFDateTime.withDate(2021, Month.JANUARY, 1, 12, 17, 33, 763, zoneId = PFDateTimeUtils.ZONE_UTC).utilDate
        val hist = createHistoryTester()
        userDao.saveInTrans(user)
        hist.loadRecentHistoryEntries(1, 0)
        userDao.getHistoryEntries(user).let { entries ->
            Assertions.assertEquals(1, entries.size)
            assertHistoryEntry(entries[0], PFUserDO::class, user.id, EntityOpType.Insert, ADMIN_USER)
        }
        user.email = "horst@acme.com"
        user.username = "$PREFIX.test_changed"
        user.firstname = "Horst"
        user.lastname = "Schlemmer"
        user.timeNotation = TimeNotation.H12
        user.firstDayOfWeekValue = 7
        user.locale = Locale.FRENCH
        user.lastPasswordChange =
            PFDateTime.withDate(2024, Month.OCTOBER, 5, 8, 39, 12, 500, zoneId = PFDateTimeUtils.ZONE_UTC).utilDate
        userDao.updateInTrans(user)
        hist.loadRecentHistoryEntries(1, 8)
        hist.loadHistory(user, 2, 8)

        userDao.getHistoryEntries(user).let { entries ->
            Assertions.assertEquals(2, entries.size)
            assertHistoryEntry(entries[0], PFUserDO::class, user.id, EntityOpType.Update, ADMIN_USER, 8)
            (entries[0] as HistoryEntryDO).let { entry ->
                HistoryTester.assertAttr(
                    entry,
                    propertyName = "username",
                    value = "$PREFIX.test_changed",
                    oldValue = "$PREFIX.test",
                    opType = PropertyOpType.Update,
                )
                HistoryTester.assertAttr(
                    entry,
                    propertyName = "email",
                    value = "horst@acme.com",
                    oldValue = "hs@gmail.private",
                    opType = PropertyOpType.Update,
                )
                HistoryTester.assertAttr(
                    entry,
                    propertyName = "firstname",
                    value = "Horst",
                    oldValue = null,
                    opType = PropertyOpType.Update,
                )
                HistoryTester.assertAttr(
                    entry,
                    propertyName = "lastname",
                    value = "Schlemmer",
                    oldValue = null,
                    opType = PropertyOpType.Update,
                )
                HistoryTester.assertAttr(
                    entry,
                    propertyName = "locale",
                    value = "fr",
                    oldValue = "de",
                    opType = PropertyOpType.Update,
                    propertyTypeClass = Locale::class,
                )
                HistoryTester.assertAttr(
                    entry,
                    propertyName = "firstDayOfWeekValue",
                    value = "7",
                    oldValue = "1",
                    opType = PropertyOpType.Update,
                    propertyTypeClass = Integer::class,
                )
                HistoryTester.assertAttr(
                    entry,
                    propertyName = "timeNotation",
                    value = "H12",
                    oldValue = "H24",
                    opType = PropertyOpType.Update,
                    propertyTypeClass = TimeNotation::class,
                )
                HistoryTester.assertAttr(
                    entry,
                    propertyName = "lastPasswordChange",
                    value = "2024-10-05 08:39:12",
                    oldValue = "2021-01-01 12:17:33",
                    opType = PropertyOpType.Update,
                    propertyTypeClass = java.util.Date::class,
                )
            }
            assertHistoryEntry(entries[1], PFUserDO::class, user.id, EntityOpType.Insert, ADMIN_USER)
        }
        user.description = "This is a deleted test user."
        userDao.markAsDeletedInTrans(user)
        hist.loadRecentHistoryEntries(1, 1)
        userDao.getHistoryEntries(user).let { entries ->
            Assertions.assertEquals(3, entries.size)
            assertHistoryEntry(entries[0], PFUserDO::class, user.id, EntityOpType.Delete, ADMIN_USER, 1)
            (entries[0] as HistoryEntryDO).let { entry ->
                HistoryTester.assertAttr(
                    entry,
                    propertyName = "description",
                    value = "This is a deleted test user.",
                    oldValue = null,
                    opType = PropertyOpType.Update,
                )
            }
        }
        user.description = "This is a undeleted test user."
        userDao.undeleteInTrans(user)
        hist.loadRecentHistoryEntries(1, 1)
        userDao.getHistoryEntries(user).let { entries ->
            Assertions.assertEquals(4, entries.size)
            assertHistoryEntry(entries[0], PFUserDO::class, user.id, EntityOpType.Undelete, ADMIN_USER, 1)
            (entries[0] as HistoryEntryDO).let { entry ->
                HistoryTester.assertAttr(
                    entry,
                    propertyName = "description",
                    value = "This is a undeleted test user.",
                    oldValue = "This is a deleted test user.",
                    opType = PropertyOpType.Update,
                )
            }
        }
    }

    @Test
    fun userRightsTests() {
        val loggedInUser = logon(ADMIN_USER)
        val user = PFUserDO()
        user.username = "$PREFIX.rightsTest"
        val hist = createHistoryTester()
        try {
            user.addRight(UserRightDO(UserRightId.ORGA_OUTGOING_MAIL, UserRightValue.READWRITE))
            userDao.saveInTrans(user)
            fail { "Cascade on saving rights shouldn't work." }
        } catch (ex: Exception) {
            // OK, expected exception.
        }
        user.rights = null
        user.id = null
        userDao.saveInTrans(user)
        user.addRight(UserRightDO(UserRightId.ORGA_OUTGOING_MAIL, UserRightValue.READWRITE))
        user.addRight(UserRightDO(UserRightId.FIBU_DATEV_IMPORT, UserRightValue.TRUE))
        userRightDao.internalSaveOrUpdateInTrans(user.rights!!)
        var rights = userRightDao.getList(user)
        Assertions.assertEquals(2, rights.size)
        hist.loadRecentHistoryEntries(3, 0)
        userDao.getHistoryEntries(user).let { entries ->
            Assertions.assertEquals(3, entries.size)
            assertHistoryEntry(entries[0], UserRightDO::class, null, EntityOpType.Insert, ADMIN_USER)
            assertHistoryEntry(entries[1], UserRightDO::class, null, EntityOpType.Insert, ADMIN_USER)
            assertHistoryEntry(entries[2], PFUserDO::class, user.id, EntityOpType.Insert, ADMIN_USER)
        }

        user.addRight(UserRightDO(UserRightId.PM_ORDER_BOOK, UserRightValue.PARTLYREADWRITE))
        user.getRight(UserRightId.ORGA_OUTGOING_MAIL).let { right ->
            right!!.value = UserRightValue.READONLY
        }
        userRightDao.internalSaveOrUpdateInTrans(user.rights!!)
        rights = userRightDao.getList(user)
        // val recent = getRecentHistoryEntries(5)
        Assertions.assertEquals(3, rights.size)
        hist.loadRecentHistoryEntries(2, 1)
        userDao.getHistoryEntries(user).let { entries ->
            Assertions.assertEquals(5, entries.size)
            entries.filter { it.entityName == UserRightDO::class.qualifiedName && it.entityOpType == EntityOpType.Insert }
                .let { inserts ->
                    Assertions.assertEquals(3, inserts.size)
                }
            entries.single { it.entityName == UserRightDO::class.qualifiedName && it.entityOpType == EntityOpType.Update }
                .let { historyEntry ->
                    assertHistoryEntry(historyEntry, UserRightDO::class, null, EntityOpType.Update, loggedInUser, 1)
                    (historyEntry as HistoryEntryDO).let { entry ->
                        HistoryTester.assertAttr(
                            entry,
                            value = "READONLY",
                            oldValue = "READWRITE",
                            propertyName = "value",
                            opType = PropertyOpType.Update,
                            propertyTypeClass = UserRightValue::class,
                        )
                    }
                }
        }
    }

    @Test
    fun invoiceTests() {
        val loggedInUser = logon(TEST_FINANCE_USER)
        val customer1 = KundeDO()
        customer1.id = 7864872 // Must be manually assigned.
        customer1.name = "7864872 Test customer 1"
        kundeDao.saveInTrans(customer1)
        val customer2 = KundeDO()
        customer2.id = 7864873 // Must be manually assigned.
        customer2.name = "7864873 Test customer 1"
        kundeDao.saveInTrans(customer2)

        var invoice = RechnungDO()
        invoice.nummer = rechnungDao.nextNumber
        invoice.betreff = "Test invoice"
        invoice.datum = LocalDate.now()
        invoice.typ = RechnungTyp.RECHNUNG
        invoice.kunde = customer1
        invoice.faelligkeit = LocalDate.now().plusDays(14)

        RechnungsPositionDO().let { pos ->
            invoice.addPosition(pos)
            pos.text = "Test position 1"
            pos.menge = BigDecimal.ONE
            pos.einzelNetto = BigDecimal.TEN
        }
        RechnungsPositionDO().let { pos ->
            invoice.addPosition(pos)
            pos.text = "Test position 2"
            pos.menge = BigDecimal("2")
            pos.einzelNetto = BigDecimal("20")
        }
        val id = rechnungDao.saveInTrans(invoice)
        invoice = rechnungDao.getById(id)!!
        invoice.kunde = customer2
        invoice.getAbstractPosition(0).let { pos ->
            pos!!.text = "Test position 1 changed"
            pos.menge = BigDecimal("1.1")
            pos.einzelNetto = BigDecimal("11")
        }
        RechnungsPositionDO().let { pos ->
            invoice.addPosition(pos)
            pos.text = "Test position 3"
            pos.menge = BigDecimal("3")
            pos.einzelNetto = BigDecimal("30")
        }
        val hist = createHistoryTester()
        rechnungDao.updateInTrans(invoice)
        invoice = rechnungDao.getById(id)!!
        val count = persistenceService.selectSingleResult(
            "select count(*) from RechnungsPositionDO WHERE rechnung.id = :id",
            Long::class.java,
            Pair("id", id),
        )
        Assertions.assertEquals(3, count)
        hist.loadRecentHistoryEntries(3, 4)
        rechnungDao.getHistoryEntries(invoice).let { entries ->
            Assertions.assertEquals(4, entries.size)
            entries.single { it.entityName == RechnungsPositionDO::class.qualifiedName && it.entityOpType == EntityOpType.Insert }
                .let { entry ->
                    assertHistoryEntry(entry, RechnungsPositionDO::class, null, EntityOpType.Insert, loggedInUser)
                }
            entries.single { it.entityName == RechnungsPositionDO::class.qualifiedName && it.entityOpType == EntityOpType.Update }
                .let { historyEntry ->
                    assertHistoryEntry(
                        historyEntry,
                        RechnungsPositionDO::class,
                        null,
                        EntityOpType.Update,
                        loggedInUser,
                        3
                    )
                    (historyEntry as HistoryEntryDO).let { entry ->
                        HistoryTester.assertAttr(
                            entry,
                            propertyName = "text",
                            value = "Test position 1 changed",
                            oldValue = "Test position 1",
                            opType = PropertyOpType.Update,
                        )
                        HistoryTester.assertAttr(
                            entry,
                            value = "1.1",
                            oldValue = "1.00000",
                            propertyName = "menge",
                            opType = PropertyOpType.Update,
                            propertyTypeClass = BigDecimal::class,
                        )
                        HistoryTester.assertAttr(
                            entry,
                            propertyName = "einzelNetto",
                            value = "11",
                            oldValue = "10.00",
                            opType = PropertyOpType.Update,
                            propertyTypeClass = BigDecimal::class,
                        )
                    }
                }
            entries.single { it.entityName == RechnungDO::class.qualifiedName && it.entityOpType == EntityOpType.Update }
                .let { entry ->
                    assertHistoryEntry(
                        entry,
                        RechnungDO::class,
                        null,
                        EntityOpType.Update,
                        loggedInUser,
                        1,
                    )
                    HistoryTester.assertAttr(
                        entry,
                        value = "7864873",
                        oldValue = "7864872",
                        propertyName = "kunde",
                        opType = PropertyOpType.Update,
                        propertyTypeClass = KundeDO::class,
                    )
                }

            // First insert:
            entries.single { it.entityName == RechnungDO::class.qualifiedName && it.entityOpType == EntityOpType.Insert }
                .let { entry ->
                    assertHistoryEntry(
                        entry,
                        RechnungDO::class,
                        null,
                        EntityOpType.Insert,
                        loggedInUser,
                    )
                }
        }
    }

    companion object {
        private const val PREFIX = "CandHHistoryTest"
    }
}
