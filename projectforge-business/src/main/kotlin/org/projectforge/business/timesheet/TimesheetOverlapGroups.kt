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

import io.github.oshai.kotlinlogging.KotlinLogging
import org.projectforge.business.fibu.kost.Kost2DO
import org.projectforge.framework.configuration.Configuration
import org.projectforge.framework.configuration.ConfigurationParam

private val log = KotlinLogging.logger {}

/**
 * The overlap groups of the time sheet overlap rule ([TimesheetDao.hasTimeOverlap]), configured by
 * [ConfigurationParam.TIMESHEET_OVERLAP_GROUPS]. A group counts like one project there: two time sheets on cost units of
 * the same group must never overlap in time, even if one of them is booked on a shared cost element. Against anything
 * outside the group, the group behaves like any other project.
 *
 * Format: groups separated by `;`, each group one or more cost 2 number prefixes separated by `,`. A prefix has one to
 * four segments `nummernkreis[.bereich[.teilbereich[.kost2Art]]]` and is compared segment-wise with the cost unit
 * (so `6` matches 6.100.00.01, but not a cost unit of number range 61). A trailing `.*` is tolerated. Spaces are
 * ignored. If several prefixes match a cost unit, the longest one decides its group. Example: `6.100,6.300;6.200` =
 * group 1 consists of 6.100.* and 6.300.*, group 2 of 6.200.*.
 */
internal class TimesheetOverlapGroups private constructor(private val prefixes: List<Prefix>) {
    private class Prefix(val segments: List<Long>, val group: Int)

    /**
     * @return The index of the group the cost unit belongs to (the longest matching prefix), or null if none.
     */
    fun groupOf(kost2: Kost2DO?): Int? {
        kost2 ?: return null
        val segments = listOf(
            kost2.nummernkreis.toLong(),
            kost2.bereich.toLong(),
            kost2.teilbereich.toLong(),
            kost2.kost2Art?.id,
        )
        return prefixes
            .filter { prefix -> prefix.segments.indices.all { prefix.segments[it] == segments[it] } }
            .maxByOrNull { it.segments.size }
            ?.group
    }

    companion object {
        /** The last parsed configuration value and its groups: the value changes rarely, but is asked for often. */
        @Volatile
        private var cached: Pair<String?, TimesheetOverlapGroups>? = null

        /**
         * @return The groups as currently configured.
         */
        fun current(): TimesheetOverlapGroups {
            val value = Configuration.instance.getStringValue(ConfigurationParam.TIMESHEET_OVERLAP_GROUPS)
            cached?.let { (cachedValue, groups) ->
                if (cachedValue == value) {
                    return groups
                }
            }
            return parse(value).also { cached = value to it }
        }

        /**
         * Parses the configuration value. Invalid or duplicate prefixes are logged and ignored (for a duplicate the
         * first occurrence wins).
         */
        fun parse(value: String?): TimesheetOverlapGroups {
            val prefixes = mutableListOf<Prefix>()
            value?.split(';')?.forEachIndexed { group, groupValue ->
                groupValue.split(',').forEach prefix@{ rawPrefix ->
                    val prefixValue = rawPrefix.replace(" ", "").removeSuffix("*").removeSuffix(".")
                    if (prefixValue.isEmpty()) {
                        return@prefix
                    }
                    val segments = prefixValue.split('.').map { it.toLongOrNull() }
                    if (segments.size > 4 || segments.any { it == null || it < 0 }) {
                        log.warn { "Invalid cost 2 prefix '$rawPrefix' in ${ConfigurationParam.TIMESHEET_OVERLAP_GROUPS.key} ignored: '$value'." }
                        return@prefix
                    }
                    if (prefixes.any { it.segments == segments }) {
                        log.warn { "Duplicate cost 2 prefix '$rawPrefix' in ${ConfigurationParam.TIMESHEET_OVERLAP_GROUPS.key} ignored: '$value'." }
                        return@prefix
                    }
                    prefixes.add(Prefix(segments.filterNotNull(), group))
                }
            }
            return TimesheetOverlapGroups(prefixes)
        }
    }
}
