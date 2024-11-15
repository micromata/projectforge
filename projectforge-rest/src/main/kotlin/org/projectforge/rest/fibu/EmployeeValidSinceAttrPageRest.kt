/////////////////////////////////////////////////////////////////////////////
//
// Project ProjectForge Community Edition
//         www.projectforge.org
//
// Copyright (C) 2001-2024 Micromata GmbH, Germany (www.micromata.com)
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

package org.projectforge.rest.fibu

import jakarta.servlet.http.HttpServletRequest
import org.projectforge.business.fibu.EmployeeService
import org.projectforge.business.fibu.EmployeeStatus
import org.projectforge.business.fibu.EmployeeValidSinceAttrDO
import org.projectforge.business.fibu.EmployeeValidSinceAttrType
import org.projectforge.framework.i18n.I18nKeyAndParams
import org.projectforge.framework.i18n.translate
import org.projectforge.framework.i18n.translateMsg
import org.projectforge.rest.config.Rest
import org.projectforge.rest.core.AbstractDynamicPageRest
import org.projectforge.rest.core.RestResolver
import org.projectforge.rest.dto.EmployeeValidSinceAttr
import org.projectforge.rest.dto.FormLayoutData
import org.projectforge.rest.dto.PostData
import org.projectforge.ui.*
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.*

/**
 * Dialog for registering a new token or modifying/deleting an existing one.
 */
@RestController
@RequestMapping("${Rest.URL}/employeeValidSinceAttr")
class EmployeeValidSinceAttrPageRest : AbstractDynamicPageRest() {
    class ResponseData(
        var annualLeaveEntries: List<EmployeeValidSinceAttr>? = null,
        var statusEntries: List<EmployeeValidSinceAttr>? = null,
    )

    @Autowired
    private lateinit var employeeService: EmployeeService

    @GetMapping("dynamic")
    fun getForm(
        request: HttpServletRequest,
        @RequestParam("id") idString: String?,
        @RequestParam("employeeId") employeeId: Long?,
        @RequestParam("type") typeString: String?,
    ): FormLayoutData {
        val type = EmployeeValidSinceAttrType.safeValueOf(typeString)
        val id = idString?.toLongOrNull()
        requiredFields(id, employeeId, type)
        val data = if (id!! > 0) {
            EmployeeValidSinceAttr(employeeService.findValidSinceAttr(id, type!!))
        } else {
            EmployeeValidSinceAttr(employeeId = employeeId, type = type)
        }
        val title = if (type == EmployeeValidSinceAttrType.STATUS) {
            "fibu.employee.status"
        } else {
            "fibu.employee.urlaubstage"
        }
        val lc = LayoutContext(EmployeeValidSinceAttrDO::class.java)
        val layout = UILayout(title)
        layout.add(lc, "validSince")
        if (type == EmployeeValidSinceAttrType.STATUS) {
            layout.add(
                UISelect<EmployeeStatus>(
                    "value",
                    label = "fibu.employee.status",
                    required = false,
                ).buildValues(
                    EmployeeStatus::class.java
                )
            )

        } else {
            layout.add(UIInput("value", dataType = UIDataType.INT, label = "fibu.employee.urlaubstage"))
        }
        layout.add(lc, "comment")
        if (id < 0) {
            // New entry
            layout.addAction(
                UIButton.createAddButton(
                    responseAction = ResponseAction(
                        url = RestResolver.getRestUrl(this::class.java, "insert"),
                        targetType = TargetType.POST,
                    )
                )
            )
        } else {
            layout.addAction(
                UIButton.createDeleteButton(
                    layout,
                    responseAction = ResponseAction(
                        RestResolver.getRestUrl(this::class.java, "delete"),
                        targetType = TargetType.POST,
                    ),
                )
            )
            layout.addAction(
                UIButton.createUpdateButton(
                    responseAction = ResponseAction(
                        RestResolver.getRestUrl(this::class.java, "update"),
                        targetType = TargetType.POST
                    ),
                )
            )
        }

        layout.addTranslations("cancel", "yes")
        LayoutUtils.process(layout)

        return FormLayoutData(data, layout, createServerData(request))
    }

    @PostMapping("delete")
    fun delete(@RequestBody postData: PostData<EmployeeValidSinceAttr>): ResponseEntity<*> {
        requiredFields(postData.data)
        val dto = postData.data
        employeeService.markValidSinceAttrAsDeleted(employeeId = dto.employeeId, attrId = dto.id)
        return closeModal(dto)
    }

    /**
     * Updates only the displayName.
     */
    @PostMapping("update")
    fun update(@RequestBody postData: PostData<EmployeeValidSinceAttr>): ResponseEntity<ResponseAction> {
        requiredFields(postData.data)
        val dto = postData.data
        validate(dto)?.let { return it }
        val attrDO = dto.cloneAsDO()
        employeeService.updateValidSinceAttr(employeeId = dto.employeeId, attrDO = attrDO)
        return closeModal(dto)
    }

    /**
     * Updates only the displayName.
     */
    @PostMapping("insert")
    fun insert(@RequestBody postData: PostData<EmployeeValidSinceAttr>): ResponseEntity<ResponseAction> {
        requiredFields(postData.data, idNull = true)
        validate(postData.data)?.let { return it }
        val dto = postData.data
        val attrDO = dto.cloneAsDO()
        employeeService.insert(employeeId = dto.employeeId!!, attrDO = attrDO)
        return closeModal(dto)
    }

    private fun closeModal(dto: EmployeeValidSinceAttr): ResponseEntity<ResponseAction> {
        val responseAction = ResponseAction(targetType = TargetType.CLOSE_MODAL, merge = true)
        if (dto.type == EmployeeValidSinceAttrType.STATUS) {
            val attrs = employeeService.selectStatusEntries(dto.employeeId!!).map { EmployeeValidSinceAttr(it) }
            responseAction.addVariable("data", ResponseData(statusEntries = attrs))
        } else {
            val attrs =
                employeeService.selectAnnualLeaveDayEntries(dto.employeeId!!).map { EmployeeValidSinceAttr(it) }
            responseAction.addVariable("data", ResponseData(annualLeaveEntries = attrs))
        }
        return ResponseEntity.ok().body(responseAction)

    }

    private fun requiredFields(dto: EmployeeValidSinceAttr, idNull: Boolean = false) {
        requiredFields(dto.id, dto.employeeId, dto.type, idNull)
    }

    private fun requiredFields(
        id: Long?,
        employeeId: Long?,
        type: EmployeeValidSinceAttrType?,
        idNull: Boolean = false,
    ) {
        if (idNull) {
            require(id == null) { "Can't insert EmployeeValidSinceAttr entry with given id." }
        } else {
            requireNotNull(id) { "Can't update/delete EmployeeValidSinceAttr entry without id." }
        }
        requireNotNull(employeeId) { "Can't update EmployeeValidSinceAttr entry without employeeId." }
        requireNotNull(type) { "Can't update EmployeeValidSinceAttr entry without type." }
    }

    private fun validate(dto: EmployeeValidSinceAttr): ResponseEntity<ResponseAction>? {
        val validationErrors = mutableListOf<ValidationError>()
        if (dto.type == EmployeeValidSinceAttrType.STATUS) {
            val status = EmployeeStatus.safeValueOf(dto.value)
            if (status == null) {
                validationErrors.add(
                    ValidationError.createFieldRequired(
                        fieldId = "value",
                        fieldName = translate("fibu.employee.status")
                    )
                )
            }
        } else {
            val annualLeave = dto.value?.toIntOrNull()
            if (annualLeave == null) {
                validationErrors.add(
                    ValidationError.createFieldRequired(
                        fieldId = "value",
                        fieldName = translate("fibu.employee.urlaubstage")
                    )
                )
            } else {
                if (annualLeave < 0 || annualLeave > 100) {
                    validationErrors.add(
                        ValidationError(
                            translateMsg("validation.error.range.integerOutOfRange", "0", "100"),
                            fieldId = "value",
                        )
                    )
                }
            }
        }
        if (dto.validSince == null) {
            validationErrors.add(
                ValidationError.createFieldRequired(
                    EmployeeValidSinceAttrDO::class.java,
                    fieldId = "validSince",
                )
            )
        } else {
            employeeService.validate(dto.cloneAsDO())?.let { msg ->
                validationErrors.add(ValidationError.create(msg))
            }
        }
        if (validationErrors.isNotEmpty()) {
            return ResponseEntity(
                ResponseAction(validationErrors = validationErrors),
                HttpStatus.NOT_ACCEPTABLE
            )
        }
        return null
    }
}
