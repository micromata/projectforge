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

package org.projectforge.business.timesheet

/**
 * Utilities for handling time sheets of a single user that may overlap in time (shared cost elements, see
 * [org.projectforge.business.task.TaskDO.allowTimeOverlap]).
 *
 * Two notions of "duration" have to be distinguished once overlaps are allowed:
 * - **Effort** (customer invoicing, project export): every time sheet counts fully with its own `stop - start`.
 * - **Attendance / worked time** (monthly report, statistics, target/actual, break checks): overlapping time is
 *   **split proportionally** — at any instant covered by n time sheets each of them gets `1/n` of that instant.
 *   Example: 1h booked on project A plus an overlapping 1h on project B => 0.5h each. The sum of the split durations
 *   equals the length of the union of all intervals (i. e. overlapping time is counted exactly once in total). This
 *   models cost sharing between the involved projects/customers.
 *
 * All methods assume the time sheets belong to a single user. Deleted sheets should be filtered out by the caller.
 *
 * @author Kai Reinhard
 */
object TimesheetOverlapUtils {
    /**
     * Splits the total worked time proportionally among overlapping time sheets (see class doc).
     *
     * @param sheets the time sheets of a single user (start/stop must be set; deleted sheets should be filtered out).
     * @return map of `timesheet.id` -> split duration in millis. Non-overlapping sheets keep their full duration.
     *   The sum of all values equals [unionDurationMillis]. Sheets without an id or without start/stop are ignored.
     */
    fun splitDurations(sheets: Collection<TimesheetDO>): Map<Long, Long> {
        val valid = sheets.filter { it.id != null && it.startTime != null && it.stopTime != null }
        if (valid.isEmpty()) {
            return emptyMap()
        }
        // Collect all distinct boundary points.
        val boundaries = sortedSetOf<Long>()
        valid.forEach {
            boundaries.add(it.startTime!!.time)
            boundaries.add(it.stopTime!!.time)
        }
        val points = boundaries.toLongArray()
        val result = HashMap<Long, Long>()
        for (i in 0 until points.size - 1) {
            val from = points[i]
            val to = points[i + 1]
            val length = to - from
            if (length <= 0) {
                continue
            }
            // Sheets covering the whole elementary interval [from, to).
            val covering = valid.filter { it.startTime!!.time <= from && it.stopTime!!.time >= to }
            val coverage = covering.size
            if (coverage == 0) {
                continue
            }
            val base = length / coverage
            var remainder = length % coverage
            // Distribute base to every covering sheet; hand out the remaining millis one by one to keep the
            // sum exactly equal to the union length.
            covering.forEach { sheet ->
                var share = base
                if (remainder > 0) {
                    share += 1
                    remainder -= 1
                }
                result.merge(sheet.id!!, share, Long::plus)
            }
        }
        return result
    }

    /**
     * @param sheets the time sheets of a single user.
     * @return the length of the union of all time sheet intervals in millis (overlapping time counted once). Equals
     *   the sum of [splitDurations].
     */
    fun unionDurationMillis(sheets: Collection<TimesheetDO>): Long {
        val valid = sheets.filter { it.startTime != null && it.stopTime != null }
        if (valid.isEmpty()) {
            return 0L
        }
        val intervals = valid.map { it.startTime!!.time to it.stopTime!!.time }.sortedBy { it.first }
        var total = 0L
        var curStart = intervals[0].first
        var curEnd = intervals[0].second
        for (i in 1 until intervals.size) {
            val (start, end) = intervals[i]
            if (start > curEnd) {
                total += curEnd - curStart
                curStart = start
                curEnd = end
            } else if (end > curEnd) {
                curEnd = end
            }
        }
        total += curEnd - curStart
        return total
    }
}
