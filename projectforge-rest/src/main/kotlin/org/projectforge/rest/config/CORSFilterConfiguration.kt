/////////////////////////////////////////////////////////////////////////////
//
// Project ProjectForge Community Edition
//         www.projectforge.org
//
// Copyright (C) 2001-2024 Micromata GmbH, Germany (www.micromata.com)
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

import jakarta.annotation.PostConstruct
import org.springframework.beans.factory.annotation.Value
import org.springframework.context.annotation.Configuration

@Configuration
open class CORSFilterConfiguration {
    @Value("\${projectforge.web.development.enableCORSFilter}")
    var enabled: Boolean = false
        private set

    @Value("\${projectforge.web.development.enableCORSFilter.defaultDomain}")
    var defaultOrigin: String? = "http://localhost:3000"

    @PostConstruct
    private fun postConstruct() {
        instance = this
    }

    companion object {
        val defaultOrigin: String
            get() = instance.defaultOrigin!!
        val enabled: Boolean
            get() = instance.enabled
        private lateinit var instance: CORSFilterConfiguration
    }
}
