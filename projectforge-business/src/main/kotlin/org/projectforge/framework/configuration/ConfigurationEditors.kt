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

package org.projectforge.framework.configuration

/**
 * Who maintains a configuration parameter (see [ConfigurationParam.getEditors]). The parameters are split
 * between the system administration and the finance department, and neither side may change the other's:
 * an admin sees the finance parameters read-only, a finance user sees only the finance parameters.
 */
enum class ConfigurationEditors {
    /** Members of PF_Admin. */
    ADMIN,

    /** Members of PF_Finance or PF_Controlling (not admins). */
    FINANCE,
}
