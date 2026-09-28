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

package org.projectforge.rest.fibu.kost

import jakarta.servlet.http.HttpServletRequest
import org.projectforge.business.fibu.KostFormatter
import org.projectforge.business.fibu.kost.Kost1DO
import org.projectforge.business.fibu.kost.Kost1Dao
import org.projectforge.business.fibu.kost.KostentraegerStatus
import org.projectforge.framework.persistence.api.BaseSearchFilter
import org.projectforge.framework.persistence.api.MagicFilter
import org.projectforge.framework.persistence.api.QueryFilter
import org.projectforge.framework.persistence.api.SortProperty
import org.projectforge.framework.persistence.api.impl.CustomResultFilter
import org.projectforge.framework.utils.NumberHelper
import org.projectforge.rest.config.Rest
import org.projectforge.rest.core.AbstractDTOEntityRest
import org.projectforge.rest.dto.Kost1
import org.projectforge.ui.UILabelledElement
import org.projectforge.ui.filter.KostStatusFilterUtils
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController

@RestController
@RequestMapping("${Rest.URL}/cost1")
class Kost1EntityRest : AbstractDTOEntityRest<Kost1DO, Kost1, Kost1Dao>(Kost1Dao::class.java, "fibu.kost1.title") {
    @Autowired
    private lateinit var kostFormatter: KostFormatter

    override fun transformFromDB(obj: Kost1DO, editMode: Boolean): Kost1 {
        val kost1 = Kost1()
        // Not the Kost1DO constructor: that one copies the minimal set meant for embedded objects.
        // The list and the edit page need every field, including the computed formattedNumber.
        kost1.copyFrom(obj)
        return kost1
    }

    override fun transformForDB(dto: Kost1): Kost1DO {
        val kost1DO = Kost1DO()
        dto.copyTo(kost1DO)
        return kost1DO
    }

    /**
     * Adds the status ("list type") filter (all / active / nonactive / notEnded / ended). This was missing
     * from the cost 1 list and is retrofitted here. See [KostStatusFilterUtils].
     */
    override fun addMagicFilterElements(elements: MutableList<UILabelledElement>) {
        KostStatusFilterUtils.addFilterElement(elements)
    }

    /**
     * The picked status filters on the real [Kost1DO.kostentraegerStatus] column, so it becomes an in-DB
     * predicate (mirrors `Kost1Dao.select`). Note cost 1's "active" excludes a null status, unlike cost 2.
     */
    override fun preProcessMagicFilter(target: QueryFilter, source: MagicFilter): List<CustomResultFilter<Kost1DO>>? {
        val listTypes = KostStatusFilterUtils.consumeListTypes(source)
        KostStatusFilterUtils.applyKost1(target, listTypes)
        return null
    }

    /**
     * Sorts by the cost number the list shows, which no database column holds: [Kost1DO.formattedNumber]
     * is a getter over the four number fields, so the criteria query can't order by it (it would log
     * "Could not resolve attribute" and return the rows unordered).
     *
     * The four fields in their own order are exactly that sort: each part is a fixed number of digits,
     * so comparing them one after the other yields the same order as comparing the formatted string.
     */
    override fun postProcessMagicFilter(target: QueryFilter, source: MagicFilter) {
        val index = target.sortProperties.indexOfFirst { it.property == "formattedNumber" }
        if (index < 0) {
            return
        }
        val sortOrder = target.sortProperties[index].sortOrder
        target.sortProperties.removeAt(index)
        target.sortProperties.addAll(
            index,
            NUMBER_PROPERTIES.map { SortProperty(it, sortOrder) },
        )
    }

    override fun queryAutocompleteObjects(request: HttpServletRequest, filter: BaseSearchFilter): List<Kost1DO> {
        val onlyActiveEntries = request.getParameter("onlyActiveEntries")?.toBooleanStrictOrNull() ?: true
        var list = super.queryAutocompleteObjects(request, filter)
        val searchString = filter.searchString?.replace(Regex("[*+]"), "")?.trim()
        if (onlyActiveEntries && !NumberHelper.isDigitsAndDotsOnly(searchString)) {
            // Don't filter by active status when searching for a number
            list =
                list.filter { it.kostentraegerStatus == null || it.kostentraegerStatus == KostentraegerStatus.ACTIVE }
        }
        list.forEach { it.displayName = kostFormatter.formatKost1(it, KostFormatter.FormatType.LONG) }
        return list
    }

    override val autoCompleteSearchFields = arrayOf("description", "nummer", "rawNumberString")

    companion object {
        /** The parts of the cost number, most significant first — [Kost1DO.formattedNumber] in columns. */
        private val NUMBER_PROPERTIES = listOf("nummernkreis", "bereich", "teilbereich", "endziffer")
    }
}
