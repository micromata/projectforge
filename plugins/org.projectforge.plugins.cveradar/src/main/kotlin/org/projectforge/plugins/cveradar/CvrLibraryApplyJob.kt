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

import kotlinx.coroutines.CancellationException
import org.projectforge.framework.i18n.translate
import org.projectforge.framework.i18n.translateMsg
import org.projectforge.framework.jobs.AbstractJob
import org.projectforge.framework.persistence.user.entities.PFUserDO
import org.projectforge.framework.utils.NumberFormatter
import org.projectforge.plugins.cveradar.model.CvrComponentDO
import org.projectforge.plugins.cveradar.rest.LibraryDiff
import org.projectforge.plugins.cveradar.rest.LibraryRequest

/**
 * Takes over the libraries of an upload for a component ([CvrLibraryService.apply]) and matches the component against
 * the vulnerabilities afterwards. Runs as a job, so the client shows the progress instead of waiting for a request
 * that may outlast a proxy's timeout. The counters are the libraries stored and removed together, [progressDetails]
 * names them apart (an upload with "replace" does both) and the phases before and after: reading, matching. The caller checks the access to the component (see [CvrInventoryService]).
 */
internal class CvrLibraryApplyJob(
    private val component: CvrComponentDO,
    request: LibraryRequest,
    private val libraryService: CvrLibraryService,
    private val rematch: () -> Unit,
    title: String,
) : AbstractJob(
    title,
    area = AREA,
    // Two uploads to the same component one after another, not interleaved.
    queueName = "$AREA-${component.id}",
    queueStrategy = QueueStrategy.PER_QUEUE,
    timeoutSeconds = 10 * 60,
) {
    /** Dropped once stored: the framework keeps a terminated job for an hour. */
    @Volatile
    private var request: LibraryRequest? = request

    @Volatile
    private var matching = false

    /** Null while the files are read. */
    @Volatile
    private var progress: CvrLibraryService.Progress? = null

    /** What was stored, once stored. */
    @Volatile
    var diff: LibraryDiff? = null
        private set

    override val progressDetails: String
        get() {
            if (matching) return translate("plugins.cveradar.libraries.progress.matching")
            val p = progress ?: return translate("plugins.cveradar.libraries.progress.reading")
            fun count(key: String, done: Int, of: Int) =
                translateMsg(key, NumberFormatter.format(done), NumberFormatter.format(of)).takeIf { of > 0 }
            return listOfNotNull(
                count("plugins.cveradar.libraries.progress.stored", p.stored, p.toStore),
                count("plugins.cveradar.libraries.progress.removed", p.removed, p.toRemove),
            ).joinToString(", ").ifEmpty { translate("plugins.cveradar.libraries.progress.unchanged") }
        }

    override suspend fun run() {
        val request = this.request ?: return
        diff = libraryService.apply(component, request) { p ->
            // Thrown inside the transaction, so a cancelled job leaves the component as it was.
            if (!isActive) throw CancellationException()
            progress = p
            totalNumber = p.toStore + p.toRemove
            processedNumber = p.stored + p.removed
        }
        this.request = null
        matching = true
        rematch()
    }

    /** Only the person who started it sees it. */
    override fun writeAccess(user: PFUserDO?) = user?.id != null && user.id == ownerId

    companion object {
        const val AREA = "CveRadarLibraries"
    }
}
