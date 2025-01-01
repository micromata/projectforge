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
import org.projectforge.common.EmphasizedLogSupport
import org.projectforge.setup.SetupData
import org.projectforge.setup.wizard.AbstractSetupWizard
import java.awt.CardLayout
import java.awt.GridBagConstraints
import java.awt.GridBagLayout
import java.awt.event.WindowAdapter
import java.awt.event.WindowEvent
import java.io.File
import java.io.IOException
import java.util.concurrent.locks.ReentrantLock
import javax.swing.JFrame
import javax.swing.JPanel
import javax.swing.JScrollPane
import kotlin.concurrent.withLock

class SwingSetupWizard(presetAppHomeDir: File? = null) : AbstractSetupWizard() {
    override val context: SwingGUIContext
    private val chooseDirectoryScreen: SwingChooseDirectoryScreen
    private val finalizeScreen: SwingFinalizeScreen
    private val frame = JFrame("ProjectForge setup")
    private val cardLayout: CardLayout
    private val cards: JPanel
    private val lock = ReentrantLock()
    private val condition = lock.newCondition()

    init {
        frame.setSize(1024, 600)
        frame.layout = GridBagLayout()
        frame.defaultCloseOperation = JFrame.DO_NOTHING_ON_CLOSE
        try {
            // Needs reflection because Apple classes may not be available:
            val applicationClass = Class.forName("com.apple.eawt.Application")
            val quitStrategyClass = Class.forName("com.apple.eawt.QuitStrategy")
            val getApplicationMethod = applicationClass.getDeclaredMethod("getApplication")
            val setQuitStrategyMethod = applicationClass.getDeclaredMethod("setQuitStrategy", quitStrategyClass)
            val disableSuddenTerminationMethod = applicationClass.getDeclaredMethod("disableSuddenTermination")

            val application = getApplicationMethod.invoke(null)
            val quitStrategy = quitStrategyClass.enumConstants.find { it.toString() == "CLOSE_ALL_WINDOWS" }//.QuitStrategy.CLOSE_ALL_WINDOWS
            setQuitStrategyMethod.invoke(application, quitStrategy)  //Application.getApplication().setQuitStrategy(QuitStrategy.CLOSE_ALL_WINDOWS)
            disableSuddenTerminationMethod.invoke(application) // Application.getApplication().disableSuddenTermination()
        } catch (ex: Exception) {
            log.debug("Not a MacOS system or MacOS JVM doesn't support com.apple.eawt.Application (OK).")
        }
        frame.addWindowListener(object : WindowAdapter() {
            override fun windowClosing(e: WindowEvent?) {
                exit()
            }
        })

        context = SwingGUIContext(this, frame)
        context.setupData.applicationHomeDir = presetAppHomeDir

        chooseDirectoryScreen = SwingChooseDirectoryScreen(context)
        finalizeScreen = SwingFinalizeScreen(context)

        cardLayout = CardLayout()
        cards = JPanel(cardLayout)
        cards.add(chooseDirectoryScreen.mainPanel, ScreenID.CHOOSE_DIR.name)
        cards.add(finalizeScreen.mainPanel, ScreenID.FINALIZE.name)
        val scrPane = JScrollPane(cards)
        frame.contentPane.add(scrPane, SwingUtils.constraints(0, 0, fill = GridBagConstraints.BOTH, weightx = 1.0, weighty = 1.0))

        frame.isVisible = true
    }

    /**
     * @return The user settings or null, if the user canceled the wizard through exit.
     */
    override fun run(): SetupData? {
        super.initialize()
        lock.withLock {
            condition.await()
        }
        return super.run()
    }

    override fun setActiveWindow(nextScreen: ScreenID) {
        val window = when (nextScreen) {
            ScreenID.CHOOSE_DIR -> chooseDirectoryScreen
            else -> finalizeScreen
        }
        window.redraw()
        val cl = cards.layout as CardLayout
        cl.show(cards, nextScreen.name)
    }


    override fun finish() {
        frame.dispose()
        lock.withLock {
            condition.signal()
        }
    }

    companion object {
        private val log = org.slf4j.LoggerFactory.getLogger(SwingSetupWizard::class.java)

        @JvmStatic
        fun run(appHomeDir: File? = null): SetupData? {
            log.info("Starting GUI wizard...")
            return try {
                SwingSetupWizard(appHomeDir).run()
            } catch (ex: IOException) {
                EmphasizedLogSupport(log)
                        .log("Can't start graphical setup wizard, a desktop seems not to be available.")
                        .logEnd()
                return null
            }
        }

        @JvmStatic
        fun main(args: Array<String>) {
            try {
                val result = SwingSetupWizard().run()
                println("result directory='${CanonicalFileUtils.absolutePath(result?.applicationHomeDir)}'")
            } catch (ex: IOException) {
                System.err.println("No graphical terminal available.")
            }
        }
    }
}
