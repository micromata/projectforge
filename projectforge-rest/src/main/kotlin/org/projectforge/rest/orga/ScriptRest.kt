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

package org.projectforge.rest.orga

import org.projectforge.business.scripting.ScriptDO
import org.projectforge.business.scripting.ScriptDao
import org.projectforge.rest.config.Rest
import org.projectforge.rest.core.AbstractDORest
import org.projectforge.ui.*
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController

@RestController
@RequestMapping("${Rest.URL}/script")
class ScriptRest: AbstractDORest<ScriptDO, ScriptDao>(baseDaoClazz = ScriptDao::class.java, i18nKeyPrefix = "scripting.title") {

    /**
     * LAYOUT List page
     */
    override fun createListLayout(): UILayout {
        val layout = super.createListLayout()
                .add(UITable.UIResultSetTable()
                        .add(lc, "name", "description", "parameter"))
        return LayoutUtils.processListPage(layout, this)
    }

    /**
     * LAYOUT Edit page
     */
    override fun createEditLayout(dto: ScriptDO): UILayout {
        val layout = super.createEditLayout(dto)
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
