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

package org.projectforge.business.fibu

/**
 * The order notification settings of one user, stored as user preference ([OrderNotificationPrefService]).
 * A rule for the project of the order wins over a rule for its customer, which wins over the [defaultMode].
 */
class OrderNotificationPrefs(
    var defaultMode: OrderNotificationMode = OrderNotificationMode.IMMEDIATE,
    /** Project id → mode. */
    var projectModes: MutableMap<Long, OrderNotificationMode> = mutableMapOf(),
    /** Customer id ([KundeDO.nummer]) → mode. */
    var customerModes: MutableMap<Long, OrderNotificationMode> = mutableMapOf(),
) {
    /** @param customerIds The customers of the order and of its project (they may differ), the first rule found wins. */
    fun resolve(projectId: Long?, vararg customerIds: Long?): OrderNotificationMode {
        return projectId?.let { projectModes[it] }
            ?: customerIds.firstNotNullOfOrNull { id -> id?.let { customerModes[it] } }
            ?: defaultMode
    }
}
