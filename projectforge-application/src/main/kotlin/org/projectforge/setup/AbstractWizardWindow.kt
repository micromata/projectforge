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
import com.googlecode.lanterna.gui2.*

abstract class AbstractWizardWindow(context: GUIContext,
                                    title: String) : BasicWindow("ProjectForge setup") {
    protected val mainPanel: Panel

    protected val context: GUIContext

    init {
        this.context = context
        size = context.windowSize
        mainPanel = Panel()
        mainPanel.layoutManager = GridLayout(1)

        val titlePanel = Panel()
        LayoutUtils.addEmptySpace(titlePanel)
        titlePanel.addComponent(Label(title))
        LayoutUtils.addEmptySpace(titlePanel)
        mainPanel.addComponent(titlePanel)

        val contentPanel = getContentPanel()
        contentPanel.setPreferredSize(TerminalSize(size.columns, size.rows - 5))
        contentPanel.layoutData = GridLayout.createLayoutData(GridLayout.Alignment.FILL, GridLayout.Alignment.FILL, true, true)
        mainPanel.addComponent(contentPanel)

        val buttonPanel = LayoutUtils.createButtonBar(context, context.terminalSize.columns - 8, *getButtons())
        mainPanel.addComponent(buttonPanel)
        component = mainPanel
    }

    open fun getButtons(): Array<Button> {
        return arrayOf()
    }

    open fun getContentPanel(): Panel {
        val panel = Panel()
        panel.addComponent(EmptySpace())
        return panel
    }
}
