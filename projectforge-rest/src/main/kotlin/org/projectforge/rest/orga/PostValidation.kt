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


package org.projectforge.rest.orga

import org.projectforge.framework.i18n.translate
import org.projectforge.framework.time.PFDay
import org.projectforge.ui.ValidationError
import java.time.LocalDate

/**
 * The check both mail books ([PosteingangEntityRest], [PostausgangEntityRest]) share: an entry is booked when the
 * mail came in or went out, so its date can't lie in the future.
 */
internal fun validatePostDate(validationErrors: MutableList<ValidationError>, datum: LocalDate?) {
    val date = PFDay.fromOrNull(datum)
    if (date != null && PFDay.now().isBefore(date)) {
        validationErrors.add(ValidationError(translate("error.dateInFuture"), fieldId = "datum"))
    }
}
