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

package org.projectforge.ui.filter

import org.projectforge.business.fibu.kost.KostFilter
import org.projectforge.business.fibu.kost.KostentraegerStatus
import org.projectforge.framework.i18n.translate
import org.projectforge.framework.persistence.api.MagicFilter
import org.projectforge.framework.persistence.api.QueryFilter
import org.projectforge.framework.persistence.api.QueryFilter.Companion.eq
import org.projectforge.framework.persistence.api.QueryFilter.Companion.isNull
import org.projectforge.framework.persistence.api.QueryFilter.Companion.ne
import org.projectforge.framework.persistence.api.QueryFilter.Companion.or
import org.projectforge.framework.persistence.api.impl.DBPredicate
import org.projectforge.ui.UILabelledElement
import org.projectforge.ui.UISelectValue

/**
 * The status ("list type") filter of the cost 1 and cost 2 lists — the multi-choice dropdown that
 * replaces the single-choice one the Wicket pages offered. Several statuses may be picked at once
 * (active / nonactive / notEnded / ended, see [KostFilter], plus [FILTER_NO_STATUS] for rows with no
 * status set); an empty selection means "all".
 *
 * The choice constrains the list by [KostentraegerStatus], but where that happens differs by entity, so
 * this object only owns what the two share — the filter element and the reading of the picked values —
 * and offers the two apply strategies the DAOs use:
 * - [applyKost1] adds in-DB predicates on the real `kostentraegerStatus` column (as `Kost1Dao.select`).
 * - [matchesKost2] filters post-fetch on the *computed* `effectiveKostentraegerStatus` (as `Kost2Dao.select`),
 *   which is no column and so can't be a predicate; a [org.projectforge.framework.persistence.api.impl.CustomResultFilter]
 *   calls it per row.
 *
 * With more than one status picked the entity's per-status predicates are OR-combined: a row matches if
 * it matches *any* of them. The active case is deliberately not the same on both: cost 1 keeps only
 * `ACTIVE` (a null status is not active there), cost 2 counts a null status as active too — this mirrors
 * the legacy DAOs exactly.
 */
object KostStatusFilterUtils {
    /** The id of the filter element and of the entry consumed in [consumeListType]. */
    const val FIELD_ID = "listType"

    /**
     * A cost unit without any status set. Not a [KostFilter] constant: the legacy Wicket dropdown had no
     * such option, this is added only for the next list where the empty status cell is common and worth
     * filtering for. For cost 1 it matches a null `kostentraegerStatus` column; for cost 2 a null
     * *effective* status (the empty cell the list shows).
     */
    const val FILTER_NO_STATUS = "noStatus"

    /** The auto-detected filter of the real status column, replaced by the grouped one (see [addFilterElement]). */
    private const val STATUS_COLUMN_ID = "kostentraegerStatus"

    /**
     * Replaces the auto-detected `kostentraegerStatus` filter with the grouped list-type dropdown and adds
     * it, so the list shows a single "Status" filter rather than two. Both share the label `status`, and the
     * raw column filter is redundant for cost 1 and outright wrong for cost 2 (whose status is the *computed*
     * `effectiveKostentraegerStatus`, not this column) - the grouped filter is the one both pages want.
     */
    fun addFilterElement(elements: MutableList<UILabelledElement>) {
        elements.removeIf { it is UIFilterElement && it.id == STATUS_COLUMN_ID }
        elements.add(createFilterElement())
    }

    /**
     * The multi-choice status dropdown to add in `addMagicFilterElements`. `all` is not an option:
     * an empty selection already means "all", so a dedicated entry would be a redundant second way to
     * say it (and a confusing one in a multi-select).
     */
    fun createFilterElement(): UIFilterListElement {
        return UIFilterListElement(
            FIELD_ID,
            label = translate("status"),
            values = listOf(
                UISelectValue(KostFilter.FILTER_ACTIVE, translate("fibu.kost.status.active")),
                UISelectValue(KostFilter.FILTER_NON_ACTIVE, translate("fibu.kost.status.nonactive")),
                UISelectValue(KostFilter.FILTER_NOT_ENDED, translate("notEnded")),
                UISelectValue(KostFilter.FILTER_ENDED, translate("ended")),
                UISelectValue(FILTER_NO_STATUS, translate("fibu.kost.status.none")),
            ),
            multi = true,
            defaultFilter = true,
        )
    }

    /**
     * Reads the picked statuses out of the filter and marks the entry synthetic, so the generic
     * [org.projectforge.framework.persistence.api.MagicFilterProcessor] doesn't also try to match `listType`
     * as a property. Returns an empty list when nothing is picked (which means "all"). Falls back to the
     * single-value form so a filter stored by the earlier single-choice dropdown is still read.
     */
    fun consumeListTypes(source: MagicFilter): List<String> {
        val entry = source.entries.find { it.field == FIELD_ID } ?: return emptyList()
        entry.synthetic = true
        val values = entry.value.values?.filter { it.isNotBlank() }
        if (!values.isNullOrEmpty()) {
            return values
        }
        return entry.value.value?.takeIf { it.isNotBlank() }?.let { listOf(it) } ?: emptyList()
    }

    /**
     * cost 1: the status is a real column, so it filters in the database (mirrors `Kost1Dao.select`).
     * The picked statuses' predicates are OR-combined; an empty selection adds no predicate ("all").
     */
    fun applyKost1(target: QueryFilter, listTypes: List<String>) {
        val predicates = listTypes.mapNotNull { predicateForKost1(it) }
        when (predicates.size) {
            0 -> {} // "all": no predicate.
            1 -> target.add(predicates.first())
            else -> target.add(or(*predicates.toTypedArray()))
        }
    }

    private fun predicateForKost1(listType: String): DBPredicate? = when (listType) {
        KostFilter.FILTER_ACTIVE -> eq("kostentraegerStatus", KostentraegerStatus.ACTIVE)
        KostFilter.FILTER_NON_ACTIVE -> eq("kostentraegerStatus", KostentraegerStatus.NONACTIVE)
        KostFilter.FILTER_ENDED -> eq("kostentraegerStatus", KostentraegerStatus.ENDED)
        KostFilter.FILTER_NOT_ENDED ->
            or(ne("kostentraegerStatus", KostentraegerStatus.ENDED), isNull("kostentraegerStatus"))
        FILTER_NO_STATUS -> isNull("kostentraegerStatus")
        else -> null
    }

    /**
     * cost 2: whether a row matches any of the picked statuses, tested against the computed
     * [KostentraegerStatus] (mirrors `Kost2Dao.select`). A null effective status counts as active. An
     * empty selection matches everything ("all").
     */
    fun matchesKost2(listTypes: List<String>, effectiveStatus: KostentraegerStatus?): Boolean {
        if (listTypes.isEmpty()) {
            return true
        }
        return listTypes.any { matchesSingleKost2(it, effectiveStatus) }
    }

    private fun matchesSingleKost2(listType: String, effectiveStatus: KostentraegerStatus?): Boolean {
        return when (listType) {
            KostFilter.FILTER_ACTIVE -> effectiveStatus == KostentraegerStatus.ACTIVE || effectiveStatus == null
            KostFilter.FILTER_NON_ACTIVE -> effectiveStatus == KostentraegerStatus.NONACTIVE
            KostFilter.FILTER_ENDED -> effectiveStatus == KostentraegerStatus.ENDED
            KostFilter.FILTER_NOT_ENDED -> effectiveStatus != KostentraegerStatus.ENDED
            FILTER_NO_STATUS -> effectiveStatus == null
            else -> false
        }
    }
}
