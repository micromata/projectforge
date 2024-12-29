/////////////////////////////////////////////////////////////////////////////
//
// Project ProjectForge Community Edition
//         www.projectforge.org
//
// Copyright (C) 2001-2024 Micromata GmbH, Germany (www.micromata.com)
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

package org.projectforge.rest.core

import org.projectforge.business.admin.SystemStatisticsData
import org.projectforge.business.admin.SystemsStatisticsBuilderInterface
import org.projectforge.common.extensions.formatForUser

class SseEmitterStatisticsBuilder : SystemsStatisticsBuilderInterface {
    override fun addStatisticsEntries(stats: SystemStatisticsData) {
        val coroutineTracker = SseEmitterTool.coroutineTracker
        val sb = StringBuilder()
        sb.append("active=")
            .append(coroutineTracker.actives.formatForUser())
            .append(", created=")
            .append(coroutineTracker.created.formatForUser())
            .append(", completed=")
            .append(coroutineTracker.completed.formatForUser())
        if (coroutineTracker.errorMessage != null) {
            sb.append(", error=[")
                .append(coroutineTracker.errorMessage)
                .append("]")
        }
        stats.add("SseEmitterCoroutines", "development", "'SseEmitterCoroutines", sb.toString())
    }
}
