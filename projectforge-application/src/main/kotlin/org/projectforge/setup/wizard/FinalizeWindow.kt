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
import org.projectforge.common.CanonicalFileUtils
import org.projectforge.framework.time.TimeNotation
import org.projectforge.framework.utils.LabelValueBean
import org.projectforge.framework.utils.NumberHelper
import org.projectforge.start.ProjectForgeHomeFinder
import java.io.File
import java.util.regex.Pattern


class FinalizeWindow(context: GUIContext) : AbstractWizardWindow(context, "Finishing the directory setup") {
    private val log = org.slf4j.LoggerFactory.getLogger(FinalizeWindow::class.java)

    private lateinit var dirLabel: Label
    private lateinit var portTextBox: TextBox

    private lateinit var databaseCombobox: ComboBox<String>
    private lateinit var jdbcSettingsButton: Button

    private lateinit var currencyTextBox: TextBox
    private lateinit var defaultLocaleCombobox: ComboBox<String>
    private lateinit var defaultTimeNotationCombobox: ComboBox<String>
    private lateinit var defaultFirstDayOfWeekCombobox: ComboBox<String>

    private lateinit var startCheckBox: CheckBox
    private lateinit var developmentCheckBox: CheckBox

    private lateinit var hintLabel: Label

    override fun getContentPanel(): Panel {
        dirLabel = Label("")
        val panel = Panel()
        panel.layoutManager = GridLayout(3)

        panel.addComponent(Label("Directory").setSize(TerminalSize(10, 1)))
                .addComponent(dirLabel.setLayoutData(GridLayout.createHorizontallyFilledLayoutData(2)))

        panel.addComponent(EmptySpace().setLayoutData(GridLayout.createHorizontallyFilledLayoutData(3)))

        portTextBox = TextBox("8080")
                .setValidationPattern(Pattern.compile("[0-9]{1,5}?"))
                .setPreferredSize(TerminalSize(7, 1))
        panel.addComponent(Label("Port"))
                .addComponent(portTextBox)
                .addComponent(EmptySpace())

        panel.addComponent(EmptySpace().setLayoutData(GridLayout.createHorizontallyFilledLayoutData(3)))

        databaseCombobox = ComboBox()
        listOfDatabases.forEach { databaseCombobox.addItem(it.label) }
        databaseCombobox.addListener() { selectedIndex, previousSelection ->
            if (previousSelection != selectedIndex) {
                if (selectedIndex > 0) {
                    jdbcSettingsButton.setEnabled(true)
                    showJdbcSettingsDialog()
                    context.setupData.useEmbeddedDatabase = false
                } else {
                    jdbcSettingsButton.setEnabled(false)
                    context.setupData.useEmbeddedDatabase = true
                }
            }
        }
        jdbcSettingsButton = Button("Jdbc settings") {
            showJdbcSettingsDialog()
        }
                .setEnabled(false)
        panel.addComponent(Label("Database"))
                .addComponent(databaseCombobox)
                .addComponent(jdbcSettingsButton)

        panel.addComponent(EmptySpace().setLayoutData(GridLayout.createHorizontallyFilledLayoutData(3)))

        currencyTextBox = TextBox("€")
                .setPreferredSize(TerminalSize(4, 1))
        panel.addComponent(Label("Currency"))
                .addComponent(currencyTextBox)
                .addComponent(EmptySpace())

        defaultLocaleCombobox = ComboBox()
        listOfLocales.forEach { defaultLocaleCombobox.addItem(it.label) }
        panel.addComponent(Label("Locale"))
                .addComponent(defaultLocaleCombobox)
                .addComponent(Label("Default locale."))

        defaultFirstDayOfWeekCombobox = ComboBox()
        listOfWeekdays.forEach { defaultFirstDayOfWeekCombobox.addItem(it.label) }
        defaultFirstDayOfWeekCombobox.selectedIndex = 1
        panel.addComponent(Label("First day"))
                .addComponent(defaultFirstDayOfWeekCombobox)
                .addComponent(Label("Default first day of week."))

        defaultTimeNotationCombobox = ComboBox()
        listOfTimeNotations.forEach { defaultTimeNotationCombobox.addItem(it.label) }
        panel.addComponent(Label("Time notation"))
                .addComponent(defaultTimeNotationCombobox)
                .addComponent(Label("Default time notation."))

        panel.addComponent(EmptySpace().setLayoutData(GridLayout.createHorizontallyFilledLayoutData(3)))

        startCheckBox = CheckBox("Start ProjectForge (create a new embedded database)")
                .setChecked(true)
                .setLayoutData(GridLayout.createHorizontallyFilledLayoutData(2))

        developmentCheckBox = CheckBox("Enable CORS filter (for development only)")
                .setChecked(false)
                .setLayoutData(GridLayout.createHorizontallyFilledLayoutData(2))
        panel.addComponent(Label("Settings"))
                .addComponent(startCheckBox)
                .addComponent(EmptySpace())
                .addComponent(developmentCheckBox)

        panel.addComponent(EmptySpace().setLayoutData(GridLayout.createHorizontallyFilledLayoutData(3)))
        hintLabel = Label("")
        hintLabel.layoutData = GridLayout.createHorizontallyFilledLayoutData(3)
        panel.addComponent(hintLabel)
        return panel
    }

    private fun showJdbcSettingsDialog() {
        // PostgreSQL is selected. Open the JdbcSetingsDialog:
        JdbcSettingsDialog(
                this,
                dialogSize = context.terminalSize,
                context = context
        ).showDialog()
    }

    override fun getButtons(): Array<Button> {
        return arrayOf(
                Button("Previous") {
                    saveValues()
                    context.setupMain.previous()
                },
                Button("Finish") {
                    saveValues()
                    context.setupMain.finish()
                })
    }

    private fun saveValues() {
        var port = NumberHelper.parseInteger(portTextBox.text)
        context.setupData.serverPort = if (port in 1..65535) port else 8080
        context.setupData.currencySymbol = currencyTextBox.text
        context.setupData.defaultLocale = listOfLocales.get(defaultLocaleCombobox.selectedIndex).value
        context.setupData.defaultFirstDayOfWeek = listOfWeekdays.get(defaultFirstDayOfWeekCombobox.selectedIndex).value
        context.setupData.defaultTimeNotation = listOfTimeNotations.get(defaultTimeNotationCombobox.selectedIndex).value
        context.setupData.startServer = startCheckBox.isChecked
        context.setupData.developmentMode = developmentCheckBox.isChecked
    }

    override fun redraw() {
        if (context.setupData.jdbcSettings != null && !context.setupData.useEmbeddedDatabase) {
            // PostgreSQL
            databaseCombobox.selectedIndex = 1
            jdbcSettingsButton.setEnabled(true)
        } else {
            // embedded
            databaseCombobox.selectedIndex = 0
            jdbcSettingsButton.setEnabled(false)
        }
        val dir = context.setupData.applicationHomeDir ?: File(System.getProperty("user.home"), "ProjectForge")
        dirLabel.setPreferredSize(TerminalSize(context.terminalSize.columns - 20, 1))
        val dirText = if (!dir.exists()) {
            "Will be created and configured."
        } else "Exists and will be checked for configuration."
        dirLabel.setText("${CanonicalFileUtils.absolutePath(dir)} ($dirText)\n")
        val sb = StringBuilder()
        sb.append("Please open your favorite browser after startup: http://localhost:${portTextBox.text} and enjoy it!\n\n")
        if (ProjectForgeHomeFinder.isStandardProjectForgeUserDir(dir)) {
            sb.append("You chose the standard directory of ProjectForge, that will be found by ProjectForge automatically (OK).\n\n")
        } else {
            sb.append("You chose a directory different to ${File(System.getProperty("user.home"), "ProjectForge")}. That's OK.\n")
            sb.append("To be sure, that this directory is found by the ProjectForge server, please refer log files or home page.\n\n")
        }
        sb.append("Press 'Finish' for starting the intialization and for starting-up the server.")
        hintLabel.text = sb.toString()
    }

    override fun resize() {
        super.resize()
        dirLabel.setPreferredSize(TerminalSize(context.terminalSize.columns - 20, 1))
    }

    companion object {
        private val listOfLocales = listOf(
                LabelValueBean("en - English", "en"),
                LabelValueBean("de - Deutsch", "de")
        )

        private val listOfTimeNotations = listOf(
                LabelValueBean("H24", TimeNotation.H24),
                LabelValueBean("H12", TimeNotation.H12)
        )

        private val listOfWeekdays = listOf(
                LabelValueBean("Sunday", 1),
                LabelValueBean("Monday", 2),
                LabelValueBean("Tuesday", 3),
                LabelValueBean("Wednesday", 4),
                LabelValueBean("Thursday", 5),
                LabelValueBean("Friday", 6),
                LabelValueBean("Saturday", 7)
        )

        private val listOfDatabases = listOf(
                LabelValueBean("Embedded", "HSQL"),
                LabelValueBean("PostgreSQL", "POSTGRES")
        )
    }
}
