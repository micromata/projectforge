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

import org.projectforge.setup.SetupData
import org.projectforge.setup.wizard.JdbcConnectionTest
import org.projectforge.setup.wizard.Texts
import java.awt.GridBagConstraints
import java.awt.GridBagLayout
import javax.swing.*


/**
 * Jdbc settings dialog (PostgreSQL).
 */
open class SwingJdbcSettingsDialog(
        finalizeWindow: SwingFinalizeScreen,
        val context: SwingGUIContext
) : JDialog(context.mainFrame, Texts.JDBC_TITLE, true) {
    private val log = org.slf4j.LoggerFactory.getLogger(SwingJdbcSettingsDialog::class.java)

    private lateinit var jdbcUrlTextField: JTextField
    private lateinit var jdbcUserTextField: JTextField
    private lateinit var jdbcPasswordTextField: JPasswordField
    private lateinit var jdbcTestButton: JButton
    private lateinit var testResultLabel: JLabel

    init {
        setSize(600, 400)
        setLocationRelativeTo(context.mainFrame);
        val panel = JPanel(GridBagLayout())

        var y = -1

        jdbcUrlTextField = JTextField()
        panel.add(JLabel(Texts.JDBC_URL), SwingUtils.constraints(0, ++y))
        panel.add(jdbcUrlTextField, SwingUtils.constraints(1, y, fill = GridBagConstraints.HORIZONTAL, weightx = 1.0))

        jdbcUserTextField = JTextField()
        panel.add(JLabel(Texts.JDBC_USER), SwingUtils.constraints(0, ++y))
        panel.add(jdbcUserTextField, SwingUtils.constraints(1, y, fill = GridBagConstraints.HORIZONTAL, weightx = 1.0))

        jdbcPasswordTextField = JPasswordField()
        panel.add(JLabel(Texts.JDBC_PASSWORD), SwingUtils.constraints(0, ++y))
        panel.add(jdbcPasswordTextField, SwingUtils.constraints(1, y, fill = GridBagConstraints.HORIZONTAL, weightx = 1.0))

        testResultLabel = JLabel()
        panel.add(JLabel(Texts.JDBC_TESTRESULT), SwingUtils.constraints(0, ++y))
        panel.add(testResultLabel, SwingUtils.constraints(1, y, fill = GridBagConstraints.HORIZONTAL, weightx = 1.0))

        jdbcTestButton = JButton(Texts.JDBC_BUTTON_TEST_CONNECTION)
        jdbcTestButton.addActionListener {
            testResultLabel.text = JdbcConnectionTest.testConnection(jdbcUrlTextField.text, jdbcUserTextField.text, jdbcPasswordTextField.text)
        }

        val resetButton = JButton(Texts.BUTTON_RESET)
        resetButton.addActionListener {
            jdbcUrlTextField.text = JdbcConnectionTest.defaultJdbcUrl
        }

        val okButton = JButton(Texts.BUTTON_OK)
        okButton.addActionListener {
            val jdbcSettings = SetupData.JdbcSettings()
            jdbcSettings.driverClass = "org.postgresql.Driver"
            jdbcSettings.jdbcUrl = jdbcUrlTextField.text
            jdbcSettings.user = jdbcUserTextField.text
            jdbcSettings.password = jdbcPasswordTextField.text
            context.setupData.jdbcSettings = jdbcSettings
            context.setupData.useEmbeddedDatabase = false
            finalizeWindow.redraw()
            isVisible = false
        }

        val cancelButton = JButton(Texts.BUTTON_CANCEL)
        cancelButton.addActionListener {
            finalizeWindow.redraw()
            isVisible = false
        }

        val cuttonBar = SwingUtils.createButtonBar(context, false, jdbcTestButton, resetButton, okButton, cancelButton)

        panel.add(cuttonBar, SwingUtils.constraints(0, ++y, width = 2, fill = GridBagConstraints.HORIZONTAL, weightx = 1.0))
        contentPane.add(panel)
    }

    fun showDialog() {
        val jdbcSettings = context.setupData.jdbcSettings

        jdbcUrlTextField.text = jdbcSettings?.jdbcUrl ?: JdbcConnectionTest.defaultJdbcUrl
        jdbcUserTextField.text = jdbcSettings?.user ?: "projectforge"
        jdbcPasswordTextField.text = jdbcSettings?.password ?: ""
        testResultLabel.text = ""

        isVisible = true
    }
}
