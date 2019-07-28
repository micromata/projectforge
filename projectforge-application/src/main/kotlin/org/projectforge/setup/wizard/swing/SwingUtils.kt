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

import java.awt.FlowLayout
import java.awt.GridBagConstraints
import javax.swing.JButton
import javax.swing.JOptionPane
import javax.swing.JPanel
import javax.swing.JSeparator

internal object SwingUtils {
    fun addEmptySpace(panel: JPanel) {
        //panel.add(EmptySpace(TerminalSize(0, 1)))
    }

    fun constraints(x: Int, y: Int,
                    width: Int = 1,
                    height: Int = 1,
                    fill: Int = GridBagConstraints.NONE,
                    anchor: Int = GridBagConstraints.LINE_START,
                    weightx: Double = 0.0,
                    weighty: Double = 0.0
    ): GridBagConstraints {
        val constraints = GridBagConstraints()
        constraints.gridx = x
        constraints.gridy = y
        constraints.gridwidth = width
        constraints.gridheight = height
        constraints.fill = fill
        constraints.anchor = anchor
        constraints.weightx = weightx
        constraints.weighty = weighty
        return constraints
    }

    /* fun createButtonBar(context: SGUIContext, width: Int, vararg buttons: JButton): JPanel {
         return createButtonBar(context,
                 JSeparator(SwingConstants.HORIZONTAL), //.setPreferredSize(TerminalSize(width, 1)),
                 *buttons)
     }*/

    fun createButtonBar(context: SwingGUIContext, separator: JSeparator, vararg buttons: JButton): JPanel {
        val panel = JPanel(FlowLayout())
        panel.add(separator)
        val buttonBar = JPanel()
        //buttonBar.layoutManager = LinearLayout(Direction.HORIZONTAL)
        for (component in buttons) {
            buttonBar.add(component)
        }
        buttonBar.add(getExitButton(context))
        panel.add(buttonBar)
        return panel
    }

    fun getExitButton(context: SwingGUIContext): JButton {
        val exitButton = JButton("Exit")
        exitButton.addActionListener {
            val options = arrayOf("Cancel", "Exit")
            if (JOptionPane.showOptionDialog(context.mainFrame,
                            "Do you really want to exit?",
                            "Exit",
                            JOptionPane.YES_NO_CANCEL_OPTION,
                            JOptionPane.QUESTION_MESSAGE,
                            null,
                            options,
                            options[0]) == 1)
                context.setupMain.exit()
        }
        return exitButton
    }
}
