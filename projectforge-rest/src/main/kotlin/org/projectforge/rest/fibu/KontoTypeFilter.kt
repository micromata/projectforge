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

import org.projectforge.business.fibu.KontoDO
import org.projectforge.framework.persistence.api.impl.CustomResultFilter
import org.projectforge.framework.utils.IntRanges

/**
 * The kinds of accounts the account list can be narrowed to, told apart by the number ranges of the
 * installation (`AccountingConfig`): debitors and creditors have a configured range, the general ledger
 * accounts are all the others.
 */
enum class KontoType {
    DEBITOR, CREDITOR, GENERAL_LEDGER;

    companion object {
        /**
         * @param debitors The configured debitor ranges, null if none. Unlike the account pickers, where an
         *   unconfigured range allows every account, none matches **no** account here: the filter would
         *   otherwise show all accounts as debitors.
         */
        fun of(nummer: Int?, debitors: IntRanges?, creditors: IntRanges?): KontoType? {
            nummer ?: return null
            return when {
                matches(debitors, nummer) -> DEBITOR
                matches(creditors, nummer) -> CREDITOR
                else -> GENERAL_LEDGER
            }
        }

        private fun matches(ranges: IntRanges?, nummer: Int): Boolean = ranges?.doesMatch(nummer) == true

        /** The ranges as configured, null if the installation configured none (see [of]). */
        fun rangesOf(configured: String?): IntRanges? =
            if (configured.isNullOrBlank()) null else IntRanges(configured)
    }
}

/** Keeps the accounts of the chosen [types] (any of them). */
class KontoTypeFilter(
    private val types: Set<KontoType>,
    private val debitors: IntRanges?,
    private val creditors: IntRanges?,
) : CustomResultFilter<KontoDO> {
    override fun match(list: MutableList<KontoDO>, element: KontoDO): Boolean {
        return KontoType.of(element.nummer, debitors, creditors) in types
    }
}
