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

package org.projectforge.rest.importer

import jakarta.servlet.http.HttpServletRequest
import jakarta.validation.Valid
import mu.KotlinLogging
import org.projectforge.common.extensions.format
import org.projectforge.framework.i18n.translate
import org.projectforge.framework.i18n.translateMsg
import org.projectforge.framework.utils.MarkdownBuilder
import org.projectforge.framework.utils.MarkdownBuilder.Color
import org.projectforge.framework.utils.NumberFormatter
import org.projectforge.model.rest.RestPaths
import org.projectforge.rest.core.AbstractDynamicPageRest
import org.projectforge.rest.core.PagesResolver
import org.projectforge.rest.core.RestResolver
import org.projectforge.rest.core.aggrid.AGGridSupport
import org.projectforge.rest.dto.FormLayoutData
import org.projectforge.rest.dto.PostData
import org.projectforge.rest.jobs.JobsMonitorPageRest
import org.projectforge.rest.multiselect.AbstractMultiSelectedPage
import org.projectforge.ui.*
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import kotlin.reflect.KProperty

private val log = KotlinLogging.logger {}

abstract class AbstractImportPageRest<O : ImportPairEntry.Modified<O>> : AbstractDynamicPageRest() {
    @Autowired
    protected lateinit var agGridSupport: AGGridSupport

    protected abstract fun callerPage(request: HttpServletRequest): String

    protected abstract fun getImportStorage(request: HttpServletRequest): ImportStorage<O>?
    protected abstract fun clearImportStorage(request: HttpServletRequest)

    /**
     * Don't forget to fill the [ImportStorage.importResult].
     * @return Job id (null, if no job was created).
     */
    protected abstract fun import(importStorage: ImportStorage<*>, selectedEntries: List<ImportPairEntry<O>>): Int?

    abstract val title: String

    protected open val creatCsvSettingsHelpText: Boolean = false

    protected fun createFormLayoutData(
        request: HttpServletRequest,
        importStorage: ImportStorage<*>?
    ): FormLayoutData {
        val layout = createLayout(request, importStorage)
        val data = importStorage?.info
        val formLayoutData = FormLayoutData(data, layout, createServerData(request))
        importStorage?.let { storage ->
            val entries = createListEntries(storage, data?.displayOptions ?: ImportStorage.DisplayOptions())
            formLayoutData.variables = mapOf("entries" to (entries))
        }
        return formLayoutData
    }

    fun createLayout(
        request: HttpServletRequest,
        importStorage: ImportStorage<*>?,
    ): UILayout {
        val data = importStorage?.info
        val layout = UILayout(title)
        if (importStorage != null) {
            layout.uid = "layout${importStorage.hashCode()}"
        }
        val title = buildString {
            append(importStorage?.title ?: translate("import.title"))
            importStorage?.filename
                ?.takeIf { it.isNotBlank() }
                ?.let { append(" - $it") }
        }
        val fieldset = UIFieldset(title = title)
        layout.add(fieldset)
        val hasEntries = importStorage?.pairEntries?.isNotEmpty() == true
        if (!hasEntries) {
            fieldset.add(UIAlert("import.error.nothingToImport", color = UIColor.DANGER))
            fieldset.add(
                UIButton.createCancelButton(
                    ResponseAction(
                        RestResolver.getRestUrl(this::class.java, "cancel"),
                        targetType = TargetType.GET,
                    )
                )
            )
        } else {
            if (data != null) {
                val row = UIRow()
                fieldset.add(row)
                row.add(
                    UICol(md = 6)
                        .add(UIAlert("'${createStatsMarkdown(importStorage)}", color = UIColor.LIGHT, markdown = true))
                )
                if (data.totalNumber > 0) {
                    val col = UICol(md = 6)
                    row.add(col)
                    val checkboxGroup = col
                    // col.add(checkboxGroup)
                    checkboxGroup.add(UILabel("import.display.options"))
                    addCheckBoxIfNotZero(layout, checkboxGroup, "new", data.numberOfNewEntries)
                    addCheckBoxIfNotZero(layout, checkboxGroup, "deleted", data.numberOfDeletedEntries)
                    addCheckBoxIfNotZero(layout, checkboxGroup, "modified", data.numberOfModifiedEntries)
                    addCheckBoxIfNotZero(layout, checkboxGroup, "unmodified", data.numberOfUnmodifiedEntries)
                    addCheckBoxIfNotZero(layout, checkboxGroup, "faulty", data.numberOfFaultyEntries)
                    addCheckBoxIfNotZero(layout, checkboxGroup, "unknown", data.numberOfUnknownEntries)
                }
            }

            importStorage?.lastJobRun?.result?.asUIAlert?.let {
                layout.add(it)
            }

            if (importStorage?.pairEntries?.isNotEmpty() == true) {
                fieldset.add(
                    UIButton.createDefaultButton(
                        "reconcile",
                        ResponseAction(
                            RestResolver.getRestUrl(this::class.java, "reconcile"),
                            targetType = TargetType.POST,
                        ),
                        title = "jobs.import.action.reconcile",
                        tooltip = "jobs.import.action.reconcile.tooltip",
                    )
                )
            }

            val agGrid = UIAgGrid("entries", listPageTable = true)
            layout.add(agGrid)
            agGridSupport.prepareUIGrid4MultiSelectionListPage(
                request,
                layout,
                agGrid,
                this,
                pageAfterMultiSelect = this::class.java,
            )
            agGrid.handleCancelUrl = RestResolver.getRestUrl(this::class.java, "cancelAndGetUrl")
            agGrid.urlAfterMultiSelect = RestResolver.getRestUrl(this::class.java, "import")
            // agGrid.height = "window.screen.height - 400"
            val col = UIAgGridColumnDef.createCol(field = "statusAsString", width = 150, headerName = "status")
                .withTooltipField("error")
            col.cellRenderer = "importStatusCell" // Custom renderer for icon + tooltip
            agGrid.add(col)
            createListLayout(request, layout, agGrid)
            // Only show import button if data has been reconciled with database
            if (importStorage?.hasBeenReconciled == true) {
                agGrid.withMultiRowSelection()
                agGrid.multiSelectButtonTitle = translate("import")
                agGrid.multiSelectButtonConfirmMessage = translate("import.confirmMessage")
            }
            agGrid.paginationPageSize = 500
            agGrid.withGetRowClass(
                """if (params.node.data.status === 'NEW') {
             return 'ag-row-green';
           } else if (['DELETED', 'FAULTY', 'UNKNOWN', 'UNKNOWN_MODIFICATION'].includes(params.node.data.status)) {
             return 'ag-row-red';
           } else if (params.node.data.status === 'MODIFIED') {
             return 'ag-row-blue';
        }""".trimMargin()
            )
        }

        addSettingsInfo(layout, importStorage)

        LayoutUtils.process(layout)
        return layout
    }

    /**
     * Will be called if the user wants to see the encryption options.
     */
    @PostMapping(RestPaths.WATCH_FIELDS)
    fun watchFields(
        request: HttpServletRequest,
        @Valid @RequestBody postData: PostData<ImportStorageInfo>
    ): ResponseEntity<ResponseAction> {
        val data = postData.data
        val importStorage = getImportStorage(request)
            ?: return ResponseEntity.ok(ResponseAction(targetType = TargetType.NOTHING))
        return ResponseEntity.ok(
            ResponseAction(targetType = TargetType.UPDATE, merge = false)
                .addVariable("data", data)
                .addVariable("variables", mapOf("entries" to createListEntries(importStorage, data.displayOptions)))
        )
    }

    protected open fun createListEntries(
        importStorage: ImportStorage<*>,
        displayOptions: ImportStorage.DisplayOptions,
    ): List<ImportEntry<*>> {
        return importStorage.createEntries(displayOptions)
    }

    /**
     * Will be called if the user wants to import the selected entries.
     */
    @PostMapping("import")
    fun import(
        request: HttpServletRequest,
        @RequestBody multiSelection: AbstractMultiSelectedPage.MultiSelection?
    ): ResponseEntity<ResponseAction> {
        val importStorage = getImportStorage(request)
        if (importStorage == null) {
            log.error("No import storage given (expired?). Can't proceed any import here.")
            return ResponseEntity.ok(ResponseAction(targetType = TargetType.NOTHING))
        }
        val selectedIds = multiSelection?.selectedIds
        val selectedEntries = mutableListOf<ImportPairEntry<O>>()
        if (selectedIds != null) {
            importStorage.pairEntries.forEach { entry ->
                if (selectedIds.contains(entry.id)) {
                    // Only allow NEW, MODIFIED, and DELETED entries to be imported
                    // Filter out UNKNOWN, UNKNOWN_MODIFICATION, FAULTY, and UNMODIFIED
                    when (entry.status) {
                        ImportEntry.Status.NEW,
                        ImportEntry.Status.MODIFIED,
                        ImportEntry.Status.DELETED -> selectedEntries.add(entry)
                        else -> { /* Ignore other statuses */ }
                    }
                }
            }
        }
        importStorage.clearErrors()
        if (selectedEntries.isEmpty()) {
            importStorage.addError(translate("import.error.noEntrySelected"))
            // TODO: merge UPDATE
            return ResponseEntity.ok(ResponseAction(targetType = TargetType.NOTHING))
        }
        log.info { "User wants to import #${selectedEntries.size} entries..." }
        val jobId = import(importStorage, selectedEntries)
        if (jobId == null) {
            importStorage.addError("Internal error: can't create batch job.")
            // TODO: merge UPDATE
            return ResponseEntity.ok(ResponseAction(targetType = TargetType.NOTHING))
        }
        // Use current import page as caller for redirect after successful job completion
        val caller = PagesResolver.getDynamicPageUrl(this::class.java, absolute = true)
        return ResponseEntity.ok(
            ResponseAction(
                url = PagesResolver.getDynamicPageUrl(
                    JobsMonitorPageRest::class.java,
                    absolute = true,
                    params = mapOf("jobId" to jobId, "caller" to caller),
                ),
                targetType = TargetType.REDIRECT,
            )
        )
    }

    /**
     * Called from UIButton cancel above.
     */
    @GetMapping("cancel")
    fun cancel(request: HttpServletRequest): ResponseAction {
        val callerPage = callerPage(request) // must be called before clearImportStorage!
        clearImportStorage(request)
        return ResponseAction(callerPage)
    }

    /**
     * Called from UIButton reconcile above.
     */
    @PostMapping("reconcile")
    fun reconcile(
        request: HttpServletRequest,
        @Valid @RequestBody postData: PostData<ImportStorageInfo>,
    ): ResponseAction {
        val importStorage = getImportStorage(request) ?: return ResponseAction(targetType = TargetType.NOTHING)
        val data = postData.data
        importStorage.reconcileImportStorage()
        val layout = createLayout(request, importStorage)
        return ResponseAction(targetType = TargetType.UPDATE, merge = true)
            .addVariable("variables", mapOf("entries" to createListEntries(importStorage, data.displayOptions)))
            .addVariable("ui", layout)
    }

    /**
     * Called by DynamicListPageAgGrid
     */
    @GetMapping("cancelAndGetUrl")
    fun cancelAndGetUrl(request: HttpServletRequest): String {
        val callerPage = callerPage(request) // must be called before clearImportStorage!
        clearImportStorage(request)
        return callerPage
    }

    protected abstract fun createListLayout(request: HttpServletRequest, layout: UILayout, agGrid: UIAgGrid)

    protected fun addReadColumn(
        agGrid: UIAgGrid,
        lc: LayoutContext,
        property: KProperty<*>,
        width: Int? = null,
        wrapText: Boolean? = null,
        formatter: UIAgGridColumnDef.Formatter? = null,
        filter: Boolean? = null,
    ) {
        val field = property.name
        val col =
            UIAgGridColumnDef.createCol(
                lc,
                "read.$field",
                lcField = field,
                width = width,
                wrapText = wrapText,
                formatter = formatter,
                filter = filter,
            )
        col.cellRenderer = "diffCell"
        agGrid.add(col)
    }

    protected fun addStoredColumn(
        agGrid: UIAgGrid,
        lc: LayoutContext,
        property: KProperty<*>,
        width: Int? = null,
        wrapText: Boolean? = null,
        formatter: UIAgGridColumnDef.Formatter? = null,
    ) {
        val field = property.name
        agGrid.add(lc, "stored.$field", lcField = field, width = width, wrapText = wrapText, formatter = formatter)
    }

    protected fun addDiffColumn(
        agGrid: UIAgGrid,
        lc: LayoutContext,
        field: String,
        headerName: String,
        width: Int? = null,
        wrapText: Boolean = true,
        formatter: UIAgGridColumnDef.Formatter? = null,
    ): UIAgGridColumnDef {
        val col = UIAgGridColumnDef.createCol(
            lc, field,
            headerName = headerName,
            width = width,
            wrapText = wrapText,
            formatter = formatter
        )
        col.cellRenderer = "diffCell"
        agGrid.add(col)
        return col
    }

    protected fun addIfNotZero(
        md: MarkdownBuilder,
        i18nKey: String,
        number: Int,
        color: MarkdownBuilder.Color? = null,
    ) {
        if (number > 0) {
            md.appendPipedValue(i18nKey, NumberFormatter.format(number), color)
        }
    }

    protected fun addCheckBoxIfNotZero(
        layout: UILayout,
        row: IUIContainer,
        id: String,
        number: Int,
    ) {
        if (number > 0) {
            val fieldId = "displayOptions.$id"
            row.add(UICheckbox(fieldId, label = "import.entry.status.$id", inline = true))
            layout.watchFields.add(fieldId)
        }
    }

    companion object {
        fun getSessionAttributeName(importPageRest: Class<*>): String {
            return "${importPageRest.name}.importStorage"
        }

        fun createSettingsHelp(importSettings: ImportSettings?): UIAlert {
            val sb = StringBuilder()
            sb.appendLine(translate("import.help.settings.info"))
            sb.appendLine("```")
            if (importSettings?.charSet == null) {
                sb.appendLine("# Uncomment the following line for other coding than UTF-8")
                sb.appendLine("#encoding=iso-8859-15")
            }
            if (importSettings != null) {
                sb.appendLine("encoding=${importSettings.charSet.name()}")
                importSettings.allFieldNames.forEach { fieldName ->
                    if (!arrayOf("id", "created", "lastUpdate", "deleted", "importSettings").contains(fieldName)) {
                        val fieldSettings = importSettings.getFieldSettings(fieldName)
                        sb.append("$fieldName=")
                        if (fieldSettings != null) {
                            sb.appendLine(fieldSettings.getSettingsAsString(true))
                        } else {
                            sb.appendLine()
                        }
                    }
                }
            }
            sb.appendLine("# Example of field 'birthday', found by header column:")
            sb.appendLine("#columnfield=[header column(s)]|:[format]")
            sb.appendLine("#birthday=birthday*|*born*|:dd.MM.yyyy|:dd.MM.yy")
            sb.appendLine("# header column(s) are mapped case-insensitive, wildchars '*' for text and '?' for single char are supported.")
            sb.appendLine("```")
            return UIAlert(sb.toString(), title = "import.help.settings.title", markdown = true, color = UIColor.LIGHT)
        }
    }

    private fun addSettingsInfo(container: IUIContainer, importStorage: ImportStorage<*>?) {
        val md = MarkdownBuilder()
            .h3(translate("import.info.detectedColumns"))
            .beginTable(translate("import.field.name"), translate("import.field.mapping"))
        if (importStorage != null) {
            importStorage.detectedColumns.entries.forEach {
                md.row(it.value.label, it.key)
            }
            md.appendLine()
                .h3(translate("import.info.unknownColumns"))
                .appendLine(importStorage.unknownColumns.joinToString())
            container.add(UIAlert("'$md", color = UIColor.WARNING, markdown = true))
            if (creatCsvSettingsHelpText) {
                container.add(createSettingsHelp(importStorage.importSettings))
            }
        }
    }

    private fun createStatsMarkdown(importStorage: ImportStorage<*>): String {
        val data = importStorage.info
        val md = MarkdownBuilder()
        md.appendPipedValue("import.stats.total", NumberFormatter.format(data.totalNumber))
        addIfNotZero(md, "import.stats.new", data.numberOfNewEntries, MarkdownBuilder.Color.GREEN)
        addIfNotZero(
            md,
            "import.stats.deleted",
            data.numberOfDeletedEntries,
            MarkdownBuilder.Color.RED,
        )
        addIfNotZero(
            md,
            "import.stats.modified",
            data.numberOfModifiedEntries,
            MarkdownBuilder.Color.BLUE,
        )
        addIfNotZero(md, "import.stats.unmodified", data.numberOfUnmodifiedEntries)
        addIfNotZero(
            md,
            "import.stats.faulty",
            data.numberOfFaultyEntries,
            MarkdownBuilder.Color.RED,
        )
        addIfNotZero(
            md,
            "import.stats.unknown",
            data.numberOfUnknownEntries,
            MarkdownBuilder.Color.RED,
        )
        add(md, "errors", importStorage.errorList, Color.RED)
        add(md, "warnings", importStorage.warningList, Color.ORANGE)
        return md.toString()
    }

    private fun add(md: MarkdownBuilder, type: String, messages: List<String>?, color: Color?) {
        if (messages.isNullOrEmpty()) {
            return
        }
        md.appendLine().h3(translate(type), color)
        val maxMessagesToShow = 10
        messages.take(maxMessagesToShow).forEach { msg -> md.appendListItem(msg, color) }
        if (messages.size > maxMessagesToShow) {
            val remainingMsgs = messages.size - maxMessagesToShow
            md.appendListItem(translateMsg("$type.remaining", remainingMsgs.format()), color)
        }
    }
}
