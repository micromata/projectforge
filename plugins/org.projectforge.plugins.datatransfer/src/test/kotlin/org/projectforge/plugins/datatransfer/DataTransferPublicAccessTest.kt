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

package org.projectforge.plugins.datatransfer

import mu.KotlinLogging
import org.apache.commons.io.IOUtils
import org.junit.jupiter.api.Assertions
import org.junit.jupiter.api.Test
import org.mockito.Mockito
import org.projectforge.framework.jcr.AttachmentsService
import org.projectforge.jcr.FileObject
import org.projectforge.plugins.datatransfer.restPublic.DataTransferPublicPageRest
import org.projectforge.plugins.datatransfer.restPublic.DataTransferPublicServicesRest
import org.projectforge.plugins.datatransfer.restPublic.DataTransferPublicSession
import org.projectforge.rest.core.PagesResolver
import org.projectforge.business.test.AbstractTestBase
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.core.io.InputStreamResource
import org.springframework.web.multipart.MultipartFile
import java.io.ByteArrayInputStream
import jakarta.annotation.PostConstruct
import jakarta.servlet.http.HttpServletRequest
import jakarta.servlet.http.HttpServletResponse

private val log = KotlinLogging.logger {}

class DataTransferPublicAccessTest : AbstractTestBase() {
  @Autowired
  private lateinit var testService: DataTransferTestService

  @Autowired
  private lateinit var dataTransferPublicServicesRest: DataTransferPublicServicesRest

  @Autowired
  private lateinit var dataTransferPublicSession: DataTransferPublicSession

  init {
    DataTransferTestService.addPluginEntitiesForTestMode()
  }

  @PostConstruct
  private fun postConstruct() {
    initJCRTestRepo(MODUL_NAME, "publicAccessTestRepo")
  }

  @Test
  fun pageResolverTest() {
    Assertions.assertEquals(
      "react/public/datatransfer/dynamic",
      PagesResolver.getDynamicPageUrl(DataTransferPublicPageRest::class.java, trailingSlash = false)
    )
  }

  @Test
  fun areaAccessTest() {
    logon(TEST_USER)
    val externalDownloadArea =
      testService.createArea("Test box of user with external download access", externalDownloadEnabled = true)
    val externalFullAccessArea =
      testService.createArea(
        "Test box of user with external download and upload access", externalDownloadEnabled = true,
        externalUploadEnabled = true
      )
    val file1a = testService.createFile(externalDownloadArea, "file1.xml")!!
    val file1b = testService.createFile(externalDownloadArea, "build.gradle.kts")!!

    val file2 = testService.createFile(externalFullAccessArea, "build.gradle.kts")!!

    val request = DataTransferTestService.mockHttpServletRequest()
    login(request, externalDownloadArea)
    login(request, externalFullAccessArea)

    checkDownload(request, externalDownloadArea, listOf(file1a, file1b))
    checkDownload(request, externalFullAccessArea, listOf(file2))

    downloadAllAndCheck(request, externalDownloadArea, file1a, file1b)

    downloadAllAndCheck(request, externalFullAccessArea, file2)

    uploadFile(request, externalDownloadArea, "no-upload-access.txt")
    downloadAllAndCheck(request, externalDownloadArea, file1a, file1b) // Unchanged list of files.

    uploadFile(request, externalFullAccessArea, "upload.txt")
    downloadAllAndCheck(request, externalFullAccessArea, file2.fileName!!, "upload.txt")
  }

  private fun login(request: HttpServletRequest, dataTransferArea: DataTransferAreaDO) {
    dataTransferPublicSession.login(
      request,
      dataTransferArea.externalAccessToken!!,
      dataTransferArea.externalPassword!!,
      "external Userinfo"
    )
  }

  private fun checkDownload(
    request: HttpServletRequest,
    dataTransferArea: DataTransferAreaDO,
    accessFiles: List<FileObject>?,
    noAccessFiles: List<FileObject>? = null
  ) {
    accessFiles?.forEach {
      checkDownloadFile(request, dataTransferArea, it, true)
    }
    noAccessFiles?.forEach {
      checkDownloadFile(request, dataTransferArea, it, false)
    }
  }

  private fun checkDownloadFile(
    request: HttpServletRequest,
    dataTransferArea: DataTransferAreaDO,
    file: FileObject,
    access: Boolean
  ) {
    val result = downloadFile(request, dataTransferArea, file)
    if (access) {
      Assertions.assertNotNull(
        result,
        "Download file expected, but no file found or access isn't given: ${file.fileName}."
      )
    } else {
      Assertions.assertNull(
        result,
        "Download file NOT expected, but file with access found: ${file.fileName}."
      )
    }
  }

  private fun downloadAllAndCheck(
    request: HttpServletRequest,
    dataTransferArea: DataTransferAreaDO,
    vararg expectedFiles: FileObject
  ) {
    downloadAllAndCheck(request, dataTransferArea, *(expectedFiles.map { it.fileName!! }.toTypedArray()))
  }

  private fun downloadAllAndCheck(
    request: HttpServletRequest,
    dataTransferArea: DataTransferAreaDO,
    vararg expectedFiles: String,
  ) {
    val response = Mockito.mock(HttpServletResponse::class.java)
    val servletOutputStream = DataTransferTestService.MyServletOutputStream()
    Mockito.`when`(response.outputStream).thenReturn(servletOutputStream)
    dataTransferPublicServicesRest.downloadAll(
      request,
      response,
      DataTransferPlugin.ID,
      dataTransferArea.id!!,

      )
    val files = DataTransferTestService.checkZipArchive(servletOutputStream.byteArray)
    Assertions.assertEquals(expectedFiles.size, files.size)
    expectedFiles.forEach {
      Assertions.assertTrue(files.contains(it), "List of files should contain file '$it'!")
    }
  }

  private fun uploadFile(
    request: HttpServletRequest,
    dataTransferArea: DataTransferAreaDO,
    filename: String
  ) {
    val file = Mockito.mock(MultipartFile::class.java)
    Mockito.`when`(file.originalFilename).thenReturn(filename)
    Mockito.`when`(file.inputStream).thenReturn(ByteArrayInputStream("fake file content".toByteArray()))
    Mockito.verifyNoInteractions(file)

    try {
      dataTransferPublicServicesRest.uploadAttachment(
        request,
        category = DataTransferPlugin.ID,
        id = dataTransferArea.id!!,
        listId = AttachmentsService.DEFAULT_NODE,
        file = file,
      )
    } catch (ex: Exception) {
      // Not found or no access
      log.info(ex.message, ex)
    }
  }

  private fun downloadFile(
    request: HttpServletRequest,
    dataTransferArea: DataTransferAreaDO,
    fileObject: FileObject,
  ): ByteArray? {
    val response = try {
      dataTransferPublicServicesRest.download(
        request,
        category = DataTransferPlugin.ID,
        id = dataTransferArea.id!!,
        fileId = fileObject.fileId!!,
        listId = AttachmentsService.DEFAULT_NODE,
      )
    } catch (ex: Exception) {
      // Not found or no access
      log.info(ex.message, ex)
      return null
    }
    (response.body as InputStreamResource).inputStream.use {
      return IOUtils.toByteArray(it)
    }
  }
}
