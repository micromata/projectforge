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

abstract class LantAbstractWizardWindow(context: LantGUIContext,
                                        title: String) : BasicWindow("ProjectForge setup") {
    protected val mainPanel: Panel

    protected val context: LantGUIContext

    private val contentPanel: Panel

    private val buttonPanel: Panel

    private val separator: Separator

    init {
        this.context = context
        size = context.windowSize
        mainPanel = Panel()
        mainPanel.layoutManager = GridLayout(1)

        val titlePanel = Panel()
        LanternaUtils.addEmptySpace(titlePanel)
        titlePanel.addComponent(Label(title))
        LanternaUtils.addEmptySpace(titlePanel)
        mainPanel.addComponent(titlePanel)

        contentPanel = getContentPanel()
        contentPanel.layoutData = GridLayout.createLayoutData(GridLayout.Alignment.FILL, GridLayout.Alignment.FILL, true, true)
        mainPanel.addComponent(contentPanel)

        separator = Separator(Direction.HORIZONTAL)
        buttonPanel = LanternaUtils.createButtonBar(context, separator, *getButtons())
        mainPanel.addComponent(buttonPanel)
        component = mainPanel
        resize()
    }

    open fun getButtons(): Array<Button> {
        return arrayOf()
    }

    open fun getContentPanel(): Panel {
        val panel = Panel()
        panel.addComponent(EmptySpace())
        return panel
    }

    /**
     * Will be called if window is shown again.
     */
    open fun redraw() {}

    open fun resize() {
        size = context.windowSize
        contentPanel.setPreferredSize(TerminalSize(size.columns, size.rows - 5))
        separator.preferredSize = TerminalSize(context.terminalSize.columns - 8, 0)
    }
}
