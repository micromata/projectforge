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

package org.projectforge.caldav.model

import io.milton.annotations.ModifiedDate
import io.milton.annotations.Name
import io.milton.annotations.UniqueId
import org.slf4j.LoggerFactory
import java.util.*

class Meeting(var calendar: Calendar) {
    var uniqueId: String? = null
        @UniqueId get() = field
        set

    // filename for the meeting. Must be unique within the user
    @get:Name
    var name: String? = null
    var createDate: Date? = null
    @get:ModifiedDate
    var modifiedDate: Date? = null
    var icalData: ByteArray? = null

    companion object {
        private val log = LoggerFactory.getLogger(Meeting::class.java)
    }

}
