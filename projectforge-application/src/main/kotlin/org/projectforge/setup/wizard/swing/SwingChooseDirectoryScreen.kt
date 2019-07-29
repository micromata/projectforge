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
import org.projectforge.setup.wizard.Texts
import org.projectforge.start.ProjectForgeHomeFinder
import java.awt.GridBagLayout
import javax.swing.*


class SwingChooseDirectoryScreen(context: SwingGUIContext) : SwingAbstractWizardWindow(context, Texts.CD_SCREEN_TITLE) {
    private val log = org.slf4j.LoggerFactory.getLogger(SwingChooseDirectoryScreen::class.java)

    private lateinit var actionListBox: JList<String>
    private lateinit var listModel: DefaultListModel<String>

    override fun getContentPanel(): JPanel {
        listModel = DefaultListModel()
        actionListBox = JList(listModel)
        actionListBox.addPropertyChangeListener() { valueChanged ->
            if (actionListBox.selectedIndex >= 0) {
                //Selection
                val dir = listModel[actionListBox.selectedIndex]
                context.setupData.applicationHomeDir = CanonicalFileUtils.absolute(dir)
                context.setupMain.next()
            }
        }
        redraw()
        val panel = JPanel(GridBagLayout())
        panel.add(actionListBox, SwingUtils.constraints(0, 0))
        val browseButton = JButton(Texts.BUTTON_BROWSE)
        panel.add(browseButton, SwingUtils.constraints(0, 1))
        browseButton.addActionListener {
            val chooser = JFileChooser()
            chooser.currentDirectory = context.setupData.applicationHomeDir
            chooser.dialogTitle = Texts.CD_CHOOSE_DIR_TITLE
            chooser.fileSelectionMode = JFileChooser.DIRECTORIES_ONLY
            chooser.isAcceptAllFileFilterUsed = false
            if (chooser.showOpenDialog(null) == JFileChooser.APPROVE_OPTION) {
                println("getCurrentDirectory(): " + chooser.currentDirectory)
                println("getSelectedFile() : " + chooser.selectedFile)
            } else {
                println("No Selection ")
            }
        }
        return panel
    }

    override fun redraw() {
        //actionListBox.preferredSize = TerminalSize(context.terminalSize.columns - 5, size.rows)
        listModel.removeAllElements()
        val prevApplicationHomeDir = CanonicalFileUtils.absolute(context.setupData.applicationHomeDir)
        var prevApplicationHomeDirInList = false
        var index = 0;
        for (dir in ProjectForgeHomeFinder.getSuggestedDirectories()) {
            listModel.addElement(CanonicalFileUtils.absolutePath(dir))
            if (dir == prevApplicationHomeDir) {
                actionListBox.selectedIndex = index
                prevApplicationHomeDirInList = true
            }
            ++index
        }
        if (prevApplicationHomeDir != null && !prevApplicationHomeDirInList) {
            // The recent select directory by the user is different and has to be added:
            listModel.addElement(CanonicalFileUtils.absolutePath(prevApplicationHomeDir))
            actionListBox.selectedIndex = index
        }
    }
}
