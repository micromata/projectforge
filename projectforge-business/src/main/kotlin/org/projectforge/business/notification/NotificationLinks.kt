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


package org.projectforge.business.notification

import org.projectforge.Constants
import org.projectforge.business.configuration.DomainService
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Service

/** The links of notifications: paths of the next app (e.g. `/timesheet`), absolute urls for mails. */
@Service
class NotificationLinks {
    @Autowired
    private lateinit var domainService: DomainService

    /** The absolute url of the given path of the next app, an absolute url as it is. */
    fun absolute(path: String): String {
        if (path.startsWith("http://") || path.startsWith("https://")) {
            return path
        }
        return domainService.getDomain("${Constants.NEXT_APP_PATH}${path.removePrefix("/")}")
    }
}
