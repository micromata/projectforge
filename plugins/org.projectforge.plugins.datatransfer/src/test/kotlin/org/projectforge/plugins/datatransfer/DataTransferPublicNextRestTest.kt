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


package org.projectforge.plugins.datatransfer

import org.junit.jupiter.api.Assertions
import org.junit.jupiter.api.Test
import org.mockito.Mockito
import org.projectforge.business.login.LoginProtection
import org.projectforge.business.test.AbstractTestBase
import org.projectforge.framework.jcr.Attachment
import org.projectforge.framework.jcr.AttachmentsService
import org.projectforge.framework.persistence.user.api.UserContext
import org.projectforge.login.LoginService
import org.projectforge.plugins.datatransfer.restPublic.DataTransferPublicNextRest
import org.projectforge.plugins.datatransfer.restPublic.DataTransferPublicServicesRest
import org.projectforge.rest.AttachmentsServicesRest
import org.projectforge.rest.dto.PostData
import org.projectforge.ui.ResponseAction
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.http.ResponseEntity
import org.springframework.web.multipart.MultipartFile
import jakarta.annotation.PostConstruct
import jakarta.servlet.http.HttpServletRequest
import java.io.ByteArrayInputStream

class DataTransferPublicNextRestTest : AbstractTestBase() {
    @Autowired
    private lateinit var testService: DataTransferTestService

    @Autowired
    private lateinit var dataTransferPublicNextRest: DataTransferPublicNextRest

    @Autowired
    private lateinit var dataTransferPublicServicesRest: DataTransferPublicServicesRest

    init {
        DataTransferTestService.addPluginEntitiesForTestMode()
    }

    @PostConstruct
    private fun postConstruct() {
        initJCRTestRepo(MODUL_NAME, "publicNextRestTestRepo")
    }

    @Test
    fun loginAndStateTest() {
        logon(TEST_USER)
        val area = testService.createArea("Next login test", externalDownloadEnabled = true)
        testService.createFile(area, "file.txt")
        val token = area.externalAccessToken!!
        val request = DataTransferTestService.mockHttpServletRequest()

        dataTransferPublicNextRest.getState(request, token).let { state ->
            Assertions.assertFalse(state.loggedIn)
            Assertions.assertNull(state.id, "Nothing of the area is known without login.")
            Assertions.assertNull(state.attachments)
        }
        // An unknown token answers the same.
        dataTransferPublicNextRest.getState(request, "x".repeat(30)).let { state ->
            Assertions.assertFalse(state.loggedIn)
            Assertions.assertNull(state.id)
        }

        val failed = dataTransferPublicNextRest.login(request, DataTransferPublicNextRest.LoginData(token, "wrong"))
        Assertions.assertEquals(400, failed.statusCode.value())
        Assertions.assertTrue((failed.body as DataTransferPublicNextRest.Message).message.isNotBlank())
        // The failed attempt locks the token for a second.
        LoginProtection.instance().clearLoginTimeOffset(token, null, null)

        val ok = dataTransferPublicNextRest.login(
            request,
            DataTransferPublicNextRest.LoginData(token, area.externalPassword, "  external user  "),
        )
        Assertions.assertEquals(200, ok.statusCode.value())
        (ok.body as DataTransferPublicNextRest.State).let { state ->
            Assertions.assertTrue(state.loggedIn)
            Assertions.assertEquals(area.id, state.id)
            Assertions.assertEquals("external user", state.userInfo)
            Assertions.assertEquals(1, state.attachments!!.size)
            Assertions.assertTrue(state.downloadAllAvailable)
        }
        Assertions.assertTrue(dataTransferPublicNextRest.getState(request, token).loggedIn)
        // The login belongs to the session (the logout invalidates it, which the mocked session ignores).
        val otherSession = DataTransferTestService.mockHttpServletRequest()
        Assertions.assertFalse(dataTransferPublicNextRest.getState(otherSession, token).loggedIn)
        Assertions.assertTrue(dataTransferPublicNextRest.logout(request).message.isNotBlank())
    }

    /**
     * Without download access an external user sees, changes and deletes only the files uploaded in his session, and
     * no answer reveals the others.
     */
    @Test
    fun uploadOnlyAreaTest() {
        logon(TEST_USER)
        val area = testService.createArea("Next upload only test", externalUploadEnabled = true)
        val foreignFileId = testService.createFile(area, "foreign.txt")!!.fileId!!
        val token = area.externalAccessToken!!
        val request = DataTransferTestService.mockHttpServletRequest()
        val state = dataTransferPublicNextRest.login(
            request,
            DataTransferPublicNextRest.LoginData(token, area.externalPassword),
        ).body as DataTransferPublicNextRest.State
        Assertions.assertEquals(0, state.attachments!!.size, "Foreign files are hidden.")
        Assertions.assertFalse(state.downloadAllAvailable)

        val uploaded = attachmentsOf(upload(request, area, "own.txt"))
        Assertions.assertEquals(1, uploaded.size)
        val own = uploaded.first()
        Assertions.assertEquals("own.txt", own.name)
        Assertions.assertNotEquals(true, own.readonly)
        Assertions.assertNull(own.createdByUser, "The uploader (ip address of external users) isn't shown.")

        // The foreign file can't be modified, the own one can, and the answer still hides the foreign file.
        Assertions.assertEquals(400, modify(request, area, foreignFileId, "renamed.txt").statusCode.value())
        val modified = attachmentsOf(modify(request, area, own.fileId!!, "own-renamed.txt"))
        Assertions.assertEquals(listOf("own-renamed.txt"), modified.map { it.name })

        val afterDelete = attachmentsOf(
            dataTransferPublicServicesRest.delete(
                request,
                postData(attachmentData(area, own.fileId!!, "own-renamed.txt")),
            )!!
        )
        Assertions.assertEquals(0, afterDelete.size, "The foreign file must not be part of the answer.")
    }

    /**
     * An external user with upload access encrypts his own files only. The ZIP replacing the file has a new file id,
     * which still counts as his: he can change it, and in an area without download access he still sees it.
     */
    @Test
    fun encryptOwnFileTest() {
        logon(TEST_USER)
        val area = testService.createArea("Next encrypt test", externalUploadEnabled = true)
        val foreignFileId = testService.createFile(area, "foreign.txt")!!.fileId!!
        val request = DataTransferTestService.mockHttpServletRequest()
        dataTransferPublicNextRest.login(
            request,
            DataTransferPublicNextRest.LoginData(area.externalAccessToken!!, area.externalPassword),
        )
        val own = attachmentsOf(upload(request, area, "own.txt")).single()

        Assertions.assertEquals(400, encrypt(request, area, foreignFileId, "secret-password").statusCode.value())
        Assertions.assertEquals(406, encrypt(request, area, own.fileId!!, "123").statusCode.value())

        val encrypted = attachmentsOf(encrypt(request, area, own.fileId!!, "secret-password")).single()
        Assertions.assertEquals("own.txt.zip", encrypted.name)
        Assertions.assertNotEquals(own.fileId, encrypted.fileId)
        Assertions.assertTrue(encrypted.encrypted == true)
        Assertions.assertNotEquals(true, encrypted.readonly, "The ZIP is an own file of the session.")
        Assertions.assertEquals(
            listOf("own-encrypted.zip"),
            attachmentsOf(modify(request, area, encrypted.fileId!!, "own-encrypted.zip")).map { it.name },
        )

        Assertions.assertEquals(406, testDecryption(request, area, encrypted.fileId!!, "wrong").let {
            (it as ResponseEntity<*>).statusCode.value()
        })
        Assertions.assertFalse(testDecryption(request, area, encrypted.fileId!!, "secret-password") is ResponseEntity<*>)
        Assertions.assertEquals(
            400,
            (testDecryption(request, area, foreignFileId, "secret-password") as ResponseEntity<*>).statusCode.value(),
        )
    }

    /**
     * Who uploaded a file is shown only to visitors logged in to ProjectForge with access to the area there (admin,
     * access user or access group), not with the area's password alone.
     */
    @Test
    fun userInfoVisibilityTest() {
        logon(TEST_USER)
        val area = testService.createArea(
            "Next user info test",
            accessGroups = listOf(getGroup(TEST_GROUP)),
            externalDownloadEnabled = true,
        )
        testService.createFile(area, "file.txt")
        fun stateFor(username: String?): DataTransferPublicNextRest.State {
            val request = DataTransferTestService.mockHttpServletRequest()
            username?.let { LoginService.internalLoginInCurrentSession(request, UserContext(getUser(it))) }
            LoginProtection.instance().clearLoginTimeOffset(area.externalAccessToken, null, null)
            return dataTransferPublicNextRest.login(
                request,
                DataTransferPublicNextRest.LoginData(area.externalAccessToken!!, area.externalPassword),
            ).body as DataTransferPublicNextRest.State
        }
        listOf(null, ADMIN).forEach { username ->
            stateFor(username).let { state ->
                Assertions.assertFalse(state.userInfoVisible, "No access to the area: $username")
                Assertions.assertNull(state.attachments!!.single().createdByUser)
            }
        }
        // The admin of the area, and a member of its access group.
        listOf(TEST_USER, TEST_USER2).forEach { username ->
            stateFor(username).let { state ->
                Assertions.assertTrue(state.userInfoVisible, "Access to the area: $username")
                Assertions.assertNotNull(state.attachments!!.single().createdByUser)
            }
        }
    }

    private fun encrypt(
        request: HttpServletRequest,
        area: DataTransferAreaDO,
        fileId: String,
        password: String,
    ): ResponseEntity<*> {
        val data = attachmentData(area, fileId, "ignored").also { it.attachment.password = password }
        return dataTransferPublicServicesRest.encrypt(request, postData(data))
    }

    private fun testDecryption(
        request: HttpServletRequest,
        area: DataTransferAreaDO,
        fileId: String,
        password: String,
    ): Any {
        val data = attachmentData(area, fileId, "ignored").also { it.attachment.password = password }
        return dataTransferPublicServicesRest.testDecryption(request, postData(data))
    }

    private fun upload(request: HttpServletRequest, area: DataTransferAreaDO, filename: String): ResponseEntity<*> {
        val file = Mockito.mock(MultipartFile::class.java)
        Mockito.`when`(file.originalFilename).thenReturn(filename)
        Mockito.`when`(file.inputStream).thenReturn(ByteArrayInputStream("fake file content".toByteArray()))
        return dataTransferPublicServicesRest.uploadAttachment(
            request,
            category = DataTransferPlugin.ID,
            id = area.id!!,
            listId = AttachmentsService.DEFAULT_NODE,
            file = file,
        )!!
    }

    private fun modify(
        request: HttpServletRequest,
        area: DataTransferAreaDO,
        fileId: String,
        name: String,
    ): ResponseEntity<*> {
        return dataTransferPublicServicesRest.modify(request, postData(attachmentData(area, fileId, name)))!!
    }

    private fun <T : Any> postData(data: T) = PostData(data, watchFieldsTriggered = null, serverData = null)

    private fun attachmentData(area: DataTransferAreaDO, fileId: String, name: String) =
        AttachmentsServicesRest.AttachmentData(
            category = DataTransferPlugin.ID,
            id = area.id!!,
            fileId = fileId,
            listId = AttachmentsService.DEFAULT_NODE,
        ).also {
            it.attachment = Attachment().also { attachment ->
                attachment.fileId = fileId
                attachment.name = name
            }
        }

    private fun attachmentsOf(response: ResponseEntity<*>): List<Attachment> {
        Assertions.assertEquals(200, response.statusCode.value())
        // The variables are for the client only (private, serialized by Jackson).
        val field = ResponseAction::class.java.getDeclaredField("variables").also { it.isAccessible = true }
        @Suppress("UNCHECKED_CAST")
        val variables = field.get(response.body as ResponseAction) as Map<String, Any>
        return (variables["data"] as AttachmentsServicesRest.ResponseData).attachments ?: emptyList()
    }
}
