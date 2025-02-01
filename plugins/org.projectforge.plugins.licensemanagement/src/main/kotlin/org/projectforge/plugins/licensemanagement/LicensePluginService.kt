/////////////////////////////////////////////////////////////////////////////
//
// Project ProjectForge Community Edition
//         www.projectforge.org
//
// Copyright (C) 2001-2025 Micromata GmbH, Germany (www.micromata.com)
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

package org.projectforge.plugins.licensemanagement

import jakarta.annotation.PostConstruct
import mu.KotlinLogging
import org.projectforge.menu.MenuConfiguration
import org.projectforge.menu.MenuVisibility
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Service

private val log = KotlinLogging.logger {}

@Service
class LicensePluginService {
    @Autowired
    private lateinit var menuConfiguration: MenuConfiguration

    private var menuVisibility: MenuVisibility? = null

    @PostConstruct
    fun postConstruct() {
        instance = this
        menuVisibility = menuConfiguration.getMenuVisibility(LicenseManagementPlugin.ID)
        if (menuVisibility == null) {
            log.error { "Development error: No menu visibility found for plugin ${LicenseManagementPlugin.ID}" }
        }
    }

    fun hasAccess(): Boolean {
        return menuVisibility?.isVisible() == true
    }


    companion object {
        @JvmStatic
        lateinit var instance: LicensePluginService
            private set
    }
}
