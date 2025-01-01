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

package org.projectforge.setup.wizard.swing

import org.projectforge.common.CanonicalFileUtils
import org.projectforge.setup.wizard.FinalizeScreenSupport
import org.projectforge.setup.wizard.Texts
import org.projectforge.setup.wizard.swing.SwingUtils.constraints
import java.awt.GridBagConstraints
import java.awt.GridBagLayout
import java.awt.event.FocusEvent
import java.awt.event.FocusListener
import java.awt.event.ItemEvent
import java.io.File
import java.text.NumberFormat
import javax.swing.*

class SwingFinalizeScreen(context: SwingGUIContext) : SwingAbstractWizardWindow(context, "Finishing the directory setup") {
    private val log = org.slf4j.LoggerFactory.getLogger(SwingFinalizeScreen::class.java)

    private lateinit var dirStateLabel: JLabel
    private lateinit var dirTextField: JTextField
    private lateinit var domainTextField: JTextField
    private lateinit var portTextField: JTextField

    private lateinit var databaseCombobox: JComboBox<String>
    private lateinit var jdbcSettingsButton: JButton

    private lateinit var currencyTextField: JTextField
    private lateinit var defaultLocaleCombobox: JComboBox<String>
    private lateinit var defaultTimeNotationCombobox: JComboBox<String>
    private lateinit var defaultFirstDayOfWeekCombobox: JComboBox<String>

    private lateinit var startCheckBox: JCheckBox
    private lateinit var developmentCheckBox: JCheckBox

    private lateinit var hintLabel: JLabel

    override fun getContentPanel(): JPanel {
        val panel = JPanel(GridBagLayout())

        var y = -1

        dirTextField = JTextField("")
        panel.add(JLabel(Texts.FS_DIRECTORY), constraints(0, ++y))
        panel.add(dirTextField, constraints(1, y, width = 2, weightx = 1.0, fill = GridBagConstraints.HORIZONTAL))
        dirTextField.addFocusListener(object : FocusListener {
            override fun focusGained(e: FocusEvent?) {
                dirStateLabel.text = FinalizeScreenSupport.getDirText(CanonicalFileUtils.absolute(dirTextField.text.trim()))
            }

            override fun focusLost(e: FocusEvent?) {
                dirStateLabel.text = FinalizeScreenSupport.getDirText(CanonicalFileUtils.absolute(dirTextField.text.trim()))
            }
        })

        dirStateLabel = JLabel(FinalizeScreenSupport.getDirText(CanonicalFileUtils.absolute(dirTextField.text.trim())))
        panel.add(dirStateLabel, constraints(1, ++y, width = 2))

        panel.add(JLabel(""), constraints(0, ++y))

        domainTextField = JTextField("http://localhost:8080")
        panel.add(JLabel(Texts.FS_DOMAIN), constraints(0, ++y))
        panel.add(domainTextField, constraints(1, y, width = 1, weightx = 1.0, fill = GridBagConstraints.HORIZONTAL))
        panel.add(JLabel(Texts.FS_DOMAIN_DESC), constraints(2, y))

        val nf = NumberFormat.getInstance()
        nf.isGroupingUsed = false
        portTextField = JFormattedTextField(nf)
        portTextField.columns = 5
        portTextField.text = "8080"
        panel.add(JLabel(Texts.FS_PORT), constraints(0, ++y))
        panel.add(portTextField, constraints(1, y))

        panel.add(JLabel(""), constraints(0, ++y))

        databaseCombobox = JComboBox()
        FinalizeScreenSupport.listOfDatabases.forEach { databaseCombobox.addItem(it.label) }
        databaseCombobox.addItemListener { event ->
            if (event.stateChange == ItemEvent.SELECTED) {
                if (databaseCombobox.selectedIndex > 0) {
                    jdbcSettingsButton.setEnabled(true)
                    showJdbcSettingsDialog()
                    context.setupData.useEmbeddedDatabase = false
                } else {
                    jdbcSettingsButton.isEnabled= false
                    context.setupData.useEmbeddedDatabase = true
                }
            }
        }
        jdbcSettingsButton = JButton(Texts.FS_JDBC_SETTINGS)
        jdbcSettingsButton.addActionListener {
            showJdbcSettingsDialog()
        }
        jdbcSettingsButton.isEnabled = false
        panel.add(JLabel(Texts.DATABASE), constraints(0, ++y))
        panel.add(databaseCombobox, constraints(1, y))
        panel.add(jdbcSettingsButton, constraints(2, y))

        panel.add(JLabel(""), constraints(0, ++y))

        currencyTextField = JTextField("Euro")
        currencyTextField.preferredSize = currencyTextField.preferredSize
        currencyTextField.text = "€"
        panel.add(JLabel(Texts.FS_CURRENCY), constraints(0, ++y))
        panel.add(currencyTextField, constraints(1, y))

        defaultLocaleCombobox = JComboBox()
        FinalizeScreenSupport.listOfLocales.forEach { defaultLocaleCombobox.addItem(it.label) }
        panel.add(JLabel(Texts.FS_LOCALE), constraints(0, ++y))
        panel.add(defaultLocaleCombobox, constraints(1, y))
        panel.add(JLabel(Texts.FS_LOCALE_DESC), constraints(2, y))

        defaultFirstDayOfWeekCombobox = JComboBox()
        FinalizeScreenSupport.listOfWeekdays.forEach { defaultFirstDayOfWeekCombobox.addItem(it.label) }
        defaultFirstDayOfWeekCombobox.selectedIndex = 0
        panel.add(JLabel(Texts.FS_FIRST_DAY), constraints(0, ++y))
        panel.add(defaultFirstDayOfWeekCombobox, constraints(1, y))
        panel.add(JLabel(Texts.FS_FIRST_DAY_DESC), constraints(2, y))

        defaultTimeNotationCombobox = JComboBox()
        FinalizeScreenSupport.listOfTimeNotations.forEach { defaultTimeNotationCombobox.addItem(it.label) }
        panel.add(JLabel(Texts.FS_TIME_NOTATION), constraints(0, ++y))
        panel.add(defaultTimeNotationCombobox, constraints(1, y))
        panel.add(JLabel(Texts.FS_TIME_NOTATION_DESC), constraints(2, y))

        startCheckBox = JCheckBox(Texts.FS_CHECKBOX_START_SERVER, true)
        panel.add(JLabel(Texts.FS_SETTINGS), constraints(0, ++y))
        panel.add(startCheckBox, constraints(1, y, width = 2))

        developmentCheckBox = JCheckBox(Texts.FS_CHECKBOX_DEV)
        panel.add(JLabel(""), constraints(0, ++y))
        panel.add(developmentCheckBox, constraints(1, y, width = 2))

        panel.add(JLabel(""), constraints(0, ++y))

        hintLabel = JLabel("")
        panel.add(hintLabel, constraints(0, ++y, width = 3))

        return panel
    }

    override fun getButtons(): Array<JButton> {
        val previousButton = JButton(Texts.BUTTON_PREVIOUS)
        previousButton.addActionListener {
            saveValues()
            context.setupMain.previous()
        }
        val finishButton = JButton(Texts.BUTTON_FINISH)
        finishButton.addActionListener {
            saveValues()
            context.setupMain.finish()
        }
        return arrayOf(previousButton, finishButton)
    }

    private fun saveValues() {
        context.setupData.applicationHomeDir = CanonicalFileUtils.absolute(dirTextField.text.trim())
        FinalizeScreenSupport.saveValues(context.setupData,
                domain = domainTextField.text,
                portText = portTextField.text,
                currencySymbol = currencyTextField.text,
                defaultLocaleSelectedIndex = defaultLocaleCombobox.selectedIndex,
                defaultFirstDayOfWeekSelectedIndex = defaultFirstDayOfWeekCombobox.selectedIndex,
                defaultTimeNotationSelectedIndex = defaultTimeNotationCombobox.selectedIndex,
                startServer = startCheckBox.isSelected,
                developmentMode = developmentCheckBox.isSelected)
    }


    override fun redraw() {
        val enabled = !FinalizeScreenSupport.configFileAlreadyExists(context.setupData.applicationHomeDir)
        if (context.setupData.jdbcSettings != null && !context.setupData.useEmbeddedDatabase) {
            // PostgreSQL
            databaseCombobox.selectedIndex = 1
            jdbcSettingsButton.isEnabled = enabled
        } else {
            // embedded
            databaseCombobox.selectedIndex = 0
            jdbcSettingsButton.isEnabled = false
        }
        domainTextField.isEnabled = enabled
        portTextField.isEnabled = enabled
        databaseCombobox.isEnabled = enabled
        currencyTextField.isEnabled = enabled
        defaultLocaleCombobox.isEnabled = enabled
        defaultTimeNotationCombobox.isEnabled = enabled
        defaultFirstDayOfWeekCombobox.isEnabled = enabled
        developmentCheckBox.isEnabled = enabled

        val dir = context.setupData.applicationHomeDir ?: File(System.getProperty("user.home"), "ProjectForge")
        dirTextField.text = CanonicalFileUtils.absolutePath(dir)
        dirStateLabel.text = FinalizeScreenSupport.getDirText(context.setupData.applicationHomeDir)
        hintLabel.text = SwingUtils.convertToMultilineLabel(FinalizeScreenSupport.getInfoText(portTextField.text, dir))
    }

    private fun showJdbcSettingsDialog() {
        // PostgreSQL is selected. Open the JdbcSettingsDialog:
        SwingJdbcSettingsDialog(this,
                context = context
        ).showDialog()
    }
}
