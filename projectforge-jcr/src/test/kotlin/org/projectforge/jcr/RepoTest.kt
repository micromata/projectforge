/////////////////////////////////////////////////////////////////////////////
//
// Project ProjectForge Community Edition
//         www.projectforge.org
//
// Copyright (C) 2001-2025 Micromata GmbH, Germany (www.micromata.com)
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

package org.projectforge.jcr

import mu.KotlinLogging
import org.junit.jupiter.api.Assertions
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.fail
import org.projectforge.common.MaxFileSizeExceeded
import org.projectforge.commons.test.TestUtils
import java.io.File
import java.io.FileOutputStream
import java.util.*
import java.util.zip.ZipOutputStream

private val log = KotlinLogging.logger {}

const val MODULE_NAME = "projectforge-jcr"

class RepoTest {
  private val repoService = RepoService()
  private var testUtils = TestUtils(MODULE_NAME)

  init {
    val repoDir = testUtils.deleteAndCreateTestFile("testRepo")
    repoService.init(repoDir)
  }

  @Test
  fun repoTest() {
    try {
      TestUtils.suppressErrorLogs {
        repoService.ensureNode("world/europe", "germany")
      }
      fail("Exception expected, because node 'world/europe' doesn't exist.")
    } catch (ex: Exception) {
      // OK, hello/world doesn't exist.
    }
    val main = repoService.mainNodeName
    Assertions.assertEquals("/$main/world/europe", repoService.ensureNode(null, "world/europe")!!.path)
    repoService.storeProperty("world/europe", "germany", "key", "value")
    Assertions.assertEquals("value", repoService.retrievePropertyString("world/europe/", "germany", "key"))

    val file = FileObject()
    file.fileName = "test/files/logo.png"
    file.description = "This is logo file."
    file.parentNodePath = "/world/europe"
    file.relPath = "germany"
    file.content = File(file.fileName).readBytes()
    file.created = Date()
    file.createdByUser = "fin"
    file.lastUpdate = Date()
    file.lastUpdateByUser = "kai"
    Assertions.assertThrows(
      MaxFileSizeExceeded::class.java
    ) { repoService.storeFile(file, FileSizeStandardChecker(100L)) }
    repoService.storeFile(file, FileSizeStandardChecker(10000L))
    Assertions.assertEquals("SHA256: 5973306df0e1395a401cca276f868c7f63781c7b8acf96f9834631a4fbf6fb47", file.checksum)
    checkFile(file, null, file.fileName)
    checkFile(file, file.fileId, null)
    checkFile(file, file.fileId, "unkown")
    checkFile(file, "unkown", file.fileName)

    val unknownFile = FileObject()
    unknownFile.fileId = "unknown id"
    unknownFile.fileName = "unknown filename"
    unknownFile.parentNodePath = file.parentNodePath
    unknownFile.relPath = file.relPath
    Assertions.assertFalse(repoService.retrieveFile(unknownFile))
    unknownFile.fileId = file.fileId
    unknownFile.relPath = "unknown"
    Assertions.assertFalse(repoService.retrieveFile(unknownFile))
    unknownFile.parentNodePath = "unknown"
    Assertions.assertFalse(repoService.retrieveFile(unknownFile))

    file.fileName = "build.gradle.kts"
    file.parentNodePath = "/world/europe"
    file.relPath = "germany"
    Assertions.assertTrue(repoService.retrieveFile(file))
    Assertions.assertTrue(repoService.deleteFile(file))
    Assertions.assertFalse(repoService.retrieveFile(file))

    cryptoTest()

    val repoBackupService = RepoBackupService()
    repoBackupService.repoService = repoService
    ZipOutputStream(FileOutputStream(testUtils.deleteAndCreateTestFile("fullbackupRepoTest.zip"))).use {
      repoBackupService.backupAsZipArchive("fullbackupRepoTest", it)
    }
    repoService.shutdown()
  }

  private fun cryptoTest() {
    val file = FileObject()
    val password = "dummyPassword"
    file.fileName = "test/files/logo.png"
    file.description = "This is the logo file."
    file.parentNodePath = "/world/europe"
    file.relPath = "germany"
    file.content = File(file.fileName).readBytes()
    file.created = Date()
    file.createdByUser = "fin"
    file.lastUpdate = Date()
    file.lastUpdateByUser = "kai"
    val content = file.content
    repoService.storeFile(file, FileSizeStandardChecker(10000L), password = password)
    file.content = null
    repoService.retrieveFile(file, "dummyPassword")
    Assertions.assertArrayEquals(content, file.content)
    TestUtils.suppressErrorLogs {
      repoService.retrieveFile(file, "dsfsd") // Wrong password
    }
    Assertions.assertNull(file.content)
    TestUtils.suppressErrorLogs {
      repoService.retrieveFile(file) // No password
    }
    Assertions.assertNull(file.content)
  }

  private fun checkFile(expected: FileObject, id: String?, fileName: String?, repo: RepoService = repoService) {
    val file = FileObject()
    file.fileId = id
    file.fileName = fileName
    file.parentNodePath = expected.parentNodePath
    file.relPath = expected.relPath
    Assertions.assertTrue(repo.retrieveFile(file))
    Assertions.assertEquals(expected.size, file.size)
    Assertions.assertEquals(expected.fileId, file.fileId)
    Assertions.assertEquals(expected.fileName, file.fileName)
    Assertions.assertEquals(expected.description, file.description)
    Assertions.assertEquals(expected.created, file.created)
    Assertions.assertEquals(expected.createdByUser ?: "", file.createdByUser)
    Assertions.assertEquals(expected.lastUpdate, file.lastUpdate)
    Assertions.assertEquals(expected.lastUpdateByUser ?: "", file.lastUpdateByUser)
    Assertions.assertEquals(expected.description, file.description)
    Assertions.assertEquals(expected.content!!.size, file.content!!.size)
    for (idx in expected.content!!.indices) {
      Assertions.assertEquals(expected.content!![idx], file.content!![idx])
    }
  }
}
