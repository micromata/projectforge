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

package org.projectforge.framework.persistence.database

import jakarta.persistence.Tuple
import java.math.BigDecimal
import java.sql.ResultSet
import java.time.LocalDate
import java.util.Date

object TupleUtils {
    fun debugTuple(tuples: Collection<Tuple>): String {
        return tuples.joinToString { tuple ->
            tuple.elements.joinToString { element ->
                val name = element.alias // Der Name des Feldes
                val type = element.javaType // Der Typ des Feldes
                "Name: $name, Typ: $type"
            }
        }
    }

    /**
     * Returns the value of the specified column as a String.
     * If the value is SQL NULL, the method returns null.
     * @param tuple The Tuple to get the value from.
     * @param columnName The name of the column to get the value from.
     * @return The value of the specified column as a String or null if the value is SQL NULL.
     */
    fun getString(tuple: Tuple, columnName: String): String? {
        return tuple.get(columnName, String::class.java)
    }

    /**
     * Returns the value of the specified column as a Long.
     * If the value is SQL NULL, the method returns null.
     * @param tuple The Tuple to get the value from.
     * @param columnName The name of the column to get the value from.
     * @return The value of the specified column as a Long or null if the value is SQL NULL.
     */
    fun getLong(tuple: Tuple, columnName: String): Long? {
        return tuple.get(columnName, Long::class.java)
    }

    /**
     * Returns the value of the specified column as an Int.
     * If the value is SQL NULL, the method returns null.
     * @param tuple The Tuple to get the value from.
     * @param columnName The name of the column to get the value from.
     * @return The value of the specified column as an Int or null if the value is SQL NULL.
     */
    fun getInt(tuple: Tuple, columnName: String): Int? {
        return tuple.get(columnName, Int::class.java)
    }

    /**
     * Returns the value of the specified column as a Short.
     * If the value is SQL NULL, the method returns null.
     * @param tuple The Tuple to get the value from.
     * @param columnName The name of the column to get the value from.
     * @return The value of the specified column as a Short or null if the value is SQL NULL.
     */
    fun getShort(tuple: Tuple, columnName: String): Short? {
        return tuple.get(columnName, Short::class.java)
    }

    /**
     * Returns the value of the specified column as a BigDecimal.
     * If the value is SQL NULL, the method returns null.
     * @param tuple The Tuple to get the value from.
     * @param columnName The name of the column to get the value from.
     * @return The value of the specified column as a BigDecimal or null if the value is SQL NULL.
     */
    fun getBigDecimal(tuple: Tuple, columnName: String): BigDecimal? {
        return tuple.get(columnName, BigDecimal::class.java)
    }

    /**
     * Returns the value of the specified column as a Boolean.
     * If the value is SQL NULL, the method returns null.
     * @param tuple The Tuple to get the value from.
     * @param columnName The name of the column to get the value from.
     * @return The value of the specified column as a Boolean or null if the value is SQL NULL.
     */
    fun getBoolean(tuple: Tuple, columnName: String): Boolean? {
        return tuple.get(columnName, Boolean::class.java)
    }

    /**
     * Returns the value of the specified column as a LocalDate.
     * If the value is SQL NULL, the method returns null.
     * @param tuple The Tuple to get the value from.
     * @param columnName The name of the column to get the value from.
     * @return The value of the specified column as a LocalDate or null if the value is SQL NULL.
     */
    fun getLocalDate(tuple: Tuple, columnName: String): LocalDate? {
        return tuple.get(columnName, LocalDate::class.java)
    }

    /**
     * Returns the value of the specified column as a Date.
     * If the value is SQL NULL, the method returns null.
     * @param tuple The Tuple to get the value from.
     * @param columnName The name of the column to get the value from.
     * @return The value of the specified column as a Date or null if the value is SQL NULL.
     */
    fun getDate(tuple: Tuple, columnName: String): Date? {
        return tuple.get(columnName, Date::class.java)
    }
}
