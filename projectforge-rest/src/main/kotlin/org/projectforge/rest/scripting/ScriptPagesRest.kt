/////////////////////////////////////////////////////////////////////////////
//
// Project ProjectForge Community Edition
//         www.projectforge.org
//
// Copyright (C) 2001-2022 Micromata GmbH, Germany (www.micromata.com)
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

package org.projectforge.rest.scripting

import org.projectforge.business.scripting.ScriptDO
import org.projectforge.business.scripting.ScriptDao
import org.projectforge.rest.config.Rest
import org.projectforge.rest.core.AbstractDTOPagesRest
import org.projectforge.rest.dto.Script
import org.projectforge.ui.*
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController

@RestController
@RequestMapping("${Rest.URL}/script")
class ScriptPagesRest: AbstractDTOPagesRest<ScriptDO, Script, ScriptDao>(baseDaoClazz = ScriptDao::class.java, i18nKeyPrefix = "scripting.title") {
    override fun transformForDB(dto: Script): ScriptDO {
        val scriptDO = ScriptDO()
        dto.copyTo(scriptDO)
        return scriptDO
    }

    override fun transformFromDB(obj: ScriptDO, editMode: Boolean): Script {
        val script = Script()
        script.copyFrom(obj)
        script.parameter = obj.getParameterNames(true)
        return script
    }

    /**
     * LAYOUT List page
     */
    override fun createListLayout(): UILayout {
        val layout = super.createListLayout()
                .add(UITable.createUIResultSetTable()
                        .add(lc, "name", "description")
                        .add(UITableColumn("parameter", title = "scripting.script.parameter")))
        return LayoutUtils.processListPage(layout, this)
    }

    /**
     * LAYOUT Edit page
     */
    override fun createEditLayout(dto: Script, userAccess: UILayout.UserAccess): UILayout {
        val layout = super.createEditLayout(dto, userAccess)
                .add(UIRow()
                        .add(UICol()
                                .add(lc, "name"))
                        .add(UICol()
                                .add(UILabel("TODO: Implement file selection"))))
                .add(UIRow()
                        .add(UICol()
                                .add(lc, "parameter1Name")
                                .add(lc, "parameter1Type"))
                        .add(UICol()
                                .add(lc, "parameter2Name")
                                .add(lc, "parameter2Type")))
                .add(UIRow()
                        .add(UICol()
                                .add(lc, "parameter3Name")
                                .add(lc, "parameter3Type"))
                        .add(UICol()
                                .add(lc, "parameter4Name")
                                .add(lc, "parameter4Type")))
                .add(UIRow()
                        .add(UICol()
                                .add(lc, "parameter5Name")
                                .add(lc, "parameter5Type"))
                        .add(UICol()
                                .add(lc, "parameter6Name")
                                .add(lc, "parameter6Type")))
                .add(lc, "description")
                .add(UILabel("TODO: Implement script cmd window"))
        return LayoutUtils.processEditPage(layout, dto, this)
    }
}
