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

package org.projectforge.setup.wizard.swing

import org.projectforge.common.CanonicalFileUtils
import org.projectforge.setup.wizard.FinalizeScreenSupport
import org.projectforge.setup.wizard.Texts
import org.projectforge.setup.wizard.swing.SwingUtils.constraints
import java.awt.GridBagConstraints
import java.awt.GridBagLayout
import java.awt.event.FocusEvent
import java.awt.event.FocusListener
import java.io.File
import java.text.NumberFormat
import javax.swing.*

class SwingFinalizeScreen(context: SwingGUIContext) : SwingAbstractWizardWindow(context, "Finishing the directory setup") {
    private val log = org.slf4j.LoggerFactory.getLogger(SwingFinalizeScreen::class.java)

    private lateinit var dirLabel: JLabel
    private lateinit var dirTextField: JTextField
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

        val path = context.setupData.applicationHomeDir ?: File(".")
        dirTextField = JTextField(CanonicalFileUtils.absolutePath(path))
        panel.add(JLabel("Directory"), constraints(0, ++y))
        panel.add(dirTextField, constraints(1, y, width = 2, weightx = 1.0, fill = GridBagConstraints.HORIZONTAL))
        dirTextField.addFocusListener(object: FocusListener {
            override fun focusGained(e: FocusEvent?) {
                dirLabel.setText(FinalizeScreenSupport.getDirText(CanonicalFileUtils.absolute(dirTextField.text.trim())))
            }

            override fun focusLost(e: FocusEvent?) {
                dirLabel.setText(FinalizeScreenSupport.getDirText(CanonicalFileUtils.absolute(dirTextField.text.trim())))
            }
        })

        dirLabel = JLabel(FinalizeScreenSupport.getDirText(CanonicalFileUtils.absolute(dirTextField.text.trim())))
        panel.add(dirLabel, constraints(1, ++y, width = 2))

        panel.add(JLabel(""), constraints(0, ++y))

        val nf = NumberFormat.getInstance()
        nf.setGroupingUsed(false)
        portTextField = JFormattedTextField(nf)
        portTextField.columns = 5
        portTextField.text = "8080"
        panel.add(JLabel(Texts.FS_PORT), constraints(0, ++y))
        panel.add(portTextField, constraints(1, y))

        panel.add(JLabel(""), constraints(0, ++y))

        databaseCombobox = JComboBox()
        FinalizeScreenSupport.listOfDatabases.forEach { databaseCombobox.addItem(it.label) }
        databaseCombobox.addPropertyChangeListener { propertyChangeEvent ->
            if (propertyChangeEvent.oldValue != propertyChangeEvent.newValue) {
                if (databaseCombobox.selectedIndex > 0) {
                    jdbcSettingsButton.setEnabled(true)
                    //showJdbcSettingsDialog()
                    context.setupData.useEmbeddedDatabase = false
                } else {
                    jdbcSettingsButton.setEnabled(false)
                    context.setupData.useEmbeddedDatabase = true
                }
            }
        }
        jdbcSettingsButton = JButton(Texts.FS_JDBC_SETTINGS)
        jdbcSettingsButton.addActionListener {
            //showJdbcSettingsDialog()
        }
        jdbcSettingsButton.setEnabled(false)
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
        defaultFirstDayOfWeekCombobox.selectedIndex = 1
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
                portText = portTextField.text,
                currencySymbol = currencyTextField.text,
                defaultLocaleSelectedIndex = defaultLocaleCombobox.selectedIndex,
                defaultFirstDayOfWeekSelectedIndex = defaultFirstDayOfWeekCombobox.selectedIndex,
                defaultTimeNotationSelectedIndex = defaultTimeNotationCombobox.selectedIndex,
                startServer = startCheckBox.isSelected,
                developmentMode = developmentCheckBox.isSelected)
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
        dirLabel.setText(FinalizeScreenSupport.getDirText(context.setupData.applicationHomeDir))
        hintLabel.text = SwingUtils.convertToMultilineLabel(FinalizeScreenSupport.getInfoText(portTextField.text, dir))
    }
}
