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

package org.projectforge.business.fibu

import org.springframework.stereotype.Service
import java.math.BigDecimal
import java.math.RoundingMode

/**
 * Service for calculating invoicing quota (Fakturaquote) in monthly employee reports.
 *
 * The invoicing quota is the percentage of work time that is billed to customers,
 * based on configurable patterns for Kost2 display names.
 *
 * @author Kai Reinhard (k.reinhard@micromata.de)
 */
@Service
class InvoicingQuotaService(
    private val config: InvoicingQuotaConfiguration
) {
    /**
     * Check if invoicing quota calculation is enabled.
     */
    fun isEnabled(): Boolean = config.enabled && config.billedPatterns.isNotEmpty()

    /**
     * Check if the given Kost2 display name is considered as billed work.
     *
     * @param kost2DisplayName The display name of the Kost2 (e.g., "5.123.45.67")
     * @return true if the Kost2 matches the billed patterns and doesn't match negated patterns
     */
    fun isBilled(kost2DisplayName: String): Boolean {
        if (config.billedPatterns.isEmpty()) return false

        // Split patterns into positive (without '!') and negative (with '!')
        val positivePatterns = config.billedPatterns.filter { !it.startsWith("!") }
        val negativePatterns = config.billedPatterns
            .filter { it.startsWith("!") }
            .map { it.substring(1) }

        val matchesPositive = positivePatterns.any { matchesPattern(kost2DisplayName, it) }
        val matchesNegative = negativePatterns.any { matchesPattern(kost2DisplayName, it) }

        return matchesPositive && !matchesNegative
    }

    /**
     * Check if the given Kost2 display name should be ignored in the calculation
     * (e.g., vacation, absences).
     *
     * @param kost2DisplayName The display name of the Kost2
     * @return true if the Kost2 should be ignored
     */
    fun isIgnored(kost2DisplayName: String): Boolean {
        return config.ignoredPatterns.any { matchesPattern(kost2DisplayName, it) }
    }

    /**
     * Calculate the invoicing quota based on Kost2 durations.
     *
     * @param kost2Durations Map of Kost2 ID to MonthlyEmployeeReportEntry
     * @return The invoicing quota as a decimal (0.0 - 1.0), or null if disabled or no data
     */
    fun calculateQuota(kost2Durations: Map<Long, MonthlyEmployeeReportEntry>): BigDecimal? {
        if (!isEnabled()) return null

        var totalDuration = 0L
        var billedDuration = 0L

        kost2Durations.values.forEach { entry ->
            entry.kost2?.let { kost2 ->
                val displayName = kost2.displayName ?: return@forEach
                if (!isIgnored(displayName)) {
                    totalDuration += entry.workFractionMillis
                    if (isBilled(displayName)) {
                        billedDuration += entry.workFractionMillis
                    }
                }
            }
        }

        return if (totalDuration > 0) {
            BigDecimal(billedDuration).divide(
                BigDecimal(totalDuration),
                4,
                RoundingMode.HALF_UP
            )
        } else {
            null
        }
    }

    /**
     * Check if a value matches a wildcard pattern.
     *
     * Pattern syntax:
     * - `*` = any characters
     * - `_` = exactly one digit (0-9)
     * - `.` = literal dot (escaped)
     *
     * Examples:
     * - `5.*` matches "5.123", "5.999.01.02"
     * - `5.___.99.*` matches "5.123.99.01", "5.456.99.99"
     * - `6.3*` matches "6.3", "6.31", "6.399.01"
     *
     * @param value The value to check
     * @param pattern The wildcard pattern
     * @return true if the value matches the pattern
     */
    private fun matchesPattern(value: String, pattern: String): Boolean {
        // Convert wildcard pattern to regex
        val regexPattern = pattern
            .replace(".", "\\.")  // Escape dots for literal matching
            .replace("*", ".*")   // * = any characters (including none)
            .replace("_", "\\d")  // _ = exactly one digit

        return value.matches(Regex(regexPattern))
    }
}
