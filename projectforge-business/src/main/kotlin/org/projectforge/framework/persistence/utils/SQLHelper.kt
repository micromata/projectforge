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

package org.projectforge.framework.persistence.utils

import jakarta.persistence.Tuple
import org.projectforge.framework.time.PFDateTime
import java.time.LocalDate
import java.time.Year
import java.util.*

/**
 * Some helper methods ...
 *
 * @author Kai Reinhard (k.reinhard@micromata.de)
 */
object SQLHelper {
    @JvmStatic
    fun getYears(minYear: Int?, maxYear: Int?): IntArray {
        val min = minYear ?: maxYear ?: Year.now().value
        val max = maxYear ?: min
        if (min > max || max - min > 30) {
            throw UnsupportedOperationException("Paranoia Exception")
        }
        val res = IntArray(max - min + 1)
        var i = 0
        for (year in min..max) {
            res[i++] = year
        }
        return res
    }

    /**
     * @param minMaxDate Tuple with two Date objects.
     */
    @JvmStatic
    fun getYearsByTupleOfDate(minMaxDate: Tuple?): IntArray {
        val result = if (minMaxDate == null) {
            val year = Year.now().value
            Pair(year, year)
        } else {
            Pair(
                PFDateTime.fromOrNull(minMaxDate[0] as? Date)?.year,
                PFDateTime.fromOrNull(minMaxDate[1] as? Date)?.year
            )
        }
        return getYears(result.first, result.second)
    }

    /**
     * @param minMaxDate Tuple with two LocalDate objects.
     */
    @JvmStatic
    fun getYearsByTupleOfLocalDate(minMaxDate: Tuple?): IntArray {
        val result = if (minMaxDate == null) {
            val year = Year.now().value
            Pair(year, year)
        } else {
            Pair((minMaxDate[0] as? LocalDate)?.year, (minMaxDate[1] as? LocalDate)?.year)
        }
        return getYears(result.first, result.second)
    }

    @JvmStatic
    fun getYearsByTupleOfYears(minMaxDate: Tuple?): IntArray {
        val result = if (minMaxDate == null) {
            val year = Year.now().value
            Pair(year, year)
        } else {
            Pair(minMaxDate[0] as? Int, minMaxDate[1] as? Int)
        }
        return getYears(result.first, result.second)
    }

    /**
     * Parses a SQL script into individual statements.
     * This method is not foolproof and may not work with all SQL scripts.
     * It is intended to be used for simple scripts.
     * @param script The SQL script to parse.
     * @return A list of individual SQL statements.
     */
    @JvmStatic
    fun splitSqlStatements(script: String): List<String> {
        val statements = mutableListOf<String>()
        val currentStatement = StringBuilder()
        var inString = false // Tracks if we are inside a string
        var stringDelimiter: Char? = null // Tracks the type of string delimiter (' or ")

        var i = 0
        while (i < script.length) {
            val c = script[i]
            val next = if (i + 1 < script.length) script[i + 1] else '\u0000'

            when {
                // Toggle `inString` when encountering string delimiters (' or ") and not escaping
                (c == '\'' || c == '"') && (stringDelimiter == null || stringDelimiter == c) -> {
                    if (inString) {
                        // End string if we are inside one
                        if (stringDelimiter == c) inString = false
                    } else {
                        // Start string
                        inString = true
                        stringDelimiter = c
                    }
                    currentStatement.append(c)
                }

                // Skip single-line comments (e.g., -- comment)
                !inString && c == '-' && next == '-' -> {
                    // Skip to the end of the line
                    while (i < script.length && script[i] != '\n') i++
                    currentStatement.append('\n')
                }

                // Add semicolon-separated statements when outside strings
                !inString && c == ';' -> {
                    currentStatement.append(c)
                    statements.add(currentStatement.toString().trim())
                    currentStatement.clear()
                }

                // Default: Add character to the current statement
                else -> {
                    currentStatement.append(c)
                }
            }

            i++
        }

        // Add the last statement if it doesn't end with a semicolon
        if (currentStatement.isNotBlank()) {
            statements.add(currentStatement.toString().trim())
        }

        return statements
    }
}
