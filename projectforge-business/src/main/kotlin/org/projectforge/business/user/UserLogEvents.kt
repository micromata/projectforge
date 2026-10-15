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

package org.projectforge.business.user

import org.projectforge.common.logging.LogCategory
import org.projectforge.common.logging.LogEvent
import org.projectforge.common.logging.LogNotify

object UserLogEvents {
    @JvmField
    val PREF_NOT_DESERIALIZABLE = LogEvent(
        code = "user.pref.notDeserializable",
        category = LogCategory.DATA,
        notify = LogNotify.DIGEST_IF_NEW,
        explanation = "A stored user preference (filter, favorites, settings) can't be read: it was written by an " +
                "incompatible ProjectForge version or is empty. The user starts with the defaults for it.",
        action = "Usually nothing: the entry is overwritten on the next save. If it recurs, check the entry named in " +
                "the message in t_user_pref (empty value_string?) and delete it.",
    )
}
