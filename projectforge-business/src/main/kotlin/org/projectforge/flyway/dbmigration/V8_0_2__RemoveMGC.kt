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
import org.projectforge.business.fibu.EmployeeValidSinceAttrDO
import org.projectforge.business.fibu.EmployeeValidSinceAttrType
import org.projectforge.business.orga.VisitorbookDO
import org.projectforge.business.orga.VisitorbookEntryDO
import org.projectforge.framework.persistence.database.JdbcUtils.getLocalDate
import org.projectforge.framework.persistence.database.JdbcUtils.getLong
import org.projectforge.framework.persistence.database.JdbcUtils.getString
import org.projectforge.framework.persistence.database.JdbcUtils.getTimestamp
import org.springframework.dao.DuplicateKeyException
import org.springframework.jdbc.core.JdbcTemplate
import org.springframework.jdbc.datasource.DriverManagerDataSource
import java.io.File
import java.sql.Connection
import java.sql.ResultSet
import kotlin.math.abs

private val log = KotlinLogging.logger {}

/**
 * Migrates old mgc attributes (timed) from employee and visitorbook to new validity period attributes.
 */
@Suppress("ClassName")
class V8_0_2__RemoveMGC : BaseJavaMigration() {
    companion object {
        private val SELECT_HISTORY_ATTR_DATA = """
            SELECT t.datacol, t.datarow, t.parent_pk
            FROM t_pf_history_attr_data AS t
            ORDER BY parent_pk, datarow
            """.trimIndent()
        private const val SELECT_VALUE_FROM_HISTORY_ATTR = "SELECT t.value FROM t_pf_history_attr AS t WHERE pk = ?"
        private const val SELECT_USER_IDS = "SELECT t.pk AS pk FROM t_pf_user AS t"
        private const val UPDATE_HISTORY_ATTR = "UPDATE t_pf_history_attr SET value = ? WHERE pk = ?"
        private val INSERT_EMPLOYEE_ATTR = """
            INSERT INTO t_fibu_employee_valid_since_attr
                        (pk, created, last_update, deleted, employee_fk, type, valid_since, value)
                    VALUES (?, ?, ?, ?, ?, ?, ?, ?)
            """.trimIndent()
        private val INSERT_VISITORBOOK = """
            INSERT INTO t_orga_visitorbook_entry (pk, created, last_update, deleted, visitorbook_fk, date_of_visit, arrived, departed)
            VALUES (?, ?, ?, ?, ?, ?, ?, ?)
        """.trimIndent()
        private val SELECT_EMPLOYEE_TIMED = """
            SELECT a.pk AS pk1, a.createdat, a.createdby, a.modifiedat, a.modifiedby, a.start_time, a.employee_id AS object_id, a.group_name,
                   b.pk AS pk2, b.value, b.propertyname, b.createdby AS createdby_b, b.createdat AS createdat_b, b.modifiedby AS modifiedby_b, b.modifiedat AS modifiedat_b
            FROM t_fibu_employee_timed a JOIN t_fibu_employee_timedattr b ON a.pk=b.parent
            WHERE a.group_name IN ('employeestatus', 'employeeannualleave') ORDER BY a.pk
        """.trimIndent()
        private val SELECT_VISITOR_BOOK_TIMED = """
            SELECT a.pk AS pk1, a.createdat, a.createdby, a.modifiedat, a.modifiedby, a.start_time, a.visitor_id AS object_id, a.group_name,
                   b.pk AS pk2, b.value, b.propertyname, b.createdby AS createdby_b, b.createdat AS createdat_b, b.modifiedby AS modifiedby_b, b.modifiedat AS modifiedat_b
            FROM t_orga_visitorbook_timed a
            JOIN t_orga_visitorbook_timedattr b ON a.pk=b.parent
            ORDER BY a.pk
        """.trimIndent()
        private const val SELECT_FROM_MASTER_TABLE = "SELECt t.pk AS object_id FROM <masterTable> AS t"
    }

    override fun migrate(context: Context) {
        log.info { "Migrating attributes with period validity from mgc: Employee (annual leave, status) and entries from Visitorbook" }
        // JdbcTemplate can't be used, because autoCommit is set to false since PF 8.0 and JdbcTemplate doesn't support autoCommit.
        val connection = context.connection
        try {
            connection.autoCommit = true
            migrateHistoryAttrWithData(connection)
            migrateEmployees(connection)
            migrateVisitorbook(connection)
        } catch (ex: Exception) {
            // Behandle Ausnahmen, falls notwendig
            ex.printStackTrace()
            throw ex
        } finally {
            connection.autoCommit = false
        }
    }

    internal fun migrateHistoryAttrWithData(connection: Connection) {
        class HistoryAttr(val pk: Long, var value: String, val alreadyProcessed: Boolean)

        var parent = HistoryAttr(-1, "", true)
        connection.createStatement().use { statement ->
            statement.executeQuery(SELECT_HISTORY_ATTR_DATA).use { resultSet ->
                while (resultSet.next()) {
                    val datacol = getString(resultSet, "datacol")
                    resultSet.getInt("datarow")
                    val parentPk = getLong(resultSet, "parent_pk")
                    if (parentPk != parent.pk) {
                        if (parent.pk != -1L) {
                            // Update parent before processing next parent.
                            updateParent(connection, parent.pk, parent.value, parent.alreadyProcessed)
                        }
                        connection.prepareStatement(SELECT_VALUE_FROM_HISTORY_ATTR).use { secondStatement ->
                            secondStatement.setLong(1, parentPk!!)
                            secondStatement.executeQuery().use { secondResultSet ->
                                secondResultSet.next()
                                val value = getString(secondResultSet, "value") ?: ""
                                require(!secondResultSet.next()) { "Multiple rows for parentPk=$parentPk not expected and not supported. Migration will fail. $SELECT_VALUE_FROM_HISTORY_ATTR with pk=$parentPk" }
                                val alreadyProcessed =
                                    if (value.length > 2990) { // Old length of value field was 2990.
                                        log.info { "Entry already processed (migration called twice?). Ignoring t_pf_history_attr_data for entry with pk=$parentPk, value.length=${value.length}." }
                                        true
                                    } else {
                                        false
                                    }
                                parent = HistoryAttr(parentPk, value, alreadyProcessed)
                            }
                        }
                    }
                    parent.value += datacol
                }
            }
        }
        updateParent(connection, parent.pk, parent.value, parent.alreadyProcessed)
    }

    private fun updateParent(connection: Connection, pk: Long, value: String, alreadyProcessed: Boolean) {
        if (!alreadyProcessed) {
            if (value.length > 50000) {
                throw IllegalArgumentException("Value too long: ${value.length} (> 50.000)")
            }
            log.info { "Migrating t_pf_history_attr entry with attr_data: $pk" }
            connection.prepareStatement(UPDATE_HISTORY_ATTR).use { secondStatement ->
                secondStatement.setString(1, value)
                secondStatement.setLong(2, pk)
                secondStatement.executeUpdate()
            }
            log.info { "update t_pf_history_attr set value = $value where pk = $pk" }
        }
    }

    internal fun migrateEmployees(connection: Connection) {
        readEmployees(connection).forEach { dbAttr ->
            val oldAttr = dbAttr.getTransientAttribute("oldAttr") as TimedAttr
            try {
                connection.prepareStatement(INSERT_EMPLOYEE_ATTR).use { statement ->
                    statement.setLong(1, dbAttr.id!!)
                    statement.setTimestamp(2, oldAttr.createdatA)
                    statement.setTimestamp(3, oldAttr.modifiedatA)
                    statement.setBoolean(4, false)
                    statement.setLong(5, dbAttr.employee!!.id!!)
                    statement.setString(6, dbAttr.type!!.name)
                    statement.setDate(7, java.sql.Date.valueOf(dbAttr.validSince))
                    statement.setString(8, dbAttr.value)
                    statement.executeUpdate()
                    log.info { "Migrated employee attribute: $dbAttr" }
                }
            } catch (ex: DuplicateKeyException) {
                log.info { "Employee already migrated (migration called twice?). Don't overwrite entry t_fibu_employee_valid_since_attr.${dbAttr.id}: ex=${ex.message}" }
            }
        }
    }

    internal fun readEmployees(connection: Connection): List<EmployeeValidSinceAttrDO> {
        val list = read(connection, "t_fibu_employee", SELECT_EMPLOYEE_TIMED, type = TYPE.EMPLOYEE)
        val resultList = mutableListOf<EmployeeValidSinceAttrDO>()
        list.forEach { attr ->
            val dbAttr = EmployeeValidSinceAttrDO()
            dbAttr.id = attr.pk2 // Re-use the pk.
            if (attr.groupName == "employeestatus") {
                val status = EmployeeStatus.findByi18nKey(attr.value ?: "???")
                    ?: throw IllegalStateException("Status not found for '${attr.value}' for attribute: $attr")
                dbAttr.value = status.name
                dbAttr.type = EmployeeValidSinceAttrType.STATUS
            } else if (attr.groupName == "employeeannualleave") {
                dbAttr.value = attr.value
                dbAttr.type = EmployeeValidSinceAttrType.ANNUAL_LEAVE
            } else {
                throw IllegalStateException("Error, unknown group: ${attr.groupName}")
            }
            val employee = EmployeeDO()
            employee.id = attr.objectId
            dbAttr.employee = employee
            attr.startTime?.let { startTime ->
                dbAttr.validSince = startTime
            }
            dbAttr.setTransientAttribute("oldAttr", attr)
            resultList.add(dbAttr)
            log.info { "Read employee attribute: $dbAttr" }
        }
        return resultList
    }

    internal fun migrateVisitorbook(connection: Connection) {
        val dbAttrs = readVisitorBook(connection)
        dbAttrs.forEach { dbAttr ->
            if (dbAttr.getTransientAttribute("oldArrivedAttr") == null && dbAttr.getTransientAttribute("oldDepartedAttr") == null) {
                throw IllegalStateException("oldArrivedAttr and oldDepartedAttr is null: $dbAttr")
            }
        }
        dbAttrs.forEach { dbAttr ->
            val oldAttr = dbAttr.getTransientAttribute("oldArrivedAttr") as? TimedAttr?
                ?: dbAttr.getTransientAttribute("oldDepartedAttr") as? TimedAttr?
            requireNotNull(oldAttr)
            try {
                connection.prepareStatement(INSERT_VISITORBOOK).use { statement ->
                    statement.setLong(1, dbAttr.id!!)
                    statement.setTimestamp(2, oldAttr.createdatB)
                    statement.setTimestamp(3, oldAttr.modifiedatB)
                    statement.setBoolean(4, false)
                    statement.setLong(5, dbAttr.visitorbook!!.id!!)
                    statement.setDate(6, java.sql.Date.valueOf(dbAttr.dateOfVisit))
                    statement.setString(7, dbAttr.arrived)
                    statement.setString(8, dbAttr.departed)
                    statement.executeUpdate()
                    log.info { "Migrated visitorbook entry: $dbAttr" }
                }
            } catch (ex: DuplicateKeyException) {
                log.info { "Visitorbook already migrated (migration called twice?). Don't overwrite entry t_orga_visitorbook_entry.${dbAttr.id}. msg=${ex.message}" }
            }
        }
    }

    internal fun readVisitorBook(connection: Connection): List<VisitorbookEntryDO> {
        val list = read(connection, "t_orga_visitorbook", SELECT_VISITOR_BOOK_TIMED, type = TYPE.VISITORBOOK)
        val result = mutableListOf<VisitorbookEntryDO>()
        list.forEach { attr ->
            if (attr.startTime == null) {
                throw IllegalStateException("startTime is null: $attr")
            }
            val existingAttr =
                result.find { it.visitorbook?.id == attr.objectId && it.dateOfVisit == attr.startTime }
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
            dbAttr.dateOfVisit = attr.startTime
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
        connection: Connection,
        masterTable: String,
        joinSelectSql: String,
        type: TYPE,
    ): List<TimedAttr> {
        val validMasterIds = mutableSetOf<Long>()
        val sql = SELECT_FROM_MASTER_TABLE.replace("<masterTable>", masterTable)
        connection.prepareStatement(sql).use { statement ->
            statement.executeQuery().use { resultSet ->
                while (resultSet.next()) {
                    validMasterIds.add(getLong(resultSet, "object_id")!!)
                }
            }
        }
        val userIds = mutableSetOf<Long>()
        connection.prepareStatement(SELECT_USER_IDS).use { statement ->
            statement.executeQuery().use { resultSet ->
                while (resultSet.next()) {
                    userIds.add(getLong(resultSet, "pk")!!)
                }
            }
        }
        val list = mutableListOf<TimedAttr>()
        connection.prepareStatement(joinSelectSql).use { statement ->
            statement.executeQuery().use { resultSet ->
                while (resultSet.next()) {
                    val data = TimedAttr(resultSet, type = type)
                    if (!validMasterIds.contains(data.objectId)) {
                        throw IllegalStateException("Object with id=${data.objectId} not found in $masterTable: $data")
                    }
                    list.add(data)
                }
            }
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

private class TimedAttr(resultSet: ResultSet, type: TYPE) {
    val pk1 = getLong(resultSet, "pk1")
    val pk2 = getLong(resultSet, "pk2")
    val createdatA = getTimestamp(resultSet, "createdat")
    val createdbyA = getString(resultSet, "createdby")
    val modifiedatA = getTimestamp(resultSet, "modifiedat")
    val modifiedbyA = getString(resultSet, "modifiedby")
    val startTime = getLocalDate(resultSet, "start_time")

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
        return abs(a.time - b.time) < Constants.MILLIS_PER_HOUR
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
    val connection = dataSource.connection
    V8_0_2__RemoveMGC().readEmployees(connection)
    V8_0_2__RemoveMGC().migrateEmployees(connection)
    V8_0_2__RemoveMGC().readVisitorBook(connection)
    V8_0_2__RemoveMGC().migrateVisitorbook(connection)
    //V8_0_0_2__RemoveMGC().migrateHistoryAttrWithData(connection)
}
