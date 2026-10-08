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

package org.projectforge.framework.access

import org.projectforge.common.logging.LogCategory
import org.projectforge.common.logging.LogEvent

object AccessLogEvents {
    @JvmField
    val USER_NOT_MEMBER_OF = LogEvent(
        code = "access.userNotMemberOf",
        category = LogCategory.SECURITY,
        threshold = 10,
        explanation = "A user was denied a function reserved for a group (e.g. an admin page): mostly a bookmark or " +
                "a link of a former admin. Reported only from 10 denials per digest period on.",
        action = "Check the security log for the users and the requested pages: an outdated link or somebody probing?",
    )
}
