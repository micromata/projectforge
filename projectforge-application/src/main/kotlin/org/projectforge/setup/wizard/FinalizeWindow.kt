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

import com.googlecode.lanterna.TerminalSize
import com.googlecode.lanterna.gui2.*
import org.projectforge.framework.utils.NumberHelper
import org.projectforge.start.ProjectForgeApplication
import org.projectforge.start.ProjectForgeHomeFinder
import java.io.File
import java.util.regex.Pattern

class FinalizeWindow(context: GUIContext) : AbstractWizardWindow(context, "Finishing the directory setup") {
    private val log = org.slf4j.LoggerFactory.getLogger(FinalizeWindow::class.java)

    private lateinit var dirLabel: Label
    private lateinit var portextBox: TextBox
    private lateinit var hintLabel: Label

    override fun getContentPanel(): Panel {
        dirLabel = Label("")
        val panel = Panel()
        panel.layoutManager = GridLayout(2)
        panel.addComponent(Label("Directory").setSize(TerminalSize(10, 1)))
                .addComponent(dirLabel)
        panel.addComponent(EmptySpace().setLayoutData(GridLayout.createHorizontallyFilledLayoutData(2)))
        portextBox = TextBox("8080")
                .setValidationPattern(Pattern.compile("[0-9]{1,5}?"))
                .setPreferredSize(TerminalSize(7, 1))
        panel.addComponent(Label("Port"))
                .addComponent(portextBox)
        panel.addComponent(EmptySpace().setLayoutData(GridLayout.createHorizontallyFilledLayoutData(2)))
        hintLabel = Label("")
        hintLabel.layoutData = GridLayout.createHorizontallyFilledLayoutData(2)
        panel.addComponent(hintLabel)
        return panel
    }

    override fun getButtons(): Array<Button> {
        return arrayOf(
                Button("Previous") {
                    setPort()
                    context.setupMain.previous()
                },
                Button("Finish") {
                    setPort()
                    context.setupMain.finish()
                })
    }

    private fun setPort() {
        var port = NumberHelper.parseInteger(portextBox.text)
        context.setupData.serverPort = if (port in 1..65535) port else 8080
    }

    override fun redraw() {
        val dir = context.setupData.applicationHomeDir ?: File(System.getProperty("user.home"), "ProjectForge")
        dirLabel.setPreferredSize(TerminalSize(context.terminalSize.columns - 20, 1))
        dirLabel.setText(dir.absolutePath)
        val sb = StringBuilder()
        sb.append("Final steps to be done:\n")
        var counter = 0
        if (dir.exists() == false) {
            sb.append(" ${++counter}. Creation of the directory\n")
        } else {
            sb.append(" ${++counter}. Directory does already exist (OK)\n")
        }
        if (!File(dir, ProjectForgeApplication.PROPERTIES_FILENAME).exists()) {
            sb.append(" ${++counter}. Initialization of the directory with a default configuration.\n")
        } else {
            sb.append(" ${++counter}. Directory contains already a configuration (OK)\n")
        }
        sb.append(" ${++counter}. Starting the server.\n\n")
        sb.append("Please open your favorite browser after startup: http://localhost:8080 and enjoy it!\n\n")
        if (ProjectForgeHomeFinder.isStandardProjectForgeUserDir(dir)) {
            sb.append("You chose the standard directory of ProjectForge, that will be found by ProjectForge automatically (OK).\n\n")
        } else {
            sb.append("You chose a directory different to ${File(System.getProperty("user.home"), "ProjectForge")}. That's OK.\n")
            sb.append("To be sure, that this directory is found by the ProjectForge server, you may:\n")
            sb.append(" 1. put the executable jar somewhere inside this directory, or\n")
            sb.append(" 2. set the system environment variable ${ProjectForgeHomeFinder.getHomeEnvironmentVariableDefinition()}'PROJECTFORGE_HOME', or\n")
            sb.append(" 3. start the jar with the command line flag -Dhome.dir=<dir>.\n\n")
        }
        sb.append("If you want to setup e. g. PostgreSQL, you may stop the server after start-up and do your configuration in:\n")
        sb.append("'projectforge.properties' inside your chosen ProjectForge directory.\n\n")
        sb.append("Press 'Finish' for starting the intialization and for starting-up the server.")
        hintLabel.text = sb.toString()
    }

    override fun resize() {
        super.resize()
        dirLabel.setPreferredSize(TerminalSize(context.terminalSize.columns - 20, 1))
    }
}
