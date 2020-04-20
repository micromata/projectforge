/////////////////////////////////////////////////////////////////////////////
//
// Project ProjectForge Community Edition
//         www.projectforge.org
//
// Copyright (C) 2001-2020 Micromata GmbH, Germany (www.micromata.com)
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

package org.projectforge.rest.dto

import org.projectforge.business.scripting.ScriptDO
import org.projectforge.business.scripting.ScriptParameterType

class Script(
        var name: String? = null,
        var description: String? = null,
        var parameter1Name: String? = null,
        var parameter1Type: ScriptParameterType? = null,
        var parameter2Name: String? = null,
        var parameter2Type: ScriptParameterType? = null,
        var parameter3Name: String? = null,
        var parameter3Type: ScriptParameterType? = null,
        var parameter4Name: String? = null,
        var parameter4Type: ScriptParameterType? = null,
        var parameter5Name: String? = null,
        var parameter5Type: ScriptParameterType? = null,
        var parameter6Name: String? = null,
        var parameter6Type: ScriptParameterType? = null
) : BaseDTO<ScriptDO>() {
    var parameter: String? = null
}
