/////////////////////////////////////////////////////////////////////////////
//
// Project ProjectForge Community Edition
//         www.projectforge.org
//
// Copyright (C) 2001-2022 Micromata GmbH, Germany (www.micromata.com)
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
import org.projectforge.framework.persistence.jpa.MyJpaWithExtLibrariesScanner
import org.projectforge.framework.persistence.user.entities.PFUserDO
import org.projectforge.jcr.FileObject
import org.projectforge.plugins.datatransfer.rest.DataTransferPageRest
import org.projectforge.test.AbstractTestBase
import org.springframework.beans.factory.annotation.Autowired
import javax.annotation.PostConstruct
import javax.servlet.http.HttpServletResponse


class DataTransferAccessTest : AbstractTestBase() {
  @Autowired
  private lateinit var testService: DataTransferTestService

  @Autowired
  private lateinit var dataTransferPageRest: DataTransferPageRest

  private lateinit var testUser1: PFUserDO
  private lateinit var testUser2: PFUserDO
  private var initialized = false

  init {
    MyJpaWithExtLibrariesScanner.addPluginEntitiesForTestMode(
      DataTransferAreaDO::class.java.canonicalName,
      DataTransferAuditDO::class.java.canonicalName,
    )
  }

  @PostConstruct
  private fun postConstruct() {
    initJCRTestRepo(MODUL_NAME, "accessTestRepo")
  }

  // @PostConstruct doesn't work, data base is initialized later.
  private fun initialize() {
    if (initialized) {
      return
    }
    initialized = true
    testUser1 = getUser(TEST_USER)
    testUser2 = getUser(TEST_USER2)
  }

  @Test
  fun areaAccessTest() {
    initialize()
    val otherUser = getUser(TEST_HR_USER)
    logon(testUser1)
    val testGroupArea =
      testService.createArea(
        "Test user's 1 box",
        accessUsers = listOf(otherUser),
        accessGroups = listOf(getGroup(TEST_GROUP))
      )
    val file1a = testService.createFile(testGroupArea, "file1.xml")!!
    val file1b = testService.createFile(testGroupArea, "pom.xml")!!

    logon(testUser2)
    val file2 = testService.createFile(testGroupArea, "user2.xml")!! // Access, because user is member of test group.
    checkDownload(testGroupArea, listOf(file1a, file1b, file2))

    logon(testUser1)
    checkDownload(testGroupArea, listOf(file1a, file1b, file2))

    logon(ADMIN_USER)
    checkDownload(testGroupArea, null, noAccessFiles = listOf(file1a, file1b, file2))
    testService.createFile(testGroupArea, "admin-user.xml") // Access, because user is member of test group.

    logon(otherUser)
    checkDownload(testGroupArea, listOf(file1a, file1b, file2))
  }

  @Test
  fun personalBoxTest() {
    initialize()
    val personalBox1 = testService.createPersonalBox(testUser1)
    val personalBox2 = testService.createPersonalBox(testUser2)

    logon(testUser1)
    // User 1 uploads file1.xml in his own and personal box of the user2.
    val box1FileOfUser1 = testService.createFile(personalBox1, "box1_file_of_user1.xml")!!
    val box2FileOfUser1 = testService.createFile(personalBox2, "box2_file_of_user1.xml")!!

    logon(testUser2)
    // User 2 uploads file2.xml in his own and personal box of the user1.
    val box1FileOfUser2 = testService.createFile(personalBox1, "box1_file_of_user2.xml")!!
    val box2FileOfUser2 = testService.createFile(personalBox2, "box2_file_of_user2.xml")!!

    checkDownload(personalBox1, listOf(box1FileOfUser2), noAccessFiles = listOf(box1FileOfUser1))
    checkDownload(personalBox2, listOf(box2FileOfUser1, box2FileOfUser2))

    downloadAllAndCheck(personalBox1, box1FileOfUser2) // foreign box, download only own files.
    downloadAllAndCheck(personalBox2, box2FileOfUser1, box2FileOfUser2) // own box, download all.

    logon(testUser1)
    downloadAllAndCheck(personalBox1, box1FileOfUser1, box1FileOfUser2) // own box, download all.
    downloadAllAndCheck(personalBox2, box2FileOfUser1) // foreign box, download only own files.
  }

  private fun checkDownload(
    dataTransferArea: DataTransferAreaDO,
    accessFiles: List<FileObject>?,
    noAccessFiles: List<FileObject>? = null
  ) {
    accessFiles?.forEach {
      checkDownloadFile(dataTransferArea, it, true)
    }
    noAccessFiles?.forEach {
      checkDownloadFile(dataTransferArea, it, false)
    }
  }

  private fun checkDownloadFile(dataTransferArea: DataTransferAreaDO, file: FileObject, access: Boolean) {
    val result = testService.downloadFile(dataTransferArea, file)
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
    // dataTransferPageRest.downloadAll(dataTransferAreaDO.id!!, response)

  }

  private fun downloadAllAndCheck(dataTransferAreaDO: DataTransferAreaDO, vararg expectedFiles: FileObject) {
    val response = Mockito.mock(HttpServletResponse::class.java)
    val servletOutputStream = DataTransferTestService.MyServletOutputStream()
    Mockito.`when`(response.outputStream).thenReturn(servletOutputStream)
    dataTransferPageRest.downloadAll(dataTransferAreaDO.id!!, response)
    val files = DataTransferTestService.checkZipArchive(servletOutputStream.byteArray)
    Assertions.assertEquals(expectedFiles.size, files.size)
    expectedFiles.forEach {
      Assertions.assertTrue(files.contains(it.fileName), "List of files should contain file '$it'!")
    }
  }
}
