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
import org.projectforge.jcr.store.FileStore
import org.projectforge.jcr.store.FileStoreTestDataSource
import java.io.File

class RepoMigrationServiceTest {
    @TempDir
    lateinit var tempDir: File

    @Test
    fun `files are copied out of the JCR in db mode only, the JCR keeps them`() {
        val repoService = RepoService()
        repoService.repoConfig = RepoConfig.createForTests()
        repoService.dataSource = FileStoreTestDataSource.dataSource
        repoService.filesStore = RepoService.FILES_STORE_JCR
        val fsDir = File(tempDir, "datatransfer")
        repoService.registerFileSystemPath(DATATRANSFER_PATH, fsDir)
        repoService.init(File(tempDir, "repo"))
        val fileStore = repoService.fileStore!!
        fileStore.internalClearForJunitTestCases()
        try {
            // jcr mode: all files are stored in the JCR (as before), DataTransfer too.
            val plain = store(repoService, "$DATATRANSFER_PATH/1", "plain.txt", "Plain content".toByteArray())
            val encrypted = store(repoService, "$DATATRANSFER_PATH/2", "secret.txt", "Secret".toByteArray(), "pwd")
            val entityFile = store(repoService, "org.projectforge.fibu.RechnungDO/42", "invoice.txt", "Invoice".toByteArray())
            Assertions.assertFalse(fileStore.exists(plain))
            Assertions.assertFalse(fsDir.exists())
            Assertions.assertEquals(setOf("1", "2"), repoService.getChildNames(DATATRANSFER_PATH))

            val migrationService = RepoMigrationService()
            migrationService.repoService = repoService
            val refused = migrationService.migrate()
            Assertions.assertFalse(refused.ok, "Migration only in db mode.")
            Assertions.assertEquals(0, refused.copied)
            Assertions.assertFalse(fileStore.exists(plain))

            repoService.filesStore = RepoService.FILES_STORE_DB
            val progress = mutableListOf<Pair<Int, Int>>()
            val result = migrationService.migrate { processed, total -> progress.add(processed to total) }
            Assertions.assertEquals(3, result.copied, result.toString())
            Assertions.assertTrue(result.ok, result.errors.joinToString())
            Assertions.assertEquals(3 to 3, progress.last())
            Assertions.assertTrue(fileStore.exists(plain))
            Assertions.assertTrue(fileStore.exists(encrypted))
            Assertions.assertTrue(fileStore.exists(entityFile))
            Assertions.assertTrue(File(fsDir, "1/attachments/${plain.fileId}").isFile)
            Assertions.assertEquals(3, jcrFileCount(repoService), "The JCR keeps the migrated files.")

            val plainCopy = copy(plain)
            Assertions.assertTrue(repoService.retrieveFile(plainCopy))
            Assertions.assertEquals("Plain content", String(plainCopy.content!!))
            Assertions.assertEquals("plain.txt", plainCopy.fileName)
            Assertions.assertEquals(plain.created, plainCopy.created)
            val encryptedCopy = copy(encrypted)
            Assertions.assertTrue(repoService.retrieveFile(encryptedCopy, "pwd"), "Encrypted payload is copied 1:1.")
            Assertions.assertEquals("Secret", String(encryptedCopy.content!!))

            // Second run: all files are skipped.
            val second = migrationService.migrate()
            Assertions.assertEquals(0, second.copied, second.toString())
            Assertions.assertEquals(3, second.skipped, second.toString())
            Assertions.assertEquals(setOf("1", "2"), repoService.getChildNames(DATATRANSFER_PATH))

            // Deletion in both stores (each file is reported once):
            Assertions.assertEquals(1, repoService.deleteAllFilesBelow("$DATATRANSFER_PATH/1").size)
            Assertions.assertEquals(setOf("2"), repoService.getChildNames(DATATRANSFER_PATH))
            Assertions.assertTrue(repoService.deleteFile(copy(encrypted)))
            Assertions.assertFalse(repoService.retrieveFile(copy(encrypted)))
            Assertions.assertEquals(1, jcrFileCount(repoService))
            repoService.filesStore = RepoService.FILES_STORE_JCR
            Assertions.assertFalse(repoService.retrieveFile(copy(encrypted)))
            Assertions.assertTrue(repoService.retrieveFile(copy(entityFile)))
        } finally {
            fileStore.internalClearForJunitTestCases()
            repoService.shutdown()
        }
    }

    /**
     * Number of files in the JCR (without the new store).
     */
    private fun jcrFileCount(repoService: RepoService): Int {
        var count = 0
        fun walk(nodeInfo: NodeInfo) {
            nodeInfo.children?.forEach { child ->
                if (nodeInfo.name == OakStorage.NODENAME_FILES) ++count else walk(child)
            }
        }
        repoService.getNodeInfoOrNull(repoService.getAbsolutePath(null), true)?.let { walk(it) }
        return count
    }

    @Test
    fun `with files store db all files are migrated, entity files are kept in the JCR`() {
        val repoService = RepoService()
        repoService.repoConfig = RepoConfig.createForTests()
        repoService.dataSource = FileStoreTestDataSource.dataSource
        repoService.filesStore = RepoService.FILES_STORE_JCR
        repoService.init(File(tempDir, "repo"))
        repoService.registerFileSystemPath(DATATRANSFER_PATH, File(tempDir, "datatransfer"))
        val fileStore = repoService.fileStore!!
        fileStore.internalClearForJunitTestCases()
        try {
            // Stored in the JCR (files store jcr):
            val invoice = store(repoService, "org.projectforge.fibu.RechnungDO/42", "invoice.txt", "Invoice".toByteArray())
            val contract = store(repoService, "org.projectforge.fibu.ContractDO/7", "contract.txt", "Contract".toByteArray())
            Assertions.assertFalse(repoService.allFilesInFileStore)
            Assertions.assertFalse(fileStore.exists(invoice))

            repoService.filesStore = RepoService.FILES_STORE_DB
            Assertions.assertTrue(repoService.allFilesInFileStore)
            // Not yet migrated files are still found in the JCR:
            Assertions.assertEquals("Invoice", String(retrieve(repoService, invoice)!!))
            // New files are stored in the data base:
            val order = store(repoService, "org.projectforge.fibu.AuftragDO/3", "order.txt", "Order".toByteArray())
            Assertions.assertTrue(fileStore.exists(order))

            val migrationService = RepoMigrationService()
            migrationService.repoService = repoService
            val result = migrationService.migrate()
            Assertions.assertTrue(result.ok, result.asText())
            Assertions.assertEquals(2, result.copied, result.toString())
            Assertions.assertTrue(fileStore.exists(invoice))
            Assertions.assertTrue(fileStore.exists(contract))
            Assertions.assertTrue(result.asText().contains("Result: OK"))

            // Second run: copies are skipped.
            val second = migrationService.migrate()
            Assertions.assertEquals(0, second.copied, second.toString())
            Assertions.assertEquals(2, second.skipped, second.toString())

            // Rename and delete in the file store; the deletion is done in the JCR too:
            repoService.changeFileInfo(copy(invoice), "kai", newFileName = "renamed.txt")
            Assertions.assertEquals(1, repoService.getFileInfos("org.projectforge.fibu.RechnungDO/42", "attachments")!!.size)
            Assertions.assertEquals("renamed.txt", repoService.getFileInfo("org.projectforge.fibu.RechnungDO/42", "attachments", fileId = invoice.fileId)!!.fileName)
            Assertions.assertTrue(repoService.deleteFile(copy(contract)))
            Assertions.assertNull(repoService.getFileInfos("org.projectforge.fibu.ContractDO/7", "attachments"))

            // Switching back: the JCR serves the (unchanged) copy of the invoice, the contract is deleted.
            repoService.filesStore = RepoService.FILES_STORE_JCR
            Assertions.assertEquals("invoice.txt", repoService.getFileInfo("org.projectforge.fibu.RechnungDO/42", "attachments", fileId = invoice.fileId)!!.fileName)
            Assertions.assertEquals("Invoice", String(retrieve(repoService, invoice)!!))
            Assertions.assertNull(repoService.getFileInfo("org.projectforge.fibu.ContractDO/7", "attachments", fileId = contract.fileId))
            // Files stored in db mode are still found in the new store:
            Assertions.assertEquals("Order", String(retrieve(repoService, order)!!))
        } finally {
            fileStore.internalClearForJunitTestCases()
            repoService.shutdown()
        }
    }

    @Test
    fun `files store is detected, if not configured`() {
        FileStore(FileStoreTestDataSource.dataSource, "ProjectForge").internalClearForJunitTestCases()
        try {
            // New installation: db.
            withRepoService(File(tempDir, "new")) { repoService ->
                Assertions.assertTrue(repoService.allFilesInFileStore)
            }
            val repoDir = File(tempDir, "existing")
            withRepoService(repoDir, RepoService.FILES_STORE_JCR) { repoService ->
                store(repoService, "org.projectforge.fibu.RechnungDO/42", "invoice.txt", "Invoice".toByteArray())
            }
            // Existing JCR: jcr.
            withRepoService(repoDir) { repoService ->
                Assertions.assertFalse(repoService.allFilesInFileStore)
            }
            withRepoService(repoDir, RepoService.FILES_STORE_DB) { repoService ->
                store(repoService, "org.projectforge.fibu.RechnungDO/43", "invoice.txt", "Invoice".toByteArray())
            }
            // Files store db was used before: db.
            withRepoService(repoDir) { repoService ->
                Assertions.assertTrue(repoService.allFilesInFileStore)
            }
        } finally {
            FileStore(FileStoreTestDataSource.dataSource, "ProjectForge").internalClearForJunitTestCases()
        }
    }

    private fun withRepoService(repoDir: File, filesStore: String = "", block: (RepoService) -> Unit) {
        val repoService = RepoService()
        repoService.repoConfig = RepoConfig.createForTests()
        repoService.dataSource = FileStoreTestDataSource.dataSource
        repoService.filesStore = filesStore
        repoService.init(repoDir)
        try {
            block(repoService)
        } finally {
            repoService.shutdown()
        }
    }

    private fun retrieve(repoService: RepoService, file: FileObject): ByteArray? {
        val copy = copy(file)
        return if (repoService.retrieveFile(copy)) copy.content else null
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
