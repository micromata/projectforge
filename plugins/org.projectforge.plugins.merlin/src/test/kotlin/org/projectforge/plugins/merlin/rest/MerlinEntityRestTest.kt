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

package org.projectforge.plugins.merlin.rest

import jakarta.annotation.PostConstruct
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.Assertions.assertThrows
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import org.projectforge.business.test.AbstractTestBase
import org.projectforge.framework.access.AccessException
import org.projectforge.framework.i18n.translate
import org.projectforge.framework.jcr.AttachmentsService
import org.projectforge.framework.persistence.jpa.MyJpaWithExtLibrariesScanner
import org.projectforge.framework.persistence.user.api.ThreadLocalUserContext
import org.projectforge.framework.persistence.user.entities.PFUserDO
import org.projectforge.jcr.RepoService
import org.projectforge.plugins.merlin.MerlinExecutionData
import org.projectforge.plugins.merlin.MerlinHandler
import org.projectforge.plugins.merlin.MerlinRunner
import org.projectforge.plugins.merlin.MerlinTemplate
import org.projectforge.plugins.merlin.MerlinTemplateDO
import org.projectforge.plugins.merlin.MerlinTemplateDao
import org.projectforge.plugins.merlin.MerlinVariable
import org.projectforge.rest.core.SessionCsrfService
import org.projectforge.rest.dto.PostData
import org.projectforge.rest.dto.User
import org.projectforge.ui.ResponseAction
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.core.io.Resource
import org.springframework.http.HttpStatus
import org.springframework.mock.web.MockHttpServletRequest
import org.springframework.mock.web.MockMultipartFile
import java.time.LocalDate
import java.util.TimeZone

/**
 * The Word® templates behind the next pages ([MerlinEntityRest], [MerlinExecuteRest]): a template is read and
 * executed by its admins and the users with access only, and changed by its admins only.
 */
class MerlinEntityRestTest : AbstractTestBase() {
    @Autowired
    private lateinit var merlinTemplateDao: MerlinTemplateDao

    @Autowired
    private lateinit var merlinEntityRest: MerlinEntityRest

    @Autowired
    private lateinit var merlinExecuteRest: MerlinExecuteRest

    @Autowired
    private lateinit var merlinTestServicesRest: MerlinTestServicesRest

    @Autowired
    private lateinit var repoService: RepoService

    @Autowired
    private lateinit var attachmentsService: AttachmentsService

    @Autowired
    private lateinit var merlinHandler: MerlinHandler

    @Autowired
    private lateinit var sessionCsrfService: SessionCsrfService

    init {
        MyJpaWithExtLibrariesScanner.addPluginEntitiesForTestMode(MerlinTemplateDO::class.java.canonicalName)
    }

    @PostConstruct
    private fun postConstruct() {
        // The Word® templates are attachments in the JCR (see AbstractTestBase.initJCRTestRepo).
        initJCRTestRepo(MODULE_NAME, "merlinEntityRestTestRepo")
    }

    override fun afterAll() {
        repoService.shutdown()
    }

    @Test
    fun `the example letter is executed with its variables, a missing required value is refused`() {
        logon(TEST_USER)
        val id = createLetter()
        val data = merlinExecuteRest.load(id)
        assertTrue(data.editAccess, "The creator is admin of the example.")
        assertEquals(listOf("base"), data.serialFills, "No users/employees for an ordinary user.")
        assertTrue(data.variables.any { it.name == "Receiver" && it.required })
        assertEquals("LetterTemplate.docx", data.wordTemplateFileName)

        val missing = merlinExecuteRest.execute(post(execution(id, "Receiver" to "")))
        assertEquals(HttpStatus.NOT_ACCEPTABLE, missing.statusCode)
        val errors = (missing.body as ResponseAction).validationErrors!!
        assertTrue(errors.any { it.fieldId == "inputVariables.Receiver" }, "Error at the field of the variable.")

        val ok = merlinExecuteRest.execute(
            post(
                execution(
                    id,
                    "Gender" to "female",
                    "Date" to "2026-10-22",
                    "Receiver" to "Jane Doe",
                    "Receiver_Address" to "Street 1",
                    "Receiver_City" to "Town",
                )
            )
        )
        assertEquals(HttpStatus.OK, ok.statusCode)
        assertTrue((ok.body as Resource).contentLength() > 0, "The Word® document.")
    }

    @Test
    fun `a file that is no Excel file for a serial execution is refused with a message`() {
        logon(TEST_USER)
        val id = createLetter()
        val expected = translate("plugins.merlin.serial.unsupportedFile")
        // Not an Excel® file at all, though named like one:
        val pdf = MockMultipartFile("file", "serial.xlsx", null, "%PDF-1.4 no workbook".toByteArray())
        val noExcel = merlinExecuteRest.serialExecution(id, pdf)
        assertEquals(HttpStatus.BAD_REQUEST, noExcel.statusCode)
        assertEquals(expected, noExcel.body)
        // Not even named like one:
        val text = MockMultipartFile("file", "serial.txt", null, "some text".toByteArray())
        assertEquals(expected, merlinExecuteRest.serialExecution(id, text).body)
    }

    @Test
    fun `a template of others is neither readable nor executable`() {
        logon(TEST_USER)
        val id = createLetter()
        logon(TEST_USER2)
        assertThrows(AccessException::class.java) { merlinEntityRest.getItem(id) }
        assertThrows(AccessException::class.java) { merlinExecuteRest.load(id) }
        assertThrows(AccessException::class.java) { merlinExecuteRest.execute(post(execution(id))) }
        assertThrows(AccessException::class.java) { merlinExecuteRest.downloadSerialExecutionTemplate(id, "base") }
        assertThrows(AccessException::class.java) { merlinEntityRest.analyze(post(MerlinTemplate(id))) }
        assertThrows(AccessException::class.java) {
            merlinEntityRest.exportExcelTemplate(post(MerlinTemplate(id)))
        }
    }

    @Test
    fun `a user with access executes the template, but doesn't change it`() {
        val admin = logon(TEST_USER)
        val id = createLetter()
        val accessUser = getUser(TEST_USER2)
        val stored = merlinTemplateDao.find(id)!!
        stored.accessUserIds = "${accessUser.id}"
        merlinTemplateDao.update(stored)

        logon(accessUser)
        val data = merlinExecuteRest.load(id)
        assertFalse(data.editAccess)
        assertNotNull(merlinEntityRest.getItem(id).body)
        val dto = merlinEntityRest.getItem(id).body as MerlinTemplate
        assertEquals(false, dto.writeAccess)
        dto.name = "taken over?"
        dto.admins = dto.admins.orEmpty() + User(accessUser)
        assertThrows(AccessException::class.java) { merlinTemplateDao.update(merlinEntityRest.transformForDB(dto)) }
        assertEquals("${admin.id}", merlinTemplateDao.find(id, checkAccess = false)!!.adminIds)
    }

    @Test
    fun `renaming a master variable renames the master of its dependent variables`() {
        logon(TEST_USER)
        val id = createLetter()
        val dto = merlinEntityRest.getItem(id).body as MerlinTemplate
        val gender = dto.variables.single { it.name == "Gender" }
        assertTrue(dto.dependentVariables.any { it.dependsOnName == "Gender" })
        val changed = MerlinVariable()
        changed.copyFrom(gender)
        changed.name = "Sex"
        dto.currentVariable = changed
        val result = merlinEntityRest.updateVariable(post(dto))
        assertTrue(result.variables.any { it.name == "Sex" && it.id == gender.id })
        assertFalse(result.variables.any { it.name == "Gender" })
        assertTrue(result.dependentVariables.all { it.dependsOnName == "Sex" })
        assertNotNull(result.lastVariableUpdate)

        // A new variable (unknown id) is added:
        val added = MerlinVariable()
        added.id = -1
        added.name = "Subject"
        dto.variables.clear()
        dto.variables.addAll(result.variables)
        dto.dependentVariables.clear()
        dto.dependentVariables.addAll(result.dependentVariables)
        dto.currentVariable = added
        val withNew = merlinEntityRest.updateVariable(post(dto))
        val subject = withNew.variables.single { it.name == "Subject" }
        assertTrue(subject.id!! > 0)
        assertTrue(subject.input, "A new variable is an input variable.")
    }

    @Test
    fun `deleting a template deletes its attachments, too`() {
        logon(TEST_USER)
        val id = createLetter()
        val stored = { attachmentsService.internalGetAttachments(merlinHandler.jcrPath, id) }
        assertTrue(stored().isNotEmpty(), "The Word® template of the example.")
        val dto = merlinEntityRest.getItem(id).body as MerlinTemplate
        val request = MockHttpServletRequest()
        request.getSession(true)
        val serverData = sessionCsrfService.createServerData(request)
        val response = merlinEntityRest.delete(request, PostData(dto, watchFieldsTriggered = null, serverData = serverData))
        assertEquals(HttpStatus.OK, response.statusCode)
        assertEquals(null, merlinTemplateDao.find(id, checkAccess = false))
        assertTrue(stored().isEmpty(), "No attachment is left behind.")
    }

    @Test
    fun `the Excel template definition of a new, unsaved template is created from its variables`() {
        logon(TEST_USER)
        val dto = MerlinTemplate()
        dto.name = "new"
        val variable = MerlinVariable()
        variable.name = "Receiver"
        dto.variables.add(variable)
        val response = merlinEntityRest.exportExcelTemplate(post(dto))
        assertEquals(HttpStatus.OK, response.statusCode)
    }

    @Test
    fun `a date is the day of the form, also west of UTC, and a timestamp of the React form is the user's day`() {
        val user = PFUserDO()
        user.username = "merlin-new-york"
        user.timeZone = TimeZone.getTimeZone("America/New_York")
        val previous = ThreadLocalUserContext.loggedInUser
        ThreadLocalUserContext.setUser(user)
        try {
            assertEquals(LocalDate.of(2026, 10, 22), MerlinRunner.parseDate("2026-10-22"))
            // Midnight in New York, as the React form stored it:
            assertEquals(LocalDate.of(2026, 10, 22), MerlinRunner.parseDate("2026-10-22T04:00:00.000Z"))
            assertEquals(null, MerlinRunner.parseDate("2026-13-45"))
        } finally {
            ThreadLocalUserContext.setUser(previous)
        }
    }

    private fun createLetter(): Long {
        @Suppress("UNCHECKED_CAST")
        val body = merlinTestServicesRest.letter().body as Map<String, Any?>
        return body["id"] as Long
    }

    companion object {
        /** Module directory this test runs in — `initJCRTestRepo` insists on it (see `TestUtils`). */
        private const val MODULE_NAME = "org.projectforge.plugins.merlin"
    }

    private fun <T : Any> post(data: T) = PostData(data, watchFieldsTriggered = null, serverData = null)

    private fun execution(id: Long, vararg values: Pair<String, Any>): MerlinExecutionData {
        val data = MerlinExecutionData(id, "Letter")
        data.inputVariables = mutableMapOf(*values)
        return data
    }
}
