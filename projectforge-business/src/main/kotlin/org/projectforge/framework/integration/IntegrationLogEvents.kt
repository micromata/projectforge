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

package org.projectforge.framework.integration

import org.projectforge.common.logging.LogCategory
import org.projectforge.common.logging.LogEvent
import org.projectforge.common.logging.LogNotify

object IntegrationLogEvents {
    /**
     * Only counted: the cause is logged (and classified) where it happens, and the support error digest lists the
     * failed runs in its own section (`SyncProblemTracker`).
     */
    @JvmField
    val SYNC_RUN_FAILED = LogEvent(
        code = "integration.sync.runFailed",
        category = LogCategory.EXTERNAL,
        notify = LogNotify.NONE,
        explanation = "The summary of a sync run that was aborted or had errors (the sync type is in the message).",
        action = "See the log messages logged before by the sync for the cause.",
    )
}
