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
import com.googlecode.lanterna.gui2.ActionListBox
import com.googlecode.lanterna.gui2.GridLayout
import com.googlecode.lanterna.gui2.Label
import com.googlecode.lanterna.gui2.Panel
import com.googlecode.lanterna.gui2.dialogs.MessageDialog
import com.googlecode.lanterna.gui2.dialogs.MessageDialogButton
import org.projectforge.common.CanonicalFileUtils
import org.projectforge.setup.wizard.Texts
import org.projectforge.start.ProjectForgeHomeFinder
import java.io.File

class LantChooseDirectoryScreen(context: LantGUIContext) : LantAbstractWizardWindow(context, Texts.CD_SCREEN_TITLE) {
    private val log = org.slf4j.LoggerFactory.getLogger(LantChooseDirectoryScreen::class.java)

    private lateinit var actionListBox: ActionListBox

    override fun getContentPanel(): Panel {
        actionListBox = ActionListBox()
        redraw()
        val panel = Panel()
        panel.layoutManager = GridLayout(2)
        panel.addComponent(actionListBox.setLayoutData(GridLayout.createHorizontallyFilledLayoutData(2)))

        val hintLabel = Label("Please use cursor and tab keys for navigation and return or space for selecting.")
        hintLabel.foregroundColor = TextColor.ANSI.RED
        hintLabel.layoutData = GridLayout.createHorizontallyFilledLayoutData(2)
        panel.addComponent(hintLabel)
        return panel
    }

    override fun redraw() {
        actionListBox.preferredSize = TerminalSize(context.terminalSize.columns - 5, 10)
        actionListBox.clearItems()
        val prevApplicationHomeDir = CanonicalFileUtils.absolute(context.setupData.applicationHomeDir)
        var prevApplicationHomeDirInList = false
        var index = 0;
        for (dir in ProjectForgeHomeFinder.getSuggestedDirectories()) {
            actionListBox.addItem(CanonicalFileUtils.absolutePath(dir)) {
                context.setupData.applicationHomeDir = CanonicalFileUtils.absolute(dir)
                context.setupMain.next()
            }
            if (dir == prevApplicationHomeDir) {
                actionListBox.selectedIndex = index
                prevApplicationHomeDirInList = true
            }
            ++index
        }
        if (prevApplicationHomeDir != null && !prevApplicationHomeDirInList) {
            // The recent select directory by the user is different and has to be added:
            actionListBox.addItem(CanonicalFileUtils.absolutePath(prevApplicationHomeDir)) {
                // Nothing to do (application dir not changed).
                context.setupMain.next()
            }
            actionListBox.selectedIndex = index
        }
        actionListBox.addItem(Texts.CD_CHOOSE_OTHER_LANT) {
            var preSelectedParent = context.setupData.applicationHomeDir
            var preselectedDirname = "ProjectForge"
            if (preSelectedParent != null && preSelectedParent.parentFile != null) {
                preselectedDirname = preSelectedParent.name
                preSelectedParent = preSelectedParent.parentFile
            }
            val dirBrowser = object : LantDirectoryBrowser(
                    title = Texts.CD_CHOOSE_DIR_TITLE,
                    description = Texts.CD_CHOOSE_DIR_DESC_LANT,
                    actionLabel = Texts.BUTTON_OK,
                    dialogSize = context.terminalSize,
                    preSelectedParent = preSelectedParent,
                    preselectedDirname = preselectedDirname,
                    context = context
            ) {
                override fun validResult(path: String, dir: String): File? {
                    var dir = super.validResult(path, dir)
                    if (dir != null && ProjectForgeHomeFinder.isProjectForgeSourceCodeRepository(dir)) {
                        MessageDialog.showMessageDialog(textGUI, Texts.ERROR_TITLE, Texts.ERROR_DIR_IS_SOURCE_REPO, MessageDialogButton.OK)
                        return null
                    }
                    return dir
                }
            }
            val file = dirBrowser.showDialog(context.textGUI)
            if (file != null)
                context.setupData.applicationHomeDir = file
            context.setupMain.next()
        }
    }

    override fun resize() {
        super.resize()
        actionListBox.preferredSize = TerminalSize(context.terminalSize.columns - 5, size.rows)
    }
}
