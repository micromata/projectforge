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

package org.projectforge.rest.importer

import jakarta.servlet.http.HttpServletRequest
import mu.KotlinLogging
import org.projectforge.framework.i18n.translate
import org.projectforge.framework.utils.FileCheck
import org.projectforge.rest.core.ExpiringSessionAttributes
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.multipart.MultipartFile
import java.io.InputStream

private val log = KotlinLogging.logger {}

/**
 * The layout-free, JSON returning analog of the UILayout based [AbstractImportPageRest] /
 * [AbstractImportUploadPageRest] pair.
 *
 * Where the legacy pages answer with `UILayout` / `ResponseAction` objects a React page rehydrates, this base
 * answers with plain [ImportView] JSON that a hand built page reads directly. The legacy classes stay in
 * place — address and banking still use them; this base is only the reusable foundation for the newer,
 * hand built import pages, starting with the incoming-invoice (Kreditor) CSV import.
 *
 * The import storage lives in the user's session under a per-controller key (see [sessionAttributeName]),
 * reusing the same [ExpiringSessionAttributes] mechanism and [AbstractImportPageRest.getSessionAttributeName]
 * scheme the upload page uses, so the two never collide.
 *
 * The endpoint contract mirrors [AbstractImportPageRest]'s actions:
 *  - `POST upload` parses the file and stashes the storage, then answers the [ImportView].
 *  - `GET state` answers the current [ImportView] (an empty, `null`-storage one if nothing is in progress).
 *  - `POST reconcile` reconciles with the database, then answers the [ImportView].
 *  - `POST commit` filters the selected entries to the importable states and enqueues the import job.
 *  - `POST cancel` drops the storage from the session.
 *
 * @param O The import DTO.
 * @param S The concrete [ImportStorage] the subclass produces and consumes.
 */
abstract class AbstractImportRest<O : ImportPairEntry.Modified<O>, S : ImportStorage<O>> {

    /** Checks that the logged-in user may run this import; throws if not. */
    protected abstract fun checkRight()

    /**
     * Parses the uploaded file into a fresh [ImportStorage]. Throwing an exception (with a message meant for
     * the user) is how a parse or format error is reported; [upload] turns it into a `400` response.
     */
    protected abstract fun proceedUpload(inputStream: InputStream, filename: String): S

    /**
     * Enqueues the import of the given entries. Don't forget to fill the [ImportStorage.importResult].
     * @return Job id (null, if no job was created).
     */
    protected abstract fun import(storage: S, selectedEntries: List<ImportPairEntry<O>>): Int?

    /** The accepted file extensions, e.g. `arrayOf("csv")`. */
    protected abstract val fileExtensions: Array<String>

    /** The maximum accepted upload size in MB. */
    protected abstract val maxFileUploadSizeMB: Long

    /**
     * Subclass specific metadata to travel with every [ImportView], e.g. `isPositionBasedImport`. Defaults
     * to none.
     */
    protected open fun extraViewMeta(storage: S): Map<String, Any>? = null

    /**
     * The uploaded file is parsed, the resulting storage stashed in the session and its [ImportView]
     * answered. On an empty file, a failed check or a parse error the response is a `400` carrying the
     * message under the `error` key.
     */
    @PostMapping("upload")
    fun upload(
        request: HttpServletRequest,
        @RequestParam("file") file: MultipartFile,
    ): ResponseEntity<*> {
        checkRight()
        val filename = file.originalFilename ?: "unknown"
        log.info { "User tries to upload import file: '$filename', size=${file.size} bytes." }
        try {
            if (file.isEmpty) {
                return ResponseEntity.badRequest().body(mapOf("error" to translate("file.upload.error.empty")))
            }
            FileCheck.checkFile(filename, file.size, *fileExtensions, megaBytes = maxFileUploadSizeMB)?.let { error ->
                return ResponseEntity.badRequest().body(mapOf("error" to error))
            }
            val storage = file.inputStream.use { inputStream ->
                proceedUpload(inputStream, filename)
            }
            ExpiringSessionAttributes.setAttribute(request, sessionAttributeName, storage, TTL_MINUTES)
            log.info { "Successfully processed file: $filename" }
            return ResponseEntity.ok(buildView(storage))
        } catch (ex: Exception) {
            log.error("Error processing uploaded file: $filename", ex)
            return ResponseEntity.badRequest().body(mapOf("error" to (ex.message ?: translate("file.upload.error"))))
        }
    }

    /**
     * The current [ImportView] from the session, or an empty one (with a `null` storage) the frontend reads
     * as "no import in progress".
     */
    @GetMapping("state")
    fun state(request: HttpServletRequest): ImportView<O> {
        checkRight()
        return buildView(getStorage(request))
    }

    /**
     * Reconciles the stashed import with the database and answers the refreshed [ImportView]. The request
     * body is the (optional) display options that filter the returned entries.
     */
    @PostMapping("reconcile")
    fun reconcile(
        request: HttpServletRequest,
        @RequestBody(required = false) displayOptions: ImportStorage.DisplayOptions?,
    ): ResponseEntity<*> {
        checkRight()
        val storage = getStorage(request)
            ?: return ResponseEntity.ok(buildView(null))
        storage.reconcileImportStorage()
        return ResponseEntity.ok(buildView(storage, displayOptions))
    }

    /**
     * Filters the stashed entries to the ids the user selected — keeping only the importable states (NEW,
     * MODIFIED, DELETED), exactly as [AbstractImportPageRest]'s import action does — and enqueues the import
     * job. Answers `{ jobId }` on success, a `400` with an `error` message otherwise.
     */
    @PostMapping("commit")
    fun commit(
        request: HttpServletRequest,
        @RequestBody commitData: CommitData,
    ): ResponseEntity<*> {
        checkRight()
        val storage = getStorage(request)
            ?: return ResponseEntity.badRequest().body(mapOf("error" to translate("import.error.nothingToImport")))
        val selectedIds = commitData.selectedIds
        val selectedEntries = mutableListOf<ImportPairEntry<O>>()
        if (selectedIds != null) {
            storage.pairEntries.forEach { entry ->
                if (selectedIds.contains(entry.id)) {
                    // Only allow NEW, MODIFIED, and DELETED entries to be imported.
                    // Filter out UNKNOWN, UNKNOWN_MODIFICATION, FAULTY, and UNMODIFIED.
                    when (entry.status) {
                        ImportEntry.Status.NEW,
                        ImportEntry.Status.MODIFIED,
                        ImportEntry.Status.DELETED -> selectedEntries.add(entry)

                        else -> { /* Ignore other statuses */
                        }
                    }
                }
            }
        }
        storage.clearErrors()
        if (selectedEntries.isEmpty()) {
            val message = translate("import.error.noEntrySelected")
            storage.addError(message)
            return ResponseEntity.badRequest().body(mapOf("error" to message))
        }
        log.info { "User wants to import #${selectedEntries.size} entries..." }
        val jobId = import(storage, selectedEntries)
            ?: run {
                val message = "Internal error: can't create batch job."
                storage.addError(message)
                return ResponseEntity.internalServerError().body(mapOf("error" to message))
            }
        return ResponseEntity.ok(mapOf("jobId" to jobId))
    }

    /**
     * Drops the stashed import from the session.
     */
    @PostMapping("cancel")
    fun cancel(request: HttpServletRequest): ResponseEntity<*> {
        checkRight()
        ExpiringSessionAttributes.removeAttribute(request, sessionAttributeName)
        return ResponseEntity.ok().build<Any>()
    }

    /**
     * Builds the [ImportView] for the given storage. A `null` storage yields an empty view the frontend
     * reads as "no import in progress". The entries are built via [ImportStorage.createEntries]; a `null`
     * [displayOptions] returns all of them.
     */
    protected fun buildView(
        storage: S?,
        displayOptions: ImportStorage.DisplayOptions? = null,
    ): ImportView<O> {
        storage ?: return ImportView()
        return ImportView(
            filename = storage.filename,
            title = storage.title,
            hasBeenReconciled = storage.hasBeenReconciled,
            info = storage.info,
            entries = storage.createEntries(displayOptions),
            meta = extraViewMeta(storage),
        )
    }

    @Suppress("UNCHECKED_CAST")
    protected fun getStorage(request: HttpServletRequest): S? {
        return ExpiringSessionAttributes.getAttribute(request, sessionAttributeName) as S?
    }

    /**
     * The per-controller session key, reusing [AbstractImportPageRest]'s scheme so it never collides with a
     * legacy import page's key.
     */
    protected val sessionAttributeName: String
        get() = AbstractImportPageRest.getSessionAttributeName(this::class.java)

    /**
     * The request body of [commit]: the ids of the selected entries and the display options.
     */
    class CommitData(
        var selectedIds: List<Long>? = null,
        var displayOptions: ImportStorage.DisplayOptions? = null,
    )

    companion object {
        /** Session TTL of a stashed import, in minutes (matching the legacy upload page). */
        private const val TTL_MINUTES = 20
    }
}
