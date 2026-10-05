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

package org.projectforge.jcr

import org.junit.jupiter.api.Assertions
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.io.TempDir
import org.projectforge.jcr.store.FileStoreTestDataSource
import java.io.File

class RepoMigrationServiceTest {
    @TempDir
    lateinit var tempDir: File

    @Test
    fun `files of file system paths are moved out of the JCR`() {
        val repoService = RepoService()
        repoService.repoConfig = RepoConfig.createForTests()
        repoService.dataSource = FileStoreTestDataSource.dataSource
        repoService.init(File(tempDir, "repo"))
        repoService.fileStore!!.internalClearForJunitTestCases()
        try {
            // Stored in the JCR, because the path isn't yet registered as file system path:
            val plain = store(repoService, "$DATATRANSFER_PATH/1", "plain.txt", "Plain content".toByteArray())
            val encrypted = store(repoService, "$DATATRANSFER_PATH/2", "secret.txt", "Secret".toByteArray(), "pwd")
            val entityFile = store(repoService, "org.projectforge.fibu.RechnungDO/42", "invoice.txt", "Invoice".toByteArray())
            Assertions.assertFalse(repoService.fileStore!!.exists(plain))

            val fsDir = File(tempDir, "datatransfer")
            repoService.registerFileSystemPath(DATATRANSFER_PATH, fsDir)
            // Not yet migrated files are still found in the JCR:
            Assertions.assertTrue(repoService.retrieveFile(copy(plain)))
            Assertions.assertEquals(setOf("1", "2"), repoService.getChildNames(DATATRANSFER_PATH))

            val migrationService = RepoMigrationService()
            migrationService.repoService = repoService
            val progress = mutableListOf<Pair<Int, Int>>()
            val result = migrationService.migrateFileSystemPaths { processed, total -> progress.add(processed to total) }
            Assertions.assertEquals(2, result.migrated, result.toString())
            Assertions.assertTrue(result.errors.isEmpty(), result.errors.joinToString())
            Assertions.assertEquals(2 to 2, progress.last())

            val fileStore = repoService.fileStore!!
            Assertions.assertTrue(fileStore.exists(plain))
            Assertions.assertTrue(fileStore.exists(encrypted))
            Assertions.assertFalse(fileStore.exists(entityFile), "Entity files stay in the JCR (release N).")
            Assertions.assertTrue(File(fsDir, "1/attachments/${plain.fileId}").isFile)

            val plainCopy = copy(plain)
            Assertions.assertTrue(repoService.retrieveFile(plainCopy))
            Assertions.assertEquals("Plain content", String(plainCopy.content!!))
            Assertions.assertEquals("plain.txt", plainCopy.fileName)
            Assertions.assertEquals(plain.created, plainCopy.created)
            val encryptedCopy = copy(encrypted)
            Assertions.assertTrue(repoService.retrieveFile(encryptedCopy, "pwd"), "Encrypted payload is moved 1:1.")
            Assertions.assertEquals("Secret", String(encryptedCopy.content!!))
            val entityCopy = copy(entityFile)
            Assertions.assertTrue(repoService.retrieveFile(entityCopy))
            Assertions.assertEquals("Invoice", String(entityCopy.content!!))

            // Second run: nothing left in the JCR.
            val second = migrationService.migrateFileSystemPaths()
            Assertions.assertEquals(0, second.migrated, second.toString())
            Assertions.assertEquals(setOf("1", "2"), repoService.getChildNames(DATATRANSFER_PATH))

            Assertions.assertEquals(1, repoService.deleteAllFilesBelow("$DATATRANSFER_PATH/1").size)
            Assertions.assertEquals(setOf("2"), repoService.getChildNames(DATATRANSFER_PATH))
            Assertions.assertTrue(repoService.deleteFile(copy(encrypted)))
            Assertions.assertFalse(repoService.retrieveFile(copy(encrypted)))
        } finally {
            repoService.fileStore!!.internalClearForJunitTestCases()
            repoService.shutdown()
        }
    }

    private fun store(
        repoService: RepoService,
        parentNodePath: String,
        fileName: String,
        content: ByteArray,
        password: String? = null,
    ): FileObject {
        repoService.ensureNode(null, "$parentNodePath/attachments")
        val file = FileObject(parentNodePath, "attachments")
        file.fileName = fileName
        file.content = content
        repoService.storeFile(file, FileSizeStandardChecker(10_000), "kai", password = password)
        return file
    }

    private fun copy(file: FileObject): FileObject {
        return FileObject(file.parentNodePath, file.relPath, fileId = file.fileId)
    }

    companion object {
        private const val DATATRANSFER_PATH = "org.projectforge.plugins.datatransfer"
    }
}
