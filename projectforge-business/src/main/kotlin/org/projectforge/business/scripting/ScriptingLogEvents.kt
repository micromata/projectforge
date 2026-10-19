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

package org.projectforge.business.scripting

import org.projectforge.common.logging.LogCategory
import org.projectforge.common.logging.LogEvent

object ScriptingLogEvents {
    /**
     * Errors of a script are shown to the user in the script log; on the server they are only counted.
     */
    @JvmField
    val SCRIPT_ERROR = LogEvent(
        code = "scripting.script.error",
        category = LogCategory.CLIENT,
        explanation = "A script of a user failed (compile error, e.g. an unknown property, or an exception while " +
                "running) or logged an error itself. The user sees the message in the script log.",
        action = "Nothing to do on the server; the author of the script has to fix it.",
    )
}
