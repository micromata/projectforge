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

package org.projectforge.rest.dto

import org.projectforge.business.orga.VisitorbookDO
import org.projectforge.business.orga.VisitorbookEntryDO
import java.time.LocalDate

class VisitorbookEntry(
    var dateOfVisit: LocalDate? = null,
    var arrived: String? = null,
    var departed: String? = null,
    var comment: String? = null,
    var visitorbookId: Long? = null,
) : BaseDTO<VisitorbookEntryDO>() {

    constructor(src: VisitorbookEntryDO?) : this() {
        id = src?.id
        dateOfVisit = src?.dateOfVisit
        arrived = src?.arrived
        departed = src?.departed
        comment = src?.comment
        visitorbookId = src?.visitorbook?.id
    }

    fun cloneAsDO(): VisitorbookEntryDO {
        val result = VisitorbookEntryDO()
        result.id = id
        result.dateOfVisit = dateOfVisit
        result.arrived = arrived
        result.departed = departed
        result.comment = comment
        visitorbookId?.let {
            result.visitorbook = VisitorbookDO().also { it.id = visitorbookId }
        }
        return result
    }

}
