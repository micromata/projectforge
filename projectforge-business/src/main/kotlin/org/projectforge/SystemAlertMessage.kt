/////////////////////////////////////////////////////////////////////////////
//
// Project ProjectForge Community Edition
//         www.projectforge.org
//
// Copyright (C) 2001-2019 Micromata GmbH, Germany (www.micromata.com)
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

package org.projectforge

import org.projectforge.business.user.filter.UserFilter

/**
 * For displaying an alert message, such as: "Attention: ProjectForge will be under maintenance between 10:00 and 10:30 am!."
 * This message will be displayed for all logged-in users on all pages and may be set and cleared on the admin page.
 * Any alert message will be lost after restart.
 */
object SystemAlertMessage {
    var alertMessage: String? = null
        /**
         * Returns the alert message, if exists. The alert message will be displayed on every screen (red on top) and is
         * edit-able via Administration -> System.
         */
        get() =
            if (UserFilter.isUpdateRequiredFirst()) {
                "Maintenance mode: Please restart ProjectForge after finishing." + if (field != null) " $field" else ""
            } else {
                field
            }
}
