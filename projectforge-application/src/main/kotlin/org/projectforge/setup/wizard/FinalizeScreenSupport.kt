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

package org.projectforge.setup.wizard

import org.projectforge.framework.time.TimeNotation
import org.projectforge.framework.utils.LabelValueBean
import org.projectforge.framework.utils.NumberHelper
import org.projectforge.setup.SetupData
import org.projectforge.start.ProjectForgeApplication
import org.projectforge.start.ProjectForgeHomeFinder
import java.io.File


object FinalizeScreenSupport {
    fun saveValues(setupData: SetupData,
                   //domain: String,
                   portText: String,
                   currencySymbol: String,
                   defaultLocaleSelectedIndex: Int,
                   defaultFirstDayOfWeekSelectedIndex: Int,
                   defaultTimeNotationSelectedIndex: Int,
                   startServer: Boolean,
                   developmentMode: Boolean
    ) {
        var port = NumberHelper.parseInteger(portText)
        //setupData.domain = domain
        setupData.serverPort = if (port in 1..65535) port else 8080
        setupData.currencySymbol = currencySymbol
        setupData.defaultLocale = listOfLocales.get(defaultLocaleSelectedIndex).value
        setupData.defaultFirstDayOfWeek = listOfWeekdays.get(defaultFirstDayOfWeekSelectedIndex).value
        setupData.defaultTimeNotation = listOfTimeNotations.get(defaultTimeNotationSelectedIndex).value
        setupData.startServer = startServer
        setupData.developmentMode = developmentMode
    }

    fun getDirText(dir: File?): String {
        return if (dir == null)
            "Please specify directory."
        else if (!dir.exists())
            "Will be created and configured."
        else if (ProjectForgeHomeFinder.isProjectForgeSourceCodeRepository(dir))
            "This seems to be the source code repository. It's recommended to select another."
        else if (configFileAlreadyExists(dir))
            "Exists, existing config files will not be overwritten."
        else
            "Exists and will be checked for configuration."
    }

    fun getInfoText(portText: String, dir: File): String {
        val sb = StringBuilder()
        sb.append("Please open your favorite browser after startup: http://localhost:${portText} and enjoy it!\n\n")
        if (ProjectForgeHomeFinder.isStandardProjectForgeUserDir(dir)) {
            sb.append("You chose the standard directory of ProjectForge, that will be found by ProjectForge automatically (OK).\n\n")
        } else {
            sb.append("You chose a directory different to ${File(System.getProperty("user.home"), "ProjectForge")}. That's OK.\n")
            sb.append("To be sure, that this directory is found by the ProjectForge server, please refer log files or home page.\n\n")
        }
        sb.append("Press 'Finish' for starting the intialization and for starting-up the server.")
        return sb.toString()
    }

    fun configFileAlreadyExists(baseDir: File?): Boolean {
        return baseDir != null && File(baseDir, ProjectForgeApplication.PROPERTIES_FILENAME).exists()
    }

    val listOfLocales = listOf(
            LabelValueBean("en - English", "en"),
            LabelValueBean("de - Deutsch", "de")
    )

    val listOfTimeNotations = listOf(
            LabelValueBean("H24", TimeNotation.H24),
            LabelValueBean("H12", TimeNotation.H12)
    )

    val listOfWeekdays = listOf(
            LabelValueBean("Sunday", 1),
            LabelValueBean("Monday", 2),
            LabelValueBean("Tuesday", 3),
            LabelValueBean("Wednesday", 4),
            LabelValueBean("Thursday", 5),
            LabelValueBean("Friday", 6),
            LabelValueBean("Saturday", 7)
    )

    val listOfDatabases = listOf(
            LabelValueBean("Embedded", "HSQL"),
            LabelValueBean("PostgreSQL", "POSTGRES")
    )
}
