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


package org.projectforge.plugins.cveradar

import io.github.oshai.kotlinlogging.KotlinLogging
import jakarta.annotation.PostConstruct
import org.projectforge.common.i18n.UserException
import org.projectforge.framework.i18n.translate
import org.projectforge.framework.jobs.JobHandler
import org.projectforge.framework.persistence.jpa.PfPersistenceService
import org.projectforge.framework.persistence.user.entities.PFUserDO
import org.projectforge.plugins.cveradar.rest.JobStarted
import org.projectforge.plugins.cveradar.rest.SbomImportApplyRequest
import org.projectforge.plugins.cveradar.rest.SbomImportDiff
import org.projectforge.plugins.cveradar.rest.SbomImportKind
import org.projectforge.plugins.cveradar.rest.SbomImportPlanView
import org.projectforge.plugins.cveradar.rest.SbomImportSkipped
import org.projectforge.plugins.cveradar.rest.SbomImportTargetView
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Service
import java.io.InputStream
import java.nio.file.Files
import java.nio.file.Path
import java.time.Duration
import java.time.Instant
import java.util.concurrent.ConcurrentHashMap

private val log = KotlinLogging.logger {}

/**
 * The bulk import of the SBOMs of many repositories and container images into an area (INV-35), in three steps:
 * 1. [startAnalysis]: the zip is stored as a temporary file and read by a job ([CvrSbomImportAnalyzeJob]); the
 *    targets are proposed ([SbomImportPlanner]) and compared with the inventory. The file is deleted afterwards, the
 *    plan stays with the job (an hour, as every terminated job).
 * 2. [plan]: the plan for the preview; the user chooses the targets and may change their codes and names.
 * 3. [startApply]: a job ([CvrSbomImportApplyJob]) creates the missing systems and components and replaces the
 *    libraries of every chosen target, each in a transaction of its own, and matches once at the end.
 * Who may write the area may import into it; it is checked at every step (the jobs run on), the plan is the starter's
 * only. The libraries always come from the stored plan, never from the request.
 */
@Service
class CvrSbomImportService {
    @Autowired
    private lateinit var inventoryService: CvrInventoryService

    @Autowired
    private lateinit var jobHandler: JobHandler

    @Autowired
    private lateinit var libraryService: CvrLibraryService

    @Autowired
    private lateinit var persistenceService: PfPersistenceService

    /**
     * The users uploading a zip right now: one upload and one analysis per user at a time, as each holds a big file
     * (and a plan, in the end). A lock, not state of a request.
     */
    private val uploading: MutableSet<Long> = ConcurrentHashMap.newKeySet()

    /**
     * Stores the zip of [input] (refused above the limit of the area) and starts reading it. Refused while the user
     * uploads another one or one of theirs is read; the plans of their earlier analyses are dropped.
     */
    fun startAnalysis(user: PFUserDO, areaId: Long, contentLength: Long, input: InputStream): JobStarted {
        val snapshot = inventoryService.importSnapshot(user, areaId)
        val limit = snapshot.limitBytes
        if (contentLength > limit) throw tooLarge(limit)
        val userId = user.id!!
        if (!uploading.add(userId)) throw UserException(ERROR_BUSY)
        try {
            val earlier = analyses(user)
            if (earlier.any { !it.terminated }) throw UserException(ERROR_BUSY)
            earlier.forEach { it.discard() }
            return store(user, areaId, snapshot.areaCode, limit, input)
        } finally {
            uploading.remove(userId)
        }
    }

    private fun store(user: PFUserDO, areaId: Long, areaCode: String, limit: Long, input: InputStream): JobStarted {
        val file = Files.createTempFile(TEMP_PREFIX, TEMP_SUFFIX)
        try {
            Files.newOutputStream(file).use { out ->
                val buffer = ByteArray(64 * 1024)
                var total = 0L
                while (true) {
                    val n = input.read(buffer)
                    if (n < 0) break
                    total += n
                    if (total > limit) throw tooLarge(limit)
                    out.write(buffer, 0, n)
                }
            }
            val job = CvrSbomImportAnalyzeJob(file, user, areaId, this, "CVE-Radar: SBOM import into area $areaCode")
            jobHandler.addJob(job)
            return JobStarted(job.id)
        } catch (ex: Exception) {
            Files.deleteIfExists(file)
            throw ex
        }
    }

    /** The analyses of the user the job handler still keeps. */
    private fun analyses(user: PFUserDO): List<CvrSbomImportAnalyzeJob> =
        jobHandler.getJobsOfUser(null).filterIsInstance<CvrSbomImportAnalyzeJob>().filter { it.ownerId == user.id }

    /** The plan of an analysis of the user, for the preview. */
    fun plan(user: PFUserDO, importId: Int): SbomImportPlanView =
        (analysis(user, importId).plan ?: throw UserException(ERROR_EXPIRED)).view

    /** Imports the chosen targets of an analysis, as a job. */
    fun startApply(user: PFUserDO, importId: Int, request: SbomImportApplyRequest): JobStarted {
        val analysis = analysis(user, importId)
        val plan = analysis.plan ?: throw UserException(ERROR_EXPIRED)
        // Refused without write access to the area (any more).
        val snapshot = inventoryService.importSnapshot(user, plan.areaId)
        val byKey = plan.targets.associateBy { it.key }
        val items = request.targets.map { selection ->
            val target = byKey[selection.key]?.takeIf { it.selectable } ?: throw UserException(ERROR_TARGET)
            val code = selection.code?.trim()?.uppercase()?.takeIf { it.isNotEmpty() }
            val name = selection.name?.trim()?.takeIf { it.isNotEmpty() }
            if (target.kind == SbomImportKind.CONTAINER) {
                CvrSbomImportApplyJob.Item(target, target.systemCode, target.systemName, code ?: target.componentCode, name ?: target.componentName)
            } else {
                CvrSbomImportApplyJob.Item(target, code ?: target.systemCode, name ?: target.systemName, target.componentCode, target.componentName)
            }
        }
        if (items.isEmpty()) throw UserException(ERROR_TARGET)
        // Two targets in one component would replace each other's libraries.
        items.groupBy { it.systemCode to it.componentCode }.values.firstOrNull { it.size > 1 }?.let { twice ->
            throw UserException(ERROR_TWICE, "${twice.first().systemCode}-${twice.first().componentCode}")
        }
        val job = CvrSbomImportApplyJob(user, plan.areaId, items, this, "CVE-Radar: SBOM import into area ${snapshot.areaCode}")
        jobHandler.addJob(job)
        // Imported once; the libraries go on with the job only.
        analysis.discard()
        return JobStarted(job.id)
    }

    /** An analysis job of the user; a job of others is not found. */
    private fun analysis(user: PFUserDO, importId: Int): CvrSbomImportAnalyzeJob {
        val job = jobHandler.getJobById(importId) as? CvrSbomImportAnalyzeJob
        if (job == null || !job.writeAccess(user)) throw UserException(ERROR_EXPIRED)
        return job
    }

    /** The plan of what was read: the targets proposed and compared with the inventory (step 1, in the job). */
    internal fun plan(user: PFUserDO, areaId: Long, importId: Int, archive: SbomImportArchive.Result): SbomImportPlan {
        val snapshot = inventoryService.importSnapshot(user, areaId)
        val inventory = SbomImportInventory(snapshot.areaCode, snapshot.systems.mapValues { it.value.keys }, snapshot.takenCodes)
        val targets = SbomImportPlanner.plan(archive.documents, inventory, translate("plugins.cveradar.sbomImport.imagesSystem"))
        val catalog = libraryService.catalog()
        val views = targets.map { target ->
            val stored = snapshot.systems[target.systemCode]?.get(target.componentCode)
            val diff = if (stored != null && target.selectable) {
                libraryService.preview(stored, target.files, replace = true, catalog).let {
                    SbomImportDiff(it.addedCount, it.removedCount, it.changedCount, it.unchangedCount)
                }
            } else null
            SbomImportTargetView(
                key = target.key,
                kind = target.kind,
                systemCode = target.systemCode,
                systemName = target.systemName,
                componentCode = target.componentCode,
                componentName = target.componentName,
                sources = target.files.take(MAX_SOURCES).map { it.name },
                sourceCount = target.files.size,
                libraryCount = target.libraryCount,
                withoutPurlCount = target.withoutPurl,
                status = target.status,
                selected = target.proposed,
                selectable = target.selectable,
                aggregateOf = target.aggregateOf,
                diff = diff,
            )
        }
        val imported = targets.map { it.systemCode }.toSet()
        val view = SbomImportPlanView(
            importId = importId,
            areaId = areaId,
            areaCode = snapshot.areaCode,
            targets = views,
            documentCount = archive.documents.size,
            skipped = SbomImportSkipped(archive.otherFiles, archive.notSbom, archive.invalid),
            notInImport = snapshot.systems.keys.filter { it !in imported }.sorted(),
        )
        return SbomImportPlan(areaId, targets, view)
    }

    /**
     * One target of the import (step 3, in the job): its component, created if missing, with the libraries replaced; in
     * one transaction, so a target failing leaves no empty system behind.
     */
    internal fun applyTarget(
        user: PFUserDO,
        areaId: Long,
        item: CvrSbomImportApplyJob.Item,
        progress: (CvrLibraryService.Progress) -> Unit,
    ): Long = persistenceService.runInTransaction {
        val component = inventoryService.importTarget(
            user, areaId, item.systemCode, item.systemName, item.componentCode, item.componentName,
        )
        libraryService.apply(component, item.target.files, replace = true, progress)
        component.id!!
    }

    internal fun rematch(componentIds: List<Long>) = inventoryService.rematchImported(componentIds)

    /** The zips left by a restart during an upload or analysis (the job deletes them otherwise). */
    @PostConstruct
    private fun deleteLeftFiles() {
        deleteLeftFiles(Path.of(System.getProperty("java.io.tmpdir")), Instant.now().minus(LEFT_AFTER))
    }

    /**
     * The zips of imports in [dir] older than [before]; younger ones may belong to another instance sharing the
     * directory (no upload and analysis takes that long).
     */
    internal fun deleteLeftFiles(dir: Path, before: Instant) {
        try {
            Files.newDirectoryStream(dir, "$TEMP_PREFIX*$TEMP_SUFFIX").use { files ->
                files.filter { Files.getLastModifiedTime(it).toInstant().isBefore(before) }.forEach { file ->
                    log.info { "Temporary file of an SBOM import left by a restart deleted: $file" }
                    deleteQuietly(file)
                }
            }
        } catch (ex: Exception) {
            log.warn(ex) { "Temporary files of SBOM imports not cleaned up in $dir" }
        }
    }

    internal fun deleteQuietly(file: Path) {
        try {
            Files.deleteIfExists(file)
        } catch (ex: Exception) {
            log.warn(ex) { "Temporary file of an SBOM import not deleted: $file" }
        }
    }

    private fun tooLarge(limit: Long) = UserException(ERROR_TOO_LARGE, (limit / 1024 / 1024).toString())

    companion object {
        private const val MAX_SOURCES = 20
        private const val TEMP_PREFIX = "cveradar-sbom-import-"
        private const val TEMP_SUFFIX = ".zip"
        private val LEFT_AFTER = Duration.ofHours(3)

        const val ERROR_TOO_LARGE = "plugins.cveradar.sbomImport.error.tooLarge"
        const val ERROR_EXPIRED = "plugins.cveradar.sbomImport.error.expired"
        const val ERROR_TARGET = "plugins.cveradar.sbomImport.error.target"
        const val ERROR_TWICE = "plugins.cveradar.sbomImport.error.twice"
        const val ERROR_BUSY = "plugins.cveradar.sbomImport.error.busy"
    }
}

/** What an analysis found: the targets with their libraries, and the preview of them. */
internal class SbomImportPlan(val areaId: Long, val targets: List<SbomImportTarget>, val view: SbomImportPlanView)
