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

package org.projectforge.plugins.merlin.rest

import io.github.oshai.kotlinlogging.KotlinLogging
import jakarta.annotation.PostConstruct
import jakarta.servlet.http.HttpServletRequest
import jakarta.validation.Valid
import org.projectforge.business.group.service.GroupService
import org.projectforge.framework.i18n.translateMsg
import org.projectforge.framework.persistence.user.api.ThreadLocalUserContext
import org.projectforge.files.store.FileStore
import org.projectforge.plugins.merlin.*
import org.projectforge.rest.config.Rest
import org.projectforge.rest.config.RestUtils
import org.projectforge.rest.core.AbstractDTOEntityRest
import org.projectforge.rest.core.AccessChecked
import org.projectforge.rest.core.RestButtonEvent
import org.projectforge.rest.dto.Group
import org.projectforge.rest.dto.PostData
import org.projectforge.rest.dto.User
import org.projectforge.ui.ResponseAction
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController
import java.util.*

private val log = KotlinLogging.logger {}

/**
 * The Word® templates (list and admin form) of projectforge-next, layout free. The execution of a template is
 * [MerlinExecuteRest].
 */
@RestController
@RequestMapping("${Rest.URL}/merlin")
class MerlinEntityRest :
    AbstractDTOEntityRest<MerlinTemplateDO, MerlinTemplate, MerlinTemplateDao>(
        MerlinTemplateDao::class.java,
        "plugins.merlin.title",
        cloneSupport = CloneSupport.CLONE,
    ) {
    /**
     * The variables of a template after an analysis of its Word® template or after a change of a variable, for
     * replacing them in the form (see [analyze], [updateVariable] and [deleteVariable]).
     */
    class VariablesResult(
        val variables: List<MerlinVariable>,
        val dependentVariables: List<MerlinVariable>,
        val lastVariableUpdate: Date?,
        val wordTemplateFileName: String?,
        val excelTemplateDefinitionFileName: String?,
        val conditionalsMarkdown: String?,
    ) {
        constructor(dto: MerlinTemplate) : this(
            dto.variables,
            dto.dependentVariables,
            dto.lastVariableUpdate,
            dto.wordTemplateFileName,
            dto.excelTemplateDefinitionFileName,
            dto.conditionalsMarkdown,
        )
    }

    @Autowired
    private lateinit var groupService: GroupService

    @Autowired
    private lateinit var merlinHandler: MerlinHandler

    @Autowired
    private lateinit var merlinTemplateDefinitionHandler: MerlinTemplateDefinitionHandler

    @Autowired
    private lateinit var fileStore: FileStore

    @PostConstruct
    private fun postConstruct() {
        enableAttachments()
        merlinHandler.attachmentsAccessChecker = attachmentsAccessChecker
        merlinHandler.attachmentsPath = attachmentsPath!!
    }

    /**
     * A template has no history, so its deletion is final: its attachments (Word® template, Excel template
     * definition) are removed as well, after the deletion succeeded.
     */
    override fun onAfterEdit(
        request: HttpServletRequest,
        obj: MerlinTemplateDO,
        postData: PostData<MerlinTemplate>,
        event: RestButtonEvent,
    ): ResponseAction {
        if (event == RestButtonEvent.DELETE) {
            obj.id?.let { id ->
                val files = fileStore.deleteAllFilesBelow(attachmentsService.getPath(attachmentsPath!!, id))
                log.info { "Merlin template #$id deleted with its ${files.size} attachment(s)." }
            }
        }
        return super.onAfterEdit(request, obj, postData, event)
    }

    /**
     * A new template: the logged-in user is its admin.
     */
    override fun newBaseDO(request: HttpServletRequest?): MerlinTemplateDO {
        val template = super.newBaseDO(request)
        template.adminIds = "${ThreadLocalUserContext.loggedInUserId}"
        template.fileNamePattern = "\${date}-document"
        return template
    }

    /**
     * The url of the user's log viewer of Merlin, for the list's "view logs".
     */
    override fun addVariablesForListPage(): Map<String, Any> {
        return mapOf("logViewerUrl" to MerlinPlugin.logViewerUrl())
    }

    /**
     * The user and group columns are joined display names, there's no database column to sort on. The groups
     * sort by their names only, without the users listed after them. See [computedSortProperties].
     */
    override val computedSortProperties: Map<String, (MerlinTemplateDO) -> Comparable<*>?>
        get() = mapOf(
            "adminsAsString" to { obj ->
                usersAsString(User.toUserList(obj.adminIds)?.also { User.restoreDisplayNames(it) })
            },
            "accessUsersAsString" to { obj ->
                usersAsString(User.toUserList(obj.accessUserIds)?.also { User.restoreDisplayNames(it) })
            },
            "accessGroupsAsString" to { obj ->
                groupsAsString(Group.toGroupList(obj.accessGroupIds)?.also { Group.restoreDisplayNames(it) })
            },
        )

    private fun usersAsString(users: List<User>?): String = users?.joinToString { it.displayName ?: "???" } ?: ""

    private fun groupsAsString(groups: List<Group>?): String = groups?.joinToString { it.displayName ?: "???" } ?: ""

    override fun transformForDB(dto: MerlinTemplate): MerlinTemplateDO {
        val obj = MerlinTemplateDO()
        dto.copyTo(obj)
        return obj
    }

    /**
     * In edit mode, the Word® template is analyzed: the variables found in it are added to the dto and the usage
     * (used, master variable) of all variables is updated.
     */
    override fun transformFromDB(obj: MerlinTemplateDO, editMode: Boolean): MerlinTemplate {
        val dto = merlinHandler.getDto(obj)

        // Display names needed by the autocompletion fields:
        Group.restoreDisplayNames(dto.accessGroups)
        User.restoreDisplayNames(dto.admins)
        User.restoreDisplayNames(dto.accessUsers)

        dto.adminsAsString = usersAsString(dto.admins)
        buildAccessGroupAsString(dto)
        dto.accessUsersAsString = usersAsString(dto.accessUsers)
        dto.dataTransferAvailable = merlinHandler.dataTransferPluginAvailable()

        if (editMode) {
            analyze(dto)
        } else {
            obj.id?.let { id ->
                dto.wordTemplateFileName = merlinHandler.getAttachments(id)?.let { merlinHandler.getWordTemplate(it) }?.name
            }
        }
        return dto
    }

    /**
     * Analyzes the Word® template and updates the variables of the given dto (as the legacy edit page did on
     * every load).
     */
    private fun analyze(dto: MerlinTemplate) {
        val analyzeResult = merlinHandler.analyze(dto)
        val stats = analyzeResult.statistics
        stats.updateDtoVariables(dto)
        dto.wordTemplateFileName = analyzeResult.wordTemplateFilename
        dto.variables.sort()
        dto.dependentVariables.sortBy { it.name.lowercase() }
        dto.conditionalsMarkdown = if (stats.conditionals.isEmpty()) null else stats.conditionalsAsMarkdown()
    }

    /**
     * The attachments (Word template and Excel template definition) aren't cloned. The user has to upload them
     * after saving the clone.
     */
    override fun prepareClone(dto: MerlinTemplate): MerlinTemplate {
        val clone = super.prepareClone(dto)
        clone.name = translateMsg("plugins.merlin.clone.name", dto.name ?: "")
        // Attachments aren't cloned:
        clone.attachments = null
        clone.attachmentsCounter = null
        clone.attachmentsSize = null
        clone.wordTemplateFileName = null
        clone.excelTemplateDefinitionFileName = null
        clone.conditionalsMarkdown = null
        clone.lastVariableUpdate = null
        return clone
    }

    /**
     * The Excel template definition of the variables as they are in the form (not yet saved).
     */
    @AccessChecked("DAO: find of MerlinTemplateDao (admins and users/groups with access) for a stored template")
    @PostMapping("exportExcelTemplate")
    fun exportExcelTemplate(@Valid @RequestBody postData: PostData<MerlinTemplate>): ResponseEntity<*> {
        val dto = postData.data
        checkSelectAccess(dto)
        val workbook = merlinTemplateDefinitionHandler.writeTemplateDefinitionWorkbook(dto)
        return RestUtils.downloadFile(workbook.first, workbook.second)
    }

    /**
     * Analyzes the Word® template of the posted (unsaved) form, e.g. after a Word® template or an Excel template
     * definition was uploaded or deleted: the variables of the form are kept, the ones found in the template are
     * added.
     */
    @AccessChecked("DAO: find of MerlinTemplateDao (admins and users/groups with access)")
    @PostMapping("analyze")
    fun analyze(@Valid @RequestBody postData: PostData<MerlinTemplate>): VariablesResult {
        val dto = postData.data
        checkSelectAccess(dto)
        restoreDependsOn(dto)
        analyze(dto)
        return VariablesResult(dto)
    }

    /**
     * The user wants to apply the changes of a variable ([MerlinTemplate.currentVariable] of the posted form, a
     * variable with an unknown id is added). Nothing is saved: the result replaces the variables of the form.
     */
    @AccessChecked("No data: transforms the posted variables, nothing is read or saved")
    @PostMapping("variable/update")
    fun updateVariable(@Valid @RequestBody postData: PostData<MerlinTemplate>): VariablesResult {
        val dto = postData.data
        restoreDependsOn(dto)
        val currentVariable =
            dto.currentVariable ?: throw IllegalArgumentException("No current variable found. Nothing to update.")
        val dest = findVariable(dto, currentVariable.id) ?: MerlinVariable().also {
            // A new variable:
            it.id = dto.findNextId()
            dto.variables.add(it)
        }
        val oldName = dest.name
        val dependsOn = currentVariable.dependsOnName?.let { name -> dto.variables.find { it.name == name } }
        val id = dest.id
        dest.copyFrom(currentVariable)
        dest.id = id
        dest.defined = true // All variables of a template are defined (see MerlinHandler.getDto).
        dest.dependsOn = dependsOn
        if (oldName.isNotEmpty() && currentVariable.name != oldName) {
            // Rename also the master of dependent variables:
            (dto.variables + dto.dependentVariables).filter { it.dependsOnName == oldName }.forEach {
                it.dependsOn = dest
            }
        }
        dto.lastVariableUpdate = Date() // Mark dto as newer than Excel template
        dto.reorderVariables()
        dto.currentVariable = null
        return VariablesResult(dto)
    }

    /**
     * The user wants to remove a variable ([MerlinTemplate.currentVariable] of the posted form). Nothing is saved:
     * the result replaces the variables of the form.
     */
    @AccessChecked("No data: transforms the posted variables, nothing is read or saved")
    @PostMapping("variable/delete")
    fun deleteVariable(@Valid @RequestBody postData: PostData<MerlinTemplate>): VariablesResult {
        val dto = postData.data
        restoreDependsOn(dto)
        val dest = findVariable(dto, dto.currentVariable?.id)
            ?: throw IllegalArgumentException("No current variable found. Nothing to delete.")
        if (dto.variables.remove(dest) || dto.dependentVariables.remove(dest)) {
            dto.lastVariableUpdate = Date() // Mark dto as newer than Excel template
        }
        dto.reorderVariables()
        dto.currentVariable = null
        return VariablesResult(dto)
    }

    /**
     * Throws an access exception, if the posted template is a stored one, which the user may not read.
     */
    private fun checkSelectAccess(dto: MerlinTemplate) {
        dto.id?.let { baseDao.find(it) ?: throw IllegalArgumentException("Template #$it not found.") }
    }

    /**
     * The client posts the master of a dependent variable by name ([MerlinVariable.dependsOnName]), the object
     * references are restored here.
     */
    private fun restoreDependsOn(dto: MerlinTemplate) {
        (dto.variables + dto.dependentVariables).forEach { variable ->
            variable.dependsOn = variable.dependsOnName?.let { name -> dto.variables.find { it.name == name } }
        }
    }

    private fun findVariable(dto: MerlinTemplate, id: Int?): MerlinVariable? {
        id ?: return null
        return dto.variables.find { it.id == id } ?: dto.dependentVariables.find { it.id == id }
    }

    internal fun buildAccessGroupAsString(dto: MerlinTemplate) {
        if (dto.accessGroups.isNullOrEmpty()) {
            dto.accessGroupsAsString = "-"
            return
        }
        dto.accessGroupsAsString = groupsAsString(dto.accessGroups)
        val accessGroupUsers =
            groupService.getGroupUsers(User.toLongArray(dto.accessGroupIds)).joinToString { it.displayName }
        dto.accessGroupsAsString += ": $accessGroupUsers"
    }
}
