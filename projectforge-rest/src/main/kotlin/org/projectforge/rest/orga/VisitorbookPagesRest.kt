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

package org.projectforge.rest.orga

import jakarta.annotation.PostConstruct
import jakarta.servlet.http.HttpServletRequest
import org.projectforge.business.PfCaches
import org.projectforge.business.orga.VisitorbookDO
import org.projectforge.business.orga.VisitorbookDao
import org.projectforge.business.orga.VisitorbookService
import org.projectforge.framework.persistence.api.MagicFilter
import org.projectforge.rest.config.JacksonConfiguration
import org.projectforge.rest.config.Rest
import org.projectforge.rest.core.AbstractDTOPagesRest
import org.projectforge.rest.core.PagesResolver
import org.projectforge.rest.dto.Employee
import org.projectforge.rest.dto.PostData
import org.projectforge.rest.dto.Visitorbook
import org.projectforge.rest.dto.VisitorbookEntry
import org.projectforge.ui.*
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController

@RestController
@RequestMapping("${Rest.URL}/visitorbook")
class VisitorbookPagesRest : AbstractDTOPagesRest<VisitorbookDO, Visitorbook, VisitorbookDao>(
    VisitorbookDao::class.java,
    "orga.visitorbook.title"
) {
    @Autowired
    private lateinit var visitorbookService: VisitorbookService

    @PostConstruct
    private fun postConstruct() {
        JacksonConfiguration.registerAllowedUnknownProperties(Visitorbook::class.java, "visitortypeAsString")
    }

    override fun transformForDB(dto: Visitorbook): VisitorbookDO {
        val visitorbookDO = VisitorbookDO()
        dto.copyTo(visitorbookDO)
        return visitorbookDO
    }

    override fun transformFromDB(obj: VisitorbookDO, editMode: Boolean): Visitorbook {
        val visitorbook = Visitorbook()
        visitorbook.copyFrom(obj)
        obj.id?.let { id ->
            visitorbookService.getVisitorbookInfo(id)?.let { info ->
                visitorbook.lastDateOfVisit = info.lastDateOfVisit
                visitorbook.latestArrived = info.latestArrived
                visitorbook.latestDeparted = info.latestDeparted
                visitorbook.numberOfVisits = info.numberOfVisits
            }
        }
        Employee.restoreDisplayNames(visitorbook.contactPersons)
        return visitorbook
    }

    override fun onBeforeGetItemAndLayout(
        request: HttpServletRequest,
        dto: Visitorbook,
        userAccess: UILayout.UserAccess
    ) {
        dto.id?.let { id ->
            visitorbookService.selectAllVisitorbookEntries(id).let { entries ->
                dto.entries = entries.map { VisitorbookEntry(it) }
            }
        }
    }

    /**
     * LAYOUT List page
     */
    override fun createListLayout(
        request: HttpServletRequest,
        layout: UILayout,
        magicFilter: MagicFilter,
        userAccess: UILayout.UserAccess
    ) {
        agGridSupport.prepareUIGrid4ListPage(
            request,
            layout,
            magicFilter,
            this,
            userAccess = userAccess,
        )
            // Name	Vorname	Status	Personalnummer	Kost1	Position	Team	Eintrittsdatum	Austrittsdatum	Bemerkung
            .add(
                lc,
                "lastname", "firstname", "company"
            )
            .add("visitortypeAsString", headerName = "orga.visitorbook.visitortype")
            .add("lastDateOfVisit", headerName = "orga.visitorbook.lastVisit")
            .add("latestArrived", headerName = "orga.visitorbook.lastVisit.arrived")
            .add("latestDeparted", headerName = "orga.visitorbook.lastVisit.departed")
            .add("numberOfVisits", headerName = "orga.visitorbook.numberOfVisits", type = UIAgGridColumnDef.Type.NUMBER)
            .add("contactPersonsAsString", headerName = "orga.visitorbook.contactPersons")
            .add(lc, "comment")
    }

    /**
     * LAYOUT Edit page
     */
    override fun createEditLayout(dto: Visitorbook, userAccess: UILayout.UserAccess): UILayout {
        val firstname = UIInput("firstname", lc) // Input-field instead of text-area (length > 255)
        val lastname = UIInput("lastname", lc)
        val company = UIInput("company", lc)
        val layout = super.createEditLayout(dto, userAccess)
            .add(
                UIRow()
                    .add(
                        UICol()
                            .add(firstname)
                            .add(lastname)
                            .add(company)
                    )
                    .add(
                        UICol()
                            .add(lc, "visitortype")
                            .add(
                                UISelect.createEmployeeSelect(
                                    lc,
                                    "contactPersons",
                                    true,
                                )
                            )
                    )
            )
            .add(lc, "comment")
        if (dto.id != null) {
            layout.layoutBelowActions.add(
                UIFieldset(title = "orga.visitorbook.visits")
                    .add(
                        UIButton.createAddButton(
                            responseAction = ResponseAction(
                                createModalUrl(dto, true),
                                targetType = TargetType.MODAL
                            ),
                            default = false,
                        )
                    )
                    .add(
                        UIAgGrid("entries")
                            .add(UIAgGridColumnDef.createCol(lc, "dateOfVisit", headerName = "date"))
                            .add(
                                UIAgGridColumnDef.createCol(
                                    lc,
                                    "arrived",
                                    headerName = "orga.visitorbook.arrive"
                                )
                            )
                            .add(
                                UIAgGridColumnDef.createCol(
                                    lc,
                                    "departed",
                                    headerName = "orga.visitorbook.depart"
                                )
                            )
                            .add(UIAgGridColumnDef.createCol(lc, "comment", headerName = "comment"))
                            .withRowClickRedirectUrl(
                                createModalUrl(dto),
                                openModal = true,
                            )
                    )
            )
        }
        return LayoutUtils.processEditPage(layout, dto, this)
    }

    override fun onAfterSave(obj: VisitorbookDO, postData: PostData<Visitorbook>): ResponseAction {
        // Redirect to edit page after insert for allowing user to add visit entries.
        return ResponseAction(PagesResolver.getEditPageUrl(VisitorbookPagesRest::class.java, obj.id, absolute = true))
            .addVariable("id", obj.id ?: -1)
    }

    private fun createModalUrl(
        visitorbook: Visitorbook,
        newEntry: Boolean = false
    ): String {
        return PagesResolver.getDynamicPageUrl(
            VisitorbookEntryPageRest::class.java,
            id = if (newEntry) "-1" else "{id}",
            params = mapOf(
                "visitorbookId" to visitorbook.id,
            ),
            absolute = true,
        )
    }
}
