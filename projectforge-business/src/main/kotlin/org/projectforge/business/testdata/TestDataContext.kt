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

import org.projectforge.business.fibu.EmployeeDO
import org.projectforge.business.fibu.KundeDO
import org.projectforge.business.fibu.ProjektDO
import org.projectforge.business.fibu.kost.Kost2ArtDO
import org.projectforge.business.fibu.kost.Kost2DO
import org.projectforge.business.task.TaskDO
import org.projectforge.framework.persistence.user.entities.GroupDO
import org.projectforge.framework.persistence.user.entities.PFUserDO
import java.time.LocalDate
import java.time.YearMonth
import java.time.ZoneId
import java.time.temporal.ChronoUnit
import java.util.Random

/**
 * Shared state of one test data run: the reference date and the objects created so far, by a speaking key,
 * so contributors refer to each other's data without ids.
 *
 * The sample data was designed for [DESIGN_DATE] (Tuesday); [week] and [month] shift a date of that design by
 * the weeks or months since then, so the data always looks current.
 *
 * @param password The password of the sample users (the one of the admin chosen in the setup), null for none.
 * @param scale Multiplies the period with time sheets (projectforge.testdata.scale).
 */
class TestDataContext(
    val password: String?,
    val scale: Int = 1,
    val today: LocalDate = LocalDate.now(),
) {
    val zone: ZoneId = ZoneId.of("Europe/Berlin")

    /** Deterministic, so every test system gets the same data. */
    val random = Random(4711)

    private val shiftWeeks = ChronoUnit.WEEKS.between(DESIGN_DATE, today)

    private val shiftMonths = ChronoUnit.MONTHS.between(YearMonth.from(DESIGN_DATE), YearMonth.from(today))

    /** A date of the design, shifted by whole weeks (keeps the day of week, e.g. for vacations). */
    fun week(year: Int, month: Int, day: Int): LocalDate = LocalDate.of(year, month, day).plusWeeks(shiftWeeks)

    /** A date of the design, shifted by whole months (keeps the day of month, e.g. for orders and invoices). */
    fun month(year: Int, month: Int, day: Int = 1): LocalDate = LocalDate.of(year, month, day).plusMonths(shiftMonths)

    val users = mutableMapOf<String, PFUserDO>()
    val groups = mutableMapOf<String, GroupDO>()
    val employees = mutableMapOf<String, EmployeeDO>()
    val kost2Arts = mutableMapOf<Long, Kost2ArtDO>()
    val kunden = mutableMapOf<Long, KundeDO>()

    /** By project name. */
    val projekte = mutableMapOf<String, ProjektDO>()

    /** Sub tasks of the projects, by project name and task title. */
    val projectTasks = mutableMapOf<Pair<String, String>, TaskDO>()

    /** Cost 2 of the projects, by project name and cost 2 type id. */
    val kost2s = mutableMapOf<Pair<String, Long>, Kost2DO>()

    /** The company task below the root task. */
    var companyTask: TaskDO? = null

    /** Approved vacations and days not available (user name and period): no time sheets on these days. */
    val absences = mutableListOf<Triple<String, LocalDate, LocalDate>>()

    /** Set by [TestDataSeeder] for the running contributor. */
    internal var progressListener: (Double) -> Unit = {}

    /**
     * Reports the progress of the running contributor, for long running ones.
     * @param fraction 0.0 (started) to 1.0 (done).
     */
    fun reportProgress(fraction: Double) {
        progressListener(fraction.coerceIn(0.0, 1.0))
    }

    /** The contributors that failed (logged and rolled back), by class name. */
    val failed = mutableListOf<String>()

    fun user(username: String): PFUserDO = users[username] ?: error("Test data: unknown user '$username'.")

    fun employee(username: String): EmployeeDO =
        employees[username] ?: error("Test data: no employee for '$username'.")

    fun projekt(name: String): ProjektDO = projekte[name] ?: error("Test data: unknown project '$name'.")

    fun task(projekt: String, title: String): TaskDO =
        projectTasks[projekt to title] ?: error("Test data: unknown task '$title' of '$projekt'.")

    fun kost2(projekt: String, kost2ArtId: Long): Kost2DO =
        kost2s[projekt to kost2ArtId] ?: error("Test data: unknown cost 2 '$kost2ArtId' of '$projekt'.")

    companion object {
        val DESIGN_DATE: LocalDate = LocalDate.of(2026, 10, 13)
    }
}
