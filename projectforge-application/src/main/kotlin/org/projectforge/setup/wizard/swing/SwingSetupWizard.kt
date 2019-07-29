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

import com.apple.eawt.Application
import com.apple.eawt.QuitStrategy
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
    private val frame: JFrame
    private val cardLayout: CardLayout
    private val cards: JPanel
    private val lock = ReentrantLock()
    private val condition = lock.newCondition()

    init {
        frame = JFrame("ProjectForge setup")
        frame.layout = GridBagLayout()
        frame.setDefaultCloseOperation(JFrame.DO_NOTHING_ON_CLOSE)
        Application.getApplication().setQuitStrategy(QuitStrategy.CLOSE_ALL_WINDOWS);
        Application.getApplication().disableSuddenTermination();
        frame.addWindowListener(object : WindowAdapter() {
            override fun windowClosing(e: WindowEvent?) {
                exit()
            }
        })
        frame.setSize(1024, 600)

        context = SwingGUIContext(this, frame)
        context.setupData.applicationHomeDir = presetAppHomeDir

        chooseDirectoryScreen = SwingChooseDirectoryScreen(context)
        finalizeScreen = SwingFinalizeScreen(context)

        //jFrame.add(context.chooseDirectoryWindow!!.mainPanel)

        cardLayout = CardLayout()
        cards = JPanel(cardLayout)
        //cards.setBorder(EmptyBorder(Insets(5, 10, 5, 10)))
        cards.add(chooseDirectoryScreen.mainPanel, ScreenID.CHOOSE_DIR.name)
        cards.add(finalizeScreen.mainPanel, ScreenID.FINALIZE.name)
        val scrPane = JScrollPane(cards)
        //frame.contentPane.add(cardPanel)
        frame.contentPane.add(scrPane, SwingUtils.constraints(0, 0, fill = GridBagConstraints.BOTH, weightx = 1.0, weighty = 1.0))

        //jFrame.setLayout(null)
        frame.setVisible(true)
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
            try {
                return SwingSetupWizard(appHomeDir).run()
            } catch (ex: IOException) {
                EmphasizedLogSupport(SwingSetupWizard.log)
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
