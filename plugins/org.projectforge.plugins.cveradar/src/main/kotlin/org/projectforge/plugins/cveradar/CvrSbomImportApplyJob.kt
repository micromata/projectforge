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
import org.projectforge.common.i18n.UserException
import org.projectforge.framework.i18n.translate
import org.projectforge.framework.i18n.translateMsg
import org.projectforge.framework.jobs.AbstractJob
import org.projectforge.framework.persistence.user.entities.PFUserDO
import org.projectforge.framework.utils.NumberFormatter

private val log = KotlinLogging.logger {}

/**
 * Imports the chosen targets of a bulk import of SBOMs one after the other ([CvrSbomImportService.applyTarget]): each in
 * a transaction of its own, so a failing one is reported and the others are stored. Cancelling stops after the target
 * being stored; the ones stored so far stay. The components are matched once at the end, the counters then start again
 * with the matching ([CvrMatchingService.Progress]). [progressDetails] names the target being stored, the step of the
 * matching, at the end what was stored and what failed; with a failed target the job fails with that text.
 * The caller checks the access to the area.
 */
internal class CvrSbomImportApplyJob(
    private val user: PFUserDO,
    private val areaId: Long,
    items: List<Item>,
    private val service: CvrSbomImportService,
    title: String,
) : AbstractJob(
    title,
    area = AREA,
    ownerId = user.id,
    // Two imports into the same area one after another, not interleaved.
    queueName = "$AREA-$areaId",
    queueStrategy = QueueStrategy.PER_QUEUE,
    timeoutSeconds = 2 * 3600,
) {
    /** A target with the codes and names it is stored under. */
    class Item(
        val target: SbomImportTarget,
        val systemCode: String,
        val systemName: String,
        val componentCode: String,
        val componentName: String,
    ) {
        val label: String
            get() = "$systemCode-$componentCode"
    }

    /** Emptied when the job is over: the libraries of the targets aren't needed any more (the job is kept an hour). */
    @Volatile
    private var items: List<Item> = items

    @Volatile
    private var current: Item? = null

    @Volatile
    private var libraries: CvrLibraryService.Progress? = null

    /** Set while the components are matched. */
    @Volatile
    private var matching: CvrMatchingService.Progress? = null

    @Volatile
    private var summary: String? = null

    override val progressDetails: String?
        get() {
            summary?.let { return it }
            matching?.let { return it.describe(translate("plugins.cveradar.sbomImport.progress.matching")) }
            val item = current ?: return null
            val p = libraries
            return if (p == null || p.toStore == 0) item.label
            else "${item.label}: " + translateMsg(
                "plugins.cveradar.libraries.progress.stored",
                NumberFormatter.format(p.stored),
                NumberFormatter.format(p.toStore),
            )
        }

    override suspend fun run() {
        val items = this.items
        this.items = emptyList()
        totalNumber = items.size
        processedNumber = 0
        val stored = mutableListOf<Long>()
        val failed = mutableListOf<String>()
        for (item in items) {
            if (!isActive) break
            current = item
            libraries = null
            try {
                stored += service.applyTarget(user, areaId, item) { libraries = it }
            } catch (ex: Exception) {
                log.warn(ex) { "SBOM import of ${item.label} failed: ${ex.message}" }
                val message = (ex as? UserException)?.let { translateMsg(it) } ?: ex.message ?: ex.javaClass.simpleName
                failed += "${item.label} ($message)"
            }
            processedNumber++
        }
        current = null
        service.rematch(stored) { p ->
            matching = p
            totalNumber = p.total
            processedNumber = p.done
        }
        matching = null
        summary = listOfNotNull(
            translateMsg("plugins.cveradar.sbomImport.progress.stored", NumberFormatter.format(stored.size)),
            failed.takeIf { it.isNotEmpty() }?.let {
                translateMsg(
                    "plugins.cveradar.sbomImport.progress.failed", NumberFormatter.format(it.size),
                    it.take(MAX_FAILURES_SHOWN).joinToString("; ") + if (it.size > MAX_FAILURES_SHOWN) "; …" else "",
                )
            },
        ).joinToString(" ")
        // Failed, so the client shows it as an error; the targets stored stay, the message says which.
        if (failed.isNotEmpty()) throw IllegalStateException(summary)
    }

    /** Only the person who started it sees it. */
    override fun writeAccess(user: PFUserDO?) = user?.id != null && user.id == ownerId

    companion object {
        const val AREA = "CveRadarSbomImport"
        private const val MAX_FAILURES_SHOWN = 5
    }
}
