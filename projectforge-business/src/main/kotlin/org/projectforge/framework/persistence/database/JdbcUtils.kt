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

package org.projectforge.framework.persistence.database

import java.math.BigDecimal
import java.sql.ResultSet
import java.sql.Timestamp
import java.time.LocalDate
import java.util.Date

object JdbcUtils {
    fun debugColumnResult(rs: ResultSet, vararg columnNames: String): String {
        return columnNames.joinToString { columnName ->
            val sb = StringBuilder()
            sb.append(columnName).append(":")
            val value = rs.getObject(columnName)
            val wasNull = rs.wasNull()
            sb.append("value=[").append(value).append("], wasNull=").append(wasNull)
            sb.toString()
        }
    }

    /**
     * Returns the value of the specified column as a String.
     * If the value is SQL NULL, the method returns null.
     * @param rs The ResultSet to get the value from.
     * @param columnName The name of the column to get the value from.
     * @return The value of the specified column as a String or null if the value is SQL NULL.
     */
    fun getString(rs: ResultSet, columnName: String): String? {
        return rs.getString(columnName).takeIf { rs.wasNull().not() }
    }

    /**
     * Returns the value of the specified column as a Long.
     * If the value is SQL NULL, the method returns null.
     * @param rs The ResultSet to get the value from.
     * @param columnName The name of the column to get the value from.
     * @return The value of the specified column as a Long or null if the value is SQL NULL.
     */
    fun getLong(rs: ResultSet, columnName: String): Long? {
        return rs.getLong(columnName).takeIf { rs.wasNull().not() }
    }

    /**
     * Returns the value of the specified column as an Int.
     * If the value is SQL NULL, the method returns null.
     * @param rs The ResultSet to get the value from.
     * @param columnName The name of the column to get the value from.
     * @return The value of the specified column as an Int or null if the value is SQL NULL.
     */
    fun getInt(rs: ResultSet, columnName: String): Int? {
        return rs.getInt(columnName).takeIf { rs.wasNull().not() }
    }

    /**
     * Returns the value of the specified column as a Short.
     * If the value is SQL NULL, the method returns null.
     * @param rs The ResultSet to get the value from.
     * @param columnName The name of the column to get the value from.
     * @return The value of the specified column as a Short or null if the value is SQL NULL.
     */
    fun getShort(rs: ResultSet, columnName: String): Short? {
        return rs.getShort(columnName).takeIf { rs.wasNull().not() }
    }

    /**
     * Returns the value of the specified column as a BigDecimal.
     * If the value is SQL NULL, the method returns null.
     * @param rs The ResultSet to get the value from.
     * @param columnName The name of the column to get the value from.
     * @return The value of the specified column as a BigDecimal or null if the value is SQL NULL.
     */
    fun getBigDecimal(rs: ResultSet, columnName: String): BigDecimal? {
        return rs.getBigDecimal(columnName).takeIf { rs.wasNull().not() }
    }

    /**
     * Returns the value of the specified column as a Boolean.
     * If the value is SQL NULL, the method returns null.
     * @param rs The ResultSet to get the value from.
     * @param columnName The name of the column to get the value from.
     * @return The value of the specified column as a Boolean or null if the value is SQL NULL.
     */
    fun getBoolean(rs: ResultSet, columnName: String): Boolean? {
        return rs.getBoolean(columnName).takeIf { rs.wasNull().not() }
    }

    /**
     * Returns the value of the specified column as a LocalDate.
     * If the value is SQL NULL, the method returns null.
     * @param rs The ResultSet to get the value from.
     * @param columnName The name of the column to get the value from.
     * @return The value of the specified column as a LocalDate or null if the value is SQL NULL.
     */
    fun getLocalDate(rs: ResultSet, columnName: String): LocalDate? {
        return rs.getObject(columnName, LocalDate::class.java).takeIf { rs.wasNull().not() }
    }

    /**
     * Returns the value of the specified column as a Date.
     * If the value is SQL NULL, the method returns null.
     * @param rs The ResultSet to get the value from.
     * @param columnName The name of the column to get the value from.
     * @return The value of the specified column as a Date or null if the value is SQL NULL.
     */
    fun getDate(rs: ResultSet, columnName: String): Date? {
        return rs.getDate(columnName).takeIf { rs.wasNull().not() }
    }

    /**
     * Returns the value of the specified column as a Timestamp.
     * If the value is SQL NULL, the method returns null.
     * @param rs The ResultSet to get the value from.
     * @param columnName The name of the column to get the value from.
     * @return The value of the specified column as a Date or null if the value is SQL NULL.
     */
    fun getTimestamp(rs: ResultSet, columnName: String): Timestamp? {
        return rs.getTimestamp(columnName).takeIf { rs.wasNull().not() }
    }
}
