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

import org.projectforge.framework.i18n.translate
import org.projectforge.framework.i18n.translateMsg
import org.projectforge.framework.jobs.AbstractJob
import org.projectforge.framework.persistence.user.entities.PFUserDO
import org.projectforge.framework.utils.NumberFormatter

/**
 * Matches the components below a node against all vulnerabilities again, started by hand from its findings (MAT-01
 * otherwise runs after every import and every change of the inventory). The matching tells no progress: the bar runs
 * without counters, the result follows as [progressDetails]. The caller checks the access to the node (see
 * [CvrInventoryService.startRematch]).
 */
internal class CvrRematchJob(
    private val componentIds: Collection<Long>,
    private val matchingService: CvrMatchingService,
    queue: String,
    title: String,
) : AbstractJob(
    title,
    area = AREA,
    // Clicked twice: the second run follows the first instead of matching the same components alongside.
    queueName = "$AREA-$queue",
    queueStrategy = QueueStrategy.PER_QUEUE,
    timeoutSeconds = 10 * 60,
) {
    @Volatile
    private var result: CvrMatchingService.Result? = null

    override val progressDetails: String
        get() = result?.let {
            translateMsg(
                "plugins.cveradar.rematch.result",
                NumberFormatter.format(it.created), NumberFormatter.format(it.updated), NumberFormatter.format(it.removed),
            )
        } ?: translate("plugins.cveradar.rematch.running")

    override suspend fun run() {
        result = matchingService.rematchComponents(componentIds)
    }

    /** Only the person who started it sees it. */
    override fun writeAccess(user: PFUserDO?) = user?.id != null && user.id == ownerId

    companion object {
        const val AREA = "CveRadarRematch"
    }
}
