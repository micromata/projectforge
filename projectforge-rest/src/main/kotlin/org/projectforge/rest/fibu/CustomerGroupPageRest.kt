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

package org.projectforge.rest.fibu

import jakarta.servlet.http.HttpServletRequest
import org.projectforge.business.PfCaches
import org.projectforge.business.fibu.customergroup.BusinessUnit
import org.projectforge.business.fibu.customergroup.CustomerGroup
import org.projectforge.business.fibu.customergroup.CustomerGroupConfig
import org.projectforge.business.fibu.customergroup.CustomerGroupError
import org.projectforge.business.fibu.customergroup.CustomerGroupService
import org.projectforge.business.fibu.customergroup.CustomerSet
import org.projectforge.business.user.ProjectForgeGroup
import org.projectforge.framework.access.AccessChecker
import org.projectforge.framework.i18n.translateMsg
import org.projectforge.rest.config.Rest
import org.projectforge.rest.core.AbstractDynamicPageRest
import org.projectforge.rest.dto.PostData
import org.projectforge.rest.task.TaskServicesRest
import org.projectforge.ui.ResponseAction
import org.projectforge.ui.TargetType
import org.projectforge.ui.ValidationError
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController

/**
 * The editor of the customer groups and business units (next page `/customerGroups`), for finance and
 * controlling only — admins included only if they are members of one of these groups.
 */
@RestController
@RequestMapping("${Rest.URL}/customerGroups")
class CustomerGroupPageRest : AbstractDynamicPageRest() {
    /** A customer or task as the multi-autocomplete holds it. */
    class EntityRef(var id: Long? = null, var displayName: String? = null)

    open class CustomerSetData(
        var key: String? = null,
        var name: String? = null,
        var customers: MutableList<EntityRef> = mutableListOf(),
        var texts: MutableList<String> = mutableListOf(),
    )

    class GroupData : CustomerSetData()

    class BusinessUnitData : CustomerSetData() {
        var groups: MutableList<String> = mutableListOf()

        /** Tasks with their path as label, as the task type-ahead (`task/tree/autosearch`) gives them. */
        var tasks: MutableList<EntityRef> = mutableListOf()
    }

    /** The names and patterns of one field, for [matches]. */
    class MatchesRequest(var texts: MutableList<String> = mutableListOf())

    class CustomerGroupsData(
        var groups: MutableList<GroupData> = mutableListOf(),
        var businessUnits: MutableList<BusinessUnitData> = mutableListOf(),
        /** The stored row's last update (epoch millis), sent back on save for the optimistic lock. */
        var lastUpdate: Long? = null,
        /** Free text about the definition as a whole, see [CustomerGroupConfig.remark]. */
        var remark: String? = null,
        /** The stored `ConfigurationDO` row, for its change history (`/rs/configuration/history/{id}`). */
        var id: Long? = null,
    )

    @Autowired
    private lateinit var accessChecker: AccessChecker

    @Autowired
    private lateinit var customerGroupService: CustomerGroupService

    @GetMapping
    fun getData(): CustomerGroupsData {
        checkAccess()
        return toData(customerGroupService.index.config)
    }

    @PostMapping("save")
    fun save(
        request: HttpServletRequest,
        @RequestBody postData: PostData<CustomerGroupsData>,
    ): ResponseEntity<ResponseAction> {
        validateCsrfToken(request, postData)?.let { return it }
        checkAccess()
        accessChecker.checkRestrictedOrDemoUser()
        val data = postData.data
        val config = toConfig(data)
        val errors = customerGroupService.save(config, data.lastUpdate)
        if (errors.isNotEmpty()) {
            return ResponseEntity(ResponseAction(validationErrors = errors.map { toValidationError(it) }), HttpStatus.NOT_ACCEPTABLE)
        }
        return ResponseEntity.ok(ResponseAction(targetType = TargetType.NOTHING).addVariable("data", getData()))
    }

    /**
     * The errors the given unsaved configuration would be refused with, so the editor shows a conflict right
     * after the change causing it (on both sets involved) instead of only on saving. Nothing is stored, hence
     * no CSRF token; the optimistic lock is [save]'s.
     */
    @PostMapping("validate")
    fun validate(@RequestBody data: CustomerGroupsData): List<ValidationError> {
        checkAccess()
        return customerGroupService.check(toConfig(data)).map { toValidationError(it) }
    }

    /**
     * The customer entities and free-text customers the given names and patterns match, so the editor can show
     * what a pattern catches before it is saved.
     */
    @PostMapping("matches")
    fun matches(@RequestBody request: MatchesRequest): CustomerGroupService.CustomerMatches {
        checkAccess()
        return customerGroupService.matches(request.texts)
    }

    /**
     * What the given unsaved configuration leaves without a business unit, shown below the business units and
     * updated while editing.
     */
    @PostMapping("unassigned")
    fun unassigned(@RequestBody data: CustomerGroupsData): CustomerGroupService.Unassigned {
        checkAccess()
        return customerGroupService.unassigned(toConfig(data))
    }

    /**
     * What each business unit of the given unsaved configuration stands for (by business-unit key), shown in
     * its row and updated while editing.
     */
    @PostMapping("businessUnitMembers")
    fun businessUnitMembers(@RequestBody data: CustomerGroupsData): Map<String, List<CustomerGroupService.BusinessUnitMember>> {
        checkAccess()
        return customerGroupService.businessUnitMembers(toConfig(data))
    }

    private fun checkAccess() {
        accessChecker.checkIsLoggedInUserMemberOfGroup(ProjectForgeGroup.FINANCE_GROUP, ProjectForgeGroup.CONTROLLING_GROUP)
    }

    private fun toData(config: CustomerGroupConfig) = CustomerGroupsData(
        groups = config.groups.map { fill(GroupData(), it) }.toMutableList(),
        businessUnits = config.businessUnits.map { bu ->
            fill(BusinessUnitData(), bu).also { data ->
                data.groups = bu.groups.toMutableList()
                data.tasks = bu.tasks.map { EntityRef(it, TaskServicesRest.formatPath(it)) }.toMutableList()
            }
        }.toMutableList(),
        remark = config.remark,
        lastUpdate = customerGroupService.lastUpdate,
        id = customerGroupService.entryId,
    )

    private fun <T : CustomerSetData> fill(dest: T, src: CustomerSet): T {
        dest.key = src.key
        dest.name = src.name
        dest.customers = src.customers.map { EntityRef(it, PfCaches.instance.getKunde(it)?.displayName ?: "$it") }
            .toMutableList()
        dest.texts = src.texts.toMutableList()
        return dest
    }

    private fun toConfig(data: CustomerGroupsData) = CustomerGroupConfig(
        groups = data.groups.map { copy(it, CustomerGroup()) }.toMutableList(),
        businessUnits = data.businessUnits.map { bu ->
            copy(bu, BusinessUnit()).also { dest ->
                dest.groups = bu.groups.toMutableList()
                dest.tasks = bu.tasks.mapNotNull { it.id }.distinct().toMutableList()
            }
        }.toMutableList(),
        remark = data.remark,
    )

    private fun <T : CustomerSet> copy(src: CustomerSetData, dest: T): T {
        dest.key = src.key?.takeIf { it.isNotBlank() }
        dest.name = src.name
        dest.customers = src.customers.mapNotNull { it.id }.toMutableList()
        dest.texts = src.texts.toMutableList()
        return dest
    }

    private fun toValidationError(error: CustomerGroupError) = ValidationError(
        message = translateMsg(error.messageKey, *error.params.toTypedArray()),
        fieldId = error.fieldId.takeIf { it.isNotEmpty() },
        messageId = error.messageKey,
    )
}
