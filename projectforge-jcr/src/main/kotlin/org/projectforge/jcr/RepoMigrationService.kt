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
import org.projectforge.jcr.store.StorageType
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.context.event.ContextClosedEvent
import org.springframework.context.event.EventListener
import org.springframework.stereotype.Service
import javax.jcr.Node

private val log = KotlinLogging.logger {}

/**
 * Migrates all files of the JCR to the new file store (only if `projectforge.files.store=db`, see
 * [RepoService.allFilesInFileStore]). Id, metadata and the (maybe encrypted) payload are taken 1:1, size and checksum
 * are verified. Files already migrated are skipped, so the migration may be restarted at any time.
 *
 * The files are copied, the JCR keeps them: switching back to `projectforge.files.store=jcr` or rolling back to a
 * release without the new store is possible. Files deleted later are deleted in both stores.
 */
@Service
open class RepoMigrationService {
    @Autowired
    internal lateinit var repoService: RepoService

    /**
     * Set on shutdown: a running migration stops after the current file and is continued on the next start.
     */
    @Volatile
    private var stopRequested = false

    class Result {
        /**
         * Number of files found in the JCR.
         */
        var total = 0
            internal set

        /**
         * Number of copied files (kept in the JCR).
         */
        var copied = 0
            internal set
        var migratedSize = 0L
            internal set

        /**
         * Number of copied files stored in the data base.
         */
        var copiedToDb = 0
            internal set

        /**
         * Number of copied files stored in the file system (e. g. DataTransfer).
         */
        var copiedToFs = 0
            internal set

        /**
         * Number of files already migrated by a former run.
         */
        var skipped = 0
            internal set
        var durationMillis = 0L
            internal set

        /**
         * True, if the migration was stopped by a shutdown before all files were processed.
         */
        var aborted = false
            internal set
        val errors = mutableListOf<String>()

        val ok: Boolean
            get() = errors.isEmpty() && !aborted

        override fun toString(): String {
            return "files=$total, copied=$copied (${FormatterUtils.formatBytes(migratedSize)}; data base=$copiedToDb, " +
                    "file system=$copiedToFs), " +
                    "already migrated=$skipped, errors=${errors.size}${if (aborted) ", aborted by shutdown" else ""}"
        }

        /**
         * The report of the migration (written by the migration job into the ProjectForge home directory).
         */
        fun asText(): String {
            val sb = StringBuilder()
            sb.appendLine("Migration of the files from the JCR to the file store")
            sb.appendLine("$RESULT_PREFIX${if (ok) RESULT_OK else if (aborted) "ABORTED" else "ERRORS"}")
            if (aborted) {
                sb.appendLine("Stopped by a shutdown, the migration is continued on the next start.")
            }
            sb.appendLine("Files in the JCR: $total")
            sb.appendLine("Copied (kept in the JCR): $copied")
            sb.appendLine("  into the data base: $copiedToDb")
            sb.appendLine("  into the file system: $copiedToFs")
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
        }
    }

    private class JcrFile(val fileNodePath: String, val parentNodePath: String, val relPath: String, val fileObject: FileObject)

    /**
     * Migrates all files of the JCR (`projectforge.files.store=db` only, otherwise an error is returned).
     * @param progress Called with (number of processed files, total number of files).
     */
    open fun migrate(progress: ((processed: Int, total: Int) -> Unit)? = null): Result {
        val started = System.currentTimeMillis()
        val result = Result()
        val fileStore = repoService.fileStore
        if (fileStore == null || !repoService.allFilesInFileStore) {
            result.error("Files aren't migrated: projectforge.files.store=db required.")
            return result
        }
        val files = collectFiles()
        result.total = files.size
        log.info { "Migrating ${files.size} files from the JCR to the file store..." }
        log.info { "Target of the migration: data base (schema pf_files), except the file system paths below." }
        fileStore.fileSystemPathDirs.forEach { (path, dir) ->
            log.info { "Target of the migration: files below '$path' are stored in the file system: ${dir.absolutePath}" }
        }
        progress?.invoke(0, files.size)
        for ((index, file) in files.withIndex()) {
            if (stopRequested) {
                result.aborted = true
                log.warn { "Migration stopped by shutdown after $index of ${files.size} files, it's continued on the next start." }
                break
            }
            try {
                migrate(fileStore, file, result)
            } catch (ex: Exception) {
                log.error(ex) { "Error while migrating file '${file.fileNodePath}': ${ex.message}" }
                result.errors.add("Error while migrating file '${file.fileNodePath}': ${ex::class.java.simpleName}: ${ex.message}")
            }
            progress?.invoke(index + 1, files.size)
        }
        result.durationMillis = System.currentTimeMillis() - started
        log.info { "Migration of files from the JCR to the file store finished: $result" }
        return result
    }

    @EventListener(ContextClosedEvent::class)
    open fun onShutdown() {
        stopRequested = true
    }

    private fun collectFiles(): List<JcrFile> {
        val mainNodePath = "/${repoService.mainNodeName}"
        val list = mutableListOf<JcrFile>()
        val exists = repoService.runInSession { session -> session.nodeExists(mainNodePath) }
        if (!exists) {
            log.info { "No files in the JCR under '$mainNodePath'." }
            return list
        }
        val walker = object : RepoTreeWalker(repoService, mainNodePath) {
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
        val jcrChecksum = file.fileObject.checksum
        val jcrSize = file.fileObject.size
        if (jcrSize != null && jcrSize >= LARGE_FILE_SIZE) {
            val target = if (fileStore.isFileSystemPath(file.parentNodePath)) "file system" else "data base"
            log.info { "Copying large file (${FormatterUtils.formatBytes(jcrSize)}) from JCR to the $target: $fileObject" }
        }
        // Already migrated before (checked first, so the JCR content isn't read on every start):
        val existing = fileStore.getFileInfo(file.parentNodePath, file.relPath, fileId = fileObject.fileId)
        val importResult = if (existing != null) {
            null
        } else {
            repoService.runInSession { session ->
                val fileNode = session.getNode(file.fileNodePath)
                repoService.getFileInputStream(fileNode, fileObject, suppressLogInfo = true, useEncryptedFile = true)
                    ?.let { istream -> fileStore.importFile(fileObject, istream) }
            }
        }
        val migrated = if (importResult == null) {
            // Already migrated before or no content in the JCR:
            existing ?: fileStore.getFileInfo(file.parentNodePath, file.relPath, fileId = fileObject.fileId)
                ?: run {
                    result.error("No content found in the JCR for file '${file.fileNodePath}'. File is left in the JCR.")
                    return
                }
        } else {
            null
        }
        val checksum = importResult?.checksum ?: migrated?.checksum
        val size = importResult?.size ?: migrated?.size
        val checksumDiffers = !jcrChecksum.isNullOrBlank() && jcrChecksum.startsWith("SHA256:") && jcrChecksum != checksum
        val sizeDiffers = jcrSize != null && size != null && jcrSize != size
        if (checksumDiffers || sizeDiffers) {
            if (importResult != null) {
                fileStore.deleteFile(fileObject) // The file is still found in the JCR.
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
            ++result.copied
            result.migratedSize += importResult.size
            if (importResult.storage == StorageType.FS) ++result.copiedToFs else ++result.copiedToDb
            log.info { "File copied from JCR to file store (${importResult.storage}): $fileObject" }
        }
    }

    companion object {
        /**
         * Files of this size or larger are logged before copying (copying takes a while).
         */
        private const val LARGE_FILE_SIZE = 100L * 1024 * 1024
    }
}
