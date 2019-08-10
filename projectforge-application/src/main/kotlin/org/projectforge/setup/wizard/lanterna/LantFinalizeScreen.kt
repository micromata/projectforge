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

package org.projectforge.setup.wizard.lanterna

import com.googlecode.lanterna.TerminalSize
import com.googlecode.lanterna.gui2.*
import org.projectforge.common.CanonicalFileUtils
import org.projectforge.setup.wizard.FinalizeScreenSupport
import org.projectforge.setup.wizard.FinalizeScreenSupport.listOfDatabases
import org.projectforge.setup.wizard.FinalizeScreenSupport.listOfLocales
import org.projectforge.setup.wizard.FinalizeScreenSupport.listOfTimeNotations
import org.projectforge.setup.wizard.FinalizeScreenSupport.listOfWeekdays
import org.projectforge.setup.wizard.Texts
import java.io.File
import java.util.regex.Pattern


class LantFinalizeScreen(context: LantGUIContext) : LantAbstractWizardWindow(context, "Finishing the directory setup") {
    private val log = org.slf4j.LoggerFactory.getLogger(LantFinalizeScreen::class.java)

    private lateinit var dirLabel: Label
    private lateinit var domainTextBox: TextBox
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
        val panel = Panel()
        panel.layoutManager = GridLayout(3)

        dirLabel = Label("")
        panel.addComponent(Label(Texts.FS_DIRECTORY).setSize(TerminalSize(10, 1)))
                .addComponent(dirLabel.setLayoutData(GridLayout.createHorizontallyFilledLayoutData(2)))

        panel.addComponent(EmptySpace().setLayoutData(GridLayout.createHorizontallyFilledLayoutData(3)))

        domainTextBox = TextBox("http://localhost:8080")
                .setPreferredSize(TerminalSize(30, 1))
        panel.addComponent(Label(Texts.FS_DOMAIN))
                .addComponent(domainTextBox)
                .addComponent(Label(Texts.FS_DOMAIN_DESC))

        portTextBox = TextBox("8080")
                .setValidationPattern(Pattern.compile("[0-9]{1,5}?"))
                .setPreferredSize(TerminalSize(7, 1))
        panel.addComponent(Label(Texts.FS_PORT))
                .addComponent(portTextBox)
                .addComponent(EmptySpace())

        panel.addComponent(EmptySpace().setLayoutData(GridLayout.createHorizontallyFilledLayoutData(3)))

        databaseCombobox = ComboBox()
        listOfDatabases.forEach { databaseCombobox.addItem(it.label) }
        databaseCombobox.addListener { selectedIndex, previousSelection ->
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
        jdbcSettingsButton = Button(Texts.FS_JDBC_SETTINGS) {
            showJdbcSettingsDialog()
        }
                .setEnabled(false)
        panel.addComponent(Label(Texts.DATABASE))
                .addComponent(databaseCombobox)
                .addComponent(jdbcSettingsButton)

        panel.addComponent(EmptySpace().setLayoutData(GridLayout.createHorizontallyFilledLayoutData(3)))

        currencyTextBox = TextBox("€")
                .setPreferredSize(TerminalSize(4, 1))
        panel.addComponent(Label(Texts.FS_CURRENCY))
                .addComponent(currencyTextBox)
                .addComponent(EmptySpace())

        defaultLocaleCombobox = ComboBox()
        listOfLocales.forEach { defaultLocaleCombobox.addItem(it.label) }
        panel.addComponent(Label(Texts.FS_LOCALE))
                .addComponent(defaultLocaleCombobox)
                .addComponent(Label(Texts.FS_LOCALE_DESC))

        defaultFirstDayOfWeekCombobox = ComboBox()
        listOfWeekdays.forEach { defaultFirstDayOfWeekCombobox.addItem(it.label) }
        defaultFirstDayOfWeekCombobox.selectedIndex = 1
        panel.addComponent(Label(Texts.FS_FIRST_DAY))
                .addComponent(defaultFirstDayOfWeekCombobox)
                .addComponent(Label(Texts.FS_FIRST_DAY_DESC))

        defaultTimeNotationCombobox = ComboBox()
        listOfTimeNotations.forEach { defaultTimeNotationCombobox.addItem(it.label) }
        panel.addComponent(Label(Texts.FS_TIME_NOTATION))
                .addComponent(defaultTimeNotationCombobox)
                .addComponent(Label(Texts.FS_TIME_NOTATION_DESC))

        panel.addComponent(EmptySpace().setLayoutData(GridLayout.createHorizontallyFilledLayoutData(3)))

        startCheckBox = CheckBox(Texts.FS_CHECKBOX_START_SERVER)
                .setChecked(true)
                .setLayoutData(GridLayout.createHorizontallyFilledLayoutData(2))

        developmentCheckBox = CheckBox(Texts.FS_CHECKBOX_DEV)
                .setChecked(false)
                .setLayoutData(GridLayout.createHorizontallyFilledLayoutData(2))
        panel.addComponent(Label(Texts.FS_SETTINGS))
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
        LantJdbcSettingsDialog(
                this,
                dialogSize = context.terminalSize,
                context = context
        ).showDialog()
    }

    override fun getButtons(): Array<Button> {
        return arrayOf(
                Button(Texts.BUTTON_PREVIOUS) {
                    saveValues()
                    context.setupMain.previous()
                },
                Button(Texts.BUTTON_FINISH) {
                    saveValues()
                    context.setupMain.finish()
                })
    }

    private fun saveValues() {
        FinalizeScreenSupport.saveValues(context.setupData,
                domain = domainTextBox.text,
                portText = portTextBox.text,
                currencySymbol = currencyTextBox.text,
                defaultLocaleSelectedIndex = defaultLocaleCombobox.selectedIndex,
                defaultFirstDayOfWeekSelectedIndex = defaultFirstDayOfWeekCombobox.selectedIndex,
                defaultTimeNotationSelectedIndex = defaultTimeNotationCombobox.selectedIndex,
                startServer = startCheckBox.isChecked,
                developmentMode = developmentCheckBox.isChecked)
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
        val dirText = FinalizeScreenSupport.getDirText(dir)
        dirLabel.setText("${CanonicalFileUtils.absolutePath(dir)} ($dirText)\n")
        hintLabel.text = FinalizeScreenSupport.getInfoText(portTextBox.text, dir)
    }

    override fun resize() {
        super.resize()
        dirLabel.setPreferredSize(TerminalSize(context.terminalSize.columns - 20, 1))
    }
}
