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

import org.flywaydb.core.api.migration.BaseJavaMigration
import org.flywaydb.core.api.migration.Context
import org.slf4j.LoggerFactory
import org.springframework.jdbc.core.JdbcTemplate
import org.springframework.jdbc.core.simple.SimpleJdbcInsert
import java.math.BigDecimal
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.Month
import java.time.ZoneOffset
import java.util.*

/**
 * EmployeeDO will now support timed annual leave days instead of fixed annual leave days (aka urlaubstage). So [org.projectforge.business.vacation.service.VacationStats]
 * may be calculated for former years properly if the amount of annual leave days was changed.
 */
class V7_0_0_11__MigrateEmployeeAnnualLeaveDays : BaseJavaMigration() {
    override fun migrate(context: Context) {
        val ds = context.configuration.dataSource
        log.info("Trying to migrate annual leave days of employees (is now time dependent).")
        val jdbc = JdbcTemplate(ds)
        val rs = jdbc.queryForRowSet("select e.pk as employeeid, e.urlaubstage as annualleavedays, e.eintritt as joindate from t_fibu_employee as e")
        var counter = 0
        val now = Date()
        val date1900 = Date.from(LocalDateTime.of(1900, Month.JANUARY, 1, 0, 0).toInstant(ZoneOffset.UTC))
        //var timedPKCounter = jdbc.queryForObject("select max(pk) from t_fibu_employee_timed", Int::class.java) ?: 0
        //var timedAttrPKCounter = jdbc.queryForObject("select max(pk) from t_fibu_employee_timedattr", Int::class.java) ?: 0
        while (rs.next()) {
            val employeeId = rs.getInt("employeeid")
            val annualLeaveDays = rs.getBigDecimal("annualleavedays")
            val joinDate = rs.getDate("joindate")
            if (annualLeaveDays == null || annualLeaveDays.compareTo(BigDecimal.ZERO) == 0) {
                continue // Don't migrate null or zero values.
            }
            //++timedPKCounter
            //++timedAttrPKCounter
            ++counter
            var simpleJdbcInsert = SimpleJdbcInsert(ds).withTableName("t_fibu_employee_timed")
            val parameters = mutableMapOf<String, Any?>()
            parameters["pk"] = counter
            parameters["createdat"] = now
            parameters["createdby"] = "anon"
            parameters["modifiedat"] = now
            parameters["modifiedby"] = "anon"
            parameters["updatecounter"] = 0
            if (joinDate != null) {
                parameters["start_time"] = joinDate
            } else {
                parameters["start_time"] = date1900
            }
            parameters["employee_id"] = employeeId
            parameters["group_name"] = "employeeannualleave"
            simpleJdbcInsert.execute(parameters)

            simpleJdbcInsert = SimpleJdbcInsert(ds).withTableName("t_fibu_employee_timedattr")
            parameters.clear()
            parameters["withdata"] = 0
            parameters["pk"] = counter
            parameters["createdat"] = now
            parameters["createdby"] = "anon"
            parameters["modifiedat"] = now
            parameters["modifiedby"] = "anon"
            parameters["updatecounter"] = 0
            parameters["value"] = annualLeaveDays
            parameters["propertyname"] = "employeeannualleavedays"
            parameters["type"] = 'K' // ConvertedStringTypes.BIGDECIMAL.shortType
            parameters["parent"] = counter
            simpleJdbcInsert.execute(parameters)
        }
        if (counter > 0) { // counter > 0
            log.info("Number of successful migrated employee entries: $counter")
        } else {
            log.info("No employee entries found to migrate (OK, if annual leave for employees wasn't in use.)")
        }
    }

    private val log = LoggerFactory.getLogger(V7_0_0_11__MigrateEmployeeAnnualLeaveDays::class.java)

        /**
         * The bigdecimal.
         */
        // de.micromata.genome.util.strings.converter.ConvertedStringTypes.BIGDECIMAL.shortType BIGDECIMAL('K', BigDecimal::class.java),
}
