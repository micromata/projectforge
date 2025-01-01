/////////////////////////////////////////////////////////////////////////////
//
// Project ProjectForge Community Edition
//         www.projectforge.org
//
// Copyright (C) 2001-2025 Micromata GmbH, Germany (www.micromata.com)
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
import com.googlecode.lanterna.gui2.MultiWindowTextGUI
import org.projectforge.setup.SetupContext
import org.projectforge.setup.wizard.AbstractSetupWizard
import org.projectforge.setup.wizard.GUIContext

class LantGUIContext(
        setupMain: AbstractSetupWizard,
        val textGUI: MultiWindowTextGUI,
        var terminalSize: TerminalSize,
        val setupContext: SetupContext
): GUIContext(Mode.CONSOLE, setupMain) {
    var windowSize: TerminalSize = TerminalSize.ZERO
        set(value) {
            field = TerminalSize(value.columns - 15, value.rows - 5)
        }

    init {
        windowSize = terminalSize
    }
}
