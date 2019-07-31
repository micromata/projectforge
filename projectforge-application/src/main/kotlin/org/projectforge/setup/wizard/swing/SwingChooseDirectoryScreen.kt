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
import java.io.File
import javax.swing.*


class SwingChooseDirectoryScreen(context: SwingGUIContext) : SwingAbstractWizardWindow(context, Texts.CD_SCREEN_TITLE) {
    private val log = org.slf4j.LoggerFactory.getLogger(SwingChooseDirectoryScreen::class.java)

    private lateinit var jlist: JList<String>
    private lateinit var listModel: DefaultListModel<String>
    private var nextButton: JButton? = null

    override fun getContentPanel(): JPanel {
        listModel = DefaultListModel()
        jlist = JList(listModel)

        redraw()
        val panel = JPanel(GridBagLayout())
        panel.add(jlist, SwingUtils.constraints(0, 0))
        jlist.addListSelectionListener { event ->
            if (!event.valueIsAdjusting) {
                if (jlist.selectedIndex >= 0) {
                    context.setupData.applicationHomeDir = CanonicalFileUtils.absolute(jlist.selectedValue)
                }
            }
        }
        val browseButton = JButton(Texts.BUTTON_BROWSE)
        panel.add(browseButton, SwingUtils.constraints(0, 1))
        browseButton.addActionListener {
            val chooser = JFileChooser()
            var dir = context.setupData.applicationHomeDir
            if (dir != null) {
                if (!dir.exists() && dir.parentFile != null) {
                    dir = dir.parentFile
                }
                if (dir?.exists() == true) {
                    chooser.currentDirectory = dir
                }
            }
            chooser.dialogTitle = Texts.CD_CHOOSE_DIR_TITLE
            chooser.fileSelectionMode = JFileChooser.DIRECTORIES_ONLY
            chooser.isAcceptAllFileFilterUsed = false
            if (chooser.showOpenDialog(this) == JFileChooser.APPROVE_OPTION) {
                var dir = File(chooser.selectedFile, "ProjectForge")
                context.setupData.applicationHomeDir = CanonicalFileUtils.absolute(dir)
                redraw()
                nextIfDirExists()
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
                jlist.selectedIndex = index
                prevApplicationHomeDirInList = true
            }
            ++index
        }
        if (prevApplicationHomeDir != null && !prevApplicationHomeDirInList) {
            // The recent select directory by the user is different and has to be added:
            listModel.addElement(CanonicalFileUtils.absolutePath(prevApplicationHomeDir))
            jlist.selectedIndex = index
        }
    }

    override fun getButtons(): Array<JButton> {
        val nextButton = JButton(Texts.BUTTON_NEXT)
        nextButton.addActionListener {
            nextIfDirExists()
        }
        this.nextButton = nextButton
        return arrayOf(nextButton)
    }

    private fun nextIfDirExists() {
        if (jlist.selectedIndex < 0)
            return
        val dir = CanonicalFileUtils.absolute(jlist.selectedValue)
        context.setupData.applicationHomeDir = dir
        if (dir == null || (!dir.exists() && dir.parentFile?.exists() != true)) {
            JOptionPane.showMessageDialog(null, Texts.ERROR_DIR_NOT_EXISTS, Texts.ERROR_TITLE, JOptionPane.ERROR_MESSAGE);
        } else if (ProjectForgeHomeFinder.isProjectForgeSourceCodeRepository(dir)) {
            JOptionPane.showMessageDialog(null, Texts.ERROR_DIR_IS_SOURCE_REPO, Texts.ERROR_TITLE, JOptionPane.ERROR_MESSAGE);
        } else {
            context.setupMain.next()
        }
    }
}
