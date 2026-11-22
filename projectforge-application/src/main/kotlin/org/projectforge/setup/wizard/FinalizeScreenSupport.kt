/////////////////////////////////////////////////////////////////////////////
//
// Project ProjectForge Community Edition
//         www.projectforge.org
//
// Copyright (C) 2001-2026 Micromata GmbH, Germany (www.micromata.com)
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

import org.projectforge.start.ProjectForgeApplication
import org.projectforge.start.ProjectForgeHomeFinder
import java.io.File

object FinalizeScreenSupport {
    fun getDirText(dir: File?): String {
        return if (dir == null)
            "Please specify directory."
        else if (!dir.exists())
            "Will be created and configured."
        else if (ProjectForgeHomeFinder.isProjectForgeSourceCodeRepository(dir))
            "This seems to be the source code repository. It's recommended to select another."
        else if (configFileAlreadyExists(dir))
            "Exists, existing config files will not be overwritten."
        else
            "Exists and will be checked for configuration."
    }

    fun configFileAlreadyExists(baseDir: File?): Boolean {
        return baseDir != null && File(baseDir, ProjectForgeApplication.PROPERTIES_FILENAME).exists()
    }
}
