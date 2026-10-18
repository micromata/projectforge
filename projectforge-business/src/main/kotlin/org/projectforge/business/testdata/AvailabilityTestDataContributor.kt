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


package org.projectforge.business.testdata

import org.projectforge.business.availability.AvailabilityDO
import org.projectforge.business.availability.AvailabilityDao
import org.projectforge.business.availability.AvailabilityLocation
import org.projectforge.business.availability.AvailabilityStatus
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Service
import java.time.LocalDate

/**
 * Availability entries around today, one of each state. The substitute Max isn't available during Alex's vacation,
 * which makes a conflict of it, and works remotely during Julia's, which doesn't. Alex in turn is the only substitute
 * of Max's training and on vacation then (a conflict of the entry); Chris's parental leave has two substitutes, one of
 * them on vacation for a while (no conflict). The types are the default ones of
 * projectforge.availability.types (AvailabilityConfig).
 */
@Service
open class AvailabilityTestDataContributor : TestDataContributor {
    @Autowired
    private lateinit var availabilityDao: AvailabilityDao

    // After the vacations (substitutes), before the time sheets (none on the days not available).
    override val order = 35

    private class Entry(
        val user: String,
        val from: LocalDate,
        val to: LocalDate,
        val status: AvailabilityStatus,
        val type: String,
        val comment: String,
        val location: AvailabilityLocation? = null,
        val internalComment: String? = null,
        val substitutes: List<String> = emptyList(),
    )

    override fun seed(ctx: TestDataContext) {
        listOf(
            Entry(
                "julia", ctx.week(2026, 10, 12), ctx.week(2026, 10, 16), AvailabilityStatus.REMOTE_REACHABLE,
                "Workation", "Workation in Lisbon, reachable by chat and phone", AvailabilityLocation.REMOTE,
            ),
            Entry(
                "ann", ctx.week(2026, 10, 15), ctx.week(2026, 10, 16), AvailabilityStatus.REACHABLE_IF_URGENT,
                "Zeitausgleich", "By phone in urgent cases only",
            ),
            Entry(
                "max", ctx.week(2026, 10, 21), ctx.week(2026, 10, 23), AvailabilityStatus.NOT_AVAILABLE,
                "Abwesenheit", "Training in Berlin", internalComment = "Booked by HR, costs covered",
                substitutes = listOf("alex"),
            ),
            Entry(
                "joe", ctx.week(2026, 10, 26), ctx.week(2026, 10, 28), AvailabilityStatus.REACHABLE_IF_NEEDED,
                "Abwesenheit", "Customer workshop at Alpine Insurance, Innsbruck", AvailabilityLocation.ON_SITE,
            ),
            Entry(
                "max", ctx.week(2026, 11, 9), ctx.week(2026, 11, 13), AvailabilityStatus.REMOTE_REACHABLE,
                "Workation", "Working from the Black Forest", AvailabilityLocation.REMOTE,
            ),
            Entry(
                "chris", ctx.week(2026, 11, 23), ctx.week(2027, 1, 29), AvailabilityStatus.NOT_AVAILABLE,
                "Elternzeit", "Parental leave, back at the end of January",
                internalComment = "Parental allowance applied for",
                substitutes = listOf("max", "laura"),
            ),
        ).forEach { e ->
            val availability = AvailabilityDO().also {
                it.user = ctx.user(e.user)
                it.startDate = e.from
                it.endDate = e.to
                it.status = e.status
                it.type = e.type
                it.location = e.location
                it.comment = e.comment
                it.internalComment = e.internalComment
                if (e.substitutes.isNotEmpty()) {
                    it.substitutes = e.substitutes.map { name -> ctx.user(name) }.toMutableSet()
                }
            }
            availabilityDao.insert(availability, checkAccess = false)
            if (e.status.absent) {
                ctx.absences.add(Triple(e.user, e.from, e.to))
            }
        }
    }
}
