/////////////////////////////////////////////////////////////////////////////
//
// Project ProjectForge Community Edition
//         www.projectforge.org
//
// Copyright (C) 2001-2020 Micromata GmbH, Germany (www.micromata.com)
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
import org.apache.jackrabbit.commons.JcrUtils
import org.junit.jupiter.api.Assertions
import org.junit.jupiter.api.Test
import java.io.FileInputStream
import java.io.FileOutputStream
import java.util.zip.ZipInputStream
import java.util.zip.ZipOutputStream

private val log = KotlinLogging.logger {}

private const val MODULE_NAME = "projectforge-jcr"

class RepoBackupTest {
    private var repoService = RepoService()
    private var repoBackupService = RepoBackupService()

    init {
        val repoDir = TestUtils.deleteAndCreateTestFile("testBackupRepo")
        repoService.init(mapOf(JcrUtils.REPOSITORY_URI to repoDir.toURI().toString()))
        repoBackupService.repoService = repoService
    }

    @Test
    fun test() {
        repoService.ensureNode(null, "world/europe")
        repoService.storeProperty("world/europe", "germany", "key", "value")

        var fileObject = createFileObject("/world/europe", "germany", "pom.xml")
        repoService.storeFile(fileObject)

        fileObject = createFileObject("/world/europe", "germany", "src", "test", "resources", "logback-test.xml")
        repoService.storeFile(fileObject)

        fileObject = createFileObject("/world/europe", "germany", "test", "files", "logo.png")
        repoService.storeFile(fileObject)
        val logoFile = fileObject.content!!

        val zipFile = TestUtils.deleteAndCreateTestFile("fullbackup.zip")
        println("Creating zip file: ${zipFile.absolutePath}")
        ZipOutputStream(FileOutputStream(zipFile)).use {
            repoBackupService.backupAsZipArchive("/world", zipFile.name, it)
        }

        val repo2Service = RepoService()
        val repo2BackupService = RepoBackupService()
        val repo2Dir = TestUtils.deleteAndCreateTestFile("testBackupRepo2")
        repo2Service.init(mapOf(JcrUtils.REPOSITORY_URI to repo2Dir.toURI().toString()))
        repo2BackupService.repoService = repo2Service

        ZipInputStream(FileInputStream(zipFile)).use {
            repo2BackupService.restoreBackupFromZipArchive("/", it, RepoBackupService.RESTORE_SECURITY_CONFIRMATION__I_KNOW_WHAT_I_M_DOING__REPO_MAY_BE_DESTROYED)
        }
        ZipOutputStream(FileOutputStream(TestUtils.deleteAndCreateTestFile("fullbackupFromRestored.zip"))).use {
            repo2BackupService.backupAsZipArchive("/world", "fullbackupFromRestored", it)
        }

        Assertions.assertEquals("value", repo2Service.retrievePropertyString("world/europe/", "germany", "key"))

        fileObject = FileObject("/world/europe", "germany", fileName = "logo.png")
        repo2Service.retrieveFile(fileObject)
        Assertions.assertEquals(logoFile.size, fileObject.content!!.size)
        for (idx in logoFile.indices) {
            Assertions.assertEquals(logoFile[idx], fileObject.content!![idx])
        }
    }

    private fun createFileObject(parentNodePath: String, relPath: String, vararg path: String): FileObject {
        val fileObject = FileObject()
        fileObject.fileName = path.last()
        fileObject.parentNodePath = parentNodePath
        fileObject.relPath = relPath
        fileObject.content = TestUtils.determineFile(*path).readBytes()
        return fileObject
    }
}
