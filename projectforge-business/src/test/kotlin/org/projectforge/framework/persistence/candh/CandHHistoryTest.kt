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
import org.projectforge.business.user.UserDao
import org.projectforge.business.user.UserRightDao
import org.projectforge.business.user.UserRightId
import org.projectforge.business.user.UserRightValue
import org.projectforge.framework.persistence.history.*
import org.projectforge.framework.persistence.user.entities.Gender
import org.projectforge.framework.persistence.user.entities.PFUserDO
import org.projectforge.framework.persistence.user.entities.UserRightDO
import org.projectforge.framework.time.PFDateTime
import org.projectforge.framework.time.PFDateTimeUtils
import org.projectforge.framework.time.TimeNotation
import org.projectforge.test.AbstractTestBase
import org.springframework.beans.factory.annotation.Autowired
import java.math.BigDecimal
import java.time.LocalDate
import java.time.Month
import java.util.*
import kotlin.reflect.KClass

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
        user.username = "${PREFIX}test"
        user.email = "hs@gmail.private"
        user.timeZone = TimeZone.getTimeZone("Europe/Berlin")
        user.gender = Gender.MALE
        user.timeNotation = TimeNotation.H24
        user.restrictedUser = false
        user.firstDayOfWeekValue = 1
        user.locale = Locale.GERMAN
        user.lastPasswordChange =
            PFDateTime.withDate(2021, Month.JANUARY, 1, 12, 17, 33, 763, zoneId = PFDateTimeUtils.ZONE_UTC).utilDate
        var lastStats = countHistoryEntries()
        userDao.saveInTrans(user)
        lastStats = assertNumberOfNewHistoryEntries(lastStats, 1, 0)
        userDao.getHistoryEntries(user).let { entries ->
            Assertions.assertEquals(1, entries.size)
            assertMasterEntry(PFUserDO::class, user.id, EntityOpType.Insert, ADMIN_USER, entries[0])
        }
        user.email = "horst@acme.com"
        user.username = "${PREFIX}test_changed"
        user.firstname = "Horst"
        user.lastname = "Schlemmer"
        user.timeNotation = TimeNotation.H12
        user.firstDayOfWeekValue = 7
        user.locale = Locale.FRENCH
        user.lastPasswordChange =
            PFDateTime.withDate(2024, Month.OCTOBER, 5, 8, 39, 12, 500, zoneId = PFDateTimeUtils.ZONE_UTC).utilDate
        userDao.updateInTrans(user)
        lastStats = assertNumberOfNewHistoryEntries(lastStats, 1, 8)
        userDao.getHistoryEntries(user).let { entries ->
            Assertions.assertEquals(2, entries.size)
            assertMasterEntry(PFUserDO::class, user.id, EntityOpType.Update, ADMIN_USER, entries[0], 8)
            (entries[0] as PfHistoryMasterDO).let { entry ->
                assertAttrEntry(
                    "java.lang.String",
                    "${PREFIX}test_changed",
                    "${PREFIX}test",
                    "username",
                    PropertyOpType.Update,
                    entry.attributes,
                )
                assertAttrEntry(
                    "java.lang.String",
                    "horst@acme.com",
                    "hs@gmail.private",
                    "email",
                    PropertyOpType.Update,
                    entry.attributes,
                )
                assertAttrEntry(
                    "java.lang.String",
                    "Horst",
                    null,
                    "firstname",
                    PropertyOpType.Update,
                    entry.attributes,
                )
                assertAttrEntry(
                    "java.lang.String",
                    "Schlemmer",
                    null,
                    "lastname",
                    PropertyOpType.Update,
                    entry.attributes,
                )
                assertAttrEntry(
                    "java.util.Locale",
                    "fr",
                    "de",
                    "locale",
                    PropertyOpType.Update,
                    entry.attributes,
                )
                assertAttrEntry(
                    "java.lang.Integer",
                    "7",
                    "1",
                    "firstDayOfWeekValue",
                    PropertyOpType.Update,
                    entry.attributes,
                )
                assertAttrEntry(
                    "org.projectforge.framework.time.TimeNotation",
                    "H12",
                    "H24",
                    "timeNotation",
                    PropertyOpType.Update,
                    entry.attributes,
                )
                assertAttrEntry(
                    "java.util.Date",
                    "2024-10-05 08:39:12",
                    "2021-01-01 12:17:33",
                    "lastPasswordChange",
                    PropertyOpType.Update,
                    entry.attributes,
                )
            }
            assertMasterEntry(PFUserDO::class, user.id, EntityOpType.Insert, ADMIN_USER, entries[1])
        }
        user.description = "This is a deleted test user."
        userDao.markAsDeletedInTrans(user)
        lastStats = assertNumberOfNewHistoryEntries(lastStats, 1, 1)
        userDao.getHistoryEntries(user).let { entries ->
            Assertions.assertEquals(3, entries.size)
            assertMasterEntry(PFUserDO::class, user.id, EntityOpType.Delete, ADMIN_USER, entries[0], 1)
            (entries[0] as PfHistoryMasterDO).let { entry ->
                assertAttrEntry(
                    "java.lang.String",
                    "This is a deleted test user.",
                    null,
                    "description",
                    PropertyOpType.Update,
                    entry.attributes,
                )
            }
        }
        user.description = "This is a undeleted test user."
        userDao.undeleteInTrans(user)
        lastStats = assertNumberOfNewHistoryEntries(lastStats, 1, 1)
        userDao.getHistoryEntries(user).let { entries ->
            Assertions.assertEquals(4, entries.size)
            assertMasterEntry(PFUserDO::class, user.id, EntityOpType.Undelete, ADMIN_USER, entries[0], 1)
            (entries[0] as PfHistoryMasterDO).let { entry ->
                assertAttrEntry(
                    "java.lang.String",
                    "This is a undeleted test user.",
                    "This is a deleted test user.",
                    "description",
                    PropertyOpType.Update,
                    entry.attributes,
                )
            }
        }
    }

    @Test
    fun userRightsTests() {
        val loggedInUser = logon(ADMIN_USER)
        val user = PFUserDO()
        user.username = "${PREFIX}rightsTest"
        var lastStats = countHistoryEntries()
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
        lastStats = assertNumberOfNewHistoryEntries(lastStats, 3, 0)
        userDao.getHistoryEntries(user).let { entries ->
            Assertions.assertEquals(3, entries.size)
            assertMasterEntry(UserRightDO::class, null, EntityOpType.Insert, ADMIN_USER, entries[0])
            assertMasterEntry(UserRightDO::class, null, EntityOpType.Insert, ADMIN_USER, entries[1])
            assertMasterEntry(PFUserDO::class, user.id, EntityOpType.Insert, ADMIN_USER, entries[2])
        }

        user.addRight(UserRightDO(UserRightId.PM_ORDER_BOOK, UserRightValue.PARTLYREADWRITE))
        user.getRight(UserRightId.ORGA_OUTGOING_MAIL).let { right ->
            right!!.value = UserRightValue.READONLY
        }
        userRightDao.internalSaveOrUpdateInTrans(user.rights!!)
        rights = userRightDao.getList(user)
        // val recent = getRecentHistoryEntries(5)
        Assertions.assertEquals(3, rights.size)
        lastStats = assertNumberOfNewHistoryEntries(lastStats, 2, 1)
        userDao.getHistoryEntries(user).let { entries ->
            Assertions.assertEquals(5, entries.size)
            entries.filter { it.entityName == UserRightDO::class.qualifiedName && it.entityOpType == EntityOpType.Insert }
                .let { inserts ->
                    Assertions.assertEquals(3, inserts.size)
                }
            entries.single { it.entityName == UserRightDO::class.qualifiedName && it.entityOpType == EntityOpType.Update }
                .let { master ->
                    assertMasterEntry(UserRightDO::class, null, EntityOpType.Update, loggedInUser, master, 1)
                    (master as PfHistoryMasterDO).let { entry ->
                        assertAttrEntry(
                            "org.projectforge.business.user.UserRightValue",
                            "READONLY",
                            "READWRITE",
                            "value",
                            PropertyOpType.Update,
                            entry.attributes,
                        )
                    }
                }
        }

        // TODO: ****** Add, remove and change rights and test history entries...
        // TODO: ****** Testing adding and removing groups and users.
        // TODO: Test of unmanaged collections, such as assigned users etc.
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
        var lastStats = countHistoryEntries()
        rechnungDao.updateInTrans(invoice)
        invoice = rechnungDao.getById(id)!!
        val count = persistenceService.selectSingleResult(
            "select count(*) from RechnungsPositionDO WHERE rechnung.id = :id",
            Long::class.java,
            Pair("id", id),
        )
        Assertions.assertEquals(3, count)
        lastStats = assertNumberOfNewHistoryEntries(lastStats, 3, 4)
        rechnungDao.getHistoryEntries(invoice).let { entries ->
            Assertions.assertEquals(4, entries.size)
            entries.single { it.entityName == RechnungsPositionDO::class.qualifiedName && it.entityOpType == EntityOpType.Insert }
                .let { master ->
                    assertMasterEntry(RechnungsPositionDO::class, null, EntityOpType.Insert, loggedInUser, master)
                }
            entries.single { it.entityName == RechnungsPositionDO::class.qualifiedName && it.entityOpType == EntityOpType.Update }
                .let { master ->
                    assertMasterEntry(RechnungsPositionDO::class, null, EntityOpType.Update, loggedInUser, master, 3)
                    (master as PfHistoryMasterDO).let { entry ->
                        assertAttrEntry(
                            "java.lang.String",
                            "Test position 1 changed",
                            "Test position 1",
                            "text",
                            PropertyOpType.Update,
                            entry.attributes,
                        )
                        assertAttrEntry(
                            "java.math.BigDecimal",
                            "1.1",
                            "1.00000",
                            "menge",
                            PropertyOpType.Update,
                            entry.attributes,
                        )
                        assertAttrEntry(
                            "java.math.BigDecimal",
                            "11",
                            "10.00",
                            "einzelNetto",
                            PropertyOpType.Update,
                            entry.attributes,
                        )
                    }
                }
            entries.single { it.entityName == RechnungDO::class.qualifiedName && it.entityOpType == EntityOpType.Update }
                .let { master ->
                    assertMasterEntry(
                        RechnungDO::class,
                        null,
                        EntityOpType.Update,
                        loggedInUser,
                        master, 1
                    )
                    (master as PfHistoryMasterDO).let { master ->
                        assertAttrEntry(
                            KundeDO::class.qualifiedName,
                            "7864873",
                            "7864872",
                            "kunde",
                            PropertyOpType.Update,
                            master.attributes,
                        )
                    }
                }

            // First insert:
            entries.single { it.entityName == RechnungDO::class.qualifiedName && it.entityOpType == EntityOpType.Insert }
                .let { master ->
                    assertMasterEntry(
                        RechnungDO::class,
                        null,
                        EntityOpType.Insert,
                        loggedInUser,
                        master
                    )
                }
        }
    }

    private fun assertMasterEntry(
        entityClass: KClass<*>,
        id: Long?,
        opType: EntityOpType,
        modUser: PFUserDO,
        entry: HistoryEntry,
        numberOfAttributes: Int = 0,
    ) {
        HistoryServiceTest.assertMasterEntry(entityClass, id, opType, modUser, entry, numberOfAttributes)
    }

    private fun assertAttrEntry(
        propertyClass: String?,
        value: String?,
        oldValue: String?,
        propertyName: String?,
        optype: PropertyOpType,
        attributes: Set<PfHistoryAttrDO>?,
    ) {
        HistoryServiceTest.assertAttrEntry(propertyClass, value, oldValue, propertyName, optype, attributes)
    }

    companion object {
        private const val PREFIX = "CandHHistoryTest_"
    }
}
