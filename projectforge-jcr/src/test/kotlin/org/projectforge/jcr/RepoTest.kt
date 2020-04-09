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
import org.junit.jupiter.api.*
import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream
import java.nio.charset.StandardCharsets
import java.util.*
import java.util.zip.GZIPInputStream
import java.util.zip.GZIPOutputStream
import javax.jcr.ImportUUIDBehavior

private val log = KotlinLogging.logger {}

class RepoTest {

    companion object {
        private lateinit var repoService: RepoService
        private val repoDir = createTempDir()

        @BeforeAll
        @JvmStatic
        fun setUp() {
            repoService = RepoService()
            repoService.init(mapOf(JcrUtils.REPOSITORY_URI to repoDir.toURI().toString()))
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
        try {
            repoService.ensureNode("world/europe", "germany")
            fail("Exception expected, because node 'world/europe' doesn't exist.")
        } catch (ex: Exception) {
            // OK, hello/world doesn't exist.
        }
        Assertions.assertEquals("/world/europe", repoService.ensureNode(null, "world/europe"))
        repoService.storeProperty("world/europe", "germany", "key", "value")
        Assertions.assertEquals("value", repoService.retrievePropertyString("world/europe/", "germany", "key"))

        val file = FileObject()
        file.fileName = "pom.xml"
        file.parentNodePath = "/world/europe"
        file.relPath = "germany"
        file.content = File(file.fileName).readBytes()
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
        try {
            Assertions.assertFalse(repoService.retrieveFile(unknownFile))
            fail("Exception expected, because parent path not found.")
        } catch (ex: Exception) {
            // OK
        }

        val repoBackupService = RepoBackupService()
        repoBackupService.repoService = repoService

        val gzFile = createTempFile(suffix = ".gz")
        GZIPOutputStream(FileOutputStream(gzFile)).use {
            repoBackupService.backupDocumentView("/world", it, skipBinary = false, noRecurse = false)
        }
        println(gzFile.absoluteFile)
        // Create second repository:
        val backupRepoService = RepoService()
        val backupRepoDir = createTempDir()
        backupRepoService.init(mapOf(JcrUtils.REPOSITORY_URI to backupRepoDir.toURI().toString()))
        GZIPInputStream(FileInputStream(gzFile)).buffered().use {
            repoBackupService.restore("/", it, RepoService.RESTORE_SECURITY_CONFIRMATION_IN_KNOW_WHAT_I_M_DOING_REPO_MAY_BE_DESTROYED,
                    ImportUUIDBehavior.IMPORT_UUID_COLLISION_REMOVE_EXISTING)
        }

        val ba = GZIPInputStream(FileInputStream(gzFile)).buffered().use {
            it.readAllBytes()
        }
        println(ba.toString(StandardCharsets.UTF_8))
        val root = backupRepoService.getNodeInfo("/", true)
        println(root)

        val testFile = FileObject()
        testFile.parentNodePath = file.parentNodePath
        testFile.relPath = file.relPath
        testFile.id = file.id
        backupRepoService.retrieveFile(testFile)
        println(decoder(testFile.content))

        checkFile(file, null, file.fileName, backupRepoService)
        gzFile.delete()
    }

    fun decoder(base64: ByteArray?): String? {
        base64 ?: return null
        return decoder(base64.toString(StandardCharsets.UTF_8))
    }

    fun decoder(base64Str: String): String {
        val imageByteArray = Base64.getDecoder().decode(base64Str)
        return imageByteArray.toString(StandardCharsets.UTF_8)
        //File(pathFile).writeBytes(imageByteArray)
    }

    fun encoder(filePath: String): String{
        val bytes = File(filePath).readBytes()
        val base64 = Base64.getEncoder().encodeToString(bytes)
        return base64
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
        Assertions.assertEquals(expected.content!!.size, file.content!!.size)
        for (idx in expected.content!!.indices) {
            Assertions.assertEquals(expected.content!![idx], file.content!![idx])
        }
    }
}
