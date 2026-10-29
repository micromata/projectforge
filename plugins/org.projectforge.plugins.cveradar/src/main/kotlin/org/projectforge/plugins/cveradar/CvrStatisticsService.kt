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

package org.projectforge.plugins.cveradar

import org.projectforge.framework.persistence.jpa.PfPersistenceService
import org.projectforge.plugins.cveradar.model.CvrCaseDO
import org.projectforge.plugins.cveradar.model.CvrVulnerabilityDO
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Service
import java.time.DayOfWeek
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.time.temporal.TemporalAdjusters
import java.util.Date

/**
 * The figures of the statistics tab. No access check here: the caller ([org.projectforge.plugins.cveradar.rest.CveRadarRest])
 * has checked the read right.
 */
@Service
class CvrStatisticsService {
    @Autowired
    private lateinit var persistenceService: PfPersistenceService

    enum class Granularity { DAY, WEEK, MONTH }

    /** The number of entries of one period, [periodStart] being the first day of the day, week (Monday) or month. */
    class Bucket(val periodStart: LocalDate, val count: Int)

    /** The cases created per period (the date they were reported to the CVE-Radar), oldest first. */
    fun casesCreated(granularity: Granularity, zone: ZoneId, today: LocalDate = LocalDate.now(zone)): List<Bucket> {
        val from = firstPeriod(granularity, today)
        return bucketize(select("select created from CvrCaseDO where created >= :from", from, zone), granularity, zone, today)
    }

    /** The vulnerabilities published by the NVD per period, oldest first. */
    fun vulnerabilitiesPublished(granularity: Granularity, zone: ZoneId, today: LocalDate = LocalDate.now(zone)): List<Bucket> {
        val from = firstPeriod(granularity, today)
        return bucketize(
            select("select published from CvrVulnerabilityDO where published >= :from", from, zone), granularity, zone, today,
        )
    }

    private fun select(hql: String, from: LocalDate, zone: ZoneId): List<Date> {
        return persistenceService.executeQuery(hql, Date::class.java, Pair("from", Date.from(from.atStartOfDay(zone).toInstant())))
    }

    companion object {
        const val DAYS = 90L
        const val WEEKS = 52L
        const val MONTHS = 24L

        fun periodStart(day: LocalDate, granularity: Granularity): LocalDate = when (granularity) {
            Granularity.DAY -> day
            Granularity.WEEK -> day.with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY))
            Granularity.MONTH -> day.withDayOfMonth(1)
        }

        private fun next(start: LocalDate, granularity: Granularity): LocalDate = when (granularity) {
            Granularity.DAY -> start.plusDays(1)
            Granularity.WEEK -> start.plusWeeks(1)
            Granularity.MONTH -> start.plusMonths(1)
        }

        /** The first period of the window ending with the period of [today]. */
        fun firstPeriod(granularity: Granularity, today: LocalDate): LocalDate = when (granularity) {
            Granularity.DAY -> today.minusDays(DAYS - 1)
            Granularity.WEEK -> periodStart(today, granularity).minusWeeks(WEEKS - 1)
            Granularity.MONTH -> periodStart(today, granularity).minusMonths(MONTHS - 1)
        }

        /**
         * Counts the dates per period in the time zone of the user. Every period of the window is there, also
         * without entries: a gap on the axis would read as "nothing to count" and is a zero instead.
         */
        fun bucketize(dates: Collection<Date>, granularity: Granularity, zone: ZoneId, today: LocalDate): List<Bucket> {
            val counts = dates.groupingBy { periodStart(Instant.ofEpochMilli(it.time).atZone(zone).toLocalDate(), granularity) }
                .eachCount()
            val result = mutableListOf<Bucket>()
            var start = firstPeriod(granularity, today)
            val last = periodStart(today, granularity)
            while (start <= last) {
                result.add(Bucket(start, counts[start] ?: 0))
                start = next(start, granularity)
            }
            return result
        }
    }
}
