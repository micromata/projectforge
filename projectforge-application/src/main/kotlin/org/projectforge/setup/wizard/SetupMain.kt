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
import com.googlecode.lanterna.TextColor
import com.googlecode.lanterna.gui2.DefaultWindowManager
import com.googlecode.lanterna.gui2.EmptySpace
import com.googlecode.lanterna.gui2.MultiWindowTextGUI
import com.googlecode.lanterna.screen.TerminalScreen
import com.googlecode.lanterna.terminal.DefaultTerminalFactory
import com.googlecode.lanterna.terminal.Terminal
import org.apache.commons.lang3.SystemUtils
import org.projectforge.common.CanonicalFileUtils
import org.projectforge.common.EmphasizedLogSupport
import org.projectforge.setup.SetupData
import java.io.File
import java.io.IOException


class SetupMain(presetAppHomeDir: File? = null) {
    private val context: GUIContext
    private val terminal: Terminal

    init {
        // Setup terminal and screen layers
        // Throws an IOException on Windows, if not started with javaw.
        val terminalFactory = DefaultTerminalFactory()
        terminalFactory.setInitialTerminalSize(TerminalSize(120, 40))
        terminal = terminalFactory.createTerminal()
        // terminal.enterPrivateMode() // may result in crash
        val screen = TerminalScreen(terminal)
        screen.startScreen()

        // Create gui and start gui
        val textGUI = MultiWindowTextGUI(screen, DefaultWindowManager(), EmptySpace(TextColor.ANSI.BLUE))
        context = GUIContext(this, textGUI, screen, TerminalSize(screen.terminalSize.columns, screen.terminalSize.rows))
        context.setupData.applicationHomeDir = presetAppHomeDir
        context.chooseDirectoryWindow = ChooseDirectoryWindow(context)
        textGUI.addWindow(context.chooseDirectoryWindow)
        context.initializeWindow = FinalizeWindow(context)
        textGUI.addWindow(context.initializeWindow)

        setActiveWindow(context.chooseDirectoryWindow!!)

        terminal.addResizeListener { terminal, newSize ->
            context.terminalSize = newSize
            context.windowSize = newSize
            context.chooseDirectoryWindow!!.resize()
            context.initializeWindow!!.resize()
        }
        textGUI.addWindow(context.currentWindow)
    }

    /**
     * @return The user settings or null, if the user canceled the wizard through exit.
     */
    internal fun run(): SetupData? {
        context.initializeWindow!!.waitUntilClosed()
        val setupData = context.setupData
        return if (setupData.applicationHomeDir != null) setupData else null
    }

    internal fun next() {
        val next =
                when (context.currentWindow) {
                    is ChooseDirectoryWindow -> context.initializeWindow
                    else -> null
                }
        if (next != null) {
            setActiveWindow(next)
        }
    }

    internal fun previous() {
        val previous =
                when (context.currentWindow) {
                    is FinalizeWindow -> context.chooseDirectoryWindow
                    else -> null
                }
        if (previous != null) {
            setActiveWindow(previous)
        }
    }

    private fun setActiveWindow(window: AbstractWizardWindow) {
        context.currentWindow = window
        window.redraw()
        context.textGUI.setActiveWindow(window)
    }

    internal fun finish() {
        context.initializeWindow!!.close()
        context.screen.stopScreen()
        //terminal.exitPrivateMode()
        terminal.close()
    }

    internal fun exit() {
        finish()
        context.setupData.applicationHomeDir = null
    }

    companion object {
        private val log = org.slf4j.LoggerFactory.getLogger(SetupMain::class.java)

        @JvmStatic
        fun run(appHomeDir: File? = null): SetupData? {
            try {
                return SetupMain(appHomeDir).run()
            } catch (ex: IOException) {
                val emphasizedLog = EmphasizedLogSupport(log)
                        .log("Can't start graphical setup wizard, your terminal seems not to be supported.")
                if (SystemUtils.IS_OS_WINDOWS) {
                    emphasizedLog.log("On Windows: Please, try to start ProjectForge with javaw.exe instead of java.exe.")
                }
                emphasizedLog.logEnd()
                return null
            }
        }

        @JvmStatic
        fun main(args: Array<String>) {
            try {
                val result = SetupMain().run()
                println("result directory='${CanonicalFileUtils.absolutePath(result?.applicationHomeDir)}'")
            } catch (ex: IOException) {
                System.err.println("No graphical terminal available.")
            }
        }
    }
}
