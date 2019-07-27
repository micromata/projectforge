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
import com.googlecode.lanterna.gui2.ActionListBox
import com.googlecode.lanterna.gui2.Panel
import com.googlecode.lanterna.gui2.dialogs.MessageDialog
import com.googlecode.lanterna.gui2.dialogs.MessageDialogButton
import org.projectforge.common.CanonicalFileUtils
import org.projectforge.start.ProjectForgeHomeFinder
import java.io.File

class ChooseDirectoryWindow(context: GUIContext) : AbstractWizardWindow(context, "Please select ProjectForge's home directory") {
    private val log = org.slf4j.LoggerFactory.getLogger(ChooseDirectoryWindow::class.java)

    private lateinit var actionListBox: ActionListBox

    override fun getContentPanel(): Panel {
        actionListBox = ActionListBox()
        redraw()
        return Panel().addComponent(actionListBox)
    }

    override fun redraw() {
        actionListBox.preferredSize = TerminalSize(context.terminalSize.columns - 5, size.rows)
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
        actionListBox.addItem("Choose other") {
            var preSelectedParent = context.setupData.applicationHomeDir
            var preselectedDirname = "ProjectForge"
            if (preSelectedParent != null && preSelectedParent.parentFile != null) {
                preselectedDirname = preSelectedParent.name
                preSelectedParent = preSelectedParent.parentFile
            }
            val dirBrowser = object : DirectoryBrowser(
                    title = "Choose ProjectForge's parent directory",
                    description = "Parent directory where to create home dir of ProjectForge",
                    actionLabel = "OK",
                    dialogSize = context.terminalSize,
                    preSelectedParent = preSelectedParent,
                    preselectedDirname = preselectedDirname,
                    context = context
            ) {
                override fun validResult(path: String, dir: String): File? {
                    var dir = super.validResult(path, dir)
                    if (dir != null && ProjectForgeHomeFinder.isProjectForgeSourceCodeRepository(dir)) {
                        MessageDialog.showMessageDialog(textGUI, "Error", "This directory seems to be ProjectForge's source code repository..", MessageDialogButton.OK)
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
