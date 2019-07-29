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

import com.googlecode.lanterna.TerminalSize
import com.googlecode.lanterna.gui2.*
import com.googlecode.lanterna.gui2.dialogs.DialogWindow
import org.projectforge.setup.SetupData
import org.projectforge.setup.wizard.Texts
import org.projectforge.setup.wizard.lanterna.LantFinalizeScreen
import org.projectforge.setup.wizard.lanterna.LantGUIContext
import org.projectforge.setup.wizard.lanterna.LanternaUtils
import java.sql.DriverManager


/**
 * Jdbc settings dialog (PostgreSQL).
 */
open class SwingJdbcSettingsDialog(
        finalizeWindow: LantFinalizeScreen,
        dialogSize: TerminalSize,
        val context: LantGUIContext
) : DialogWindow(Texts.JDBC_TITLE) {
    private val log = org.slf4j.LoggerFactory.getLogger(LantFinalizeScreen::class.java)

    private var jdbcUrlTextBox: TextBox
    private var jdbcUserTextBox: TextBox
    private var jdbcPasswordBox: TextBox
    private var jdbcTestButton: Button
    private var testResultLabel: Label

    init {
        val contentPane = Panel()
        contentPane.layoutManager = GridLayout(2)

        val jdbcSettings = context.setupData.jdbcSettings

        jdbcUrlTextBox = TextBox(jdbcSettings?.jdbcUrl ?: defaultJdbcUrl)
                .setPreferredSize(TerminalSize(60, 1))
        contentPane.addComponent(Label(Texts.JDBC_URL))
                .addComponent(jdbcUrlTextBox)

        jdbcUserTextBox = TextBox(jdbcSettings?.user ?: "projectforge")
                .setPreferredSize(TerminalSize(20, 1))
        contentPane.addComponent(Label(Texts.JDBC_USER))
                .addComponent(jdbcUserTextBox)

        jdbcPasswordBox = TextBox(jdbcSettings?.password ?: "")
                .setPreferredSize(TerminalSize(20, 1))
                .setMask('*')

        contentPane.addComponent(Label(Texts.JDBC_PASSWORD))
                .addComponent(jdbcPasswordBox)

        testResultLabel = Label("")
        contentPane.addComponent(Label(Texts.JDBC_TESTRESULT))
                .addComponent(testResultLabel)

        jdbcTestButton = Button(Texts.JDBC_BUTTON_TEST_CONNECTION) {
            Class.forName("org.postgresql.Driver")
            val jdbcUrl = jdbcUrlTextBox.text
            val username = jdbcUserTextBox.text
            val password = jdbcPasswordBox.text
            try {
                val connection = DriverManager.getConnection(jdbcUrl, username, password)
                if (connection.isValid(10)) {
                    testResultLabel.text = Texts.JDBC_TESTRESULT_OK
                } else {
                    testResultLabel.text = Texts.JDBC_TESTRESULT_NOT_VALID
                }
            } catch (ex: Exception) {
                testResultLabel.text = "${Texts.JDBC_TESTRESULT_CONNECTION_FAILED}: ${ex.message}!"
            }
        }

        val unitWidth = (dialogSize.columns - 10)

        contentPane.addComponent(LanternaUtils.createButtonBar(context, unitWidth,
                jdbcTestButton,
                Button(Texts.BUTTON_RESET) {
                    jdbcUrlTextBox.text = defaultJdbcUrl
                },
                Button(Texts.BUTTON_OK) {
                    val jdbcSettings = SetupData.JdbcSettings()
                    jdbcSettings.driverClass = "org.postgresql.Driver"
                    jdbcSettings.jdbcUrl = jdbcUrlTextBox.text
                    jdbcSettings.user = jdbcUserTextBox.text
                    jdbcSettings.password = jdbcPasswordBox.text
                    context.setupData.jdbcSettings = jdbcSettings
                    context.setupData.useEmbeddedDatabase = false
                    finalizeWindow.redraw()
                    close()
                },
                Button(LocalizedString.Cancel.toString()) {
                    finalizeWindow.redraw()
                    close()
                })
                .setLayoutData(GridLayout.createHorizontallyFilledLayoutData(2))
        )

        component = contentPane
    }

    fun showDialog(): Any? {
        super.showDialog(context.textGUI)
        return null
    }

    companion object {
        const val defaultJdbcUrl = "jdbc:postgresql://localhost:15432/projectforge"
    }
}
