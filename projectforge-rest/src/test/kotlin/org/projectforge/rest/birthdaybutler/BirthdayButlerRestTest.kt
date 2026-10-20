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


package org.projectforge.rest.birthdaybutler

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import org.projectforge.birthdaybutler.BirthdayButlerConfiguration
import org.projectforge.business.address.AddressDO
import org.projectforge.business.address.AddressDao
import org.projectforge.business.address.AddressStatus
import org.projectforge.business.address.ContactStatus
import org.projectforge.business.test.AbstractTestBase
import org.projectforge.framework.access.AccessException
import org.projectforge.framework.i18n.translate
import org.projectforge.framework.persistence.user.entities.PFUserDO
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.core.io.ByteArrayResource
import org.springframework.http.HttpHeaders
import org.springframework.http.HttpStatus
import java.time.LocalDate
import java.time.Month

/**
 * The endpoints of the birthday butler page. How the Word document looks is `BirthdayButlerService`'s business;
 * what belongs here is who may use the page, and that a list which can't be created is a 400 with the reason.
 */
class BirthdayButlerRestTest : AbstractTestBase() {
    @Autowired
    private lateinit var birthdayButlerRest: BirthdayButlerRest

    @Autowired
    private lateinit var birthdayButlerConfiguration: BirthdayButlerConfiguration

    @Autowired
    private lateinit var addressDao: AddressDao

    @Test
    fun `a user outside the orga team and the hr group reaches none of the endpoints`() {
        logon(TEST_USER)
        assertThrows<AccessException> { birthdayButlerRest.getInit() }
        assertThrows<AccessException> { birthdayButlerRest.download(BirthdayButlerRest.Request(1)) }
    }

    @Test
    fun `an hr user gets the current month and is told that no organization is configured`() {
        logon(TEST_HR_USER)
        val init = birthdayButlerRest.getInit()
        assertEquals(LocalDate.now().monthValue, init.month)
        assertEquals(false, init.configured, "The test installation configures no organization.")

        val invalid = birthdayButlerRest.download(BirthdayButlerRest.Request(13))
        assertEquals(HttpStatus.BAD_REQUEST, invalid.statusCode)
        assertEquals(translate("birthdayButler.month.response.nothingSelected"), invalid.body)

        val notConfigured = birthdayButlerRest.download(BirthdayButlerRest.Request(1))
        assertEquals(HttpStatus.BAD_REQUEST, notConfigured.statusCode)
        assertEquals(translate("birthdayButler.organization.notSet"), notConfigured.body)
    }

    @Test
    fun `the word document lists the staff of the organization with a birthday in the month`() {
        logon(TEST_ADMIN_USER)
        initTestDB.addUser(PFUserDO().also {
            it.username = "BirthdayButlerRestTest.user"
            it.firstname = "Birgit"
            it.lastname = "Butler"
        })
        addressDao.insert(AddressDO().also {
            it.firstName = "Birgit"
            it.name = "Butler"
            it.organization = "BirthdayButlerRestTest GmbH"
            it.birthday = LocalDate.of(1980, Month.MARCH, 14)
            it.addressStatus = AddressStatus.UPTODATE
            it.contactStatus = ContactStatus.ACTIVE
        }, checkAccess = false)
        val organization = birthdayButlerConfiguration.organization
        birthdayButlerConfiguration.organization = "BirthdayButlerRestTest GmbH"
        try {
            logon(TEST_HR_USER)
            assertEquals(true, birthdayButlerRest.getInit().configured)

            val download = birthdayButlerRest.download(BirthdayButlerRest.Request(3))
            assertEquals(HttpStatus.OK, download.statusCode)
            assertTrue(download.headers.getFirst(HttpHeaders.CONTENT_DISPOSITION)!!.contains(".docx"))
            val bytes = (download.body as ByteArrayResource).byteArray
            assertTrue(bytes.size > 2 && bytes[0] == 'P'.code.toByte() && bytes[1] == 'K'.code.toByte(), "A docx (zip).")

            val nobody = birthdayButlerRest.download(BirthdayButlerRest.Request(4))
            assertEquals(HttpStatus.BAD_REQUEST, nobody.statusCode)
            assertEquals(translate("birthdayButler.month.response.noEntry"), nobody.body)
        } finally {
            birthdayButlerConfiguration.organization = organization
        }
    }
}
