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
import org.junit.jupiter.api.Assertions
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.fail
import java.io.File
import java.io.FileOutputStream
import java.util.*
import java.util.zip.ZipOutputStream

private val log = KotlinLogging.logger {}

class RepoTest {
    private val repoService = RepoService()

    init {
        val repoDir = TestUtils.deleteAndCreateTestFile("testRepo")
        repoService.init(repoDir)
    }

    @Test
    fun repoTest() {
        try {
            repoService.ensureNode("world/europe", "germany")
            fail("Exception expected, because node 'world/europe' doesn't exist.")
        } catch (ex: Exception) {
            // OK, hello/world doesn't exist.
        }
        val main = repoService.mainNodeName
        Assertions.assertEquals("/$main/world/europe", repoService.ensureNode(null, "world/europe"))
        repoService.storeProperty("world/europe", "germany", "key", "value")
        Assertions.assertEquals("value", repoService.retrievePropertyString("world/europe/", "germany", "key"))

        val file = FileObject()
        file.fileName = "pom.xml"
        file.description = "This is the maven pom file."
        file.parentNodePath = "/world/europe"
        file.relPath = "germany"
        file.content = File(file.fileName).readBytes()
        file.created = Date()
        file.createdByUser = "fin"
        file.lastUpdate = Date()
        file.lastUpdateByUser = "kai"
        repoService.storeFile(file)

        checkFile(file, null, file.fileName)
        checkFile(file, file.id, null)
        checkFile(file, file.id, "unkown")
        checkFile(file, "unkown", file.fileName)

        val unknownFile = FileObject()
        unknownFile.id = "unknown id"
        unknownFile.fileName = "unknown filename"
        unknownFile.parentNodePath = file.parentNodePath
        unknownFile.relPath = file.relPath
        Assertions.assertFalse(repoService.retrieveFile(unknownFile))
        unknownFile.id = file.id
        unknownFile.relPath = "unknown"
        Assertions.assertFalse(repoService.retrieveFile(unknownFile))
        unknownFile.parentNodePath = "unknown"
        Assertions.assertFalse(repoService.retrieveFile(unknownFile))

        file.fileName = "pom.xml"
        file.parentNodePath = "/world/europe"
        file.relPath = "germany"
        Assertions.assertTrue(repoService.retrieveFile(file))
        Assertions.assertTrue(repoService.deleteFile(file))
        Assertions.assertFalse(repoService.retrieveFile(file))

        val repoBackupService = RepoBackupService()
        repoBackupService.repoService = repoService
        ZipOutputStream(FileOutputStream(TestUtils.deleteAndCreateTestFile("fullbackupRepoTest.zip"))).use {
            repoBackupService.backupAsZipArchive("fullbackupRepoTest", it)
        }
        repoService.shutdown()
    }

    private fun checkFile(expected: FileObject, id: String?, fileName: String?, repo: RepoService = repoService) {
        val file = FileObject()
        file.id = id
        file.fileName = fileName
        file.parentNodePath = expected.parentNodePath
        file.relPath = expected.relPath
        Assertions.assertTrue(repo.retrieveFile(file))
        Assertions.assertEquals(expected.size, file.size)
        Assertions.assertEquals(expected.id, file.id)
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
