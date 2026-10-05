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

package org.projectforge.plugins.todo.rest

import jakarta.servlet.http.HttpServletRequest
import io.github.oshai.kotlinlogging.KotlinLogging
import org.projectforge.NextMigration
import org.projectforge.business.configuration.ConfigurationService
import org.projectforge.business.configuration.DomainService
import org.projectforge.common.i18n.UserException
import org.projectforge.framework.i18n.InternalErrorException
import org.projectforge.framework.i18n.translate
import org.projectforge.framework.i18n.translateMsg
import org.projectforge.framework.persistence.api.MagicFilter
import org.projectforge.framework.persistence.api.QueryFilter
import org.projectforge.framework.persistence.api.impl.CustomResultFilter
import org.projectforge.framework.persistence.user.api.ThreadLocalUserContext
import org.projectforge.plugins.todo.ToDoDO
import org.projectforge.plugins.todo.ToDoDao
import org.projectforge.plugins.todo.ToDoFavorite
import org.projectforge.plugins.todo.ToDoFavoritesService
import org.projectforge.plugins.todo.ToDoStatus
import org.projectforge.plugins.todo.ToDoType
import org.projectforge.plugins.todo.dto.ToDo
import org.projectforge.rest.config.Rest
import org.projectforge.rest.core.AbstractDTOEntityRest
import org.projectforge.rest.core.AccessChecked
import org.projectforge.rest.core.RestButtonEvent
import org.projectforge.rest.core.saveOrUpdate
import org.projectforge.rest.dto.PostData
import org.projectforge.ui.ResponseAction
import org.projectforge.ui.UIColor
import org.projectforge.ui.UILabelledElement
import org.projectforge.ui.AutoCompletion
import org.projectforge.ui.ValidationError
import org.projectforge.ui.filter.UIFilterBooleanElement
import org.projectforge.ui.filter.UIFilterElement
import org.projectforge.ui.filter.UIFilterListElement
import org.projectforge.ui.filter.UIFilterObjectElement
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.RestController

private val log = KotlinLogging.logger {}

/**
 * The layout-free REST endpoint of the to-dos (menu "To-dos"), serving the hand-built projectforge-next page
 * (see components/features/todo). Replaces the removed Wicket pages, whose behaviour it takes over:
 *
 * - a new to-do is reported by the logged-in user, opened, and of the type and priority of the last saved one,
 * - the assignee opening a to-do clears its recent flag (the menu badge counts the recent ones),
 * - a save notifies assignee and reporter by e-mail if asked for, and always for a new to-do or a changed
 *   assignee, status or deletion state ([ToDoDao.sendNotification]),
 * - the close action closes a to-do (with the comment the dialog edits),
 * - the user's templates ([TEMPLATES_PATH]).
 */
@RestController
@RequestMapping("${Rest.URL}/todo")
class ToDoEntityRest : AbstractDTOEntityRest<ToDoDO, ToDo, ToDoDao>(
    baseDaoClazz = ToDoDao::class.java,
    i18nKeyPrefix = "plugins.todo.title",
) {
    @Autowired
    private lateinit var configurationService: ConfigurationService

    @Autowired
    private lateinit var domainService: DomainService

    @Autowired
    private lateinit var toDoFavoritesService: ToDoFavoritesService

    /** What the notification rule compares a write with: the stored to-do before it. */
    private class StoredState(val assigneeId: Long?, val status: ToDoStatus?, val deleted: Boolean)

    /**
     * The stored state of the to-do of the current write request, from [transformForDB] (the last hook seeing the
     * stored to-do before the write) to [onAfterEdit]. A thread local because this rest service is a singleton
     * serving concurrent requests; null for a new to-do.
     */
    private val storedState = ThreadLocal<StoredState?>()

    /**
     * Takes over the recent flag from the stored to-do: the client can't post it (read only), and the dao only
     * sets it (on a change of somebody else but the assignee), a save never clears it.
     */
    override fun transformForDB(dto: ToDo): ToDoDO {
        val todo = ToDoDO()
        dto.copyTo(todo)
        val dbObj = dto.id?.let { baseDao.find(it, checkAccess = false) }
        todo.recent = dbObj?.recent ?: false
        storedState.set(dbObj?.let { StoredState(it.assigneeId, it.status, it.deleted) })
        return todo
    }

    /**
     * In edit mode, the assignee has now seen the to-do: its recent flag is cleared, as the Wicket edit page
     * did (not for restricted or demo users, who mustn't write).
     */
    override fun transformFromDB(obj: ToDoDO, editMode: Boolean): ToDo {
        if (editMode && obj.id != null && obj.recent && !accessChecker.isRestrictedOrDemoUser
            && obj.assigneeId == ThreadLocalUserContext.loggedInUserId
        ) {
            obj.recent = false
            baseDao.update(obj)
        }
        val todo = ToDo()
        todo.copyFrom(obj)
        if (editMode) {
            todo.mailConfigured = configurationService.isSendMailConfigured
        }
        return todo
    }

    override fun newBaseDO(request: HttpServletRequest?): ToDoDO {
        val todo = ToDoDO()
        toDoFavoritesService.getLastValues()?.let {
            todo.type = it.type
            todo.priority = it.priority
        }
        todo.reporter = ThreadLocalUserContext.loggedInUser
        todo.status = ToDoStatus.OPENED
        return todo
    }

    /** A new to-do is always edited, so the form needs to know whether it may offer the notification. */
    override fun newBaseDTO(request: HttpServletRequest?): ToDo {
        return transformFromDB(newBaseDO(request), editMode = true)
    }

    /**
     * Pinned: status, type, assignee and reporter. "Only recent": the to-dos of the logged-in user changed by
     * somebody else and not seen yet, as the filter of the Wicket list offered.
     *
     * Status and type are plain enum filters (they replace the ones derived from the search fields, see
     * `LayoutListFilterUtils.dedupById`); assignee and reporter are user pickers consumed in
     * [preProcessMagicFilter], replacing the free-text pills derived from the indexed user fields.
     */
    override fun addMagicFilterElements(elements: MutableList<UILabelledElement>) {
        elements.removeIf {
            it is UIFilterElement && USER_FILTERS.any { field -> it.id == field || it.id.startsWith("$field.") }
        }
        elements.add(
            UIFilterListElement("status", label = translate("plugins.todo.status"), defaultFilter = true)
                .buildValues(ToDoStatus::class.java)
        )
        elements.add(
            UIFilterListElement("type", label = translate("plugins.todo.type"), defaultFilter = true)
                .buildValues(ToDoType::class.java, addNullValue = true)
        )
        USER_FILTERS.forEach { field ->
            elements.add(
                UIFilterObjectElement(
                    field,
                    label = translate("plugins.todo.$field"),
                    autoCompletion = AutoCompletion.getAutoCompletion4Users(),
                ).also { it.defaultFilter = true }
            )
        }
        elements.add(
            UIFilterBooleanElement(ONLY_RECENT, label = translate("plugins.todo.status.onlyRecent"))
                .also { it.tooltip = translate("plugins.todo.status.onlyRecent.tooltip") }
        )
    }

    override fun preProcessMagicFilter(target: QueryFilter, source: MagicFilter): List<CustomResultFilter<ToDoDO>>? {
        // The picker sends the user as `value.id`, which the generic processor doesn't read.
        USER_FILTERS.forEach { field ->
            source.entries.find { it.field == field }?.let { entry ->
                entry.synthetic = true
                (entry.value.id ?: entry.value.value?.toLongOrNull())?.let {
                    target.add(QueryFilter.eq("$field.id", it))
                }
            }
        }
        source.entries.find { it.field == ONLY_RECENT }?.let { entry ->
            entry.synthetic = true
            if (entry.isTrueValue) {
                target.add(QueryFilter.eq("assignee.id", ThreadLocalUserContext.loggedInUserId!!))
                target.add(QueryFilter.eq("recent", true))
            }
        }
        return null
    }

    /**
     * The assignee is mandatory, as on the Wicket form. Checked here, because `required` of `@PropertyInfo`
     * doesn't reach a reference ([org.projectforge.ui.ElementsRegistry] applies it to columns only).
     */
    override fun validate(validationErrors: MutableList<ValidationError>, dto: ToDo) {
        super.validate(validationErrors, dto)
        if (dto.assignee?.id == null) {
            validationErrors.add(
                ValidationError(
                    translateMsg("validation.error.fieldRequired", translate("plugins.todo.assignee")),
                    fieldId = "assignee",
                )
            )
        }
    }

    /** The type and priority of the saved to-do are those the next new one starts with. */
    override fun onAfterSaveOrUpdate(request: HttpServletRequest, obj: ToDoDO, postData: PostData<ToDo>) {
        super.onAfterSaveOrUpdate(request, obj, postData)
        toDoFavoritesService.storeLastValues(obj)
    }

    /**
     * Sends the notification after a save, a deletion or an undeletion and attaches the warning of a failed
     * one. Here and not in [onAfterSaveOrUpdate], because a deletion doesn't pass that hook.
     */
    override fun onAfterEdit(
        request: HttpServletRequest,
        obj: ToDoDO,
        postData: PostData<ToDo>,
        event: RestButtonEvent
    ): ResponseAction {
        val stored = storedState.get()
        // Unconditionally, so nothing is left behind for the next request on this thread.
        storedState.remove()
        val message = if (event != RestButtonEvent.CANCEL && isNotificationRequired(obj, postData.data, stored)) {
            sendNotification(obj)
        } else {
            null
        }
        val responseAction = super.onAfterEdit(request, obj, postData, event)
        message?.let {
            responseAction.message = ResponseAction.Message(message = it, color = UIColor.WARNING)
        }
        return responseAction
    }

    /** The rule of the Wicket edit page: the checkbox, a new to-do, or a change of assignee, status or deletion. */
    private fun isNotificationRequired(obj: ToDoDO, dto: ToDo, stored: StoredState?): Boolean {
        return dto.sendNotification
                || stored == null
                || stored.assigneeId != obj.assigneeId
                || stored.status != obj.status
                || stored.deleted != obj.deleted
    }

    /**
     * Notifies assignee and reporter of the written to-do. A failing notification must never fail the write,
     * which is committed already, nor stay silent: the warning is returned for the response (see
     * `OrderEntityRest`). Also used by the mass update ([ToDoMultiSelectedPageRest]).
     *
     * @return The translated warning of a failed notification, or null.
     */
    fun sendNotification(obj: ToDoDO): String? {
        val url = domainService.getDomain(
            NextMigration.standardEditPage(category).replace(NextMigration.ID_PLACEHOLDER, "${obj.id}")
        )
        // The mail shows the names of the references, the written object carries id-only stubs.
        val todo = baseDao.find(obj.id, checkAccess = false) ?: return null
        try {
            baseDao.sendNotification(todo, url)
        } catch (ex: Exception) {
            log.error(ex) { "To-do #${obj.id} was saved, but sending the notification mail failed: ${ex.message}" }
            val reason = when (ex) {
                is UserException -> translateMsg(ex)
                is InternalErrorException -> translateMsg(ex.i18nKey, *(ex.params ?: emptyArray()))
                else -> ex.message
            }
            return translateMsg("plugins.todo.notification.error", reason ?: "")
        }
        return null
    }

    /**
     * Closes the to-do with the values of the form (the close dialog edits its comment), as the close button of
     * the Wicket edit page did. Being a change of the status, it notifies.
     */
    @AccessChecked("DAO: update access (saveOrUpdate)")
    @PostMapping("close")
    fun close(request: HttpServletRequest, @RequestBody postData: PostData<ToDo>): ResponseEntity<ResponseAction> {
        sessionCsrfService.validateCsrfToken(request, postData, "Close")?.let { return it }
        val todo = transformForDB(postData.data)
        todo.status = ToDoStatus.CLOSED
        return saveOrUpdate(request, baseDao, todo, postData, this, validate(todo, postData))
    }

    // ------------------------------------------------------------------------------------------
    // The user's templates
    // ------------------------------------------------------------------------------------------

    class NewTemplate(var name: String? = null, var todo: ToDo? = null)

    /** The names and ids of the user's templates. */
    @AccessChecked("own user only (user prefs of the logged-in user)")
    @GetMapping("$TEMPLATES_PATH/list")
    fun getTemplates(): Map<String, Any> {
        return mapOf(TEMPLATES to toDoFavoritesService.getList())
    }

    /** Stores the posted form values as new template. */
    @AccessChecked("own user only (user prefs of the logged-in user)")
    @PostMapping("$TEMPLATES_PATH/create")
    fun createTemplate(@RequestBody newTemplate: NewTemplate): Map<String, Any> {
        val favorite = ToDoFavorite(name = newTemplate.name)
        newTemplate.todo?.let { dto ->
            favorite.fillFrom(ToDoDO().also { dto.copyTo(it) })
        }
        toDoFavoritesService.create(favorite)
        return getTemplates()
    }

    /**
     * The values of the given template as a to-do, the references with their display names. Null values are no
     * part of the template: the client keeps the form's values for them.
     */
    @AccessChecked("own user only (user prefs of the logged-in user)")
    @GetMapping("$TEMPLATES_PATH/select")
    fun selectTemplate(@RequestParam("id") id: Long): ResponseEntity<ToDo> {
        val favorite = toDoFavoritesService.get(id) ?: return ResponseEntity.notFound().build()
        val todo = ToDoDO()
        favorite.copyTo(todo)
        return ResponseEntity.ok(ToDo().also { it.copyFrom(todo) })
    }

    @AccessChecked("own user only (user prefs of the logged-in user)")
    @GetMapping("$TEMPLATES_PATH/delete")
    fun deleteTemplate(@RequestParam("id") id: Long): Map<String, Any> {
        toDoFavoritesService.delete(id)
        return getTemplates()
    }

    @AccessChecked("own user only (user prefs of the logged-in user)")
    @GetMapping("$TEMPLATES_PATH/rename")
    fun renameTemplate(@RequestParam("id") id: Long, @RequestParam("newName") newName: String): Map<String, Any> {
        toDoFavoritesService.rename(id, newName)
        return getTemplates()
    }

    companion object {
        const val TEMPLATES_PATH = "templates"

        private const val TEMPLATES = "templates"

        private const val ONLY_RECENT = "onlyRecent"

        /** The pinned user filters, named after their property. */
        private val USER_FILTERS = listOf("assignee", "reporter")
    }
}
