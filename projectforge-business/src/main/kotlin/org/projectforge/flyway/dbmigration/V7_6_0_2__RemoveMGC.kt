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
import org.projectforge.Constants
import org.projectforge.business.fibu.EmployeeDO
import org.projectforge.business.fibu.EmployeeStatus
import org.projectforge.business.fibu.EmployeeValidityPeriodAttrDO
import org.projectforge.business.fibu.EmployeeValidityPeriodAttrType
import org.projectforge.business.orga.VisitorbookDO
import org.projectforge.business.orga.VisitorbookEntryDO
import org.springframework.dao.DuplicateKeyException
import org.springframework.jdbc.core.JdbcTemplate
import org.springframework.jdbc.datasource.DriverManagerDataSource
import org.springframework.jdbc.support.rowset.SqlRowSet
import java.io.File
import javax.sql.DataSource

private val log = KotlinLogging.logger {}

/**
 * Migrates old mgc attributes (timed) from employee and visitorbook to new validity period attributes.
 */
class V7_6_0_2___RemoveMGC : BaseJavaMigration() {
    override fun migrate(context: Context) {
        log.info { "Migrating attributes with period validity from mgc: Employee (annual leave, status) and entries from Visitorbook" }
        val dataSource = context.configuration.dataSource
        migrateEmployees(dataSource)
        migrateVisitorbook(dataSource)
    }

    internal fun migrateHistoryAttrWithData(dataSource: DataSource) {
        class HistoryAttr(val pk: Long, var value: String, val alreadyProcessed: Boolean)

        val jdbcTemplate = JdbcTemplate(dataSource)
        var resultSet =
            jdbcTemplate.queryForRowSet("select t.datacol, t.datarow, t.parent_pk from t_pf_history_attr_data as t order by parent_pk, datarow")
        var parent = HistoryAttr(-1, "", true)
        while (resultSet.next()) {
            val value = resultSet.getString("datacol")
            val rowNumber = resultSet.getInt("datarow")
            val parentPk = resultSet.getLong("parent_pk")
            if (parentPk != parent.pk) {
                if (parent.pk != -1L) {
                    // Update parent before processing next parent.
                    updateParent(jdbcTemplate, parent.pk, parent.value, parent.alreadyProcessed)
                }
                val parentMap =
                    jdbcTemplate.queryForList("select t.pk, t.value from t_pf_history_attr as t where pk = ?", parentPk)
                require(parentMap.size == 1)
                val value = parentMap[0]["value"] as String
                val alreadyProcessed =
                    if (value.length > 2990) { // Old length of value field was 2990.
                        log.info { "Entry already processed (migration called twice?). Ignoring t_pf_history_attr_data for entry with pk=$parentPk, value.length=${value.length}." }
                        true
                    } else {
                        false
                    }
                parent = HistoryAttr(parentPk, value, alreadyProcessed)
            }
            parent.value += value
        }
        updateParent(jdbcTemplate, parent.pk, parent.value, parent.alreadyProcessed)
        val userIds = mutableSetOf<Long>()
        resultSet = jdbcTemplate.queryForRowSet("select t.pk as pk from t_pf_user as t")
        while (resultSet.next()) {
            userIds.add(resultSet.getLong("pk"))
        }
    }

    private fun updateParent(jdbcTemplate: JdbcTemplate, pk: Long, value: String, alreadyProcessed: Boolean) {
        if (!alreadyProcessed) {
            if (value.length > 50000) {
                throw IllegalArgumentException("Value too long: ${value.length} (> 50.000)")
            }
            log.info { "Migrating t_pf_history_attr entry with attr_data: $pk" }
            // jdbcTemplate.update("update t_pf_history_attr set value = ? where pk = ?", value, pk)
            // println("update t_pf_history_attr set value = $value where pk = $pk")
        }
    }

    internal fun migrateEmployees(dataSource: DataSource) {
        val jdbcTemplate = JdbcTemplate(dataSource)
        readEmployees(dataSource).forEach { dbAttr ->
            val oldAttr = dbAttr.getTransientAttribute("oldAttr") as TimedAttr
            val sqlInsert =
                "INSERT INTO t_fibu_employee_validity_period_attr (pk, created, last_update, deleted, employee_fk, attribute, valid_from, value) VALUES (?, ?, ?, ?, ?, ?, ?, ?)"
            try {
                jdbcTemplate.update(
                    sqlInsert,
                    dbAttr.id,
                    oldAttr.createdatA,
                    oldAttr.modifiedatA,
                    false,
                    dbAttr.employee!!.id,
                    dbAttr.attribute!!.name,
                    dbAttr.validFrom,
                    dbAttr.value,
                )
                log.info { "Migrated employee attribute: $dbAttr" }
            } catch (ex: DuplicateKeyException) {
                log.info { "Employee already migrated (migration called twice?). Don't overwrite entry t_fibu_employee_validity_period_attr.${dbAttr.id}." }
            }
        }
    }

    internal fun readEmployees(dataSource: DataSource): List<EmployeeValidityPeriodAttrDO> {
        val list = read(
            dataSource,
            "t_fibu_employee",
            "select a.pk as pk1, a.createdat, a.createdby, a.modifiedat, a.modifiedby, a.start_time, a.end_time, a.employee_id as object_id, a.group_name, b.pk as pk2, b.value, b.propertyname, b.createdby as createdby_b, b.createdat as createdat_b, b.modifiedby as modifiedby_b, b.modifiedat as modifiedat_b "
                    + "from t_fibu_employee_timed a JOIN t_fibu_employee_timedattr b ON a.pk=b.parent "
                    + "WHERE a.group_name IN ('employeestatus', 'employeeannualleave') ORDER BY a.pk",
            type = TYPE.EMPLOYEE,
        )
        val resultList = mutableListOf<EmployeeValidityPeriodAttrDO>()
        list.forEach { attr ->
            val dbAttr = EmployeeValidityPeriodAttrDO()
            dbAttr.id = attr.pk2 // Re-use the pk.
            if (attr.groupName == "employeestatus") {
                val status = EmployeeStatus.findByi18nKey(attr.value)
                    ?: throw IllegalStateException("Status not found for '${attr.value}' for attribute: $attr")
                dbAttr.value = status.name
                dbAttr.attribute = EmployeeValidityPeriodAttrType.STATUS
            } else if (attr.groupName == "employeeannualleave") {
                dbAttr.value = attr.value
                dbAttr.attribute = EmployeeValidityPeriodAttrType.ANNUAL_LEAVE
            } else {
                throw IllegalStateException("Error, unknown group: ${attr.groupName}")
            }
            val employee = EmployeeDO()
            employee.id = attr.objectId
            dbAttr.employee = employee
            dbAttr.validFrom = attr.startTime?.toLocalDate()
            dbAttr.setTransientAttribute("oldAttr", attr)
            resultList.add(dbAttr)
            log.info { "Read employee attribute: $dbAttr" }
        }
        return resultList
    }

    internal fun migrateVisitorbook(dataSource: DataSource) {
        val dbAttrs = readVisitorBook(dataSource)
        dbAttrs.forEach { dbAttr ->
            if (dbAttr.getTransientAttribute("oldArrivedAttr") == null && dbAttr.getTransientAttribute("oldDepartedAttr") == null) {
                throw IllegalStateException("oldArrivedAttr and oldDepartedAttr is null: $dbAttr")
            }
        }
        val jdbcTemplate = JdbcTemplate(dataSource)
        dbAttrs.forEach { dbAttr ->
            val oldAttr = dbAttr.getTransientAttribute("oldArrivedAttr") as? TimedAttr?
                ?: dbAttr.getTransientAttribute("oldDepartedAttr") as? TimedAttr?
            requireNotNull(oldAttr)
            val sqlInsert =
                "INSERT INTO t_orga_visitorbook_entry (pk, created, last_update, deleted, visitorbook_fk, date_of_visit, arrived, departed) VALUES (?, ?, ?, ?, ?, ?, ?, ?)"
            try {
                jdbcTemplate.update(
                    sqlInsert,
                    dbAttr.id,
                    oldAttr.createdatB,
                    oldAttr.modifiedatB, // Ignore oldDepartedAttr (doesn't really matter)
                    false,
                    dbAttr.visitorbook!!.id,
                    dbAttr.dateOfVisit,
                    dbAttr.arrived,
                    dbAttr.departed,
                )
                log.info { "Migrated visitorbook entry: $dbAttr" }
            } catch (ex: DuplicateKeyException) {
                log.info { "Visitorbook already migrated (migration called twice?). Don't overwrite entry t_orga_visitorbook_entry.${dbAttr.id}." }
            }
        }
    }

    internal fun readVisitorBook(dataSource: DataSource): List<VisitorbookEntryDO> {
        val list = read(
            dataSource,
            "t_orga_visitorbook",
            "select a.pk as pk1, a.createdat, a.createdby, a.modifiedat, a.modifiedby, a.start_time, a.visitor_id as object_id, a.group_name, b.pk as pk2, b.value, b.propertyname, b.createdby as createdby_b, b.createdat as createdat_b, b.modifiedby as modifiedby_b, b.modifiedat as modifiedat_b "
                    + "from t_orga_visitorbook_timed a JOIN t_orga_visitorbook_timedattr b ON a.pk=b.parent ORDER BY a.pk",
            readEndTime = false,
            type = TYPE.VISITORBOOK,
        )
        val result = mutableListOf<VisitorbookEntryDO>()
        list.forEach { attr ->
            if (attr.startTime == null) {
                throw IllegalStateException("startTime is null: $attr")
            }
            val existingAttr =
                result.find { it.visitorbook?.id == attr.objectId && it.dateOfVisit == attr.startTime.toLocalDate() }
            val dbAttr = existingAttr ?: VisitorbookEntryDO()
            if (existingAttr == null) {
                result.add(dbAttr)
                dbAttr.id = attr.pk2
            }
            val visitorbook = VisitorbookDO()
            visitorbook.id = attr.objectId
            if (visitorbook.id == null) {
                throw IllegalStateException("visitorbook.id is null: $attr")
            }
            dbAttr.visitorbook = visitorbook
            dbAttr.dateOfVisit = attr.startTime.toLocalDate()
            if (dbAttr.dateOfVisit == null) {
                throw IllegalStateException("Date of visit (startTime) is null: $attr")
            }
            // modifiedat dates aren't correct, but migrate them anyway.
            if (attr.propertyname == "arrive") {
                if (existingAttr?.arrived != null) {
                    throw IllegalStateException("Arrived already set: $dbAttr")
                }
                dbAttr.arrived = attr.value // Null allowed.
                dbAttr.setTransientAttribute("oldArrivedAttr", attr)
            } else if (attr.propertyname == "depart") {
                if (existingAttr?.departed != null) {
                    throw IllegalStateException("Departed already set: $dbAttr")
                }
                dbAttr.departed = attr.value // Null allowed.
                dbAttr.setTransientAttribute("oldDepartedAttr", attr)
            } else {
                throw IllegalStateException("Unknown propertyname: ${attr.propertyname}")
            }
        }
        result.forEach { dbAttr ->
            log.info { "Read visitorbook attributes: $dbAttr" }
        }
        return result
    }

    private fun read(
        dataSource: DataSource,
        masterTable: String,
        joinSelectSql: String,
        readEndTime: Boolean = true,
        type: TYPE,
    ): List<TimedAttr> {
        val jdbcTemplate = JdbcTemplate(dataSource)
        val validMasterIds = mutableSetOf<Long>()
        var resultSet = jdbcTemplate.queryForRowSet("select t.pk as object_id from $masterTable as t")
        while (resultSet.next()) {
            validMasterIds.add(resultSet.getLong("object_id"))
        }
        val userIds = mutableSetOf<Long>()
        resultSet = jdbcTemplate.queryForRowSet("select t.pk as pk from t_pf_user as t")
        // select a.pk as pk1, a.createdat, a.createdby, a.modifiedat, a.modifiedby, a.start_time, a.end_time, a.employee_id as object_id, a.group_name, b.pk as pk2, b.value, b.propertyname, b.createdby as createdby_b, b.createdat as createdat_b, b.modifiedby as modifiedby_b, b.modifiedat as modifiedat_b from t_fibu_employee_timed a JOIN t_fibu_employee_timedattr b ON a.pk=b.parent WHERE a.group_name IN ('employeestatus', 'employeeannualleave') ORDER BY pk
        while (resultSet.next()) {
            userIds.add(resultSet.getLong("pk"))
        }
        val list = mutableListOf<TimedAttr>()
        resultSet =
            jdbcTemplate.queryForRowSet(joinSelectSql)
        while (resultSet.next()) {
            val data = TimedAttr(
                resultSet,
                type = type,
            )
            if (!validMasterIds.contains(data.objectId)) {
                throw IllegalStateException("Object with id=${data.objectId} not found in $masterTable: $data")
            }
            list.add(data)
        }
        if (type == TYPE.EMPLOYEE) {
            val occurrence = list.groupingBy { it.pk1 }.eachCount()
            val notUnique = list.filter { (occurrence[it.pk1] ?: 0) > 1 }
            if (notUnique.isNotEmpty()) {
                notUnique.forEach { log.error("Not unique: ${it.pk1}, group=${it.groupName}, property=${it.propertyname}") }
                throw IllegalStateException("Not unique entries found: ${notUnique.size}")
            }
        } else {
            val occurrence = list.groupingBy { it.pk2 }.eachCount()
            val notUnique = list.filter { (occurrence[it.pk2] ?: 0) > 1 }
            if (notUnique.isNotEmpty()) {
                notUnique.forEach { log.error("Not unique: ${it.pk2}, group=${it.groupName}, property=${it.propertyname}") }
                throw IllegalStateException("Not unique entries found: ${notUnique.size}")
            }
        }
        return list
    }
}

private enum class TYPE { EMPLOYEE, VISITORBOOK }

private class TimedAttr(resultSet: SqlRowSet, type: TYPE) {
    val pk1 = resultSet.getLong("pk1")
    val pk2 = resultSet.getLong("pk2")
    val createdatA = resultSet.getTimestamp("createdat")
    val createdbyA = resultSet.getString("createdby")
    val modifiedatA = resultSet.getTimestamp("modifiedat")
    val modifiedbyA = resultSet.getString("modifiedby")
    val startTime = resultSet.getDate("start_time")
    val endTime = if (type == TYPE.EMPLOYEE) resultSet.getDate("end_time") else null

    /**
     * The employee id or visitor id.
     */
    val objectId = resultSet.getLong("object_id")
    val groupName = resultSet.getString("group_name")
    val value = resultSet.getString("value")
    val propertyname = resultSet.getString("propertyname")
    val createdatB = resultSet.getTimestamp("createdat_b")
    val createdbyB = resultSet.getString("createdby_b")
    val modifiedatB = resultSet.getTimestamp("modifiedat_b")
    val modifiedbyB = resultSet.getString("modifiedby_b")

    init {
        if (endTime != null) {
            throw IllegalStateException("endTime isn't null in entry with pk=$pk1: $endTime")
        }
        if (type == TYPE.EMPLOYEE && createdbyA != createdbyB) {
            throw IllegalStateException("createdby doesn't match with pk=$pk1: createdbyA: $createdbyA != createdbyB: $createdbyB")
        }
        if (type == TYPE.EMPLOYEE && !isSameTimestamp(createdatA, createdatB)) {
            throw IllegalStateException("createdat doesn't match with pk=$pk1: createdatA: $createdatA != createdatB: $createdatB")
        }
        if (type == TYPE.EMPLOYEE && modifiedbyA != modifiedbyB) {
            throw IllegalStateException("modifiedby doesn't match with pk=$pk1: modifiedbyA: $modifiedbyA != modifiedbyB: $modifiedbyB")
        }
        if (type == TYPE.EMPLOYEE && !isSameTimestamp(modifiedatA, modifiedatB)) {
            throw IllegalStateException("modifiedat doesn't match with pk=$pk1: modifiedatA: $modifiedatA != modifiedatB: $modifiedatB")
        }
        if (type == TYPE.EMPLOYEE) {
            if (groupName == "employeeannualleave" && propertyname != "employeeannualleavedays" ||
                groupName == "employeestatus" && propertyname != "status"
            ) {
                throw IllegalStateException("propertyname isn't '$propertyname' for groupName '$groupName' in entry with pk=$pk1: $propertyname")
            }
        } else {
            if (groupName != "timeofvisit" || propertyname != "arrive" && propertyname != "depart") {
                throw IllegalStateException("groupName isn't 'visitorbook' and or propertyname isn't 'arrived/departed' in entry with pk=$pk1: groupName=$groupName, propertyname=$propertyname")
            }
        }
        log.info { "Read $type: $this" }
    }

    fun isSameTimestamp(a: java.sql.Timestamp?, b: java.sql.Timestamp?): Boolean {
        if (a == null && b == null) {
            return true
        }
        if (a == null || b == null) {
            return false
        }
        return Math.abs(a.time - b.time) < Constants.MILLIS_PER_HOUR
    }

    override fun toString(): String {
        return "pk=$pk1: created=${show(createdatA, createdatB)} (${show(createdbyA, createdbyB)}), modified=${
            show(
                modifiedatA,
                modifiedatB
            )
        } (${
            show(
                modifiedbyA,
                modifiedbyB
            )
        }) object=$objectId. group=$groupName, property=$propertyname, value=$value, start=$startTime"
    }

    fun show(timestampA: java.sql.Timestamp?, timestampB: java.sql.Timestamp?): String {
        return if (isSameTimestamp(timestampA, timestampB)) {
            timestampA.toString()
        } else {
            "($timestampA, $timestampB)"
        }
    }

    fun show(byA: String?, byB: String?): String {
        return if (byA == byB) {
            byA ?: "null"
        } else {
            "$byA, $byB"
        }
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
    // V7_6_0_2___RemoveMGC().readEmployees(dataSource)
    // V7_6_0_2___RemoveMGC().migrateEmployees(dataSource)
    // V7_6_0_2___RemoveMGC().readVisitorBook(dataSource)
    // V7_6_0_2___RemoveMGC().migrateVisitorbook(dataSource)
    // V7_6_0_2___RemoveMGC().migrateHistoryAttrWithData(dataSource)
}
