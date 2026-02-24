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

package org.projectforge.rest.dto

import org.projectforge.business.availability.AvailabilityDO
import org.projectforge.business.availability.AvailabilityLocation
import org.projectforge.business.availability.AvailabilityStatus
import org.projectforge.framework.i18n.translate
import java.time.LocalDate

class Availability(
    var employee: Employee? = null,
    var startDate: LocalDate? = null,
    var endDate: LocalDate? = null,
    var type: String? = null,
    var status: AvailabilityStatus? = null,
    var statusAsString: String? = null,
    var location: AvailabilityLocation? = null,
    var locationAsString: String? = null,
    var description: String? = null,
) : BaseDTO<AvailabilityDO>() {

    override fun copyFrom(src: AvailabilityDO) {
        super.copyFrom(src)
        status?.let { statusAsString = translate(it.i18nKey) }
        location?.let { locationAsString = translate(it.i18nKey) }
    }

    override fun copyTo(dest: AvailabilityDO) {
        super.copyTo(dest)
    }
}
