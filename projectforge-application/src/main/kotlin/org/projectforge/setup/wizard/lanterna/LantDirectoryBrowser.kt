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
import com.googlecode.lanterna.gui2.*
import com.googlecode.lanterna.gui2.dialogs.DialogWindow
import com.googlecode.lanterna.gui2.dialogs.MessageDialog
import com.googlecode.lanterna.gui2.dialogs.MessageDialogButton
import org.projectforge.common.CanonicalFileUtils
import java.io.File
import java.util.*

/**
 * Copy of FileDialog of lanterna to implement directory selection.
 */
open class LantDirectoryBrowser(
        title: String,
        description: String?,
        actionLabel: String,
        dialogSize: TerminalSize,
        context: LantGUIContext,
        preselectedDirname: String? = "",
        showHiddenFilesAndDirs: Boolean = false,
        preSelectedParent: File? = null
) : DialogWindow(title) {
    private var directoryListBox: ActionListBox
    private var pathTextBox: TextBox
    private var dirTextBox: TextBox
    private val showHiddenFilesAndDirs: Boolean

    private var directory: File? = null
    private var selectedFile: File? = null

    init {
        var selectedObject = preSelectedParent
        this.selectedFile = null
        this.showHiddenFilesAndDirs = showHiddenFilesAndDirs

        if (selectedObject == null || !selectedObject.exists()) {
            selectedObject = CanonicalFileUtils.absolute(File("."))
        }
        selectedObject = CanonicalFileUtils.absolute(selectedObject)

        val contentPane = Panel()
        contentPane.layoutManager = LinearLayout()

        if (description != null) {
            LanternaUtils.addEmptySpace(contentPane)
            Label(description).addTo(contentPane)
        }

        val unitWidth = (dialogSize.columns - 10)
        val unitHeight = dialogSize.rows - 16

        directoryListBox = ActionListBox(TerminalSize(unitWidth, unitHeight))
        directoryListBox.withBorder(Borders.singleLine())
                .addTo(contentPane)

        pathTextBox = PathTextBox(unitWidth - 6)
        dirTextBox = TextBox(TerminalSize(20, 1), preselectedDirname)
        val formPanel = Panel().setLayoutManager(GridLayout(2))
                .addComponent(Label("Path:"))
                .addComponent(pathTextBox)
                .addComponent(EmptySpace(), GridLayout.createHorizontallyFilledLayoutData(2))
                .addComponent(Label("Dir:"))
                .addComponent(dirTextBox)
        contentPane.addComponent(formPanel)

        contentPane.addComponent(LanternaUtils.createButtonBar(context, unitWidth,
                Button(actionLabel, OkHandler()),
                Button(LocalizedString.Cancel.toString(), CancelHandler())))

        directory = selectedObject

        reloadViews(directory!!)
        component = contentPane
    }

    /**
     * {@inheritDoc}
     * @param textGUI Text GUI to add the dialog to
     * @return The file which was selected in the dialog or `null` if the dialog was cancelled
     */
    override fun showDialog(textGUI: WindowBasedTextGUI): File? {
        selectedFile = null
        super.showDialog(textGUI)
        return selectedFile
    }

    protected open fun validResult(path: String, dir: String): File? {
        val dir = File(pathTextBox.text, dirTextBox.text)
        if (!dir.exists() && dir.parentFile?.exists() != true) {
            MessageDialog.showMessageDialog(textGUI, "Error", "Please select an existing directory.", MessageDialogButton.OK)
            return null
        }
        return dir
    }

    private inner class OkHandler : Runnable {
        override fun run() {
            val dir = validResult(pathTextBox.text, dirTextBox.text)
            if (dir != null) {
                selectedFile = CanonicalFileUtils.absolute(dir)
                close()
            }
        }
    }

    private inner class CancelHandler : Runnable {
        override fun run() {
            selectedFile = null
            close()
        }
    }

    private fun reloadViews(directory: File) {
        pathTextBox.text = CanonicalFileUtils.absolutePath(directory)
        directoryListBox.clearItems()
        val entries = directory.listFiles() ?: return
        Arrays.sort(entries) { o1, o2 -> o1.name.toLowerCase().compareTo(o2.name.toLowerCase()) }
        val parentFile = CanonicalFileUtils.absolute(directory.absoluteFile.parentFile)
        if (parentFile != null) {
            directoryListBox.addItem("..") {
                this@LantDirectoryBrowser.directory = parentFile
                reloadViews(parentFile)
            }
        } else {
            val roots = File.listRoots()
            for (entry in roots) {
                if (entry.canRead()) {
                    directoryListBox.addItem('['.toString() + entry.path + ']'.toString()) {
                        this@LantDirectoryBrowser.directory = entry
                        reloadViews(entry)
                    }
                }
            }
        }
        for (entry in entries) {
            if (entry.isHidden && !showHiddenFilesAndDirs) {
                continue
            }
            if (entry.isDirectory) {
                directoryListBox.addItem(entry.name) {
                    this@LantDirectoryBrowser.directory = entry
                    reloadViews(entry)
                }
            }
        }
    }

    private inner class PathTextBox(columns: Int) : TextBox(TerminalSize(columns, 1), "") {
        override fun afterLeaveFocus(direction: Interactable.FocusChangeDirection?, nextInFocus: Interactable?) {
            // The user might have changed the path in text field.
            val file = File(text)
            if (file.exists() && file.isDirectory) {
                reloadViews(file)
            }
        }
    }

}
