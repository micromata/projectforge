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

package org.projectforge.rest

import de.micromata.merlin.excel.ExcelWorkbook
import jakarta.servlet.http.HttpServletRequest
import jakarta.validation.Valid
import mu.KotlinLogging
import org.projectforge.SystemStatus
import org.projectforge.business.ldap.GroupDOConverter
import org.projectforge.business.ldap.LdapGroupValues
import org.projectforge.business.ldap.LdapPosixGroupsUtils
import org.projectforge.business.ldap.LdapUserDao
import org.projectforge.business.login.Login
import org.projectforge.business.user.GroupDao
import org.projectforge.business.user.UserGroupCache
import org.projectforge.business.user.service.UserService
import org.projectforge.excel.ExcelUtils
import org.projectforge.framework.access.AccessChecker
import org.projectforge.framework.i18n.translate
import org.projectforge.framework.i18n.translateMsg
import org.projectforge.framework.persistence.api.MagicFilter
import org.projectforge.framework.persistence.api.QueryFilter
import org.projectforge.framework.persistence.api.impl.CustomResultFilter
import org.projectforge.framework.persistence.user.api.ThreadLocalUserContext
import org.projectforge.framework.persistence.user.entities.GroupDO
import org.projectforge.framework.time.DateHelper
import org.projectforge.model.rest.RestPaths
import org.projectforge.rest.config.Rest
import org.projectforge.rest.core.AbstractDTOPagesRest
import org.projectforge.rest.core.RestResolver
import org.projectforge.rest.core.getObjectList
import org.projectforge.rest.dto.Group
import org.projectforge.rest.dto.PostData
import org.projectforge.ui.*
import org.projectforge.ui.filter.UIFilterListElement
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.core.io.ByteArrayResource
import org.springframework.http.HttpHeaders
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController
import java.util.*

private val log = KotlinLogging.logger {}

@RestController
@RequestMapping("${Rest.URL}/group")
class GroupPagesRest : AbstractDTOPagesRest<GroupDO, Group, GroupDao>(
    GroupDao::class.java, "group.title",
    cloneSupport = CloneSupport.CLONE,
) {

    @Autowired
    private lateinit var accessChecker: AccessChecker

    @Autowired
    private lateinit var groupDOConverter: GroupDOConverter

    @Autowired
    private lateinit var ldapPosixGroupsUtils: LdapPosixGroupsUtils

    @Autowired
    private lateinit var ldapUserDao: LdapUserDao

    @Autowired
    private lateinit var userService: UserService

    @Autowired
    private lateinit var userGroupCache: UserGroupCache

    override fun transformFromDB(obj: GroupDO, editMode: Boolean): Group {
        val group = Group()
        group.copyFrom(obj)
        group.assignedUsers?.forEach {
            val user = userService.getUser(it.id)
            if (user != null) {
                it.username = user.username
                it.firstname = user.firstname
                it.lastname = user.lastname
            }
        }
        if (useLdapStuff) {
            groupDOConverter.readLdapGroupValues(obj.ldapValues)?.let { ldapGroupValues ->
                group.gidNumber = ldapGroupValues.gidNumber
            }
        }
        return group
    }

    override fun transformForDB(dto: Group): GroupDO {
        val groupDO = GroupDO()
        dto.copyTo(groupDO)
        //groupDao.setNestedGroups(getData(), form.nestedGroupsListHelper.getAssignedItems());
        dto.gidNumber.let { gidNumber ->
            if (gidNumber != null) {
                val values = LdapGroupValues()
                values.gidNumber = gidNumber
                val xml: String = groupDOConverter.getLdapValuesAsXml(values)
                groupDO.ldapValues = xml
            } else {
                groupDO.ldapValues = null
            }
        }
        return groupDO
    }

    override val classicsLinkListUrl: String
        get() = "wa/groupList"

    /**
     * LAYOUT List page
     */
    override fun createListLayout(
        request: HttpServletRequest,
        layout: UILayout,
        magicFilter: MagicFilter,
        userAccess: UILayout.UserAccess
    ) {
        userAccess.update = accessChecker.isLoggedInUserMemberOfAdminGroup
        val agGrid = agGridSupport.prepareUIGrid4ListPage(
            request,
            layout,
            magicFilter,
            this,
            userAccess = userAccess,
        )
            .add(lc, "name")
        if (userGroupCache.isUserMemberOfAdminGroup) {
            agGrid.add(lc, "organization", "description", wrapText = true)
        }
        agGrid.add(
            lc,
            "assignedUsers",
            formatter = UIAgGridColumnDef.Formatter.SHOW_LIST_OF_DISPLAYNAMES,
            wrapText = true
        )
        if (userGroupCache.isUserMemberOfAdminGroup && useLdapStuff) {
            agGrid.add(lc, "ldapValues")
        }

        if (userGroupCache.isUserMemberOfAdminGroup) {
            layout.excelExportSupported = true
        }
    }

    override fun addMagicFilterElements(elements: MutableList<UILabelledElement>) {
        elements.add(
            UIFilterListElement("type", label = translate("status"), defaultFilter = true, multi = false)
                .buildValues(GroupTypeFilter.TYPE::class.java)
        )
    }

    override fun preProcessMagicFilter(target: QueryFilter, source: MagicFilter): List<CustomResultFilter<GroupDO>> {
        val filters = mutableListOf<CustomResultFilter<GroupDO>>()
        val localGroupFilterEntry = source.entries.find { it.field == "type" }
        if (localGroupFilterEntry != null) {
            localGroupFilterEntry.synthetic = true
            val values = localGroupFilterEntry.value.values
            if (!values.isNullOrEmpty() && values.size == 1) {
                val value = values[0]
                try {
                    GroupTypeFilter.TYPE.valueOf(value).let {
                        filters.add(GroupTypeFilter(it))
                    }
                } catch (ex: IllegalArgumentException) {
                    log.warn { "Oups, can't convert '$value': ${ex.message}" }
                }
            }
        }
        return filters
    }

    @PostMapping("createGid")
    fun createGid(@Valid @RequestBody postData: PostData<Group>):
            ResponseAction {
        val data = postData.data
        data.gidNumber = ldapPosixGroupsUtils.nextFreeGidNumber
        return ResponseAction(targetType = TargetType.UPDATE)
            .addVariable("data", data)
    }

    override fun validate(validationErrors: MutableList<ValidationError>, dto: Group) {
        super.validate(validationErrors, dto)
        val group = GroupDO()
        dto.copyTo(group)
        if (baseDao.doesGroupnameAlreadyExist(group)) {
            validationErrors.add(
                ValidationError(
                    translate("group.error.groupnameAlreadyExists"),
                    fieldId = GroupDO::name.name,
                )
            )
        }
        dto.gidNumber?.let { gidNumber ->
            if (!ldapPosixGroupsUtils.isGivenNumberFree(dto.id ?: -1, gidNumber)) {
                validationErrors.add(
                    ValidationError(
                        translateMsg("ldap.gidNumber.alreadyInUse", ldapPosixGroupsUtils.nextFreeGidNumber),
                        fieldId = "gidNumber",
                    )
                )
            }
        }
    }

    /**
     * LAYOUT Edit page
     */
    override fun createEditLayout(dto: Group, userAccess: UILayout.UserAccess): UILayout {
        val layout = super.createEditLayout(dto, userAccess)
            .add(
                UIRow()
                    .add(
                        UICol(lg = 6)
                            .add(lc, "name", "localGroup")
                    )
                    .add(
                        UICol(lg = 6)
                            .add(lc, "organization", "groupOwner")
                    )
            )
            .add(UISelect.createUserSelect(lc, "assignedUsers", true, "group.assignedUsers"))
            .add(lc, "description")
        if (useLdapStuff) {
            val gidInput = UIInput(
                "gidNumber",
                label = "ldap.gidNumber",
                additionalLabel = "ldap.posixAccount",
                tooltip = "ldap.gidNumber.tooltip"
            )
            val fieldset = UIFieldset(title = "ldap")
            layout.add(fieldset)
            if (dto.gidNumber != null) {
                fieldset.add(gidInput)
            } else {
                val button = UIButton.createLinkButton(
                    id = "createGidNumber",
                    title = "create",
                    tooltip = "ldap.gidNumber.createDefault.tooltip",
                    responseAction = ResponseAction(
                        RestResolver.getRestUrl(
                            GroupPagesRest::class.java,
                            "createGid"
                        ), targetType = TargetType.POST
                    )
                )
                fieldset.add(UIRow().add(UICol().add(gidInput)).add(UICol().add(button)))
            }
        }
        dto.setEmails()
        layout.add(UIReadOnlyField("emails", label = "address.emails"))
        return LayoutUtils.processEditPage(layout, dto, this)
    }

    /**
     * Exports groups as Excel file.
     */
    @PostMapping(RestPaths.REST_EXCEL_SUB_PATH)
    fun exportAsExcel(@RequestBody filter: MagicFilter): ResponseEntity<*> {
        log.info("Exporting groups as Excel file.")
        accessChecker.checkIsLoggedInUserMemberOfAdminGroup()

        @Suppress("UNCHECKED_CAST")
        val list = getObjectList(this, baseDao, filter)
        ExcelWorkbook.createEmptyWorkbook(ThreadLocalUserContext.locale!!).use { workbook ->
            val sheet = workbook.createOrGetSheet(translate("group.title.heading"))
            val boldFont = ExcelUtils.createFont(workbook, "bold", bold = true)
            val boldStyle = workbook.createOrGetCellStyle("hr", font = boldFont)
            val wrapTextStyle = workbook.createOrGetCellStyle("wrap")
            wrapTextStyle.wrapText = true

            ExcelUtils.registerColumn(sheet, GroupDO::name, 20)
            ExcelUtils.registerColumn(sheet, GroupDO::localGroup)
            ExcelUtils.registerColumn(sheet, GroupDO::organization, 20)
            ExcelUtils.registerColumn(sheet, GroupDO::description, 50)
            sheet.registerColumn(translate("group.assignedUsers"), "assignedUsers").withSize(100)
            sheet.registerColumn(translate("group.owner"), "groupOwner").withSize(30)
            sheet.registerColumn(translate("address.emails"), "emails").withSize(100)
            if (useLdapStuff) {
                ExcelUtils.registerColumn(sheet, GroupDO::ldapValues, 50)
                sheet.registerColumn(translate("ldap.gidNumber"), "gidNumber").withSize(15)
            }

            ExcelUtils.addHeadRow(sheet, boldStyle)
            list.forEach { groupDO ->
                val group = Group()
                group.copyFrom(groupDO)
                val row = sheet.createRow()
                row.autoFillFromObject(group, "assignedUsers", "groupOwner", "emails", "gidNumber")

                row.getCell("assignedUsers")?.let {
                    it.setCellValue(group.assignedUsers?.joinToString { user -> user.displayName ?: "???" })
                    it.setCellStyle(wrapTextStyle)
                }

                row.getCell("groupOwner")?.let {
                    it.setCellValue(group.groupOwner?.displayName ?: "")
                    it.setCellStyle(wrapTextStyle)
                }

                row.getCell("emails")?.let {
                    group.setEmails()
                    it.setCellValue(group.emails ?: "")
                    it.setCellStyle(wrapTextStyle)
                }

                if (useLdapStuff) {
                    row.getCell("gidNumber")?.setCellValue(group.gidNumber?.toDouble() ?: 0.0)
                }

                ExcelUtils.getCell(row, GroupDO::description)?.setCellStyle(wrapTextStyle)
                ExcelUtils.getCell(row, GroupDO::ldapValues)?.setCellStyle(wrapTextStyle)
            }
            sheet.setAutoFilter()
            val filename = ("GroupList_${DateHelper.getDateAsFilenameSuffix(Date())}.xlsx")
            val resource = ByteArrayResource(workbook.asByteArrayOutputStream.toByteArray())
            return ResponseEntity.ok()
                .contentType(org.springframework.http.MediaType.parseMediaType("application/octet-stream"))
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=$filename")
                .body(resource)
        }
    }

    private val useLdapStuff: Boolean
        get() = SystemStatus.isDevelopmentMode() || (accessChecker.isLoggedInUserMemberOfAdminGroup && Login.getInstance()
            .hasExternalUsermanagementSystem() && ldapUserDao.isPosixAccountsConfigured)

    override val autoCompleteSearchFields = arrayOf("name", "organization")
}
