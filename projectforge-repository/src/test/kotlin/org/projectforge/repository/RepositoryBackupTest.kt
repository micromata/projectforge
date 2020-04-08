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

package org.projectforge.repository

import mu.KotlinLogging
import org.apache.jackrabbit.commons.JcrUtils
import org.junit.jupiter.api.AfterAll
import org.junit.jupiter.api.Assertions
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.Test
import java.io.File
import java.io.FileOutputStream
import java.util.zip.ZipOutputStream

private val log = KotlinLogging.logger {}

class RepositoryBackupTest {

    companion object {
        private lateinit var repoService: RepositoryService
        private lateinit var repoBackupService: RepositoryBackupService
        private val repoDir = createTempDir()

        @BeforeAll
        @JvmStatic
        fun setUp() {
            repoService = RepositoryService()
            repoService.init(mapOf(JcrUtils.REPOSITORY_URI to repoDir.toURI().toString()))
            repoBackupService = RepositoryBackupService()
            repoBackupService.repositoryService = repoService
            // repoDir.deleteOnExit() // Doesn't work reliable.
        }

        @AfterAll
        @JvmStatic
        fun tearDown() {
            log.info { "Deleting JackRabbit test repo: $repoDir." }
            Assertions.assertTrue(repoDir.deleteRecursively(), "Couldn't delte JackRabbit test repo: $repoDir.")
        }
    }

    @Test
    fun test() {
        repoService.ensureNode(null, "world/europe")
        repoService.storeProperty("world/europe", "germany", "key", "value")

        val file = FileObject()
        file.fileName = "pom.xml"
        file.parentNodePath = "/world/europe"
        file.relPath = "germany"
        file.content = File(file.fileName).readBytes()
        repoService.storeFile(file)

        val zipFile = createTempFile(suffix = ".zip")
        println("Creating zip file: ${zipFile.absolutePath}")
        ZipOutputStream(FileOutputStream(zipFile)).use {
            repoBackupService.backupAsZipArchive("/world", it)
        }
        //zipFile.delete()
    }
}
