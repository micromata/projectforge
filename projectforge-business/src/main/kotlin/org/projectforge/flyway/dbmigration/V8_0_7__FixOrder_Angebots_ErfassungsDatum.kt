/////////////////////////////////////////////////////////////////////////////
//
// Project ProjectForge Community Edition
//         www.projectforge.org
//
// Copyright (C) 2001-2025 Micromata GmbH, Germany (www.micromata.com)
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
import org.projectforge.framework.persistence.database.JdbcUtils.getDate
import org.projectforge.framework.persistence.database.JdbcUtils.getInt
import org.projectforge.framework.persistence.database.JdbcUtils.getLocalDate
import org.projectforge.framework.persistence.database.JdbcUtils.getLong
import org.springframework.jdbc.datasource.DriverManagerDataSource
import java.io.File
import java.sql.Connection
import java.time.LocalDate
import java.time.Month
import java.time.ZoneId
import java.util.*
import kotlin.math.absoluteValue

private val log = KotlinLogging.logger {}

/**
 * Due to a bug from 14.12.2016, the erfassungsDatum and angebotsDatum are stored switched.
 */
@Suppress("ClassName")
class V8_0_7__FixOrder_Angebots_ErfassungsDatum : BaseJavaMigration() {
    companion object {
        private val SELECT_ORDERS = """
            SELECT t.pk, t.created, t.nummer, t.angebots_datum, t.erfassungs_datum, t.nummer
            FROM t_fibu_auftrag AS t
            ORDER BY t.pk DESC
            """.trimIndent()
        private val UPDATE_ORDER = """
            UPDATE t_fibu_auftrag set erfassungs_datum=?, angebots_datum=?
            WHERE pk = ?
        """.trimIndent()
    }

    override fun migrate(context: Context) {
        log.info { "Fixing bug since 14.12.2016 (erfassungsDatum and angebotsDatum from orders were switched)." }
        // JdbcTemplate can't be used, because autoCommit is set to false since PF 8.0 and JdbcTemplate doesn't support autoCommit.
        val connection = context.connection
        try {
            connection.autoCommit = true
            fixOrders(connection)
        } catch (ex: Exception) {
            log.error(ex.message, ex)
            throw ex
        } finally {
            connection.autoCommit = false
        }
    }

    internal fun fixOrders(connection: Connection) {
        connection.createStatement().use { statement ->
            statement.executeQuery(SELECT_ORDERS).use { resultSet ->
                while (resultSet.next()) {
                    val id = getLong(resultSet, "pk")!!
                    val dbCreated = getDate(resultSet, "created") ?: continue // Ignore (old orders before 2007).
                    val nummer = getInt(resultSet, "nummer")
                    val dbAngebotsDatum = getLocalDate(resultSet, "angebots_datum")
                    val dbErfassungsDatum = getLocalDate(resultSet, "erfassungs_datum")

                    val created = dbCreated
                    val creationDay = convertToLocalDate(created)
                    if (creationDay < LocalDate.of(2016, Month.DECEMBER, 14)) {
                        continue
                    }
                    val erfassungsDatum = dbErfassungsDatum ?: creationDay
                    val angebotsDatum = dbAngebotsDatum ?: dbErfassungsDatum ?: creationDay
                    if ((creationDay.toEpochDay() - erfassungsDatum.toEpochDay()).absoluteValue < 1) {
                        // Already fixed?
                        println("Already fixed? $nummer, created=$dbCreated, angebotsDatum=$angebotsDatum, erfassungsDatum=$erfassungsDatum")
                        continue
                    }
                    if ((creationDay.toEpochDay() - angebotsDatum.toEpochDay()).absoluteValue > 1) {
                        // Ignore these orders (created date is not the same as angebotsDatum).
                        println("Strange dates, leave it untouched: $nummer, created=$dbCreated, angebotsDatum=$angebotsDatum, erfassungsDatum=$erfassungsDatum")
                        continue
                    } else {
                        try {
                            val newErfassungsDatum = creationDay
                            val newAngebotsDatum = erfassungsDatum
                            log.info { "Fixed erfassungsDatum and angebotsDatum of order #$nummer, created=$dbCreated, angebotsDatum=$newAngebotsDatum (was $angebotsDatum), erfassungsDatum=$newErfassungsDatum (was $erfassungsDatum)" }
                            connection.prepareStatement(UPDATE_ORDER).use { statement ->
                                statement.setDate(1, java.sql.Date.valueOf(newErfassungsDatum))
                                statement.setDate(2, java.sql.Date.valueOf(newAngebotsDatum))
                                statement.setLong(3, id)
                                statement.executeUpdate()
                            }
                        } catch (ex: Exception) {
                            log.info { "Oups, can't fix order: ex=${ex.message}" }
                        }
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
    V8_0_7__FixOrder_Angebots_ErfassungsDatum().fixOrders(connection)
}
