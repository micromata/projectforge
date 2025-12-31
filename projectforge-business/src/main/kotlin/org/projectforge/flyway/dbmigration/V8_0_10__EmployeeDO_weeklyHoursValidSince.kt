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

package org.projectforge.flyway.dbmigration

import mu.KotlinLogging
import org.flywaydb.core.api.migration.BaseJavaMigration
import org.flywaydb.core.api.migration.Context
import org.projectforge.business.fibu.EmployeeValidSinceAttrType
import org.projectforge.framework.persistence.database.JdbcUtils.getBigDecimal
import org.projectforge.framework.persistence.database.JdbcUtils.getDate
import org.projectforge.framework.persistence.database.JdbcUtils.getInt
import org.projectforge.framework.persistence.database.JdbcUtils.getLocalDate
import org.projectforge.framework.persistence.database.JdbcUtils.getLong
import org.springframework.jdbc.datasource.DriverManagerDataSource
import java.io.File
import java.sql.Connection
import java.sql.Timestamp
import java.time.LocalDate
import java.time.ZoneId
import java.util.*

private val log = KotlinLogging.logger {}

/**
 * Adds the weekly working hours of employees as timed attribute.
 *
 * Rollback:
 * ```
 * delete from t_fibu_employee_valid_since_attr where type='WEEKLY_HOURS';
 * ```
 */
@Suppress("ClassName")
class V8_0_10__EmployeeDO_weeklyHoursValidSince : BaseJavaMigration() {
    companion object {
        private val SELECT_EMPLOYEES = """
            SELECT t.pk, t.created, eintritt, weekly_working_hours
            FROM t_fibu_employee AS t
            ORDER BY t.pk DESC
            """.trimIndent()
        private val INSERT_EMPLOYEE_WEEKLYHOURS = """
            INSERT INTO t_fibu_employee_valid_since_attr
                    (pk, created, last_update, deleted, employee_fk, type, valid_since, value)
                    VALUES (?, ?, ?, ?, ?, ?, ?, ?)
            """.trimIndent()
    }

    override fun migrate(context: Context) {
        log.info { "Adding weekly hours for employee's as timed attribute." }
        // JdbcTemplate can't be used, because autoCommit is set to false since PF 8.0 and JdbcTemplate doesn't support autoCommit.
        val connection = context.connection
        try {
            connection.autoCommit = true
            setInitialWeeklyHours(connection)
        } catch (ex: Exception) {
            log.error(ex.message, ex)
            throw ex
        } finally {
            connection.autoCommit = false
        }
    }

    internal fun setInitialWeeklyHours(connection: Connection) {
        connection.createStatement().use { statement ->
            statement.executeQuery(SELECT_EMPLOYEES).use { resultSet ->
                while (resultSet.next()) {
                    val id = getLong(resultSet, "pk")!!
                    val created = getDate(resultSet, "created")
                    val eintritt = getLocalDate(resultSet, "eintritt")
                    val weeklyWorkingHours = getBigDecimal(resultSet, "weekly_working_hours") ?: continue

                    try {
                        log.info { "Creating weekly-hours entry id=$id, created=$created, eintritt=$eintritt: weeklyWorkingHours=$weeklyWorkingHours..." }
                        connection.prepareStatement(INSERT_EMPLOYEE_WEEKLYHOURS).use { statement ->
                            statement.setLong(1, id) // Simply use the same id.
                            created?.getTime()?.let {
                                statement.setTimestamp(2, Timestamp(it))
                                statement.setTimestamp(3, Timestamp(it))
                            } ?: run {
                                statement.setTimestamp(2, null)
                                statement.setTimestamp(3, null)
                            }
                            statement.setBoolean(4, false)
                            statement.setLong(5, id)
                            statement.setString(6, EmployeeValidSinceAttrType.WEEKLY_HOURS.name)
                            statement.setDate(7, java.sql.Date.valueOf(eintritt))
                            statement.setBigDecimal(8, weeklyWorkingHours.stripTrailingZeros())
                            statement.executeUpdate()
                        }
                    } catch (ex: Exception) {
                        log.info { "Oups, can't insert employee attr: ex=${ex.message}" }
                    }
                }
            }
        }
    }

    private fun convertToLocalDate(date: Date): LocalDate {
        if (date is java.sql.Date) {
            return date.toLocalDate()
        }
        return date.toInstant().atZone(ZoneId.systemDefault()).toLocalDate()
    }

    private fun convertToDate(localDate: LocalDate?): Date? {
        localDate ?: return null
        return Date.from(localDate.atStartOfDay(ZoneId.systemDefault()).toInstant())
    }
}

// Test main
fun main() {
    val jdbcUrl = "jdbc:postgresql://localhost:5432/projectforge"
    val username = "projectforge"
    val userhome = System.getProperty("user.home")
    val password = File("$userhome/tmp/PGPASSWORD.txt").readText().trim()

    val dataSource = DriverManagerDataSource().apply {
        setDriverClassName("org.postgresql.Driver")
        url = jdbcUrl
        this.username = username
        this.password = password
    }
    val connection = dataSource.connection
    V8_0_10__EmployeeDO_weeklyHoursValidSince().setInitialWeeklyHours(connection)
}
