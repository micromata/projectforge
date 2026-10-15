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

package org.projectforge.business.fibu

import org.junit.jupiter.api.Assertions
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import org.projectforge.framework.integration.SubsystemState
import org.projectforge.framework.integration.SubsystemStatus
import org.projectforge.framework.integration.SyncStats
import org.projectforge.framework.integration.SyncStatsRegistry
import java.math.BigDecimal
import java.time.LocalDate

class ExchangeRateApiProbeJobTest {
    @Test
    fun `probe fetches the latest and a historical rate`() {
        val dates = mutableListOf<LocalDate?>()
        ExchangeRateApiProbeJob.probe { source, target, date ->
            Assertions.assertEquals("EUR", source)
            Assertions.assertEquals("USD", target)
            dates.add(date)
            BigDecimal("1.08")
        }
        Assertions.assertEquals(listOf(null, ExchangeRateApiProbeJob.HISTORICAL_DATE), dates)
        Assertions.assertEquals(SyncStats.Status.SUCCESS, stats.lastStatus)
        Assertions.assertEquals(SubsystemState.OK, tileState())
    }

    @Test
    fun `missing or invalid rate aborts the probe`() {
        assertThrows<IllegalStateException> { ExchangeRateApiProbeJob.probe { _, _, _ -> null } }
        Assertions.assertEquals(SyncStats.Status.ABORTED, stats.lastStatus)
        Assertions.assertEquals(SubsystemState.DOWN, tileState())

        ExchangeRateApiProbeJob.probe { _, _, _ -> BigDecimal.ONE }
        Assertions.assertEquals(SubsystemState.OK, tileState())

        // A historical rate missing (e.g. the date parameter changed) is a failure too.
        assertThrows<IllegalStateException> {
            ExchangeRateApiProbeJob.probe { _, _, date -> if (date == null) BigDecimal.ONE else BigDecimal.ZERO }
        }
        Assertions.assertEquals(SubsystemState.DOWN, tileState())
    }

    private val stats: SyncStats
        get() = SyncStatsRegistry.get(ExchangeRateApiProbeJob.SYNC_TYPE)

    private fun tileState() = SubsystemStatus.ofSyncs(listOf(ExchangeRateApiProbeJob.SYNC_TYPE)).state
}
