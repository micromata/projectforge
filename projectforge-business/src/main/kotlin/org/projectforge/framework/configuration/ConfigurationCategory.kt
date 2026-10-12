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
 * The category of a configuration parameter (see [ConfigurationParam.getCategory]), by which the configuration
 * list groups and sorts the parameters. It is for display only; who sees and changes a parameter is given by
 * [ConfigurationEditors].
 */
enum class ConfigurationCategory {
    /** Technical settings of the installation: mail addresses, plugins, message of the day, calendar domain. */
    SYSTEM,

    /** Passwords and login. */
    SECURITY,

    /** Organization, time zone, date formats and phone prefix. */
    REGIONAL,

    /** Accounting, costs, contribution margin, forecast and customer groups. */
    FINANCE,

    /** Time sheets, including the booking of vacation as time sheets. */
    TIMESHEET,

    /** Human resources and vacation. */
    HR;

    val i18nKey: String
        get() = "administration.configuration.categories.${name.lowercase()}"
}
