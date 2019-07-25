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
import com.googlecode.lanterna.gui2.BasicWindow
import com.googlecode.lanterna.gui2.MultiWindowTextGUI
import com.googlecode.lanterna.screen.Screen
import org.projectforge.setup.SetupData

class GUIContext(
        val setupMain: SetupMain,
        val textGUI: MultiWindowTextGUI,
        val screen: Screen,
        var terminalSize: TerminalSize
) {
    var currentWindow: BasicWindow? = null
    var chooseDirectoryWindow: ChooseDirectoryWindow? = null
    var initializeWindow: FinalizeWindow? = null
    var windowSize: TerminalSize = TerminalSize.ZERO

        set(value) {
            field = TerminalSize(value.columns - 15, value.rows - 5)
        }
    val setupData = SetupData()

    init {
        windowSize = terminalSize
    }
}
