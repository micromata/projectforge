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

package org.projectforge.plugins.cveradar

import org.projectforge.business.user.UserRightAccessCheck
import org.projectforge.business.user.UserRightCategory
import org.projectforge.framework.persistence.api.UserRightService

/**
 * Read-only or read-write, given per user ([CveRadarRightId.PLUGIN_CVERADAR]): the default checks of
 * [UserRightAccessCheck] (select: read-only or read-write, everything else: read-write).
 */
class CveRadarRight : UserRightAccessCheck<Any>(
    CveRadarRightId.PLUGIN_CVERADAR,
    UserRightCategory.PLUGINS,
    *UserRightService.FALSE_READONLY_READWRITE,
)
