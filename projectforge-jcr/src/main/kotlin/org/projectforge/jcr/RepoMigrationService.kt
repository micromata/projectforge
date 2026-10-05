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
 * Migrates the files of the JCR to the new file store. Id, metadata and the (maybe encrypted) payload are taken 1:1,
 * size and checksum are verified. Files already migrated are skipped, so the migration may be restarted at any time.
 * - Files of the paths registered for the file system storage (e. g. DataTransfer, see
 *   [RepoService.registerFileSystemPath]) are moved: they're deleted in the JCR after the verification. The disk
 *   space of the JCR is freed after the clean-up (compaction) of the JCR at the end of the run.
 * - All other files (entity files) are only migrated, if all files are stored by the file store
 *   (`projectforge.files.store=db`, see [RepoService.allFilesInFileStore]). They're copied: the JCR keeps them, so
 *   it's possible to switch back to `projectforge.files.store=jcr`.
 */
@Service
open class RepoMigrationService {
    @Autowired
    internal lateinit var repoService: RepoService

    class Result(
        /**
         * True, if all files were migrated (projectforge.files.store=db), false, if only the file system paths.
         */
        val allFiles: Boolean,
    ) {
        /**
         * Number of files found in the JCR.
         */
        var total = 0
            internal set

        /**
         * Number of moved files (file system paths, deleted in the JCR).
         */
        var moved = 0
            internal set

        /**
         * Number of copied files (entity files, kept in the JCR).
         */
        var copied = 0
            internal set
        var migratedSize = 0L
            internal set

        /**
         * Number of files already migrated by a former run.
         */
        var skipped = 0
            internal set
        var durationMillis = 0L
            internal set
        val errors = mutableListOf<String>()

        val ok: Boolean
            get() = errors.isEmpty()

        override fun toString(): String {
            return "files=$total, moved=$moved, copied=$copied (${FormatterUtils.formatBytes(migratedSize)}), " +
                    "already migrated=$skipped, errors=${errors.size}"
        }

        /**
         * The report of the migration (written by the migration job into the ProjectForge home directory).
         */
        fun asText(): String {
            val sb = StringBuilder()
            sb.appendLine("Migration of the files from the JCR to the file store")
            sb.appendLine("$RESULT_PREFIX${if (ok) RESULT_OK else "ERRORS"}")
            sb.appendLine("$ALL_FILES_PREFIX$allFiles")
            sb.appendLine("Files in the JCR: $total")
            sb.appendLine("Moved (DataTransfer, deleted in the JCR): $moved")
            sb.appendLine("Copied (kept in the JCR): $copied")
            sb.appendLine("Size of migrated files: ${FormatterUtils.formatBytes(migratedSize)}")
            sb.appendLine("Already migrated before: $skipped")
            sb.appendLine("Duration: ${durationMillis / 1000} s")
            sb.appendLine()
            sb.appendLine("Errors (${errors.size}):")
            errors.forEach { sb.appendLine("  $it") }
            return sb.toString()
        }

        internal fun error(msg: String) {
            log.error { msg }
            errors.add(msg)
        }

        companion object {
            const val RESULT_PREFIX = "Result: "
            const val RESULT_OK = "OK"
            const val ALL_FILES_PREFIX = "All files: "
        }
    }

    private class JcrFile(val fileNodePath: String, val parentNodePath: String, val relPath: String, val fileObject: FileObject)

    /**
     * Migrates the files of the file system paths (DataTransfer) and, if all files are stored by the file store
     * (`projectforge.files.store=db`), all other files too.
     * @param progress Called with (number of processed files, total number of files).
     */
    open fun migrate(progress: ((processed: Int, total: Int) -> Unit)? = null): Result {
        val started = System.currentTimeMillis()
        val fileStore = repoService.fileStore ?: throw IllegalStateException("File store not available (no data source).")
        val result = Result(repoService.allFilesInFileStore)
        val files = if (result.allFiles) {
            collectFiles(null)
        } else {
            repoService.registeredFileSystemPaths.flatMap { collectFiles(it) }
        }
        result.total = files.size
        log.info { "Migrating ${files.size} files from the JCR to the file store (all files=${result.allFiles})..." }
        progress?.invoke(0, files.size)
        files.forEachIndexed { index, file ->
            try {
                migrate(fileStore, file, result)
            } catch (ex: Exception) {
                log.error(ex) { "Error while migrating file '${file.fileNodePath}': ${ex.message}" }
                result.errors.add("Error while migrating file '${file.fileNodePath}': ${ex::class.java.simpleName}: ${ex.message}")
            }
            progress?.invoke(index + 1, files.size)
        }
        if (result.moved > 0) {
            repoService.cleanup() // Frees the disk space of the deleted files.
        }
        result.durationMillis = System.currentTimeMillis() - started
        log.info { "Migration of files from the JCR to the file store finished: $result" }
        return result
    }

    /**
     * @param path The path to walk, or null for the whole repository.
     */
    private fun collectFiles(path: String?): List<JcrFile> {
        val mainNodePath = "/${repoService.mainNodeName}"
        val absPath = if (path == null) mainNodePath else repoService.getAbsolutePath(path)
        val list = mutableListOf<JcrFile>()
        val exists = repoService.runInSession { session -> session.nodeExists(absPath) }
        if (!exists) {
            log.info { "No files in the JCR under '$absPath'." }
            return list
        }
        val walker = object : RepoTreeWalker(repoService, absPath) {
            override fun visitFile(fileNode: Node, fileObject: FileObject) {
                // Path: /ProjectForge/<parentNodePath>/<relPath>/__FILES/<fileId>
                val location = fileNode.parent.parent.path.removePrefix("$mainNodePath/")
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
        // Entity files are kept in the JCR (for switching back), DataTransfer files are moved:
        val move = fileStore.isFileSystemPath(OakStorage.getAbsolutePath(file.parentNodePath, file.relPath))
        val jcrChecksum = file.fileObject.checksum
        val importResult = repoService.runInSession { session ->
            val fileNode = session.getNode(file.fileNodePath)
            repoService.getFileInputStream(fileNode, fileObject, suppressLogInfo = true, useEncryptedFile = true)
                ?.let { istream -> fileStore.importFile(fileObject, istream) }
        }
        val migrated = if (importResult == null) {
            // Already migrated before (the deletion in the JCR may have failed) or no content in the JCR:
            fileStore.getFileInfo(file.parentNodePath, file.relPath, fileId = fileObject.fileId)
                ?: run {
                    result.error("No content found in the JCR for file '${file.fileNodePath}'. File is left in the JCR.")
                    return
                }
        } else {
            null
        }
        val checksum = importResult?.checksum ?: migrated?.checksum
        val size = importResult?.size ?: migrated?.size
        val jcrSize = file.fileObject.size
        val checksumDiffers = !jcrChecksum.isNullOrBlank() && jcrChecksum.startsWith("SHA256:") && jcrChecksum != checksum
        val sizeDiffers = jcrSize != null && size != null && jcrSize != size
        if (checksumDiffers || sizeDiffers) {
            if (importResult != null) {
                fileStore.deleteFile(fileObject) // Keep the file in the JCR.
            }
            result.error(
                "Migrated file '${file.fileNodePath}' (checksum=$checksum, size=$size) differs from the JCR " +
                        "(checksum=$jcrChecksum, size=$jcrSize). File is left in the JCR."
            )
            return
        }
        if (importResult == null) {
            ++result.skipped
        } else {
            if (move) ++result.moved else ++result.copied
            result.migratedSize += importResult.size
            log.info { "File ${if (move) "moved" else "copied"} from JCR to file store: $fileObject" }
        }
        if (move) {
            repoService.jcrDeleteFile(FileObject(file.parentNodePath, file.relPath, fileObject.fileId))
        }
    }
}
