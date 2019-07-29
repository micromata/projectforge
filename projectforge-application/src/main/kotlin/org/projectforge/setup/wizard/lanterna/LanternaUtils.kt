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
import com.googlecode.lanterna.gui2.dialogs.MessageDialogBuilder
import com.googlecode.lanterna.gui2.dialogs.MessageDialogButton
import org.projectforge.setup.wizard.Texts

internal object LanternaUtils {
    fun addEmptySpace(panel: Panel) {
        panel.addComponent(EmptySpace(TerminalSize(0, 1)))
    }

    fun createButtonBar(context: LantGUIContext, width: Int, vararg buttons: Button): Panel {
        return createButtonBar(context,
                Separator(Direction.HORIZONTAL).setPreferredSize(TerminalSize(width, 1)),
                *buttons)
    }

    fun createButtonBar(context: LantGUIContext, separator: Separator, vararg buttons: Button): Panel {
        val panel = Panel()
        separator.addTo(panel)
        val buttonBar = Panel()
        buttonBar.layoutManager = LinearLayout(Direction.HORIZONTAL)
        for (component in buttons) {
            buttonBar.addComponent(component)
        }
        buttonBar.addComponent(getExitButton(context))

        panel.addComponent(buttonBar)
        return panel
    }

    fun getExitButton(context: LantGUIContext): Button {
        return Button(Texts.BUTTON_EXIT) {
            val button = MessageDialogBuilder()
                    .setTitle(Texts.EXIT_TITLE)
                    .setText(Texts.EXIT_QUESTION)
                    .addButton(MessageDialogButton.No)
                    .addButton(MessageDialogButton.Yes)
                    .build()
                    .showDialog(context.textGUI)
            if (button == MessageDialogButton.Yes) {
                context.setupMain.exit()
            }
        }
    }
}
