/////////////////////////////////////////////////////////////////////////////
//
// Project ProjectForge Community Edition
//         www.projectforge.org
//
// Copyright (C) 2001-2020 Micromata GmbH, Germany (www.micromata.com)
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

package org.projectforge.business.teamcal

import org.projectforge.common.i18n.I18nEnum

/**
 * @author Kai Reinhard (k.reinhard@micromata.de)
 */
enum class CalendarAccessStatus(val key: String) : I18nEnum {
    OWNER("owner"), FULL_ACCESS("fullAccess"), READONLY_ACCESS("readonlyAccess"), MINIMAL_ACCESS("minimalAccess"), ADMIN_ACCESS("adminAccess");

    /**
     * @return The full i18n key including the i18n prefix "fibu.auftrag.status.".
     */
    override val i18nKey: String
        get() = "plugins.teamcal.$key"
}
