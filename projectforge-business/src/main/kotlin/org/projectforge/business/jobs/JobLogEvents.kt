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

package org.projectforge.business.jobs

import org.projectforge.common.logging.LogCategory
import org.projectforge.common.logging.LogEvent

object JobLogEvents {
    @JvmField
    val NIGHTLY_TOKEN_PURGE_FAILED = LogEvent(
        code = "jobs.nightly.tokenPurgeFailed",
        category = LogCategory.BUG,
        explanation = "The nightly job couldn't delete the expired stay-logged-in tokens. Harmless for security " +
                "(expired tokens are never accepted), the table only grows.",
        action = "Analyze the stack trace; the next night retries it.",
    )

    @JvmField
    val NIGHTLY_REINDEX_FAILED = LogEvent(
        code = "jobs.nightly.reindexFailed",
        category = LogCategory.DATA,
        explanation = "The nightly re-index of the full text search failed: search results may be incomplete or outdated.",
        action = "Analyze the stack trace and start the re-index manually in the administration area.",
    )
}
