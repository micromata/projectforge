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

import org.projectforge.business.PfCaches
import org.projectforge.business.fibu.EmployeeSalaryDO
import org.projectforge.business.fibu.EmployeeSalaryDao
import org.projectforge.business.fibu.EmployeeSalaryType
import org.projectforge.menu.builder.MenuItemDefId
import org.projectforge.rest.config.Rest
import org.projectforge.rest.multiselect.AbstractMultiSelectedPage
import org.projectforge.rest.multiselect.MassUpdateContext
import org.projectforge.rest.multiselect.MassUpdateFieldDeclaration
import org.projectforge.rest.multiselect.MassUpdateParameter
import org.projectforge.ui.LayoutContext
import org.projectforge.ui.UILayout
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController
import java.io.Serializable
import jakarta.annotation.PostConstruct
import jakarta.servlet.http.HttpServletRequest

/**
 * Mass update after selection of salaries: payment type and comment.
 */
@RestController
@RequestMapping("${Rest.URL}/employeeSalary${AbstractMultiSelectedPage.URL_SUFFIX_SELECTED}")
class EmployeeSalaryMultiSelectedPageRest : AbstractMultiSelectedPage<EmployeeSalaryDO>() {

    @Autowired
    private lateinit var caches: PfCaches

    @Autowired
    private lateinit var employeeSalaryDao: EmployeeSalaryDao

    @Autowired
    private lateinit var employeeSalaryEntityRest: EmployeeSalaryEntityRest

    override val layoutContext: LayoutContext = LayoutContext(EmployeeSalaryDO::class.java)

    override val listPageUrl: String = "/${MenuItemDefId.EMPLOYEE_SALARY_LIST.url}"

    @PostConstruct
    private fun postConstruct() {
        pagesRest = employeeSalaryEntityRest
    }

    /**
     * The same two fields [fillForm] lays out, for a client that renders the form itself.
     */
    override fun fieldDeclarations(): List<MassUpdateFieldDeclaration> {
        return listOf(
            MassUpdateFieldDeclaration("type"),
            MassUpdateFieldDeclaration("comment", showAppendOption = true),
        )
    }

    override fun fillForm(
        request: HttpServletRequest,
        layout: UILayout,
        massUpdateData: MutableMap<String, MassUpdateParameter>,
        selectedIds: Collection<Serializable>?,
        variables: MutableMap<String, Any>,
    ) {
        val lc = LayoutContext(EmployeeSalaryDO::class.java)
        createAndAddFields(lc, massUpdateData, layout, "type")
        createAndAddFields(lc, massUpdateData, layout, "comment", showAppendOption = true)
    }

    override fun proceedMassUpdate(
        request: HttpServletRequest,
        selectedIds: Collection<Serializable>,
        massUpdateContext: MassUpdateContext<EmployeeSalaryDO>,
    ): ResponseEntity<*>? {
        val salaries = employeeSalaryDao.select(selectedIds)
        if (salaries.isNullOrEmpty()) {
            return null
        }
        val params = massUpdateContext.massUpdateParams
        salaries.forEach { salary ->
            massUpdateContext.startUpdate(salary)
            params["type"]?.let { param ->
                if (param.delete == true) {
                    salary.type = null
                }
                param.textValue?.let { textValue ->
                    salary.type = EmployeeSalaryType.valueOf(textValue)
                }
            }
            processTextParameter(salary, "comment", params)
            // Who and which month, as the edit page's title.
            val employee = caches.getEmployeeIfNotInitialized(salary.employee)
            massUpdateContext.commitUpdate(
                identifier4Message = "${employee?.displayName} ${salary.formattedYearAndMonth}",
                salary,
                update = { employeeSalaryDao.update(salary) },
            )
        }
        return null
    }
}
