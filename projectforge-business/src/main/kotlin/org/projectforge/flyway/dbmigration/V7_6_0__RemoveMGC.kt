/////////////////////////////////////////////////////////////////////////////
//
// Project ProjectForge Community Edition
//         www.projectforge.org
//
// Copyright (C) 2001-2024 Micromata GmbH, Germany (www.micromata.com)
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
import org.projectforge.business.fibu.EmployeeDO
import org.projectforge.business.fibu.EmployeeValidityPeriodAttrType
import org.projectforge.business.fibu.EmployeeValidityPeriodAttrDO
import org.springframework.jdbc.core.JdbcTemplate
import org.springframework.jdbc.core.simple.SimpleJdbcInsert
import org.springframework.jdbc.datasource.DriverManagerDataSource
import org.springframework.jdbc.support.rowset.SqlRowSet
import java.io.File
import java.util.*
import javax.sql.DataSource

private val log = KotlinLogging.logger {}

/**
 * Migrates old mgc attributes (timed) from employee and visitorbook to new validity period attributes.
 */
class V7_6_0__RemoveMGC : BaseJavaMigration() {
    override fun migrate(context: Context) {
        log.info { "Migrating attributes with period validity from mgc: Employee (annual leave, status) and Visitorbook" }
        val ds = context.configuration.dataSource

        val jdbcTemplate = JdbcTemplate(ds)
        val validEmployeeIds = mutableSetOf<Int>()
        val resultSet = jdbcTemplate.queryForRowSet("select e.pk as employeeid from t_fibu_employee as e")
        while (resultSet.next()) {
            validEmployeeIds.add(resultSet.getInt("employeeid"))
        }
        val attr = doIt(ds)


        var counter = 0
        val simpleJdbcInsert = SimpleJdbcInsert(jdbcTemplate)
        simpleJdbcInsert.withTableName("t_fibu_employee_validity_period_attr")
        simpleJdbcInsert.usingGeneratedKeyColumns("pk")
        val sql =
            "SELECT * FROM t_fibu_employee_timedattr WHERE group_name = 'employeeannualleave' OR group_name = 'employeestatus'"
        val rows = jdbcTemplate.queryForList(sql)
        for (row in rows) {
            val pk = row["pk"] as Int
            val parent = row["parent"] as Int
            val value = row["value"] as String
            val propertyname = row["propertyname"] as String
            val createdby = row["createdby"] as String
            val createdat = row["createdat"] as Date
            val modifiedby = row["modifiedby"] as String
            val modifiedat = row["modifiedat"] as Date
            val sqlInsert =
                "INSERT INTO t_fibu_employee_validity_period_attr (employee_id, attribute, valid_from, value, createdby, createdat, modifiedby, modifiedat) VALUES (?, ?, ?, ?, ?, ?, ?, ?)"
            jdbcTemplate.update(
                sqlInsert,
                parent,
                propertyname,
                createdat,
                value,
                createdby,
                createdat,
                modifiedby,
                modifiedat
            )
            val sqlDelete = "DELETE FROM t_fibu_employee_timedattr WHERE pk = ?"
            jdbcTemplate.update(sqlDelete, pk)
        }
    }

    internal fun doIt(dataSource: DataSource) {
        val list = mutableListOf<EmployeeTimedAttr>()
        val jdbcTemplate = JdbcTemplate(dataSource)
        val resultSet =
            jdbcTemplate.queryForRowSet("select a.pk, a.createdat, a.createdby, a.modifiedat, a.modifiedby, a.start_time, a.end_time, a.employee_id, a.group_name, b.value, b.propertyname, b.createdby as createdby_b, b.createdat as createdat_b, b.modifiedby as modifiedby_b, b.modifiedat as modifiedat_b from t_fibu_employee_timed a JOIN t_fibu_employee_timedattr b ON a.pk=b.parent ORDER BY pk")

        // ResultSet verarbeiten
        while (resultSet.next()) {
            val data = EmployeeTimedAttr(resultSet)
            if (data.groupName == "employeestatus" || data.groupName == "employeeannualleave") {
                list.add(data)
            }
        }

        val occurrence = list.groupingBy { it.pk }.eachCount()

        val notUnique = list.filter { (occurrence[it.pk] ?: 0) > 1 }
        if (notUnique.isNotEmpty()) {
            notUnique.forEach { log.error("Not unique: ${it.pk}, group=${it.groupName}, property=${it.propertyname}") }
            throw IllegalStateException("Not unique entries found: ${notUnique.size}")
        }
        val resultList = mutableListOf<EmployeeValidityPeriodAttrDO>()
        list.forEach { attr ->
            val dbAttr = EmployeeValidityPeriodAttrDO()
            dbAttr.value = attr.value
            dbAttr.attribute =
                if (attr.groupName == "employeestatus") {
                    EmployeeValidityPeriodAttrType.STATUS
                } else if (attr.groupName == "employeeannualleave") {
                    EmployeeValidityPeriodAttrType.ANNUAL_LEAVE
                } else {
                    throw IllegalStateException("Error, unknown group: ${attr.groupName}")
                }
            val employee = EmployeeDO()
            employee.id = attr.employeeId
            dbAttr.employee = employee
            dbAttr.validFrom = attr.startTime?.toLocalDate()
            resultList.add(dbAttr)
            log.info { "Migration of employee attribute: $dbAttr" }
        }
        // Einträge generieren und History generieren.
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
    V7_6_0__RemoveMGC().doIt(dataSource)
}

private class EmployeeTimedAttr(resultSet: SqlRowSet) {
    val pk = resultSet.getInt("pk")
    val createdatA = resultSet.getTimestamp("createdat")
    val createdbyA = resultSet.getString("createdby")
    val modifiedatA = resultSet.getTimestamp("modifiedat")
    val modifiedbyA = resultSet.getString("modifiedby")
    val startTime = resultSet.getDate("start_time")
    val endTime = resultSet.getDate("end_time")
    val employeeId = resultSet.getInt("employee_id")
    val groupName = resultSet.getString("group_name")
    val value = resultSet.getString("value")
    val propertyname = resultSet.getString("propertyname")
    val createdatB = resultSet.getTimestamp("createdat_b")
    val createdbyB = resultSet.getString("createdby_b")
    val modifiedatB = resultSet.getTimestamp("modifiedat_b")
    val modifiedbyB = resultSet.getString("modifiedby_b")

    init {
        if (endTime != null) {
            throw IllegalStateException("endTime isn't null in entry with pk=$pk: $endTime")
        }
        if (createdbyA != createdbyB) {
            throw IllegalStateException("createdby doesn't match with pk=$pk: createdbyA: $createdbyA != createdbyB: $createdbyB")
        }
        if (createdatA != createdatB) {
            throw IllegalStateException("createdat doesn't match with pk=$pk: createdatA: $createdatA != createdatB: $createdatB")
        }
        if (modifiedbyA != modifiedbyB) {
            throw IllegalStateException("modifiedby doesn't match with pk=$pk: modifiedbyA: $modifiedbyA != modifiedbyB: $modifiedbyB")
        }
        if (modifiedatA != modifiedatB) {
            throw IllegalStateException("modifiedat doesn't match with pk=$pk: modifiedatA: $modifiedatA != modifiedatB: $modifiedatB")
        }
    }

    override fun toString(): String {
        return "$pk: created=$createdatA ($createdbyA), modified=$modifiedatA ($modifiedbyA) employee=$employeeId. group=$groupName, property=$propertyname, value=$value, start=$startTime"
    }
}

