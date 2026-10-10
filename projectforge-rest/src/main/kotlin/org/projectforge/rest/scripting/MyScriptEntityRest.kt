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


package org.projectforge.rest.scripting

import jakarta.annotation.PostConstruct
import jakarta.servlet.http.HttpServletRequest
import org.projectforge.business.scripting.MyScriptDao
import org.projectforge.business.scripting.ScriptDO
import org.projectforge.framework.jcr.AttachmentsDaoAccessChecker
import org.projectforge.framework.jcr.AttachmentsService
import org.projectforge.framework.persistence.api.MagicFilter
import org.projectforge.jcr.FileSizeStandardChecker
import org.projectforge.rest.config.Rest
import org.projectforge.rest.core.AbstractDTOEntityRest
import org.projectforge.rest.core.AccessChecked
import org.projectforge.rest.dto.Script
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.RestController

/**
 * The layout-free REST endpoint of the script list usable by all users, serving the hand-built
 * projectforge-next page (see components/features/script). A user sees only the scripts they are allowed to
 * execute by the script configuration; there is no form, a row leads to the execution ([MyScriptExecuteRest]).
 * @see ScriptDO.executableByUserIds
 * @see ScriptDO.executableByGroupIds
 */
@RestController
@RequestMapping("${Rest.URL}/myscript")
class MyScriptEntityRest : AbstractDTOEntityRest<ScriptDO, Script, MyScriptDao>(
    baseDaoClazz = MyScriptDao::class.java,
    i18nKeyPrefix = "scripting.myScript"
) {
    /** A button of a script on a page, see [ScriptDO.pageTargets]. */
    class PageButton(val id: Long, val label: String, val tooltip: String?)

    @Autowired
    private lateinit var scriptEntityRest: ScriptEntityRest

    @PostConstruct
    private fun postConstruct() {
        val maxFileSize = attachmentsService.maxDefaultFileSize.toBytes()
        val maxFileSizeSpringProperty = AttachmentsService.MAX_DEFAULT_FILE_SIZE_SPRING_PROPERTY

        this.jcrPath = scriptEntityRest.jcrPath
        this.attachmentsAccessChecker = AttachmentsDaoAccessChecker(
            baseDao, jcrPath, null, FileSizeStandardChecker(maxFileSize, maxFileSizeSpringProperty)
        )
    }

    override fun newBaseDO(request: HttpServletRequest?): ScriptDO {
        val script = ScriptDO()
        script.type = ScriptDO.ScriptType.KOTLIN
        return script
    }

    override fun transformForDB(dto: Script): ScriptDO {
        return ScriptDO() // Not needed, no modifications will be done in the data base.
    }

    /**
     * Only what the list shows: the script itself, its access configuration and its attachments are none
     * of the executing user's business.
     */
    override fun transformFromDB(obj: ScriptDO, editMode: Boolean): Script {
        return AbstractScriptExecuteRest.createUserView(obj)
    }

    /**
     * The buttons of the scripts the logged-in user may execute on the page of [target], e.g. `list:order`
     * (see [ScriptPageTargets]). Mostly none.
     */
    @AccessChecked("DAO: MyScriptDao.selectByPageTarget checks select access (executableByUserIds/executableByGroupIds) per script")
    @GetMapping("pageButtons")
    fun getPageButtons(@RequestParam("target") target: String): List<PageButton> {
        return baseDao.selectByPageTarget(target).map { script ->
            PageButton(
                id = script.id!!, // Not null: a stored script.
                label = script.buttonLabel?.takeIf { it.isNotBlank() } ?: script.name ?: "???",
                tooltip = script.buttonTooltip?.takeIf { it.isNotBlank() } ?: script.description?.takeIf { it.isNotBlank() },
            )
        }
    }

    /**
     * Don't show include scripts, because the user can't do anything with it.
     */
    override fun filterList(resultSet: MutableList<ScriptDO>, filter: MagicFilter): List<ScriptDO> {
        return resultSet.filter { !it.deleted && it.type != ScriptDO.ScriptType.INCLUDE }
    }
}
