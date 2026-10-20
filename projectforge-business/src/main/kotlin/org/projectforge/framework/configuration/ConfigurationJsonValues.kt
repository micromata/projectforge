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

import java.util.concurrent.ConcurrentHashMap

/**
 * The values of the configuration parameters of type [ConfigurationType.JSON] the holders of these settings parse
 * (e.g. [org.projectforge.framework.calendar.HolidaysConfig]). Test cases without Spring context (and without data
 * base) set their values here, otherwise the parameters' default values apply there.
 */
object ConfigurationJsonValues {
    private val testValues = ConcurrentHashMap<ConfigurationParam, String>()

    /** The JSON value of the given parameter. */
    @JvmStatic
    fun get(param: ConfigurationParam): String? {
        testValues[param]?.let { return it }
        if (!Configuration.instanceInitialized) {
            return param.defaultStringValue
        }
        return Configuration.instance.getStringValue(param)
    }

    /** For test cases only: overrides the value of the given parameter (null: removes the override). */
    @JvmStatic
    fun internalSetForTest(param: ConfigurationParam, json: String?) {
        if (json == null) {
            testValues.remove(param)
        } else {
            testValues[param] = json
        }
    }
}
