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

import java.awt.GridBagConstraints
import java.awt.GridBagLayout
import javax.swing.*

abstract class SwingAbstractWizardWindow(context: SwingGUIContext,
                                         title: String) : JPanel() {
    val mainPanel: JPanel

    protected val context: SwingGUIContext

    private val contentPanel: JPanel

    private val buttonPanel: JPanel

    private val separator: JSeparator

    init {
        this.context = context
        mainPanel = JPanel(GridBagLayout())

        val titlePanel = JPanel()
        //LayoutUtils.addEmptySpace(titlePanel)
        titlePanel.add(JLabel(title))
        //LayoutUtils.addEmptySpace(titlePanel)
        mainPanel.add(titlePanel, SwingUtils.constraints(0, 0))

        contentPanel = getContentPanel()
        //contentPanel.layoutData = GridLayout.createLayoutData(GridLayout.Alignment.FILL, GridLayout.Alignment.FILL, true, true)
        mainPanel.add(contentPanel, SwingUtils.constraints(0, 1))

        mainPanel.add(JLabel(""), SwingUtils.constraints(0, 2, fill = GridBagConstraints.BOTH, weighty = 1.0))
        separator = JSeparator(SwingConstants.HORIZONTAL)
        buttonPanel = SwingUtils.createButtonBar(context, separator, *getButtons())
        mainPanel.add(buttonPanel, SwingUtils.constraints(0, 3, fill = GridBagConstraints.HORIZONTAL))
    }

    open fun getButtons(): Array<JButton> {
        return arrayOf()
    }

    open fun getContentPanel(): JPanel {
        val panel = JPanel()
        //panel.addComponent(EmptySpace())
        return panel
    }


    fun aboutToDisplay() {

        // Place code here that will be executed before the
        // panel is displayed.

    }

    fun displaying() {

        // Place code here that will be executed when the
        // panel is displayed.

    }

    fun aboutToHide() {

        // Place code here that will be executed when the
        // panel is hidden.

    }

    /**
     * Will be called if window is shown again.
     */
    open fun redraw() {}
}
