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


package org.projectforge.business.fibu.contributionmargin

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertSame
import org.junit.jupiter.api.Test
import java.time.LocalDate
import java.time.YearMonth
import java.time.ZoneId
import java.util.Locale

class ContributionMarginCacheTest {
  private var now = 0L

  private val cache = ContributionMarginCache().also { it.clock = { now } }

  private val loaded = mutableListOf<Set<Long>>()

  private val load: (Set<Long>) -> Map<Long, ContributionMarginCache.ProjectPart> = { ids ->
    loaded.add(ids)
    ids.associateWith { ContributionMarginCache.ProjectPart(emptyList(), emptyList()) }
  }

  @Test
  fun `only the projects not cached yet are loaded`() {
    val first = cache.get(context(), setOf(1L), load)
    val all = cache.get(context(), setOf(1L, 2L, 3L), load)
    assertEquals(listOf(setOf(1L), setOf(2L, 3L)), loaded)
    assertSame(first[1L], all[1L], "Project 1 is taken from the cache.")
    assertEquals(setOf(1L, 2L, 3L), all.keys)
    cache.get(context(), setOf(2L), load)
    assertEquals(2, loaded.size, "All cached.")
  }

  @Test
  fun `another context gets own parts`() {
    cache.get(context(), setOf(1L), load)
    cache.get(context(startMonth = YearMonth.of(2025, 1)), setOf(1L), load)
    cache.get(context(locale = Locale.ENGLISH), setOf(1L), load)
    cache.get(context(bookingImportEnd = LocalDate.of(2026, 8, 31)), setOf(1L), load)
    assertEquals(4, loaded.size)
  }

  @Test
  fun `parts expire and are cleared`() {
    cache.get(context(), setOf(1L), load)
    now += ContributionMarginCache.TTL_MILLIS - 1
    cache.get(context(), setOf(1L), load)
    assertEquals(1, loaded.size)
    now += 1
    cache.get(context(), setOf(1L), load)
    assertEquals(2, loaded.size, "Expired.")
    cache.clear()
    assertEquals(0, cache.size)
    cache.get(context(), setOf(1L), load)
    assertEquals(3, loaded.size)
  }

  @Test
  fun `parts loaded while the cache is cleared are not stored`() {
    cache.get(context(), setOf(1L)) { ids ->
      cache.clear() // E. g. an invoice changed during the calculation.
      load(ids)
    }
    assertEquals(0, cache.size)
  }

  @Test
  fun `the least recently used context is dropped`() {
    repeat(ContributionMarginCache.MAX_CONTEXTS + 2) {
      cache.get(context(startMonth = YearMonth.of(2020 + it, 1)), setOf(1L), load)
    }
    assertEquals(ContributionMarginCache.MAX_CONTEXTS, cache.size)
  }

  private fun context(
    startMonth: YearMonth = YearMonth.of(2026, 1),
    bookingImportEnd: LocalDate? = LocalDate.of(2026, 7, 31),
    locale: Locale = Locale.GERMAN,
  ) = ContributionMarginCache.Context(
    startMonth = startMonth,
    today = LocalDate.of(2026, 10, 6),
    bookingImportEnd = bookingImportEnd,
    config = "{}",
    zoneId = ZoneId.of("Europe/Berlin"),
    locale = locale,
  )
}
