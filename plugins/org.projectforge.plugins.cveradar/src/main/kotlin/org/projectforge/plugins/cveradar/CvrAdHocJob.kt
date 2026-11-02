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

import org.projectforge.framework.jobs.AbstractJob
import org.projectforge.framework.persistence.user.entities.PFUserDO
import org.projectforge.plugins.cveradar.model.CvrFindingCertainty
import java.util.concurrent.CopyOnWriteArrayList

/**
 * A library of an ad-hoc check that matched a vulnerability: a stored one ([vulnerabilityId]) or OSV advisories not
 * linked to one yet ([vulnerabilityKey], see [CvrMatchingService.QueryHit]).
 */
internal class AdHocHit(
    val entry: LibraryEntry,
    val vulnerabilityId: Long?,
    val vulnerabilityKey: String?,
    val certainty: CvrFindingCertainty,
    val matched: String,
) {
    /** The vulnerability, whether stored or not. */
    val key: Any get() = vulnerabilityId ?: vulnerabilityKey!!
}

/** A file of an ad-hoc check, read by [CvrAdHocCheckService]; the job counts [checked] and collects the [hits]. */
internal class AdHocFile(val name: String, val format: LibraryFormat?, val entries: List<LibraryEntry>) {
    @Volatile
    var checked = 0

    @Volatile
    var done = false

    val hits = CopyOnWriteArrayList<AdHocHit>()
}

/**
 * Checks the libraries of the files against all vulnerabilities of the index, file by file and in blocks, so the client
 * can show the progress per file; nothing is stored. The result stays with the job (the framework drops it an hour
 * after the end) and is turned into a report by [CvrAdHocCheckService].
 */
internal class CvrAdHocJob(
    val files: List<AdHocFile>,
    private val matching: CvrMatchingService,
    private val catalog: CvrLibraryService.Catalog,
    title: String,
) : AbstractJob(
    title,
    area = AREA,
    queueName = AREA,
    // The service refuses a second check of the same person before; this is the guard if two start at once.
    queueStrategy = QueueStrategy.REFUSE_PER_QUEUE_AND_USER,
    timeoutSeconds = 10 * 60,
) {
    override suspend fun run() {
        totalNumber = files.sumOf { it.entries.size }
        processedNumber = 0
        for (file in files) {
            for (block in file.entries.chunked(BLOCK)) {
                if (!isActive) return
                val queries = block.map { entry ->
                    val node = catalog.nodeOf(entry)
                    CvrMatchingService.Query(null, node?.cpeVendor, entry.product, entry.version, node, entry.purl)
                }
                matching.match(queries).forEachIndexed { index, hits ->
                    hits.forEach { file.hits.add(AdHocHit(block[index], it.vulnerabilityId, it.vulnerabilityKey, it.certainty, it.matched)) }
                }
                file.checked += block.size
                processedNumber += block.size
            }
            file.done = true
        }
    }

    /** Only the person who started it sees it. */
    override fun writeAccess(user: PFUserDO?) = user?.id != null && user.id == ownerId

    companion object {
        const val AREA = "CveRadarAdHoc"

        /** The libraries checked in one go: one query for their products, then the progress moves. */
        const val BLOCK = 100
    }
}
