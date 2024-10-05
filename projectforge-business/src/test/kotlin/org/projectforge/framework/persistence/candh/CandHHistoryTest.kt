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
        logon(ADMIN_USER)
        var user = PFUserDO()
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
        val rights = userRightDao.getList(user)
        lastStats = assertNumberOfNewHistoryEntries(lastStats, 3, 0)
        userDao.getHistoryEntries(user).let { entries ->
            Assertions.assertEquals(3, entries.size)
            assertMasterEntry(UserRightDO::class, null, EntityOpType.Insert, ADMIN_USER, entries[0])
            assertMasterEntry(UserRightDO::class, null, EntityOpType.Insert, ADMIN_USER, entries[1])
            assertMasterEntry(PFUserDO::class, user.id, EntityOpType.Insert, ADMIN_USER, entries[2])
        }
        // TODO: ****** Add, remove and change rights and test history entries...
        // TODO: ****** Testing adding and removing groups and users.
    }

    @Test
    fun invoiceTests() {
/*        logon(TEST_FINANCE_USER)
        val customer = KundeDO()
        customer.name = "Test customer"
        kundeDao.saveInTrans(customer)

        val invoice = RechnungDO()
        invoice.betreff = "Test invoice"
        invoice.datum = LocalDate.now()
        invoice.typ = RechnungTyp.RECHNUNG
        invoice.kunde = customer

        RechnungsPositionDO().let { pos ->
            invoice.addPosition(pos)
            pos.text = "Test position 1"
            pos.menge = BigDecimal.ONE
            pos.einzelNetto = BigDecimal.TEN
        }
        RechnungsPositionDO().let { pos ->
            invoice.addPosition(pos)
            pos.text = "Test position 2"
            pos.menge = BigDecimal.TEN
            pos.einzelNetto = BigDecimal("5.5")
        }
        rechnungDao.saveInTrans(invoice)*/
        // kunde, projekt, status
        /*        invoice.apply {
                     = "12345"
                    invoiceDate = "2020-01-01"
                    invoiceAmount = 123.45
                }
                var lastStats = countHistoryEntries()
                rechnungDao.saveInTrans(invoice)
                lastStats = assertNumberOfNewHistoryEntries(lastStats, 1, 0)
                userDao.getHistoryEntries(invoice).let { entries ->
                    Assertions.assertEquals(1, entries.size)
                    assertMasterEntry(invoice::class, invoice.id, EntityOpType.Insert, ADMIN_USER, entries[0])
                }
                invoice.invoiceNumber = "123456"
                invoice.invoiceDate = "2021-01-01"
                invoice.invoiceAmount = 123.45
                userDao.updateInTrans(invoice)
                assertNumberOfNewHistoryEntries(lastStats, 1, 3)
                userDao.getHistoryEntries(invoice).let { entries ->
                    Assertions.assertEquals(2, entries.size)
                    assertMasterEntry(invoice::class, invoice.id, EntityOpType.Update, ADMIN_USER, entries[0], 3)
                    (entries[0] as PfHistoryMasterDO).let { entry ->
                        assertAttrEntry(
                            "java.lang.String",
                            "123456",
                            null,
                            "invoiceNumber",
                            PropertyOpType.Update,
                            entry.attributes,
                        )
                        assertAttrEntry(
                            "java.lang.String",
                            "2021-01-01",
                            null,
                            "invoiceDate",
                            PropertyOpType.Update,
                            entry.attributes,
                        )
                        assertAttrEntry(
                            "java.lang.Double",
                            "123.45",
                            null,
                            "invoiceAmount",
                            PropertyOpType.Update,
                            entry.attributes,
                        )
                    }
                    assertMasterEntry(invoice::class, invoice.id, EntityOpType.Insert, ADMIN_USER, entries[1])
                }*/
    }

    private fun assertMasterEntry(
        entityClass: KClass<*>,
        id: Long?,
        opType: EntityOpType,
        modUser: PFUserDO,
        entry: HistoryEntry,
        numberOfAttributes: Int = 0,
    ) {
        Assertions.assertEquals(entityClass.java.name, entry.entityName)
        if (id != null) {
            Assertions.assertEquals(id, entry.entityId)
        }
        Assertions.assertEquals(opType, entry.entityOpType)
        Assertions.assertEquals(modUser.id?.toString(), entry.modifiedBy)
        Assertions.assertTrue(
            System.currentTimeMillis() - entry.modifiedAt!!.time < 10000,
            "Time difference is too big",
        )
        entry as PfHistoryMasterDO
        Assertions.assertEquals(numberOfAttributes, entry.attributes?.size ?: 0)
    }

    private fun assertAttrEntry(
        propertyClass: String?,
        value: String?,
        oldValue: String?,
        propertyName: String?,
        optype: PropertyOpType,
        attributes: Set<PfHistoryAttrDO>?,
    ) {
        Assertions.assertFalse(attributes.isNullOrEmpty())
        val attr = attributes?.firstOrNull { it.propertyName == propertyName }
        Assertions.assertNotNull(attr, "Property $propertyName not found")
        Assertions.assertEquals(propertyClass, attr!!.propertyTypeClass)
        Assertions.assertEquals(value, attr.value)
        Assertions.assertEquals(oldValue, attr.oldValue)
        Assertions.assertEquals(propertyName, attr.propertyName)
        Assertions.assertEquals(optype, attr.optype)

    }

    companion object {
        private const val PREFIX = "CandHHistoryTest_"
    }
}
