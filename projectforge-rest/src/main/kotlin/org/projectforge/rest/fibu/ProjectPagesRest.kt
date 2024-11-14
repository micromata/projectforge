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
import org.projectforge.business.PfCaches
import org.projectforge.business.fibu.ProjektDO
import org.projectforge.business.fibu.ProjektDao
import org.projectforge.framework.persistence.api.MagicFilter
import org.projectforge.rest.config.Rest
import org.projectforge.rest.core.AbstractDTOPagesRest
import org.projectforge.rest.dto.PostData
import org.projectforge.rest.dto.Project
import org.projectforge.ui.*
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController

@RestController
@RequestMapping("${Rest.URL}/project")
class ProjectPagesRest
    : AbstractDTOPagesRest<ProjektDO, Project, ProjektDao>(
    ProjektDao::class.java,
    "fibu.projekt.title"
) {
    @Autowired
    private lateinit var caches: PfCaches

    override val addNewEntryUrl = "wa/projectEdit"

    override fun getStandardEditPage(): String {
        return "wa/projectEdit?id=:id"
    }

    override fun transformFromDB(obj: ProjektDO, editMode: Boolean): Project {
        val projekt = Project()
        caches.populate(obj)
        projekt.copyFrom(obj)
        return projekt
    }

    override fun transformForDB(dto: Project): ProjektDO {
        val projektDO = ProjektDO()
        dto.copyTo(projektDO)
        return projektDO
    }

    override val classicsLinkListUrl: String?
        get() = "wa/projectList"

    /**
     * LAYOUT List page
     */
    override fun createListLayout(
        request: HttpServletRequest,
        layout: UILayout,
        magicFilter: MagicFilter,
        userAccess: UILayout.UserAccess
    ) {
        val agGrid = agGridSupport.prepareUIGrid4ListPage(
            request,
            layout,
            magicFilter,
            this,
            userAccess = userAccess,
            pageAfterMultiSelect = ProjectMultiSelectedPageRest::class.java,
            rowClickUrl = "/wa/projectEdit?id={id}"
        )
        agGrid.add(Project::kostFormatted.name, headerName = "fibu.projekt.nummer")
        agGrid.add(Project::identifier.name, headerName = "fibu.projekt.identifier")
        agGrid.add("customer", headerName = "fibu.kunde.name", formatter = UIAgGridColumnDef.Formatter.CUSTOMER)
        agGrid.add("customer.division", headerName = "fibu.kunde.division")
        agGrid.add(Project::name.name, headerName = "fibu.projekt.name")
        agGrid.add(Project::task.name, headerName = "task", formatter = UIAgGridColumnDef.Formatter.TASK_PATH)
        agGrid.add(Project::statusAsString.name, headerName = "status")
        agGrid.add(
            Project::headOfBusinessManager.name,
            headerName = "fibu.headOfBusinessManager",
            formatter = UIAgGridColumnDef.Formatter.USER
        )
        agGrid.add(
            Project::salesManager.name,
            headerName = "fibu.salesManager",
            formatter = UIAgGridColumnDef.Formatter.USER
        )
        agGrid.add(
            Project::projectManager.name,
            headerName = "fibu.projectManager",
            formatter = UIAgGridColumnDef.Formatter.USER
        )
        agGrid.add(
            Project::projektManagerGroup.name,
            headerName = "fibu.projekt.projektManagerGroup",
            formatter = UIAgGridColumnDef.Formatter.GROUP,
        )
        agGrid.add(Project::description.name, headerName = "description")
        agGrid.add(
            Project::kost2Arts.name,
            headerName = "fibu.kost2art.kost2arten",
            formatter = UIAgGridColumnDef.Formatter.SHOW_LIST_OF_DISPLAYNAMES,
        )
        agGrid.withMultiRowSelection(request, magicFilter)

        // layout.excelExportSupported = true
    }

    /**
     * LAYOUT Edit page
     */
    override fun createEditLayout(dto: Project, userAccess: UILayout.UserAccess): UILayout {
        /*
        val konto = UIInput("konto", lc, tooltip = "fibu.projekt.konto.tooltip")

        val layout = super.createEditLayout(dto, userAccess)
          .add(
            UIRow()
              .add(
                UICol()
                  .add(UICustomized("cost.number24"))
                  .add(UISelect.createCustomerSelect(lc, "kunde", false, "fibu.kunde"))
                  .add(konto)
                  .add(lc, "name", "identifier", "task")
                  .add(UISelect.createGroupSelect(lc, "projektManagerGroup", false, "fibu.projekt.projektManagerGroup"))
                  .add(lc, "projectManager", "headOfBusinessManager", "description")
              )
          )

        dto.kost2Arts?.forEach {
          var label = it.getFormattedId() + " " + it.name
          if (!it.fakturiert) {
            label += " (nf)"
          }
          val uiCheckbox = UICheckbox("" + it.getFormattedId(), label = label)
          layout.add(UIRow().add(UICol().add(uiCheckbox)))
        }
        */
        val layout = super.createEditLayout(dto, userAccess)
        layout.add(UIAlert("Not yet implemented.", color = UIColor.DANGER))

        return LayoutUtils.processEditPage(layout, dto, this)
    }

    override fun onBeforeSaveOrUpdate(request: HttpServletRequest, obj: ProjektDO, postData: PostData<Project>) {
        throw IllegalArgumentException("Not yet implemented")
    }

    override val autoCompleteSearchFields = arrayOf("name", "identifier")
}
