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

import org.junit.jupiter.api.Assertions
import org.junit.jupiter.api.Test
import org.projectforge.commons.test.TestUtils
import java.io.FileInputStream
import java.io.FileOutputStream
import java.util.zip.ZipInputStream
import java.util.zip.ZipOutputStream
import javax.jcr.Node

class RepoBackupTest {
  private var repoService = RepoService()
  private var repoBackupService = RepoBackupService()
  private var testUtils = TestUtils(MODULE_NAME)

  init {
    val repoDir = testUtils.deleteAndCreateTestFile("testBackupRepo")
    repoService.repoConfig = RepoConfig.createForTests()
    repoService.init(repoDir)
    repoBackupService.repoService = repoService
    repoBackupService.jcrCheckSanityJob = JCRCheckSanityCheckJob()
    repoBackupService.jcrCheckSanityJob.repoService = repoService
  }

  @Test
  fun test() {
    repoService.ensureNode(null, "world/europe")
    repoService.storeProperty("world/europe", "germany", "key", "value")

    var fileObject = createFileObject("/world/europe", "germany", "build.gradle.kts")
    repoService.storeFile(fileObject, FileSizeStandardChecker(100000L))

    fileObject = createFileObject("/world/europe", "germany", "src", "test", "resources", "logback-test.xml")
    repoService.storeFile(fileObject, FileSizeStandardChecker(100000L))

    fileObject = createFileObject("/world/europe", "germany", "test", "files", "logo.png")
    repoService.storeFile(fileObject, FileSizeStandardChecker(100000L))
    val logoFile = fileObject.content!!

    repoService.ensureNode(null, "datatransfer/europe")
    fileObject = createFileObject("/datatransfer/europe", "germany", "test", "files", "logo.png")
    repoService.storeFile(fileObject, FileSizeStandardChecker(100000L))

    // Test path matching logic with string paths instead of Node objects
    val nodePathToTest = "/ProjectForge/datatransfer/europe"
    Assertions.assertTrue(nodePathToTest.contains("/datatransfer"))
    Assertions.assertTrue(nodePathToTest.endsWith("/datatransfer/europe"))

    repoBackupService.registerNodePathToIgnore("datatransfer")
    // Test that the ignored path matches our test path
    Assertions.assertTrue(repoBackupService.listOfIgnoredNodePaths.any { nodePathToTest.contains(it) })

    val zipFile = testUtils.deleteAndCreateTestFile("fullbackup.zip")
    ZipOutputStream(FileOutputStream(zipFile)).use {
      repoBackupService.backupAsZipArchive(zipFile.name, it)
    }

    val repo2Service = RepoService()
    repo2Service.repoConfig = RepoConfig.createForTests()
    val repo2BackupService = RepoBackupService()
    val repo2Dir = testUtils.deleteAndCreateTestFile("testBackupRepo2")
    repo2Service.init(repo2Dir)
    repo2BackupService.repoService = repo2Service
    repo2BackupService.jcrCheckSanityJob = JCRCheckSanityCheckJob()
    repo2BackupService.jcrCheckSanityJob.repoService = repo2Service

    ZipInputStream(FileInputStream(zipFile)).use {
      val checkResult = repo2BackupService.restoreBackupFromZipArchive(
        it,
        RepoBackupService.RESTORE_SECURITY_CONFIRMATION__I_KNOW_WHAT_I_M_DOING__REPO_MAY_BE_DESTROYED
      )
      Assertions.assertNotNull(checkResult)
      Assertions.assertEquals(0, checkResult.errors.size)
      Assertions.assertEquals(0, checkResult.warnings.size)
      Assertions.assertEquals(8, checkResult.numberOfVisitedNodes)
      Assertions.assertEquals(3, checkResult.numberOfVisitedFiles)
    }
    ZipInputStream(FileInputStream(zipFile)).use {
      var zipEntry = it.nextEntry
      while (zipEntry != null) {
        Assertions.assertFalse(zipEntry.name.contains("datatransfer", true), "DataTransfer should be ignored: ${zipEntry.name}")
        zipEntry = it.nextEntry
      }
    }
    ZipOutputStream(FileOutputStream(testUtils.deleteAndCreateTestFile("fullbackupFromRestored.zip"))).use {
      repo2BackupService.backupAsZipArchive("fullbackupFromRestored", it)
    }

    Assertions.assertEquals("value", repo2Service.retrievePropertyString("world/europe/", "germany", "key"))

    fileObject = FileObject("/world/europe", "germany", fileInfo = FileInfo("logo.png"))
    repo2Service.retrieveFile(fileObject)
    Assertions.assertEquals(logoFile.size, fileObject.content!!.size)
    for (idx in logoFile.indices) {
      Assertions.assertEquals(logoFile[idx], fileObject.content!![idx])
    }
    fileObject = FileObject("/datatransfer/europe", "germany", fileInfo = FileInfo("logo.png"))
    Assertions.assertFalse(repo2Service.retrieveFile(fileObject), "datatransfer stuff should be ignored in backup.")

    repoService.shutdown()
    repo2Service.shutdown()
  }

  private fun createFileObject(parentNodePath: String, relPath: String, vararg path: String): FileObject {
    val fileObject = FileObject()
    fileObject.fileName = path.last()
    fileObject.parentNodePath = parentNodePath
    fileObject.relPath = relPath
    fileObject.content = testUtils.determineBaseDirFile(*path).readBytes()
    return fileObject
  }
}
