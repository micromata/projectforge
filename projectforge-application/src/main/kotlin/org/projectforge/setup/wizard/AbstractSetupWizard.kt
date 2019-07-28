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

import org.projectforge.setup.SetupData
import java.io.File


abstract class AbstractSetupWizard(presetAppHomeDir: File? = null) {
    protected var currentScreen: ScreenID = ScreenID.CHOOSE_DIR
        private set

    protected abstract val context: GUIContext

    enum class ScreenID { CHOOSE_DIR, FINALIZE }

    protected fun initialize() {
        setActiveWindow(ScreenID.CHOOSE_DIR)
    }

    /**
     * @return The user settings or null, if the user canceled the wizard through exit.
     */
    internal open fun run(): SetupData? {
        val setupData = context.setupData
        return if (setupData.applicationHomeDir != null) setupData else null
    }

    internal fun next() {
        val next =
                when (currentScreen) {
                    ScreenID.CHOOSE_DIR -> ScreenID.FINALIZE
                    else -> null
                }
        if (next != null) {
            setActiveWindow(next)
            currentScreen = next
        }
    }

    internal fun previous() {
        val previous =
                when (currentScreen) {
                    ScreenID.FINALIZE -> ScreenID.CHOOSE_DIR
                    else -> null
                }
        if (previous != null) {
            setActiveWindow(previous)
            currentScreen = previous
        }
    }

    protected abstract fun setActiveWindow(nextScreen: ScreenID)

    abstract fun finish()

    internal fun exit() {
        finish()
        context.setupData.applicationHomeDir = null
    }
}
