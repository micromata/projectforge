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

import org.projectforge.common.i18n.UserException
import org.projectforge.framework.i18n.translate
import org.projectforge.framework.i18n.translateMsg
import org.projectforge.framework.jobs.AbstractJob
import org.projectforge.framework.persistence.user.entities.PFUserDO
import org.projectforge.framework.utils.NumberFormatter
import java.nio.file.Path

/**
 * Reads the zip of a bulk import of SBOMs ([SbomImportArchive]) and plans the import ([CvrSbomImportService.plan]).
 * The temporary file is deleted when the job is over, whatever the outcome; the [plan] stays with the job, which the
 * framework keeps for an hour. The progress counts the packed MB read. The caller checks the access to the area.
 */
internal class CvrSbomImportAnalyzeJob(
    private val file: Path,
    private val user: PFUserDO,
    private val areaId: Long,
    private val service: CvrSbomImportService,
    title: String,
) : AbstractJob(
    title,
    area = AREA,
    ownerId = user.id,
    // One analysis of a user after the other: each holds a big file.
    queueName = "$AREA-analysis",
    queueStrategy = QueueStrategy.PER_QUEUE_AND_USER,
    timeoutSeconds = 30 * 60,
) {
    @Volatile
    private var planning = false

    /** The plan, once read. */
    @Volatile
    var plan: SbomImportPlan? = null
        private set

    /** What was read, once planned; stays when the plan is dropped. */
    @Volatile
    private var summary: String? = null

    /** Drops the plan (imported, or replaced by a newer analysis of the user): it holds all libraries read. */
    fun discard() {
        plan = null
    }

    override val progressDetails: String
        get() {
            summary?.let { return it }
            if (planning) return translate("plugins.cveradar.sbomImport.progress.planning")
            return translateMsg(
                "plugins.cveradar.sbomImport.progress.reading",
                NumberFormatter.format(processedNumber.coerceAtLeast(0)),
                NumberFormatter.format(totalNumber.coerceAtLeast(0)),
            )
        }

    override suspend fun run() {
        try {
            val archive = SbomImportArchive.read(file, progress = { read, total ->
                totalNumber = (total / MB).toInt().coerceAtLeast(1)
                processedNumber = (read / MB).toInt()
            }, isActive = { isActive })
            if (!isActive) return
            planning = true
            val planned = service.plan(user, areaId, id, archive)
            summary = translateMsg(
                "plugins.cveradar.sbomImport.progress.analyzed",
                NumberFormatter.format(planned.view.documentCount),
                NumberFormatter.format(planned.view.targets.size),
            )
            plan = planned
        } catch (ex: UserException) {
            // The job keeps the message of the exception for the client, which is the key of a UserException.
            throw IllegalStateException(translateMsg(ex), ex)
        } finally {
            service.deleteQuietly(file)
        }
    }

    /** Also if it never ran (cancelled while waiting). */
    override fun onAfterTermination() {
        service.deleteQuietly(file)
    }

    /** Only the person who started it sees it. */
    override fun writeAccess(user: PFUserDO?) = user?.id != null && user.id == ownerId

    companion object {
        const val AREA = "CveRadarSbomImport"
        private const val MB = 1024L * 1024
    }
}
