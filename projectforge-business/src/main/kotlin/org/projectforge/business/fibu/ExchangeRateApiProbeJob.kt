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

import jakarta.annotation.PostConstruct
import org.projectforge.common.scheduling.SchedulerJobArea
import org.projectforge.common.scheduling.SchedulerJobDefinition
import org.projectforge.common.scheduling.SchedulerJobRunner
import org.projectforge.common.scheduling.SchedulerSchedule
import org.projectforge.framework.integration.SyncStatsRegistry
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.scheduling.annotation.Scheduled
import org.springframework.stereotype.Component
import java.math.BigDecimal
import java.time.LocalDate

/**
 * Tests the exchange rate API daily (`projectforge.cron.exchangeRateApiProbe`): the currency conversion is used
 * rarely, so a changed or moved API would otherwise be noticed late. The result is shown by the tile of the
 * interface on the system dashboard ([ExchangeRateApiSubsystemStatusProvider]); a failure is a problem of the log
 * aggregation (error digest). Started at once by "run now" of the scheduler tab.
 */
@Component
class ExchangeRateApiProbeJob {
    @Autowired
    private lateinit var exchangeRateApiService: ExchangeRateApiService

    @Autowired
    private lateinit var schedulerJobRunner: SchedulerJobRunner

    @PostConstruct
    private fun postConstruct() {
        schedulerJobRunner.register(SCHEDULER_JOB, ::execute)
    }

    @Scheduled(cron = CRON)
    fun execute() {
        schedulerJobRunner.runAsync(SCHEDULER_JOB) { run ->
            try {
                probe(exchangeRateApiService::fetchRateForDate)
            } catch (ex: Exception) {
                // Already logged by the service and the sync stats (problems of the interface, not of the scheduler).
                run.fail(ex.message ?: ex.javaClass.simpleName)
            }
        }
    }

    companion object {
        private const val CRON = "\${projectforge.cron.exchangeRateApiProbe}"

        const val SYNC_TYPE = "exchangeRateApi"

        internal const val SOURCE_CURRENCY = "EUR"

        internal const val TARGET_CURRENCY = "USD"

        /** A fixed date in the past, for the request of a historical rate (`&date=`). */
        internal val HISTORICAL_DATE: LocalDate = LocalDate.of(2024, 1, 2)

        val SCHEDULER_JOB = SchedulerJobDefinition(
            "fibu.exchangeRateApiProbe", SchedulerJobArea.INTEGRATION, ExchangeRateApiProbeJob::class.java, "execute",
            SchedulerSchedule.Cron(CRON),
        )

        /**
         * Fetches the latest and a historical rate, recorded as a run of the sync stats [SYNC_TYPE].
         * @throws IllegalStateException if a rate isn't received (the run is recorded as aborted).
         */
        internal fun probe(fetch: (String, String, LocalDate?) -> BigDecimal?) {
            SyncStatsRegistry.get(SYNC_TYPE).execute("$SOURCE_CURRENCY -> $TARGET_CURRENCY") { run ->
                listOf("latest" to null, "historical" to HISTORICAL_DATE).forEach { (name, date) ->
                    run.step(name) { counts ->
                        val rate = fetch(SOURCE_CURRENCY, TARGET_CURRENCY, date)
                        check(rate != null && rate.signum() > 0) {
                            "No valid rate received for $SOURCE_CURRENCY -> $TARGET_CURRENCY on ${date ?: "latest"}: $rate"
                        }
                        counts.unchanged++
                    }
                }
            }
        }
    }
}
