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

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import org.projectforge.business.fibu.kost.KostFilter
import org.projectforge.business.fibu.kost.KostentraegerStatus
import org.projectforge.framework.persistence.api.MagicFilter
import org.projectforge.framework.persistence.api.MagicFilterEntry
import org.projectforge.framework.persistence.api.QueryFilter
import org.projectforge.framework.persistence.api.impl.DBPredicate

/**
 * The multi-choice status ("list type") filter shared by the cost 1 and cost 2 lists. No database and
 * no Spring context: what is under test is the reading of the picked values and how each entity turns
 * them into a filter, which is plain logic.
 */
class KostStatusFilterUtilsTest {
    @Test
    fun `reads the picked values and marks the entry synthetic`() {
        val filter = MagicFilter()
        val entry = MagicFilterEntry(field = KostStatusFilterUtils.FIELD_ID)
        entry.value.values = arrayOf(KostFilter.FILTER_ACTIVE, "", KostFilter.FILTER_ENDED)
        filter.entries.add(entry)

        assertEquals(
            listOf(KostFilter.FILTER_ACTIVE, KostFilter.FILTER_ENDED),
            KostStatusFilterUtils.consumeListTypes(filter),
            "blank values are dropped",
        )
        assertTrue(entry.synthetic == true, "the generic processor must not match listType as a property")
    }

    @Test
    fun `an absent filter means all`() {
        assertEquals(emptyList<String>(), KostStatusFilterUtils.consumeListTypes(MagicFilter()))
    }

    @Test
    fun `a stored single-choice value is still read`() {
        val filter = MagicFilter()
        val entry = MagicFilterEntry(field = KostStatusFilterUtils.FIELD_ID)
        entry.value.value = KostFilter.FILTER_NON_ACTIVE
        filter.entries.add(entry)

        assertEquals(listOf(KostFilter.FILTER_NON_ACTIVE), KostStatusFilterUtils.consumeListTypes(filter))
    }

    @Test
    fun `cost 1 adds no predicate when nothing is picked`() {
        assertEquals(0, statusPredicates(emptyList()).size, "an empty selection means all")
    }

    @Test
    fun `cost 1 adds a single equality predicate for one status`() {
        val predicates = statusPredicates(listOf(KostFilter.FILTER_ACTIVE))
        assertEquals(1, predicates.size)
        val equal = predicates.first() as DBPredicate.Equal
        assertEquals("kostentraegerStatus", equal.field)
        assertEquals(KostentraegerStatus.ACTIVE, equal.value)
    }

    @Test
    fun `cost 1 OR-combines several picked statuses into one predicate`() {
        val predicates = statusPredicates(listOf(KostFilter.FILTER_ACTIVE, KostFilter.FILTER_NON_ACTIVE))
        assertEquals(1, predicates.size, "the OR is a single predicate, not one per status")
        assertTrue(predicates.first() is DBPredicate.Or)
    }

    @Test
    fun `cost 1 filters for a null status with the no-status option`() {
        val predicates = statusPredicates(listOf(KostStatusFilterUtils.FILTER_NO_STATUS))
        assertEquals(1, predicates.size)
        val isNull = predicates.first() as DBPredicate.IsNull
        assertEquals("kostentraegerStatus", isNull.field)
    }

    @Test
    fun `cost 2 no-status matches only a null effective status`() {
        val listTypes = listOf(KostStatusFilterUtils.FILTER_NO_STATUS)
        assertTrue(KostStatusFilterUtils.matchesKost2(listTypes, null))
        assertFalse(KostStatusFilterUtils.matchesKost2(listTypes, KostentraegerStatus.ACTIVE))
        assertFalse(KostStatusFilterUtils.matchesKost2(listTypes, KostentraegerStatus.ENDED))
    }

    @Test
    fun `cost 2 matches any of the picked statuses`() {
        val listTypes = listOf(KostFilter.FILTER_NON_ACTIVE, KostFilter.FILTER_ENDED)
        assertTrue(KostStatusFilterUtils.matchesKost2(listTypes, KostentraegerStatus.NONACTIVE))
        assertTrue(KostStatusFilterUtils.matchesKost2(listTypes, KostentraegerStatus.ENDED))
        assertFalse(KostStatusFilterUtils.matchesKost2(listTypes, KostentraegerStatus.ACTIVE))
    }

    @Test
    fun `cost 2 counts a null status as active, unlike cost 1`() {
        assertTrue(
            KostStatusFilterUtils.matchesKost2(listOf(KostFilter.FILTER_ACTIVE), null),
            "cost 2 active includes a null status",
        )
        assertFalse(
            KostStatusFilterUtils.matchesKost2(listOf(KostFilter.FILTER_NON_ACTIVE), null),
        )
    }

    @Test
    fun `cost 2 matches everything when nothing is picked`() {
        assertTrue(KostStatusFilterUtils.matchesKost2(emptyList(), KostentraegerStatus.ENDED))
        assertTrue(KostStatusFilterUtils.matchesKost2(emptyList(), null))
    }

    /** The status predicates [applyKost1] added, without the always-present `deleted` flag. */
    private fun statusPredicates(listTypes: List<String>): List<DBPredicate> {
        val queryFilter = QueryFilter()
        KostStatusFilterUtils.applyKost1(queryFilter, listTypes)
        return queryFilter.createDBFilter().allPredicates.filter { it.field != "deleted" }
    }
}
