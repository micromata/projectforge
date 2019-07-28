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

import java.awt.GridBagLayout
import javax.swing.JButton
import javax.swing.JPanel

class SwingFinalizeScreen(context: SwingGUIContext) : SwingAbstractWizardWindow(context, "Please select ProjectForge's home directory") {
    private val log = org.slf4j.LoggerFactory.getLogger(SwingFinalizeScreen::class.java)

    override fun getContentPanel(): JPanel {
        redraw()
        val panel = JPanel(GridBagLayout())
        val browseButton = JButton("Browse")
        panel.add(browseButton, SwingUtils.constraints(0, 0))
        browseButton.addActionListener {
            println("Browse")
        }
        return panel
    }

    override fun redraw() {
        //actionListBox.preferredSize = TerminalSize(context.terminalSize.columns - 5, size.rows)
    }
}
