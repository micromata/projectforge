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

package org.projectforge.rest.multiselect

import com.fasterxml.jackson.annotation.JsonProperty
import de.micromata.merlin.excel.ExcelCell
import de.micromata.merlin.utils.ReplaceUtils
import jakarta.servlet.http.HttpServletRequest
import mu.KotlinLogging
import org.projectforge.business.user.service.UserService
import org.projectforge.common.extensions.capitalize
import org.projectforge.common.logging.LogSubscription
import org.projectforge.datatransfer.DataTransferBridge
import org.projectforge.framework.i18n.translate
import org.projectforge.framework.i18n.translateMsg
import org.projectforge.framework.persistence.api.BaseDao
import org.projectforge.framework.persistence.api.IdObject
import org.projectforge.framework.persistence.user.entities.PFUserDO
import org.projectforge.framework.time.PFDateTime
import org.projectforge.framework.utils.NumberFormatter
import org.projectforge.menu.MenuItem
import org.projectforge.menu.MenuItemTargetType
import org.projectforge.model.rest.RestPaths
import org.projectforge.rest.admin.LogViewerPageRest
import org.projectforge.rest.config.RestUtils
import org.projectforge.rest.core.*
import org.projectforge.rest.dto.FormLayoutData
import org.projectforge.rest.dto.PostData
import org.projectforge.ui.*
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import java.io.Serializable
import kotlin.reflect.KMutableProperty

private val log = KotlinLogging.logger {}

/**
 * Base class of mass updates after multi selection.
 */
abstract class AbstractMultiSelectedPage<T> : AbstractDynamicPageRest() {
    @Autowired
    protected lateinit var userService: UserService

    @Autowired
    private lateinit var dataTransferBridge: DataTransferBridge

    class MultiSelection {
        private var _selectedIds: Collection<Serializable>? = null

        @get:JsonProperty
        var selectedIds: Collection<Serializable>?
            get() = _selectedIds
            set(value) {
                // Convert all numeric types (Int, Integer, etc.) to Long for consistency
                // ProjectForge entity IDs are always Long
                // Jackson may deserialize numbers as Int by default
                _selectedIds = value?.map { id ->
                    when (id) {
                        is Long -> id
                        is Number -> id.toLong()  // Handles Int, Integer, Short, etc.
                        is String -> id.toLongOrNull() ?: id
                        else -> id
                    } as Serializable
                }
            }
    }

    protected open fun getId(obj: T): Long {
        if (obj is IdObject<*>) {
            return obj.id as Long
        }
        throw NotImplementedError("Please override getId(T).")
    }

    /**
     * If not a standard react page (e. g. Wicket-Page), modify this variable. The standard list and multi-selection-page
     * is auto-detected by [PagesResolver] with parameter [pageRestClass].
     */
    protected open val listPageUrl: String
        get() = PagesResolver.getListPageUrl(pagesRest::class.java, absolute = true)

    abstract fun getTitleKey(): String

    protected lateinit var pagesRest: AbstractEntityRest<*, *, *>

    /**
     * Create log subscription, if the user should view the log messages. At default it's disabled.
     */
    protected open fun ensureUserLogSubscription(): LogSubscription? {
        return null
    }

    private val downloadFileSupport =
        DownloadFileSupport(
            expiringSessionAttribute = "${this::class.java.name}.downloadFile",
            downloadExpiryMinutes = 5
        )

    /**
     * Should be set for i18n translations of Excel-Export.
     */
    open val layoutContext: LayoutContext? = null

    // ------------------------------------------------------------------------------------------
    // The layout free protocol, for a client that renders the page itself
    // ------------------------------------------------------------------------------------------

    /**
     * Everything a hand built mass update page needs: which fields it may change, how many entries are
     * selected, and what those add up to.
     *
     * The layout free counterpart of [getForm], which answers the same information as a `UILayout` of
     * rows, inputs, checkboxes and buttons. Same relation as `{entity}/listMeta` to `initialList` (see
     * [MultiSelectMetaData]).
     */
    @GetMapping("meta")
    fun requestMeta(request: HttpServletRequest): MultiSelectMetaData {
        val selectedIds = MultiSelectionSupport.getRegisteredSelectedEntityIds(request, pagesRest::class.java)
        val registeredIds = MultiSelectionSupport.getRegisteredEntityIds(request, pagesRest::class.java)
        val lc = layoutContext ?: LayoutContext(pagesRest.baseDao.doClass)
        return MultiSelectMetaData(
            title = translate(getTitleKey()),
            selectedCount = selectedIds?.size ?: 0,
            registeredCount = registeredIds?.size ?: 0,
            fields = fieldDeclarations().map { resolveFieldMeta(lc, it) },
            listPage = listPageUrl,
            maxMassUpdate = BaseDao.MAX_MASS_UPDATE,
            info = infoMessageKey()?.let { translate(it) },
            statistics = getStatistics(selectedIds),
            statisticsData = getStatisticsData(selectedIds),
        )
    }

    /**
     * The selected entries themselves, as the rows of the entity's list.
     *
     * So a hand built page can *show* what a mass update is about to change, not only how many entries
     * that is ([requestMeta] answers the count). The client cannot answer this itself: the ids are kept
     * across a change of the list's filter, so the selection is the union over several filter runs while
     * the list only ever holds what the current filter matched.
     *
     * Read only, and no session state of its own - the ids come from where [select] put them, the rows
     * from the list rest (see [AbstractEntityRest.getResultSetByIds]). Nothing selected answers an empty
     * result set.
     */
    @GetMapping("selectedList")
    fun requestSelectedList(request: HttpServletRequest): ResultSet<*> {
        val selectedIds = MultiSelectionSupport.getRegisteredSelectedEntityIds(request, pagesRest::class.java)
        return pagesRest.getResultSetByIds(request, selectedIds)
    }

    /**
     * Runs the mass update and answers what it did.
     *
     * The layout free counterpart of [massUpdate], which answers the same counters and errors wrapped in
     * a `UILayout` of markdown alerts. A failure the user can fix - nothing selected, nothing to do, two
     * actions on one field - stays an HTTP 406 with `validationErrors`, exactly as everywhere else in
     * this app.
     */
    @PostMapping("update")
    fun update(
        request: HttpServletRequest,
        @RequestBody params: Map<String, MassUpdateParameter>,
    ): ResponseEntity<*> {
        val selectedIds = MultiSelectionSupport.getRegisteredSelectedEntityIds(request, pagesRest::class.java)
        val massUpdateContext = object : MassUpdateContext<T>(params.toMutableMap()) {
            override fun getId(obj: T): Long {
                return this@AbstractMultiSelectedPage.getId(obj)
            }
        }
        handleClientMassUpdateCall(request, massUpdateContext)
        massUpdate(request, selectedIds, massUpdateContext)?.let { return it }
        val changedFields = changedFieldsOf(massUpdateContext)
        storeProtocol(request, massUpdateContext, changedFields)
        return ResponseEntity.ok(
            MassUpdateResult(
                modifiedCounter = massUpdateContext.modifiedCounter,
                unmodifiedCounter = massUpdateContext.unmodifiedCounter,
                errorCounter = massUpdateContext.errorCounter,
                resultMessage = massUpdateContext.resultMessage,
                errors = massUpdateContext.errorMessages.map { MassUpdateError(it.identifier, it.message) },
                downloadUrl = "${getRestPath()}/download",
                changedFields = changedFields,
            )
        )
    }

    /**
     * Stores the ticked subset of the registered ids and answers where the mass update page lives.
     *
     * The layout free counterpart of [selected], which answers the same url as a redirect
     * `ResponseAction`. A hand built client knows its own route, but posts here all the same: the
     * selection is session state, and [requestMeta] reads it from there.
     */
    @PostMapping("select")
    fun select(request: HttpServletRequest, @RequestBody selection: MultiSelection?): MultiSelectNavigation {
        MultiSelectionSupport.registerSelectedEntityIds(request, pagesRest::class.java, selection?.selectedIds)
        return MultiSelectNavigation(
            url = PagesResolver.getDynamicPageUrl(this::class.java, absolute = true),
            selectedCount = selection?.selectedIds?.size ?: 0,
        )
    }

    /**
     * Drops the selection and answers where the user came from - the layout free counterpart of
     * [handleCancelUrl].
     */
    @GetMapping("cancel")
    fun cancel(request: HttpServletRequest): MultiSelectNavigation {
        return MultiSelectNavigation(url = MultiSelectionSupport.clear(request, pagesRest) ?: listPageUrl)
    }

    /**
     * The fields the update acted on, translated - what the Excel protocol is described by.
     *
     * Only the ones with exactly one action: a field the user filled but combined with a second action is
     * an error the run never got past (see [checkParamHasAction]).
     */
    private fun changedFieldsOf(massUpdateContext: MassUpdateContext<T>): List<String> {
        val params = massUpdateContext.massUpdateParams
        return params.filter { checkParamHasAction(params, it.value, it.key) }.values.map { translate(it.displayName) }
    }

    /**
     * Writes the Excel protocol of a run into the download slot and into the user's data transfer box -
     * what the run is documented by, no matter which of the two endpoints triggered it.
     */
    private fun storeProtocol(
        request: HttpServletRequest,
        massUpdateContext: MassUpdateContext<T>,
        changedFields: List<String>,
    ) {
        val excel = MultiSelectionExcelExport.export(massUpdateContext, this)
        val filename =
            ReplaceUtils.encodeFilename("${translate(getTitleKey())}_${PFDateTime.now().format4Filenames()}.xlsx", true)
        downloadFileSupport.storeDownloadFile(request, filename, excel)
        val message = StringBuilder()
        message.appendLine(massUpdateContext.resultMessage)
        message.append(translate("massUpdate.fields.changed")).append(": ")
        message.append(changedFields.joinToString())
        dataTransferBridge.putFileInUsersInBox(filename, excel, description = message.toString())
    }

    /**
     * The fields this page may update, in the order they are shown - the layout free counterpart of the
     * `createAndAddFields` calls in [fillForm].
     *
     * Empty at default, so a page that only serves the `UILayout` path keeps working; a page migrated to
     * a hand built frontend declares them here and [fillForm] renders the same list (see
     * `RechnungMultiSelectedPageRest`).
     */
    protected open fun fieldDeclarations(): List<MassUpdateFieldDeclaration> {
        return emptyList()
    }

    /**
     * Message key of the note above the fields, as markdown - the `UIAlert` a page adds at the end of
     * [fillForm].
     */
    protected open fun infoMessageKey(): String? {
        return null
    }

    /**
     * What the selected entries add up to, as markdown - the invoice statistics.
     *
     * Null at default. The shape is the entity's own (`RechnungsStatistik.asMarkdown`), which is why this
     * is text rather than numbers: it is a summary a reader reads, not a value anything computes with.
     */
    protected open fun getStatistics(selectedIds: Collection<Serializable>?): String? {
        return null
    }

    /**
     * The same summary as [getStatistics], as values rather than as markdown - for a client that renders
     * its own statistics line.
     *
     * Null at default, and served *next to* the markdown rather than instead of it: the `UILayout` form
     * has nowhere to put a number but a `UIAlert`, so it needs the pre-rendered text, while a hand built
     * page needs the numbers - only they can be formatted in the user's locale and currency, and the
     * markdown carries `<span style="color:blue">` for its colours, which no next page renders.
     *
     * Typed as [Any] because the shape is the entity's own (`InvoiceStatistics` for the invoice) and this
     * class has nothing in common to say about it; the page that declares the field set also knows what
     * its statistics look like (see `PageDef.massUpdate`).
     */
    protected open fun getStatisticsData(selectedIds: Collection<Serializable>?): Any? {
        return null
    }

    /**
     * Resolves what a declaration leaves out from the entity itself, so the layout free answer and the
     * `UILayout` say the same thing about a field.
     *
     * The value property in particular must not be derived by the client: which property of a
     * [MassUpdateParameter] a value goes into is this class's mapping (see `createInputFieldRow`), and a
     * second copy of it in the frontend would silently drop values the day a type is added.
     */
    private fun resolveFieldMeta(lc: LayoutContext, declaration: MassUpdateFieldDeclaration): MassUpdateFieldMeta {
        val field = declaration.field
        val el = LayoutUtils.buildLabelInputElement(lc, field, declaration.minLengthOfTextArea)
        val elementInfo = ElementsRegistry.getElementInfo(lc, field)
        val dataType = (el as? UIInput)?.dataType
        val isString = el is UITextArea || (el is UIInput && el.dataType == UIDataType.STRING)
        @Suppress("UNCHECKED_CAST")
        val values = (el as? UISelect<String>)?.values
        return MassUpdateFieldMeta(
            field = field,
            valueProperty = valuePropertyOf(el, dataType),
            // Translated here, not passed on: `LayoutUtils.setLabels` puts the *key* into `element.label`
            // and the `UILayout` path translates it on its way out (`processAllElements`) - which this
            // layout free answer does not go through, so an untranslated key would reach the client.
            label = ((el as? UILabelledElement)?.label ?: elementInfo?.i18nKey)?.let { translate(it) },
            dataType = dataType ?: if (el is UISelect<*>) UIDataType.STRING else null,
            maxLength = elementInfo?.maxLength,
            rows = (el as? UITextArea)?.rows,
            values = values,
            // The default of the `UILayout` path as well: a field the entity requires cannot be emptied.
            deleteOption = declaration.showDeleteOption ?: (elementInfo?.required != true),
            replaceOption = declaration.showReplaceOption != false && isString,
            // A text area offers appending; so does any field a page explicitly presets it for
            // (`showAppendOption`), even where the entity lost its length and the element fell back to a
            // single line input (e. g. `bemerkung`, mapped on a superclass) - otherwise the preset would
            // arm an action the form never offers.
            appendOption = el is UITextArea || (isString && declaration.showAppendOption == true),
            appendPreset = declaration.showAppendOption == true,
        )
    }

    /**
     * Which property of the [MassUpdateParameter] a value goes into - the same mapping
     * [createInputFieldRow] applies to the element's id, and the reason it is answered rather than
     * derived by the client.
     */
    private fun valuePropertyOf(el: UIElement, dataType: UIDataType?): String {
        if (el !is UIInput) {
            // A select posts its value as text; an entity picker (task, user) posts an id.
            return "textValue"
        }
        return when (dataType) {
            UIDataType.DATE -> "localDateValue"
            UIDataType.AMOUNT, UIDataType.DECIMAL -> "decimalValue"
            // `longValue`, although [createInputFieldRow] says `intValue` for the same type: that is the
            // property [MassUpdateParameter] actually has, and the name in the deprecated path reaches
            // nothing. Not corrected there - the legacy frontend is the only caller and no page of it has
            // an integer field, so changing its element ids would be a risk with no gain.
            UIDataType.INT -> "longValue"
            UIDataType.KONTO, UIDataType.USER, UIDataType.TASK, UIDataType.GROUP, UIDataType.EMPLOYEE -> "id"
            UIDataType.BOOLEAN -> "booleanValue"
            UIDataType.TIMESTAMP -> "timestampValue"
            UIDataType.TIME -> "timeValue"
            else -> "textValue"
        }
    }

    // ------------------------------------------------------------------------------------------
    // The UILayout protocol (deprecated, see the layout free endpoints above)
    // ------------------------------------------------------------------------------------------

    @Deprecated("Use requestMeta (GET meta) instead: the layout free protocol of projectforge-next.")
    @GetMapping("dynamic")
    fun getForm(request: HttpServletRequest): FormLayoutData {
        val massUpdateData = mutableMapOf<String, MassUpdateParameter>()
        val variables = mutableMapOf<String, Any>()
        val layout = getLayout(request, massUpdateData, variables)
        return FormLayoutData(massUpdateData, layout, createServerData(request), variables)
    }

    @Deprecated("Use update (POST update) instead: the layout free protocol of projectforge-next.")
    @PostMapping("massUpdate")
    fun massUpdate(
        request: HttpServletRequest,
        @RequestBody postData: PostData<Map<String, MassUpdateParameter>>
    ): ResponseEntity<*> {
        val selectedIds = MultiSelectionSupport.getRegisteredSelectedEntityIds(request, pagesRest::class.java)

        val massUpdateContext = object : MassUpdateContext<T>(postData.data.toMutableMap()) {
            override fun getId(obj: T): Long {
                return this@AbstractMultiSelectedPage.getId(obj)
            }
        }
        handleClientMassUpdateCall(request, massUpdateContext)
        massUpdate(request, selectedIds, massUpdateContext)?.let { return it }
        storeProtocol(request, massUpdateContext, changedFieldsOf(massUpdateContext))
        val variables = mutableMapOf<String, Any>()

        val massUpdateData = postData.data.toMutableMap()
        val layout = getLayout(request, massUpdateData, variables, massUpdateContext)
        return ResponseEntity.ok(
            ResponseAction(targetType = TargetType.UPDATE)
                .addVariable("ui", layout)
                .addVariable("data", massUpdateData)
        )
    }

    @GetMapping("download")
    fun download(request: HttpServletRequest): ResponseEntity<*> {
        val downloadFile = downloadFileSupport.getDownloadFile(request)
            ?: return RestUtils.badRequest(translate("download.expired"))
        log.info("Downloading '${downloadFile.filename}' of size ${downloadFile.sizeHumanReadable}.")
        return RestUtils.downloadFile(downloadFile.filename, downloadFile.bytes)
    }

    /**
     * This rest service will be called on multi selection list pages, if the user wants to cancel the multi selection.
     * @return redirect url
     */
    @Deprecated("Use cancel (GET cancel) instead: the layout free protocol of projectforge-next.")
    @GetMapping(RestPaths.CANCEL_MULTI_SELECTION)
    fun handleCancelUrl(request: HttpServletRequest): ResponseAction {
        val callerUrl = MultiSelectionSupport.clear(request, pagesRest) ?: listPageUrl
        return ResponseAction(callerUrl, targetType = TargetType.REDIRECT)
    }


    /**
     * Used by TimesheetMultiSelectedPage for fixing kost2 issues. Does nothing at default.
     */
    protected open fun handleClientMassUpdateCall(
        request: HttpServletRequest,
        massUpdateContext: MassUpdateContext<T>
    ) {
    }

    /**
     * First excel columns for identification. Default is "Element|30", means db id of column width 11 and
     * identifier of length 30. Must match [getExcelIdentifierCells].
     */
    open fun customizeExcelIdentifierHeadCells(): Array<String> {
        return arrayOf("Element|30")
    }

    /**
     * First excel columns for identification. Default is id and identifier. Must match [customizeExcelIdentifierHeadCells].
     */
    open fun getExcelIdentifierCells(massUpdateObject: MassUpdateObject<T>): List<Any?> {
        return mutableListOf(massUpdateObject.identifier)
    }

    open fun handleValue(
        cell: ExcelCell,
        field: String,
        value: Any?,
    ): Boolean {
        return false
    }

    /**
     * @param cellValue may differ from value (e. g. this is the displayValue).
     * @return true, if the cell style was set or false, if nothing was done and the cell style could be set by [MultiSelectionExcelExport].
     */
    open fun handleCellStyle(cell: ExcelCell, field: String, value: Any?, cellValue: Any?): Boolean {
        return false
    }

    /**
     * Field translation is used by Excel export. Returns translation of field from LayoutContext, if available in this
     * class, or capitalized field name itself at default.
     * You may use [getFieldTranslation] with param [LayoutContext] for auto translation of known fields in your derived fun.
     */
    open fun getFieldTranslation(field: String): String {
        ElementsRegistry.getElementInfo(layoutContext, field)?.i18nKey?.let {
            return translate(it)
        }
        return field.capitalize()
    }

    fun massUpdate(
        request: HttpServletRequest,
        selectedIds: Collection<Serializable>?,
        massUpdateContext: MassUpdateContext<T>
    ): ResponseEntity<*>? {
        if (selectedIds.isNullOrEmpty()) {
            return showNoEntriesValidationError()
        }
        if (selectedIds.size > BaseDao.MAX_MASS_UPDATE) {
            return showValidationErrors(
                ValidationError(
                    translateMsg(
                        BaseDao.MAX_MASS_UPDATE_EXCEEDED_EXCEPTION_I18N,
                        BaseDao.MAX_MASS_UPDATE
                    )
                )
            )
        }
        val massUpdateData = massUpdateContext.massUpdateParams
        var nothingToDo = true
        val validationErrors = mutableListOf<ValidationError>()
        massUpdateData.forEach { (field, param) ->
            if (checkParamHasAction(massUpdateData, param, field, validationErrors)) {
                nothingToDo = false
            }
        }
        if (!validationErrors.isEmpty()) {
            return showValidationErrors(*validationErrors.toTypedArray())
        }
        if (nothingToDo) {
            return showNothingToDoValidationError()
        }

        proceedMassUpdate(request, selectedIds, massUpdateContext)?.let { responseEntity ->
            return responseEntity
        }
        if (massUpdateContext.nothingDone) {
            return showNoEntriesValidationError()
        }
        return null
    }

    protected fun checkParamHasAction(
        params: Map<String, MassUpdateParameter>,
        param: MassUpdateParameter,
        field: String,
        validationErrors: MutableList<ValidationError>
    ): Boolean {
        param.error?.let { message ->
            validationErrors.add(ValidationError(translate(message), "$field.textValue"))
            validationErrors.add(ValidationError("${translate(message)}: $field"))
            return false
        }
        return checkParamHasAction(params, param, field)
    }

    /**
     * @params Supply all params for complexer checks (e.g. taskAndKost2 has to look at parameter task and kost2).
     */
    protected open fun checkParamHasAction(
        params: Map<String, MassUpdateParameter>,
        param: MassUpdateParameter,
        field: String,
    ): Boolean {
        return param.hasAction
    }

    /**
     * @return null to handle ResponseEntity result by this class. If ResponseEntity is returned, it will be used.
     */
    protected abstract fun proceedMassUpdate(
        request: HttpServletRequest,
        selectedIds: Collection<Serializable>,
        massUpdateContext: MassUpdateContext<T>,
    ): ResponseEntity<*>?

    /**
     * Calls #proceedMassUpdateUserField
     * @param params The param is get by property name of this given map.
     */
    protected fun proceedMassUpdateUserField(
        params: Map<String, MassUpdateParameter>,
        property: KMutableProperty<PFUserDO?>,
        obj: Any,
    ) {
        proceedMassUpdateUserField(params[property.name], property, obj)
    }

    protected fun proceedMassUpdateUserField(
        param: MassUpdateParameter?,
        property: KMutableProperty<PFUserDO?>,
        obj: Any,
    ) {
        param ?: return
        if (param.delete == true) {
            param.id.let { userId ->
                if (userId == null || property.getter.call(obj)?.id == userId) {
                    property.setter.call(obj, null)
                }
            }
        } else {
            param.id?.let { userId ->
                property.setter.call(obj, userService.find(userId, false))
            }
        }
    }

    /**
     * Builds the form as a `UILayout`.
     *
     * Deprecated along with [getForm], but not annotated: it is abstract, and every page of this kind is
     * still served to the legacy frontend, so the annotation would only warn in six overrides that have
     * no alternative yet. A page migrated to a hand built frontend declares the same fields in
     * [fieldDeclarations] and keeps this one until its legacy page is gone.
     */
    abstract fun fillForm(
        request: HttpServletRequest,
        layout: UILayout,
        massUpdateData: MutableMap<String, MassUpdateParameter>,
        selectedIds: Collection<Serializable>?,
        variables: MutableMap<String, Any>,
    )

    protected fun getLayout(
        request: HttpServletRequest,
        massUpdateData: MutableMap<String, MassUpdateParameter>,
        variables: MutableMap<String, Any>,
        massUpdateContext: MassUpdateContext<T>? = null,
    ): UILayout {
        val layout = UILayout(getTitleKey())

        val selectedIds = MultiSelectionSupport.getRegisteredSelectedEntityIds(request, pagesRest::class.java)
        val formattedSize = NumberFormatter.format(selectedIds?.size)
        if (selectedIds.isNullOrEmpty()) {
            layout.add(UIAlert("massUpdate.error.noEntriesSelected", color = UIColor.DANGER))
        } else {
            layout.add(
                UIAlert(
                    "'${translateMsg("massUpdate.entriesFound", formattedSize)}",
                    color = UIColor.SUCCESS
                )
            )
        }

        fillForm(request, layout, massUpdateData, selectedIds, variables)

        layout.add(UIAlert(message = "massUpdate.info", color = UIColor.INFO))
        layout.add(
            UIButton.createCancelButton(
                ResponseAction(
                    RestResolver.getRestUrl(this::class.java, RestPaths.CANCEL_MULTI_SELECTION),
                    targetType = TargetType.GET,
                ),
                title = translate("stop")
            )
        )
        if (!MultiSelectionSupport.getRegisteredEntityIds(request, pagesRest::class.java).isNullOrEmpty()) {
            layout.add(
                UIButton.createBackButton(
                    ResponseAction(
                        PagesResolver.getMultiSelectionPageUrl(pagesRest::class.java, absolute = true),
                        targetType = TargetType.REDIRECT
                    ),
                    title = "massUpdate.changeSelection",
                )
            )
        }
        if (!selectedIds.isNullOrEmpty()) {
            layout.add(
                UIButton.createDefaultButton(
                    id = "execute",
                    title = "execute",
                    responseAction = ResponseAction(
                        url = "${getRestPath()}/massUpdate",
                        targetType = TargetType.POST
                    ),
                    confirmMessage = translateMsg("massUpdate.confirmQuestion", formattedSize),
                )
            )
        }
        massUpdateContext?.let { stats ->
            if (stats.errorCounter > 0) {
                val sb = StringBuilder()
                sb.appendLine("'*${stats.resultMessage}*")
                sb.appendLine()
                sb.appendLine("| # | ${translate("massUpdate.error.table.element")} | ${translate("massUpdate.error.table.message")}    |")
                    .appendLine("| --: | :-- | :-- |")
                stats.errorMessages.forEachIndexed { index, error ->
                    sb.appendLine("| ${index + 1} | ${error.identifier} | ${error.message} |")
                }
                layout.add(
                    UIAlert(
                        sb.toString(),
                        title = "massUpdate.error.table.title",
                        color = UIColor.DANGER,
                        markdown = true
                    )
                )
            } else if (stats.total > 0) {
                layout.add(UIAlert(message = "'${stats.resultMessage}"))
            } else {
                // Do nothing.
            }
        }
        downloadFileSupport.getDownloadFile(request)?.let { downloadFile ->
            val download = DownloadFileSupport.Download(downloadFile)
            variables["download"] = download
            layout.add(
                UIRow().add(
                    downloadFileSupport.createDownloadFieldset(
                        "massUpdate.excel.download",
                        "${getRestPath()}/download",
                        download,
                        useDataObject = false,
                    )
                )
            )
        }
        ensureUserLogSubscription()?.let { logSubscription ->
            layout.add(
                MenuItem(
                    "logViewer",
                    i18nKey = "plugins.merlin.viewLogs",
                    url = PagesResolver.getDynamicPageUrl(
                        LogViewerPageRest::class.java,
                        id = logSubscription.id
                    ),
                    type = MenuItemTargetType.REDIRECT,
                )
            )
        }
        LayoutUtils.process(layout)
        return layout
    }

    @Deprecated("Use select (POST select) instead: the layout free protocol of projectforge-next.")
    @PostMapping(URL_PATH_SELECTED)
    fun selected(
        request: HttpServletRequest,
        @RequestBody selectedIds: MultiSelection?
    ): ResponseEntity<*> {
        MultiSelectionSupport.registerSelectedEntityIds(request, pagesRest::class.java, selectedIds?.selectedIds)
        return ResponseEntity.ok(
            ResponseAction(
                targetType = TargetType.REDIRECT,
                url = PagesResolver.getDynamicPageUrl(this::class.java, absolute = true)
            )
        )
    }

    /**
     * @param minLengthOfTextArea See [LayoutUtils.buildLabelInputElement]
     * @param showDeleteOption If set, controls whether the delete checkbox is shown (overrides default behavior)
     * @param showReplaceOption If set to false, hides the replace text input (default is to show for text fields)
     */
    protected fun createInputFieldRow(
        lc: LayoutContext,
        field: String,
        massUpdateData: MutableMap<String, MassUpdateParameter>,
        minLengthOfTextArea: Int = LayoutUtils.DEFAULT_MIN_LENGTH_OF_TEXT_AREA,
        showDeleteOption: Boolean? = null,
        showReplaceOption: Boolean? = null,
    ): UIRow {
        val el = LayoutUtils.buildLabelInputElement(lc, field, minLengthOfTextArea)
        if (el is UIInput) {
            el.id = when (el.dataType) {
                UIDataType.DATE -> "$field.localDateValue"
                UIDataType.AMOUNT, UIDataType.DECIMAL -> "$field.decimalValue"
                UIDataType.INT -> "$field.intValue"
                UIDataType.KONTO, UIDataType.USER, UIDataType.TASK, UIDataType.GROUP, UIDataType.EMPLOYEE -> field
                UIDataType.BOOLEAN -> "$field.booleanValue"
                UIDataType.TIMESTAMP -> "$field.timestampValue"
                UIDataType.TIME -> "$field.timeValue"
                else -> "$field.textValue"
            }
            el.required = false //
        } else if (el is IUIId) {
            el.id = "$field.textValue"
        }
        if (el is UILabelledElement) {
            el.tooltip = null
        }
        val elementInfo = ElementsRegistry.getElementInfo(lc, field)
        return createInputFieldRow(
            field,
            el,
            massUpdateData,
            showDeleteOption = showDeleteOption ?: (elementInfo?.required != true),
            showReplaceOption = showReplaceOption,
        )
    }

    protected fun createInputFieldRow(
        field: String,
        el: UIElement,
        massUpdateData: MutableMap<String, MassUpdateParameter>,
        showDeleteOption: Boolean = false,
        showReplaceOption: Boolean? = null,
        myOptions: List<UIElement>? = null,
        displayName: String? = null,
    ): UIRow {
        val useDisplayName = displayName ?: if (el is UILabelledElement) el.label ?: field else field
        val param = massUpdateData[field] ?: MassUpdateParameter(field, useDisplayName)
        param.delete = false
        massUpdateData[field] = param
        UIRow().let { row ->
            row.add(UICol(md = 7).add(el))
            val optionsGroup = UIInlineGroup()
            row.add(UICol(md = 5).add(optionsGroup))

            if (showDeleteOption) {
                optionsGroup.add(
                    UICheckbox(
                        "$field.delete",
                        label = "massUpdate.field.checkbox4deletion",
                        tooltip = "massUpdate.field.checkbox4deletion.info",
                    )
                )
            }
            // Show replace option only if showReplaceOption is true (default) or null (auto-detect)
            if (showReplaceOption != false && (el is UITextArea || (el is UIInput && el.dataType == UIDataType.STRING))) {
                optionsGroup.add(
                    UIInput(
                        "$field.replaceText",
                        label = "massUpdate.field.replace",
                        tooltip = "massUpdate.field.replace.info"
                    )
                )
            }
            if (el is UITextArea) {
                optionsGroup.add(
                    UICheckbox(
                        "$field.append",
                        label = "massUpdate.field.checkbox4appending",
                        tooltip = "massUpdate.field.checkbox4appending.info"
                    )
                )
            }
            myOptions?.let { options ->
                options.forEach { optionsGroup.add(it) }
            }
            return row
        }
    }

    /**
     * @param minLengthOfTextArea See [LayoutUtils.buildLabelInputElement]
     * @param showAppendOption If true, the append checkbox will be preset (without function for non-text-area-fields)
     * @param showDeleteOption If set, controls whether the delete checkbox is shown (overrides default behavior)
     * @param showReplaceOption If set to false, hides the replace text input (default is to show for text fields)
     */
    protected fun createAndAddFields(
        lc: LayoutContext,
        massUpdateData: MutableMap<String, MassUpdateParameter>,
        container: IUIContainer,
        vararg fields: String,
        minLengthOfTextArea: Int = LayoutUtils.DEFAULT_MIN_LENGTH_OF_TEXT_AREA,
        showAppendOption: Boolean? = null,
        showDeleteOption: Boolean? = null,
        showReplaceOption: Boolean? = null,
    ) {
        fields.forEach { field ->
            if (massUpdateData[field] == null && showAppendOption == true) { // Only preset for the initial call:
                val displayName = ElementsRegistry.getElementInfo(lc, field)?.i18nKey ?: field
                ensureMassUpdateParam(massUpdateData, field, displayName).append = true
            }
            container.add(createInputFieldRow(lc, field, massUpdateData, minLengthOfTextArea, showDeleteOption, showReplaceOption))
        }
    }

    protected fun showNoEntriesValidationError(): ResponseEntity<ResponseAction> {
        return showValidationErrors(ValidationError(translate("massUpdate.error.noEntriesSelected")))
    }

    protected fun showNothingToDoValidationError(): ResponseEntity<ResponseAction> {
        return showValidationErrors(ValidationError(translate("massUpdate.error.nothingToDo")))
    }

    companion object {
        const val URL_PATH_SELECTED = "selected"
        const val URL_SUFFIX_SELECTED = "Selected"

        fun ensureMassUpdateParam(
            massUpdateData: MutableMap<String, MassUpdateParameter>,
            name: String,
            displayName: String,
        ): MassUpdateParameter {
            massUpdateData[name]?.let { return it }
            MassUpdateParameter(name, displayName = displayName).let {
                massUpdateData[name] = it
                return it
            }
        }

        fun processTextParameter(
            data: Any,
            property: String,
            params: Map<String, MassUpdateParameter>,
        ) {
            TextFieldModification.processTextParameter(data, property, params)
        }
    }
}
