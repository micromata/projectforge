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
import jakarta.servlet.http.HttpServletResponse
import jakarta.validation.Valid
import org.projectforge.business.fibu.EmployeeService
import org.projectforge.business.fibu.EmployeeValidityPeriodAttrDO
import org.projectforge.business.fibu.EmployeeValidityPeriodAttrType
import org.projectforge.rest.config.Rest
import org.projectforge.rest.core.AbstractDynamicPageRest
import org.projectforge.rest.core.ExpiringSessionAttributes
import org.projectforge.rest.core.PagesResolver
import org.projectforge.rest.core.RestResolver
import org.projectforge.rest.dto.Employee
import org.projectforge.rest.dto.EmployeeValidityPeriodAttr
import org.projectforge.rest.dto.FormLayoutData
import org.projectforge.rest.dto.PostData
import org.projectforge.ui.*
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.*

/**
 * Dialog for registering a new token or modifying/deleting an existing one.
 */
@RestController
@RequestMapping("${Rest.URL}/employeePeriodAttr")
class EmployeeValidityPeriodAttrPageRest : AbstractDynamicPageRest() {
    @Autowired
    private lateinit var employeeService: EmployeeService

    /**
     * @param id PK of the database entry.
     */
    @GetMapping("dynamic")
    fun getForm(
        request: HttpServletRequest,
        @RequestParam("id") idString: String?,
        @RequestParam("employeeId") employeeId: Long?,
        @RequestParam("type") typeString: String?,
    ): FormLayoutData {
        employeeId ?: throw IllegalArgumentException("employeeId is required.")
        val type = EmployeeValidityPeriodAttrType.safeValueOf(typeString)
            ?: throw IllegalArgumentException("type is required, but not given or unknown: $typeString.")
        val id = idString?.toLongOrNull()
        val data = if (id != null) {
            EmployeeValidityPeriodAttr(employeeService.findValidityPeriodAttr(id, type))
        } else {
            EmployeeValidityPeriodAttr()
        }
        val title = if (type == EmployeeValidityPeriodAttrType.STATUS) {
            "fibu.employee.status"
        } else {
            "fibu.employee.urlaubstage"
        }
        val lc = LayoutContext(EmployeeValidityPeriodAttrDO::class.java)
        val layout = UILayout(title)
        layout.add(lc, "validFrom", "value", "comment")
        if (id == null) {
            // New entry
            layout.addAction(
                UIButton.createAddButton(
                    responseAction = ResponseAction(
                        url = RestResolver.getRestUrl(this::class.java, "delete"),
                        targetType = TargetType.REDIRECT,
                    )
                )
            )
        } else {
            layout.addAction(
                UIButton.createUpdateButton(
                    responseAction = ResponseAction(
                        RestResolver.getRestUrl(this::class.java, "update"),
                        targetType = TargetType.POST
                    ),
                )
            )
            layout.addAction(
                UIButton.createDeleteButton(
                    layout,
                    responseAction = ResponseAction(
                        RestResolver.getRestUrl(this::class.java, "delete"),
                        targetType = TargetType.POST,
                    ),
                )
            )
        }

        layout.addTranslations("cancel", "yes")
        LayoutUtils.process(layout)

        return FormLayoutData(data, layout, createServerData(request))
    }

    @PostMapping("delete")
    fun delete(
        request: HttpServletRequest,
        response: HttpServletResponse,
        @RequestBody postData: PostData<EmployeeValidityPeriodAttr>
    ): ResponseEntity<*> {
        val id = postData.data.id
        requireNotNull(id) { "Can't delete EmployeeValidityPeriodAttr entry without id." }
        return ResponseEntity.ok()
            .body(
                ResponseAction(targetType = TargetType.CLOSE_MODAL, merge = true)
                //.addVariable("data", ResponseData(dto.variables, dto.dependentVariables))
                //.addVariable("ui", ui)
            )
    }

    /**
     * Updates only the displayName.
     */
    @PostMapping("update")
    fun update(@RequestBody postData: PostData<EmployeeValidityPeriodAttr>): ResponseAction {
        val id = postData.data.id
        requireNotNull(id) { "Can't update EmployeeValidityPeriodAttr entry without id." }
        throw UnsupportedOperationException("Not implemented yet.")
    }
}
