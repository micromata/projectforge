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
import com.googlecode.lanterna.TextColor
import com.googlecode.lanterna.gui2.DefaultWindowManager
import com.googlecode.lanterna.gui2.EmptySpace
import com.googlecode.lanterna.gui2.MultiWindowTextGUI
import com.googlecode.lanterna.screen.Screen
import com.googlecode.lanterna.screen.TerminalScreen
import com.googlecode.lanterna.terminal.DefaultTerminalFactory
import com.googlecode.lanterna.terminal.Terminal
import org.apache.commons.lang3.SystemUtils
import org.projectforge.common.CanonicalFileUtils
import org.projectforge.common.EmphasizedLogSupport
import org.projectforge.setup.SetupData
import org.projectforge.setup.wizard.AbstractSetupWizard
import java.io.File
import java.io.IOException


class LantSetupWizard(presetAppHomeDir: File? = null) : AbstractSetupWizard() {
    override val context: LantGUIContext
    private val terminal: Terminal
    private val chooseDirectoryScreen: LantChooseDirectoryScreen
    private val finalizeScreen: LantFinalizeScreen
    val lanternaScreen: Screen

    init {
        // Setup terminal and screen layers
        // Throws an IOException on Windows, if not started with javaw.
        val terminalFactory = DefaultTerminalFactory()
        terminalFactory.setInitialTerminalSize(TerminalSize(120, 40))
        terminal = terminalFactory.createTerminal()
        // terminal.enterPrivateMode() // may result in crash
        lanternaScreen = TerminalScreen(terminal)
        lanternaScreen.startScreen()

        // Create gui and start gui
        val textGUI = MultiWindowTextGUI(lanternaScreen, DefaultWindowManager(), EmptySpace(TextColor.ANSI.BLUE))
        context = LantGUIContext(this, textGUI, TerminalSize(lanternaScreen.terminalSize.columns, lanternaScreen.terminalSize.rows))
        context.setupData.applicationHomeDir = presetAppHomeDir
        chooseDirectoryScreen = LantChooseDirectoryScreen(context)
        textGUI.addWindow(chooseDirectoryScreen)
        finalizeScreen = LantFinalizeScreen(context)
        textGUI.addWindow(finalizeScreen)

        terminal.addResizeListener { terminal, newSize ->
            context.terminalSize = newSize
            context.windowSize = newSize
            chooseDirectoryScreen.resize()
            finalizeScreen.resize()
        }
    }

    /**
     * @return The user settings or null, if the user canceled the wizard through exit.
     */
    override fun run(): SetupData? {
        super.initialize()
        chooseDirectoryScreen.waitUntilClosed()
        return super.run();
    }

    override fun setActiveWindow(nextScreen: ScreenID) {
        val window = when (nextScreen) {
            ScreenID.CHOOSE_DIR -> chooseDirectoryScreen
            else -> finalizeScreen
        }
        window.redraw()
        context.textGUI.activeWindow = window
    }

    override fun finish() {
        chooseDirectoryScreen.close()
        finalizeScreen.close()
        lanternaScreen.stopScreen()
        //terminal.exitPrivateMode()
        terminal.close()
        println("")
    }

    companion object {
        private val log = org.slf4j.LoggerFactory.getLogger(LantSetupWizard::class.java)

        @JvmStatic
        fun run(appHomeDir: File? = null): SetupData? {
            log.info("Starting console wizard...")
            return try {
                LantSetupWizard(appHomeDir).run()
            } catch (ex: IOException) {
                val emphasizedLog = EmphasizedLogSupport(log)
                        .log("Can't start the console based setup wizard, your terminal seems not to be supported.")
                if (SystemUtils.IS_OS_WINDOWS) {
                    emphasizedLog.log("On Windows: Please, try to start ProjectForge with javaw.exe or try the desktop wizard.")
                }
                emphasizedLog.logEnd()
                return null
            }
        }

        @JvmStatic
        fun main(args: Array<String>) {
            try {
                val result = LantSetupWizard().run()
                println("result directory='${CanonicalFileUtils.absolutePath(result?.applicationHomeDir)}'")
            } catch (ex: IOException) {
                System.err.println("No graphical terminal available.")
            }
        }
    }
}
