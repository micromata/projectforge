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

package org.projectforge.rest.config

import org.projectforge.common.StringHelper
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import javax.servlet.Filter
import javax.servlet.FilterRegistration
import javax.servlet.ServletContext

object RestUtils {
    private val log: Logger = LoggerFactory.getLogger(RestUtils::class.java)

    @JvmStatic
    fun registerFilter(sc: ServletContext, name: String, filterClass: Class<out Filter?>, isMatchAfter: Boolean, vararg patterns: String?): FilterRegistration {
        val filterRegistration: FilterRegistration = sc.addFilter(name, filterClass)
        filterRegistration.addMappingForUrlPatterns(null, isMatchAfter, *patterns)
        log.info("Registering filter '" + name + "' of class '" + filterClass.name + "' for urls: " + StringHelper.listToString(", ", *patterns))
        return filterRegistration
    }
}
