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

package org.projectforge.setup

import org.projectforge.framework.time.TimeNotation
import java.io.File
import java.util.*

class SetupData(
        var applicationHomeDir: File? = null,
        //var domain: String? = "http://localhost:8080",
        var serverPort: Int = 8080,
        var startServer: Boolean = true,
        var developmentMode: Boolean = false,
        var defaultLocale: String = "en",
        var currencySymbol: String = "€",
        var defaultTimeNotation: TimeNotation = TimeNotation.H24,
        var defaultFirstDayOfWeek: Int = Calendar.MONDAY,
        var useEmbeddedDatabase: Boolean = true,
        var jdbcSettings: JdbcSettings? = null) {

    class JdbcSettings(var jdbcUrl: String? = null,
                       var user: String? = null,
                       var password: String? = null,
                       var driverClass: String? = null)
}

