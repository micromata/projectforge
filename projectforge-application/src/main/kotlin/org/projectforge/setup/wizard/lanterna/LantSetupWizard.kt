/////////////////////////////////////////////////////////////////////////////
//
// Project ProjectForge Community Edition
//         www.projectforge.org
//
// Copyright (C) 2001-2024 Micromata GmbH, Germany (www.micromata.com)
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
import mu.KotlinLogging
import org.apache.commons.lang3.SystemUtils
import org.projectforge.common.CanonicalFileUtils
import org.projectforge.common.EmphasizedLogSupport
import org.projectforge.setup.SetupContext
import org.projectforge.setup.SetupData
import org.projectforge.setup.wizard.AbstractSetupWizard
import org.projectforge.setup.wizard.JdbcConnectionTest
import java.io.File
import java.io.IOException

private val log = KotlinLogging.logger {}

class LantSetupWizard(presetAppHomeDir: File? = null, setupContext: SetupContext = SetupContext()) :
  AbstractSetupWizard() {
  override val context: LantGUIContext
  private val terminal: Terminal
  private var chooseDirectoryScreen: LantChooseDirectoryScreen? = null
  private val finalizeScreen: LantFinalizeScreen
  private val lanternaScreen: Screen

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
    context =
      LantGUIContext(
        this,
        textGUI,
        TerminalSize(lanternaScreen.terminalSize.columns, lanternaScreen.terminalSize.rows),
        setupContext
      )
    context.setupData.applicationHomeDir = presetAppHomeDir
    if (!setupContext.embeddedDatabaseSupported) {
      JdbcConnectionTest.defaultJdbcUrl = "jdbc:postgresql://projectforge-db:5432/projectforge"
      logInfo("Force PostgreSQL: ${JdbcConnectionTest.defaultJdbcUrl} (OK)")
    }
    if (setupContext.runAsDockerContainer) {
      // In docker container the bind address must be 0.0.0.0 for exporting the port to the host:
      context.setupData.serverAdress = "0.0.0.0"
      logInfo("Running in docker container. Force server.address=${context.setupData.serverAdress} and skip base directory selection (OK)")
    } else {
      chooseDirectoryScreen = LantChooseDirectoryScreen(context)
      textGUI.addWindow(chooseDirectoryScreen)
    }
    finalizeScreen = LantFinalizeScreen(context)
    textGUI.addWindow(finalizeScreen)

    terminal.addResizeListener { _, newSize ->
      context.terminalSize = newSize
      context.windowSize = newSize
      chooseDirectoryScreen?.resize()
      finalizeScreen.resize()
    }
  }

  /**
   * @return The user settings or null, if the user canceled the wizard through exit.
   */
  override fun run(): SetupData? {
    super.initialize()
    chooseDirectoryScreen?.waitUntilClosed() ?: finalizeScreen.waitUntilClosed()
    return super.run()
  }

  override fun setActiveWindow(nextScreen: ScreenID) {
    val window = when (nextScreen) {
      ScreenID.CHOOSE_DIR -> chooseDirectoryScreen ?: finalizeScreen
      else -> finalizeScreen
    }
    window.redraw()
    context.textGUI.activeWindow = window
  }

  override fun finish() {
    chooseDirectoryScreen?.close()
    finalizeScreen.close()
    lanternaScreen.stopScreen()
    //terminal.exitPrivateMode()
    terminal.close()
    println("")
  }

  companion object {
    /**
     * @param dockerMode In docker mode, no directory browser for ProjectForge's base dir is shown. /ProjectForge is used.
     */
    @JvmStatic
    fun run(appHomeDir: File?, setupContext: SetupContext): SetupData? {
      logInfo("Starting console wizard...")
      return try {
        LantSetupWizard(appHomeDir, setupContext).run()
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

    private fun logInfo(msg: String) {
      println("LantSetup: $msg")
      log.info { msg }
    }

    private fun logError(msg: String) {
      System.err.println("LantSetup***: $msg")
      log.error { msg }
    }

    @JvmStatic
    fun main(args: Array<String>) {
      try {
        val result = LantSetupWizard().run()
        logInfo("result directory='${CanonicalFileUtils.absolutePath(result?.applicationHomeDir)}'")
      } catch (ex: IOException) {
        logError("No graphical terminal available.")
      }
    }
  }
}
