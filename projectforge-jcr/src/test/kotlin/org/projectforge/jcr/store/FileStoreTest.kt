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

package org.projectforge.jcr.store

import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Assertions
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.io.TempDir
import org.projectforge.common.MaxFileSizeExceeded
import org.projectforge.jcr.FileInfo
import org.projectforge.jcr.FileObject
import org.projectforge.jcr.FileSizeChecker
import org.projectforge.jcr.FileSizeStandardChecker
import java.io.File
import java.security.MessageDigest
import java.util.*
import javax.sql.DataSource
import kotlin.random.Random

class FileStoreTest {
    @TempDir
    lateinit var tempDir: File

    private lateinit var fileStore: FileStore

    private lateinit var fsDir: File

    @BeforeEach
    fun setUp() {
        fileStore = FileStore(dataSource)
        fsDir = File(tempDir, "datatransfer")
        fileStore.registerFileSystemPath(DATATRANSFER_PATH, fsDir)
    }

    @AfterEach
    fun tearDown() {
        // While the temp dir of the test still exists:
        fileStore.internalClearForJunitTestCases()
        Assertions.assertTrue(fileStore.getStatistics().all { it.count == 0L }, "All files should be deleted.")
    }

    @Test
    fun `store, retrieve, change and delete a compressed file in the data base`() {
        val content = "Hello world! ".repeat(10_000).toByteArray()
        val file = store("/ProjectForge/org.projectforge.fibu.RechnungDO/42", "attachments", "invoice.txt", content)
        Assertions.assertEquals(20, file.fileId!!.length)
        Assertions.assertTrue(file.fileId!!.all { it in 'a'..'z' })
        Assertions.assertEquals(sha256(content), file.checksum)
        Assertions.assertEquals(content.size.toLong(), file.size)

        val dbStats = fileStore.getStatistics().first { it.storage == StorageType.DB }
        Assertions.assertEquals(1, dbStats.count)
        Assertions.assertEquals(content.size.toLong(), dbStats.size)
        Assertions.assertTrue(dbStats.storedSize < content.size / 10, "Content should be gzipped.")

        // Only the concatenation of parent node path and rel path matters (as in the JCR):
        val retrieved = FileObject("org.projectforge.fibu.RechnungDO", "42/attachments", fileId = file.fileId)
        Assertions.assertTrue(fileStore.retrieveFile(retrieved))
        Assertions.assertArrayEquals(content, retrieved.content)
        Assertions.assertEquals("invoice.txt", retrieved.fileName)
        Assertions.assertEquals("kai", retrieved.createdByUser)

        Assertions.assertNotNull(fileStore.getFileInfo(file.parentNodePath, file.relPath, fileName = "invoice.txt"))
        Assertions.assertNull(fileStore.getFileInfo(file.parentNodePath, file.relPath, fileName = "unknown.txt"))
        Assertions.assertNull(fileStore.getFileInfos(file.parentNodePath, "unknown"))
        Assertions.assertEquals(1, fileStore.getFileInfos(file.parentNodePath, file.relPath)!!.size)

        val changed = fileStore.changeFileInfo(file, "fin", newFileName = "renamed.txt", newDescription = "Description")!!
        Assertions.assertEquals("renamed.txt", changed.fileName)
        Assertions.assertEquals("Description", changed.description)
        Assertions.assertEquals("fin", changed.lastUpdateByUser)
        fileStore.getFileInfo(file.parentNodePath, file.relPath, file.fileId)!!.let { reread ->
            Assertions.assertEquals("renamed.txt", reread.fileName, "change must be committed")
            Assertions.assertEquals("Description", reread.description)
        }
        Assertions.assertEquals(sha256(content), fileStore.checksum(file))

        Assertions.assertTrue(fileStore.deleteFile(file))
        Assertions.assertFalse(fileStore.exists(file))
        Assertions.assertFalse(fileStore.deleteFile(file))
        Assertions.assertEquals(0, fileStore.getStatistics().first { it.storage == StorageType.DB }.count)
    }

    @Test
    fun `big uncompressed file is split into chunks`() {
        val content = Random(42).nextBytes(2 * DbBlobStore.CHUNK_SIZE + 1000)
        val file = store("org.projectforge.fibu.RechnungDO/42", "attachments", "scan.pdf", content)
        val stats = fileStore.getStatistics().first { it.storage == StorageType.DB }
        Assertions.assertEquals(content.size.toLong(), stats.storedSize, "PDF shouldn't be compressed.")
        fileStore.retrieveFileInputStream(file)!!.use {
            Assertions.assertArrayEquals(content, it.readBytes())
        }
    }

    @Test
    fun `encrypted file needs the password`() {
        val content = "Top secret ".repeat(1000).toByteArray()
        val file = fileObject("org.projectforge.fibu.RechnungDO/42", "attachments", "secret.txt")
        fileStore.storeFile(file, content.inputStream(), FileSizeStandardChecker(1_000_000), "kai", password = "password")
        Assertions.assertTrue(file.aesEncrypted == true)

        val withoutPassword = FileObject(file.parentNodePath, file.relPath, fileId = file.fileId)
        Assertions.assertTrue(fileStore.retrieveFile(withoutPassword))
        Assertions.assertNull(withoutPassword.content)
        val wrongPassword = FileObject(file.parentNodePath, file.relPath, fileId = file.fileId)
        Assertions.assertTrue(fileStore.retrieveFile(wrongPassword, "wrong"))
        Assertions.assertNull(wrongPassword.content)
        val withPassword = FileObject(file.parentNodePath, file.relPath, fileId = file.fileId)
        Assertions.assertTrue(fileStore.retrieveFile(withPassword, "password"))
        Assertions.assertArrayEquals(content, withPassword.content)
    }

    @Test
    fun `too big files are rejected while streaming`() {
        val file = fileObject("org.projectforge.fibu.RechnungDO/42", "attachments", "big.txt")
        Assertions.assertThrows(MaxFileSizeExceeded::class.java) {
            fileStore.storeFile(file, ByteArray(1000).inputStream(), FileSizeStandardChecker(100))
        }
        Assertions.assertNull(fileStore.getFileInfos(file.parentNodePath, file.relPath))
        val fsFile = fileObject("$DATATRANSFER_PATH/1", "attachments", "big.txt")
        Assertions.assertThrows(MaxFileSizeExceeded::class.java) {
            fileStore.storeFile(fsFile, ByteArray(1000).inputStream(), FileSizeStandardChecker(100))
        }
        Assertions.assertNull(fileStore.getFileInfos(fsFile.parentNodePath, fsFile.relPath))
        Assertions.assertTrue(fsDir.walkTopDown().none { it.isFile }, "No temp file should be left.")
    }

    @Test
    fun `checker without fixed limit checks after streaming`() {
        // As the DataTransferFileSizeChecker: the limit is the free capacity of the area given as data.
        val checker = object : FileSizeChecker {
            override fun checkSize(file: FileInfo, data: Any?, displayUserMessage: Boolean) {
                checkSize(file, data as Long, null, displayUserMessage)
            }

            override val maxFileSize: Long
                get() = throw IllegalArgumentException("No fixed limit.")
        }
        val file = fileObject("$DATATRANSFER_PATH/1", "attachments", "a.txt")
        fileStore.storeFile(file, ByteArray(1000).inputStream(), checker, data = 1000L)
        Assertions.assertTrue(fileStore.exists(file))
        val tooBig = fileObject("$DATATRANSFER_PATH/1", "attachments", "b.txt")
        Assertions.assertThrows(MaxFileSizeExceeded::class.java) {
            fileStore.storeFile(tooBig, ByteArray(1001).inputStream(), checker, data = 1000L)
        }
        Assertions.assertEquals(1, fileStore.getFileInfos(tooBig.parentNodePath, tooBig.relPath)!!.size)
    }

    @Test
    fun `files of registered paths are stored in the file system`() {
        val content = "DataTransfer".repeat(100).toByteArray()
        val file1 = store("$DATATRANSFER_PATH/1", "attachments", "a.txt", content)
        val file2 = store("$DATATRANSFER_PATH/1", "attachments", "b.txt", content)
        val file3 = store("$DATATRANSFER_PATH/2", "attachments", "c.txt", content)
        Assertions.assertTrue(fileStore.isFileSystemPath("/ProjectForge/$DATATRANSFER_PATH/1"))
        Assertions.assertFalse(fileStore.isFileSystemPath("org.projectforge.fibu.RechnungDO"))
        val diskFile = File(fsDir, "1/attachments/${file1.fileId}")
        Assertions.assertArrayEquals(content, diskFile.readBytes(), "File system content is stored uncompressed.")
        Assertions.assertEquals(setOf("1", "2"), fileStore.getChildNames(DATATRANSFER_PATH))

        val retrieved = FileObject(file2.parentNodePath, file2.relPath, fileId = file2.fileId)
        Assertions.assertTrue(fileStore.retrieveFile(retrieved))
        Assertions.assertArrayEquals(content, retrieved.content)

        val deleted = fileStore.deleteAllFilesBelow("$DATATRANSFER_PATH/1")
        Assertions.assertEquals(setOf(file1.fileId, file2.fileId), deleted.map { it.fileId }.toSet())
        Assertions.assertFalse(File(fsDir, "1").exists(), "Empty directories should be removed.")
        Assertions.assertEquals(setOf("2"), fileStore.getChildNames(DATATRANSFER_PATH))
        Assertions.assertTrue(fileStore.exists(file3))
    }

    @Test
    fun `sanity check detects modified and missing files`() {
        val content = "Sanity".repeat(100).toByteArray()
        store("org.projectforge.fibu.RechnungDO/42", "attachments", "ok.txt", content)
        val modified = store("$DATATRANSFER_PATH/1", "attachments", "modified.txt", content)
        val missing = store("$DATATRANSFER_PATH/1", "attachments", "missing.txt", content)
        File(fsDir, "1/attachments/${modified.fileId}").writeText("Modified")
        File(fsDir, "1/attachments/${missing.fileId}").delete()

        val errors = mutableMapOf<String, String>()
        var count = 0
        fileStore.checkSanity { result ->
            count++
            result.error?.let { errors[result.fileObject.fileId!!] = it }
        }
        Assertions.assertEquals(3, count)
        Assertions.assertEquals(setOf(modified.fileId, missing.fileId), errors.keys)
    }

    @Test
    fun `import preserves the metadata and skips already imported files`() {
        val content = "Imported".repeat(100).toByteArray()
        val created = Date(System.currentTimeMillis() - 100_000_000)
        val file = fileObject("org.projectforge.fibu.AuftragDO/7", "attachments", "order.txt").also {
            it.fileId = "abcdefghijklmnopqrst"
            it.created = created
            it.createdByUser = "fin"
            it.lastUpdate = created
            it.lastUpdateByUser = "kai"
            it.description = "Imported file"
        }
        val result = fileStore.importFile(file, content.inputStream())!!
        Assertions.assertEquals(content.size.toLong(), result.size)
        Assertions.assertEquals(sha256(content), result.checksum)
        Assertions.assertEquals(StorageType.DB, result.storage)
        Assertions.assertNull(fileStore.importFile(file, content.inputStream()), "Second import should be skipped.")

        val info = fileStore.getFileInfo(file.parentNodePath, file.relPath, fileId = file.fileId)!!
        Assertions.assertEquals(created, info.created)
        Assertions.assertEquals("fin", info.createdByUser)
        Assertions.assertEquals("kai", info.lastUpdateByUser)
        Assertions.assertEquals("Imported file", info.description)
    }

    private fun store(parentNodePath: String, relPath: String, fileName: String, content: ByteArray): FileObject {
        val file = fileObject(parentNodePath, relPath, fileName)
        fileStore.storeFile(file, content.inputStream(), FileSizeStandardChecker(10_000_000), "kai")
        return file
    }

    private fun fileObject(parentNodePath: String, relPath: String, fileName: String): FileObject {
        return FileObject(parentNodePath, relPath).also { it.fileName = fileName }
    }

    private fun sha256(content: ByteArray): String {
        return "SHA256: " + MessageDigest.getInstance("SHA-256").digest(content).joinToString("") { "%02x".format(it) }
    }

    companion object {
        private const val DATATRANSFER_PATH = "org.projectforge.plugins.datatransfer"

        private val dataSource: DataSource
            get() = FileStoreTestDataSource.dataSource
    }
}
