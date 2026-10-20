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


package org.projectforge.business.fibu.kost

import org.projectforge.business.scripting.GroovyExecutor
import org.projectforge.framework.i18n.translateMsg
import org.projectforge.framework.utils.IntRanges

/**
 * Checks the accounting settings of the configuration page (parameter `fibu.accounting`, [AccountingConfig]):
 * the account number ranges, the ids of the business assessment rows (unique names usable in formulas and
 * scripts), the rows referred to and the values (formulas and Groovy scripts must compile).
 */
object AccountingConfigValidator {
    private val ID_REGEX = Regex("[a-zA-Z][a-zA-Z0-9_]*")

    /** The translated errors, empty if the settings are valid. */
    fun validate(config: AccountingConfig): List<String> {
        val errors = mutableListOf<String>()
        checkRanges(
            errors,
            translateMsg("administration.configuration.accounting.debitorsAccountNumberRanges"),
            config.debitorsAccountNumberRangesString,
        )
        checkRanges(
            errors,
            translateMsg("administration.configuration.accounting.creditorsAccountNumberRanges"),
            config.creditorsAccountNumberRangesString,
        )
        val bwa = config.businessAssessmentConfig ?: return errors
        val rows = bwa.rows ?: emptyList()
        val ids = mutableSetOf<String>()
        rows.forEachIndexed { index, row ->
            val name = row.title?.trim()?.ifEmpty { null } ?: row.no?.trim()?.ifEmpty { null } ?: "${index + 1}"
            row.id?.trim()?.ifEmpty { null }?.let { id ->
                if (!ID_REGEX.matches(id)) {
                    errors.add(translateMsg("administration.configuration.accounting.error.id", name, id))
                } else if (!ids.add(id)) {
                    errors.add(translateMsg("administration.configuration.accounting.error.duplicateId", name, id))
                }
            }
            checkRanges(errors, name, row.accountRangeConfig)
            val value = row.valueConfig?.trim()?.ifEmpty { null }
            if (value != null && value != "=") {
                val script = if (value.startsWith("=")) "return ${value.substring(1)}" else value
                val compiled = try {
                    GroovyExecutor().compileGroovy(script, false)
                } catch (ex: Exception) {
                    null
                }
                if (compiled == null) {
                    errors.add(translateMsg("administration.configuration.accounting.error.value", name))
                }
            }
        }
        listOf(
            "administration.configuration.accounting.overallPerformance" to bwa.overallPerformance,
            "administration.configuration.accounting.merchandisePurchase" to bwa.merchandisePurchase,
            "administration.configuration.accounting.preliminaryResult" to bwa.preliminaryResult,
        ).forEach { (labelKey, id) ->
            val trimmed = id?.trim()?.ifEmpty { null } ?: return@forEach
            if (!ids.contains(trimmed)) {
                errors.add(translateMsg("administration.configuration.accounting.error.unknownRow", translateMsg(labelKey), trimmed))
            }
        }
        return errors
    }

    private fun checkRanges(errors: MutableList<String>, name: String, ranges: String?) {
        if (ranges.isNullOrBlank()) {
            return
        }
        try {
            IntRanges(ranges)
        } catch (ex: Exception) {
            errors.add(translateMsg("administration.configuration.accounting.error.accountRange", name, ranges.trim()))
        }
    }
}
