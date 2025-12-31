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

package org.projectforge.rest

import de.micromata.merlin.excel.ExcelSheet
import de.micromata.merlin.excel.ExcelWorkbook
import jakarta.servlet.http.HttpServletRequest
import mu.KotlinLogging
import org.apache.poi.ss.usermodel.CellType
import org.apache.poi.ss.usermodel.Row
import org.apache.poi.ss.usermodel.VerticalAlignment
import org.projectforge.Constants
import org.projectforge.common.FormatterUtils
import org.projectforge.excel.ExcelUtils
import org.projectforge.framework.i18n.translate
import org.projectforge.framework.i18n.translateMsg
import org.projectforge.framework.persistence.user.api.ThreadLocalUserContext
import org.projectforge.framework.time.PFDateTime
import org.projectforge.framework.utils.MarkdownBuilder
import org.projectforge.menu.Menu
import org.projectforge.menu.MenuItem
import org.projectforge.menu.builder.FavoritesMenuCreator
import org.projectforge.menu.builder.FavoritesMenuReaderWriter
import org.projectforge.menu.builder.MenuCreator
import org.projectforge.menu.builder.MenuCreatorContext
import org.projectforge.rest.config.Rest
import org.projectforge.rest.config.RestUtils
import org.projectforge.rest.core.AbstractDynamicPageRest
import org.projectforge.rest.core.ExpiringSessionAttributes
import org.projectforge.rest.core.RestResolver
import org.projectforge.rest.dto.FormLayoutData
import org.projectforge.ui.*
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.*
import org.springframework.web.multipart.MultipartFile

private val log = KotlinLogging.logger {}

/**
 * For customizing the personal menu (favorites).
 */
@RestController
@RequestMapping("${Rest.URL}/myMenu")
class MyMenuPageRest : AbstractDynamicPageRest() {
    @Autowired
    private lateinit var menuCreator: MenuCreator

    @Autowired
    private lateinit var favoritesMenuCreator: FavoritesMenuCreator

    @GetMapping("dynamic")
    fun getForm(request: HttpServletRequest): FormLayoutData {
        val layout = createLayout(request)
        return FormLayoutData(null, layout, createServerData(request))
    }

    private fun createLayout(
        request: HttpServletRequest,
        result: String? = null,
        hasError: Boolean = false,
    ): UILayout {
        val importData = getImportData(request)
        val layout = UILayout("user.myMenu.title")
        layout.add(UIAlert("user.myMenu.description", markdown = true, color = UIColor.INFO))
            .add(
                UIDropArea(
                    "user.myMenu.dropArea",
                    uploadUrl = RestResolver.getRestUrl(MyMenuPageRest::class.java, "import")
                )
            )
        val markdown = importData?.asMarkDown() ?: result
        val error =
            importData?.hasErrors ?: hasError // Must be placed after asMarkDown()! asMarkDown sets error type as well.
        if (markdown != null) {
            layout.add(UIAlert(markdown, markdown = true, color = if (error) UIColor.DANGER else UIColor.SUCCESS))
        }
        if (importData?.readyToImport == true) {
            layout.addAction(
                UIButton.createCancelButton(
                    responseAction = ResponseAction(
                        RestResolver.getRestUrl(this::class.java, "cancel"),
                        targetType = TargetType.POST
                    ),
                )
            )
            layout.addAction(
                UIButton.createUpdateButton(
                    responseAction = ResponseAction(
                        RestResolver.getRestUrl(this::class.java, "update"),
                        targetType = TargetType.POST
                    ),
                    default = true
                )
            )
        }
        layout.addAction(
            UIButton.createDownloadButton(
                responseAction = ResponseAction(
                    RestResolver.getRestUrl(
                        this.javaClass,
                        "exportExcel"
                    ), targetType = TargetType.DOWNLOAD
                ),
                default = true
            )
        )
        if (hasImportData(request)) {
            UIButton.createUpdateButton(
                responseAction = ResponseAction(
                    RestResolver.getRestUrl(
                        this.javaClass,
                        "update"
                    ), targetType = TargetType.UPDATE
                ),
                default = true
            )
        }
        LayoutUtils.process(layout)
        return layout
    }

    @GetMapping("exportExcel")
    fun exportExcel(request: HttpServletRequest): ResponseEntity<*> {
        log.info { "Exporting Excel sheet for customizing the personal menu." }
        val mainMenu = menuCreator.build(MenuCreatorContext(ThreadLocalUserContext.requiredLoggedInUser))
        val favoritesMenu = favoritesMenuCreator.getFavoriteMenu()
        val workbook = ExcelUtils.prepareWorkbook()
        val wrapTextStyle = workbook.createOrGetCellStyle("wrap")
        workbook.createOrGetCellStyle(ExcelUtils.HEAD_ROW_STYLE).verticalAlignment = VerticalAlignment.CENTER
        wrapTextStyle.wrapText = true
        createSheet(
            workbook,
            translate("user.myMenu.excel.sheet.favorites"),
            favoritesMenu,
            "user.myMenu.excel.sheet.favorites.info",
        )
        createSheet(
            workbook, translate("user.myMenu.excel.sheet.mainMenu"), mainMenu,
            "user.myMenu.excel.sheet.mainMenu.info",
        )
        createSheet(
            workbook,
            translate("user.myMenu.excel.sheet.default"),
            favoritesMenuCreator.createDefaultFavoriteMenu(),
            "user.myMenu.excel.sheet.default.info",
        )
        createSheet(
            workbook,
            translate("user.myMenu.excel.sheet.backup"),
            favoritesMenu,
            "user.myMenu.excel.sheet.backup.info",
        )
        val ba = ExcelUtils.exportExcel(workbook)
        return RestUtils.downloadFile("MyMenu-${PFDateTime.now().format4Filenames()}.xlsx", ba)
    }

    /**
     * Save the imported version of the menu.
     */
    @PostMapping("update")
    fun update(request: HttpServletRequest): ResponseEntity<*> {
        log.info { "Saving the imported personal menu." }
        val importData = getImportData(request)
        if (importData == null || !importData.readyToImport) {
            log.warn { "No import data found or not ready to import." }
            return result(request, translate("user.myMenu.error.notReadyToImport"), true)
        }
        val newMenu = importData.menu
        FavoritesMenuReaderWriter.storeAsUserPref(newMenu)
        clearImportData(request)
        return result(request, translate("user.myMenu.imported"))
    }

    /**
     * Save the imported version of the menu.
     */
    @PostMapping("cancel")
    fun cancel(request: HttpServletRequest): ResponseEntity<*> {
        log.info { "Cancelling the imported personal menu." }
        clearImportData(request)
        return result(request)
    }

    @PostMapping("import")
    fun import(
        request: HttpServletRequest,
        @RequestParam("file") file: MultipartFile
    ): ResponseEntity<*> {
        val filename = file.originalFilename ?: "unknown"
        log.info {
            "User tries to upload menu configuration file: '$filename', size=${
                FormatterUtils.formatBytes(
                    file.size
                )
            }."
        }
        if (file.size > 1 * Constants.MB) {
            log.warn("Upload file size to big: ${file.size} > 100MB")
            throw IllegalArgumentException("Upload file size to big: ${file.size} > 1MB")
        }
        val importData = ImportData()
        file.inputStream.use { inputStream ->
            val workbook = ExcelWorkbook(inputStream, filename)
            val sheet = workbook.getSheet(translate("user.myMenu.excel.sheet.favorites"))
                ?: return result(
                    request,
                    translateMsg(
                        "user.myMenu.error.fileUpload.sheetNotFound",
                        translate("user.myMenu.excel.sheet.favorites")
                    ), true
                )
            val rowsIterator = sheet.dataRowIterator
            if (!rowsIterator.hasNext()) {
                return result(
                    request,
                    translateMsg(
                        "user.myMenu.error.fileUpload.sheetIsEmpty",
                        translate("user.myMenu.excel.sheet.favorites")
                    ), true
                )
            }
            rowsIterator.next().let { row ->
                if (getCellStringValue(row, 0) != "ProjectForge") {
                    return result(
                        request,
                        translateMsg(
                            "user.myMenu.error.fileUpload.unknownFirstCell",
                            translate("user.myMenu.excel.sheet.favorites")
                        ), true
                    )
                }
            }
            val allEntries =
                menuCreator.build(MenuCreatorContext(ThreadLocalUserContext.requiredLoggedInUser)).getAllDescendants()
            var mainRowData: RowData? = null
            while (rowsIterator.hasNext()) {
                val row = rowsIterator.next()
                val main = getCellStringValue(row, 0) // Text or null (if cell was blank).
                val sub = getCellStringValue(row, 1)  // Text or null (if cell was blank).
                if (main == null && sub == null) {
                    continue
                }
                if (main != null && sub != null) {
                    importData.add(
                        RowData(
                            main,
                            sub,
                            translate("user.myMenu.error.fileUpload.parentAndSub"),
                            Type.ERROR
                        )
                    )
                    continue
                }
                val isSubMenu = main.isNullOrBlank()
                if (mainRowData == null && isSubMenu) {
                    importData.add(
                        RowData(
                            main,
                            sub,
                            translate("user.myMenu.error.fileUpload.parentMissing"),
                            Type.ERROR
                        )
                    )
                    continue
                }
                val title = main ?: sub ?: ""
                val found = allEntries.find { (it.title == title || it.id == title) && it.isLeaf() }
                if (found != null) {
                    // Existing entry with link (leaf):
                    if (isSubMenu) {
                        importData.add(RowData(null, sub, type = Type.LINK).also {
                            it.menuItem = found
                        })
                        mainRowData?.type = Type.PARENT // Mark parent as parent, if it was a link before.
                        mainRowData?.menuItem = null
                    } else {
                        mainRowData = RowData(main, null, type = Type.LINK) // Link, might be parent later.
                        mainRowData.menuItem = found
                        importData.add(mainRowData)
                    }
                    continue
                }
                // No existing entry, should be Main entry with customized name:
                if (isSubMenu) {
                    importData.add(
                        RowData(
                            main,
                            sub,
                            translateMsg("user.myMenu.error.fileUpload.menuNotFound", sub),
                            Type.ERROR
                        )
                    )
                    continue
                }
                mainRowData = RowData(main, null, type = Type.PARENT)
                importData.add(mainRowData)
            }
        }
        val newMenu = Menu()
        var parent: MenuItem? = null
        importData.rowData.forEach { row ->
            if (row.type == Type.PARENT) {
                val menuItem = MenuItem(title = row.main!!)
                newMenu.add(menuItem)
                parent = menuItem
                row.menuItem = menuItem
            } else if (row.type == Type.LINK) {
                val menuItem = row.menuItem!!
                if (row.main != null) {
                    newMenu.add(menuItem)
                    parent = menuItem
                } else {
                    parent?.add(menuItem)
                }
            }
        }
        importData.menu = newMenu
        storeImportData(request, importData)
        return result(request)
    }

    private fun result(
        request: HttpServletRequest,
        text: String? = null,
        hasError: Boolean = false,
    ): ResponseEntity<*> {
        return ResponseEntity.ok(
            ResponseAction(targetType = TargetType.UPDATE)
                .addVariable("ui", createLayout(request, text, hasError = hasError))
        )
    }

    private fun getCellStringValue(row: Row, col: Int): String? {
        val cell = row.getCell(col) ?: return null
        val result = when (cell.cellType) {
            CellType.STRING -> cell.stringCellValue
            else -> null
        }
        return if (result.isNullOrBlank()) {
            null
        } else {
            result.trim()
        }
    }

    private fun createSheet(workbook: ExcelWorkbook, title: String, menu: Menu, info: String? = null): ExcelSheet {
        val headStyle = workbook.createOrGetCellStyle(ExcelUtils.HEAD_ROW_STYLE)
        workbook.createOrGetSheet(title).let { sheet ->
            sheet.setColumnWidth(0, 5 * 256)
            sheet.setColumnWidth(1, 35 * 256)
            sheet.setColumnWidth(2, 100 * 256)
            sheet.createRow().let { row ->
                sheet.setMergedRegion(0, 0, 0, 1, "ProjectForge").setCellStyle(headStyle)
                if (info != null) {
                    row.getCell(2).setCellValue(translate(info)).setCellStyle(workbook.createOrGetCellStyle("wrap"))
                }
            }
            menu.menuItems.forEach { item ->
                sheet.createRow().getCell(0).setCellValue(item.title)
                item.subMenu?.forEach { subItem ->
                    sheet.createRow().getCell(1).setCellValue(subItem.title)
                }
            }
            return sheet
        }
    }

    private fun storeImportData(request: HttpServletRequest, importData: ImportData) {
        // Store the menu up to 10 minutes:
        ExpiringSessionAttributes.setAttribute(request, MENU_SESSION_ATTRIBUTE, importData, 10)
    }

    private fun clearImportData(request: HttpServletRequest) {
        ExpiringSessionAttributes.removeAttribute(request, MENU_SESSION_ATTRIBUTE)
    }

    private fun getImportData(request: HttpServletRequest): ImportData? {
        return ExpiringSessionAttributes.getAttribute(request, MENU_SESSION_ATTRIBUTE, ImportData::class.java)
    }

    private fun hasImportData(request: HttpServletRequest): Boolean {
        return getImportData(request) != null
    }

    companion object {
        private val MENU_SESSION_ATTRIBUTE = "${MyMenuPageRest::class.java.name}.menu"
    }

    private class ImportData {
        var menu: Menu? = null
        val rowData = mutableListOf<RowData>()
        val hasErrors: Boolean
            get() = rowData.any { it.type == Type.ERROR }
        val readyToImport: Boolean
            get() = menu != null && !hasErrors

        fun add(rowData: RowData) {
            this.rowData.add(rowData)
        }

        fun asMarkDown(): String {
            val md = MarkdownBuilder()
            md.beginTable(translate("user.myMenu.mainMenu"), translate("user.myMenu.subMenu"), translate("comment"))
            rowData.forEach { it.appendMarkdown(md) }
            return md.toString()
        }
    }

    private enum class Type { PARENT, LINK, ERROR }
    private class RowData(val main: String?, val sub: String?, var remarks: String? = null, var type: Type) {
        var menuItem: MenuItem? = null
        fun appendMarkdown(md: MarkdownBuilder) {
            when (type) {
                Type.PARENT -> {
                    if (menuItem?.subMenu?.isNotEmpty() == true) {
                        md.append(main, MarkdownBuilder.Color.BLACK).append(" | | ")
                            .appendLine(translate("user.myMenu.excel.sheet.mainMenu"))
                    } else {
                        type = Type.ERROR
                        remarks = translate("user.myMenu.error.fileUpload.parentWithoutChilds")
                        appendError(md)
                    }
                }

                Type.LINK -> {
                    if (main != null) {
                        md.append(main, MarkdownBuilder.Color.BLUE).appendLine(" | | ")
                    } else {
                        md.append("| | ").append(sub, MarkdownBuilder.Color.BLUE).appendLine(" | ")
                    }
                }

                Type.ERROR -> {
                    appendError(md)
                }
            }
        }

        private fun appendError(md: MarkdownBuilder) {
            md.append("${main ?: "|"} | ${sub ?: ""} |")
            md.appendLine(remarks, MarkdownBuilder.Color.RED)
        }
    }
}
