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

package org.projectforge.rest.fibu.kost

import jakarta.servlet.http.HttpServletRequest
import jakarta.validation.Valid
import org.projectforge.business.fibu.KostFormatter
import org.projectforge.business.fibu.kost.Kost1Dao
import org.projectforge.business.fibu.kost.Kost2Dao
import org.projectforge.framework.DisplayNameCapable
import org.projectforge.model.rest.RestPaths
import org.projectforge.rest.config.Rest
import org.projectforge.rest.core.AbstractDynamicPageRest
import org.projectforge.rest.dto.FormLayoutData
import org.projectforge.rest.dto.PostData
import org.projectforge.ui.*
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.*

@RestController
@RequestMapping("${Rest.URL}/costSearch")
class CostSearchPageRest : AbstractDynamicPageRest() {
    class CostObject(
        var id: Long? = null,
        override val displayName: String?
    ) : DisplayNameCapable

    class SearchData {
        var cost1: CostObject? = null
        var cost2: CostObject? = null
        var cost1Number: Int? = null
        var cost1Name: String? = null
        var cost2Number: Int? = null
        var cost2Name: String? = null
    }

    @Autowired
    private lateinit var kost1Dao: Kost1Dao

    @Autowired
    private lateinit var kost2Dao: Kost2Dao

    @GetMapping("dynamic")
    fun getForm(request: HttpServletRequest): FormLayoutData {
        val layout = UILayout("menu.fibu.kostSearch")

        layout.add(
            UIRow().add(
                UICol(sm = 6).add(
                    UIRow().add(
                        UIInput(
                            "cost1",
                            label = "fibu.kost1",
                            dataType = UIDataType.COST1,
                        )
                    )
                ).add(
                    UIRow().add(
                        UICol(xs = 4).add(
                            UIReadOnlyField("cost1Number", canCopy = true)
                        )
                    ).add(
                        UICol(xs = 8).add(
                            UIReadOnlyField("cost1Name")
                        )
                    )
                )
            ).add(
                UICol(sm = 6).add(
                    UIRow().add(
                        UIInput(
                            "cost2",
                            label = "fibu.kost2",
                            dataType = UIDataType.COST2,
                        )
                    )
                ).add(
                    UIRow().add(
                        UICol(xs = 4).add(
                            UIReadOnlyField("cost2Number", canCopy = true)
                        )
                    ).add(
                        UICol(xs = 8).add(
                            UIReadOnlyField("cost2Name")
                        )
                    )
                )
            )
        )
        layout.watchFields.addAll(arrayOf("cost1", "cost2"))
        LayoutUtils.process(layout)
        return FormLayoutData(SearchData(), layout, createServerData(request))
    }

    @PostMapping(RestPaths.WATCH_FIELDS)
    fun watchFields(@Valid @RequestBody postData: PostData<SearchData>): ResponseEntity<ResponseAction> {
        val data = postData.data
        postData.watchFieldsTriggered?.let { watchFieldsTriggered ->
            if (watchFieldsTriggered.contains("cost1")) {
                data.cost1?.id?.let { id ->
                    kost1Dao.find(id)?.let { kost ->
                        data.cost1Number = kost.nummer
                        data.cost1Name = KostFormatter.instance.formatKost1(kost, KostFormatter.FormatType.LONG)
                    }
                }
                data.cost1 = null
            }
            if (watchFieldsTriggered.contains("cost2")) {
                data.cost2?.id?.let { id ->
                    kost2Dao.find(id)?.let { kost ->
                        data.cost2Number = kost.nummer
                        data.cost2Name = KostFormatter.instance.formatKost2(kost, KostFormatter.FormatType.LONG)
                    }
                }
                data.cost2 = null
            }
        }
        return ResponseEntity.ok(
            ResponseAction(targetType = TargetType.UPDATE)
                .addVariable("data", data)
        )
    }
}
