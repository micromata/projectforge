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

package org.projectforge.setup

import com.googlecode.lanterna.TerminalSize
import com.googlecode.lanterna.TextColor
import com.googlecode.lanterna.gui2.DefaultWindowManager
import com.googlecode.lanterna.gui2.EmptySpace
import com.googlecode.lanterna.gui2.MultiWindowTextGUI
import com.googlecode.lanterna.screen.TerminalScreen
import com.googlecode.lanterna.terminal.DefaultTerminalFactory
import com.googlecode.lanterna.terminal.Terminal
import java.io.IOException



class SetupMain {
    private val log = org.slf4j.LoggerFactory.getLogger(SetupMain::class.java)

    private val context: GUIContext
    private val terminal: Terminal

    init {
        // Setup terminal and screen layers
        // Throws an IOException on Windows, if not started with javaw.
        val terminalFactory = DefaultTerminalFactory()
        terminalFactory.setInitialTerminalSize(TerminalSize(120, 40))
        terminal = terminalFactory.createTerminal()
        terminal.enterPrivateMode()
        val screen = TerminalScreen(terminal)
        screen.startScreen()

        // Create gui and start gui
        val textGUI = MultiWindowTextGUI(screen, DefaultWindowManager(), EmptySpace(TextColor.ANSI.BLUE))
        context = GUIContext(this, textGUI, screen, TerminalSize(screen.terminalSize.columns, screen.terminalSize.rows))
        context.chooseDirectoryWindow = ChooseDirectoryWindow(context)
        textGUI.addWindow(context.chooseDirectoryWindow)
        context.initializeWindow = InitializeWindow(context)
        textGUI.addWindow(context.initializeWindow)

        setActiveWindow(context.chooseDirectoryWindow!!)

        terminal.addResizeListener { terminal, newSize ->
            context.terminalSize = newSize
            context.windowSize = newSize
            context.chooseDirectoryWindow!!.resize()
            context.initializeWindow!!.resize()
        }
        textGUI.addWindow(context.currentWindow)
        context.currentWindow!!.waitUntilClosed()

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
                    is InitializeWindow -> context.chooseDirectoryWindow
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

    internal fun exit() {
        context.screen.stopScreen()
        terminal.exitPrivateMode()
        terminal.close()
    }

    companion object {
        @JvmStatic
        fun main(args: Array<String>) {
            try {
                SetupMain()
            } catch (ex: IOException) {
                System.err.println("No graphical terminal available.")
            }
        }
    }
}
