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

import io.github.oshai.kotlinlogging.KotlinLogging
import org.projectforge.common.FormatterUtils
import org.projectforge.jcr.store.FileStore
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Service
import javax.jcr.Node

private val log = KotlinLogging.logger {}

/**
 * Moves the files of the paths registered for the file system storage (e. g. DataTransfer, see
 * [RepoService.registerFileSystemPath]) from the JCR to the new file store. Id, metadata and the (maybe encrypted)
 * payload are taken 1:1, the checksum is verified before the file is deleted in the JCR. Files already migrated are
 * skipped, so the migration may be restarted at any time.
 *
 * The disk space of the JCR is freed after the next clean-up (compaction) of the JCR.
 */
@Service
open class RepoMigrationService {
    @Autowired
    internal lateinit var repoService: RepoService

    class Result {
        var migrated = 0
            internal set
        var migratedSize = 0L
            internal set
        var skipped = 0
            internal set
        val errors = mutableListOf<String>()

        override fun toString(): String {
            return "migrated=$migrated (${FormatterUtils.formatBytes(migratedSize)}), already migrated=$skipped, errors=${errors.size}"
        }
    }

    private class JcrFile(val fileNodePath: String, val parentNodePath: String, val relPath: String, val fileObject: FileObject)

    /**
     * @param progress Called with (number of processed files, total number of files).
     */
    open fun migrateFileSystemPaths(progress: ((processed: Int, total: Int) -> Unit)? = null): Result {
        val fileStore = repoService.fileStore ?: throw IllegalStateException("File store not available (no data source).")
        val result = Result()
        val files = repoService.registeredFileSystemPaths.flatMap { collectFiles(it) }
        log.info { "Migrating ${files.size} files from the JCR to the file store..." }
        progress?.invoke(0, files.size)
        files.forEachIndexed { index, file ->
            try {
                migrate(fileStore, file, result)
            } catch (ex: Exception) {
                val msg = "Error while migrating file '${file.fileNodePath}': ${ex::class.java.simpleName}: ${ex.message}"
                log.error(ex) { msg }
                result.errors.add(msg)
            }
            progress?.invoke(index + 1, files.size)
        }
        if (result.migrated > 0) {
            repoService.cleanup() // Frees the disk space of the deleted files.
        }
        log.info { "Migration of files from the JCR to the file store finished: $result" }
        return result
    }

    private fun collectFiles(path: String): List<JcrFile> {
        val absPath = repoService.getAbsolutePath(path)
        val list = mutableListOf<JcrFile>()
        val exists = repoService.runInSession { session -> session.nodeExists(absPath) }
        if (!exists) {
            log.info { "No files in the JCR under '$absPath'." }
            return list
        }
        val mainNodePrefix = "/${repoService.mainNodeName}/"
        val walker = object : RepoTreeWalker(repoService, absPath) {
            override fun visitFile(fileNode: Node, fileObject: FileObject) {
                // Path: /ProjectForge/<parentNodePath>/<relPath>/__FILES/<fileId>
                val location = fileNode.parent.parent.path.removePrefix(mainNodePrefix)
                list.add(
                    JcrFile(
                        fileNode.path,
                        location.substringBeforeLast('/', ""),
                        location.substringAfterLast('/'),
                        fileObject,
                    )
                )
            }
        }
        walker.walk()
        return list
    }

    private fun migrate(fileStore: FileStore, file: JcrFile, result: Result) {
        val fileObject = FileObject(file.parentNodePath, file.relPath, file.fileObject.fileId, file.fileObject)
        val jcrChecksum = file.fileObject.checksum
        val importResult = repoService.runInSession { session ->
            val fileNode = session.getNode(file.fileNodePath)
            repoService.getFileInputStream(fileNode, fileObject, suppressLogInfo = true, useEncryptedFile = true)
                ?.let { istream -> fileStore.importFile(fileObject, istream) }
        }
        val checksum = if (importResult == null) {
            // Already migrated before (the deletion in the JCR may have failed) or no content in the JCR:
            val migrated = fileStore.getFileInfo(file.parentNodePath, file.relPath, fileId = fileObject.fileId)
            if (migrated == null) {
                val msg = "No content found in the JCR for file '${file.fileNodePath}'. File is left in the JCR."
                log.error { msg }
                result.errors.add(msg)
                return
            }
            migrated.checksum
        } else {
            importResult.checksum
        }
        if (!jcrChecksum.isNullOrBlank() && jcrChecksum.startsWith("SHA256:") && jcrChecksum != checksum) {
            if (importResult != null) {
                fileStore.deleteFile(fileObject) // Keep the file in the JCR.
            }
            val msg = "Checksum of migrated file '${file.fileNodePath}' ($checksum) differs from the JCR ($jcrChecksum). File is left in the JCR."
            log.error { msg }
            result.errors.add(msg)
            return
        }
        if (importResult == null) {
            ++result.skipped
        } else {
            ++result.migrated
            result.migratedSize += importResult.size
        }
        log.info { "File migrated from JCR to file store: $fileObject" }
        repoService.jcrDeleteFile(FileObject(file.parentNodePath, file.relPath, fileObject.fileId))
    }
}
