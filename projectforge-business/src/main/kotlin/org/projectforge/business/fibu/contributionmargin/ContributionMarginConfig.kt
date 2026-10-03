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

package org.projectforge.business.fibu.contributionmargin

import com.fasterxml.jackson.annotation.JsonIgnore
import mu.KotlinLogging
import org.projectforge.framework.json.JsonUtils
import java.math.BigDecimal

private val log = KotlinLogging.logger {}

/**
 * The settings of the contribution margin, stored as JSON in the configuration parameter
 * `ConfigurationParam.FIBU_CONTRIBUTION_MARGIN` and edited as a whole on the configuration page.
 */
class ContributionMarginConfig(
  /** The revenue accounts as comma separated account ranges, e.g. `4000-4799` (SKR04). */
  var revenueAccounts: String? = DEFAULT_REVENUE_ACCOUNTS,
  /**
   * The flat hourly rate the time sheets of months without imported accounting records are valued with as
   * costs. Without a value, these months show no time sheet costs.
   */
  var hourlyRate: BigDecimal? = null,
  /** The target contribution margin in %: green from here on, yellow below. */
  var targetPercentage: Int = DEFAULT_TARGET_PERCENTAGE,
  /** The contribution margin in % below which it is shown red. */
  var redThreshold: Int = DEFAULT_RED_THRESHOLD,
  var kost2Assignments: List<Kost2Assignment> = emptyList(),
  /**
   * Free text of finance and controlling about the settings, e.g. how the calculated rate is derived. Not
   * evaluated; changes are traced through the history of the configuration parameter.
   */
  var remark: String? = null,
) {
  /**
   * Assigns the kost2 without an own project whose number starts with [kost2] (nummernkreis, bereich,
   * teilbereich, e.g. `6.000.10`, any kost2 type) to the project with the kost [project] (e.g. `5.999.10`).
   */
  class Kost2Assignment(var kost2: String? = null, var project: String? = null) {
    /** An empty row of the editor, ignored. */
    @get:JsonIgnore
    val isBlank: Boolean
      get() = kost2.isNullOrBlank() && project.isNullOrBlank()
  }

  /**
   * An error of [validate]: the [field] of the JSON object (and the [index] of the kost2 assignment) and the
   * i18n key of the message with its parameter.
   */
  data class Error(val field: String, val i18nKey: String, val index: Int? = null, val param: String? = null)

  /** The revenue accounts as predicate, see [ContributionMarginCalculator.parseAccountRanges]. */
  @get:JsonIgnore
  val isRevenueAccount: (Int) -> Boolean
    get() = ContributionMarginCalculator.parseAccountRanges(revenueAccounts)

  /** The configured hourly rate if positive. */
  @get:JsonIgnore
  val effectiveHourlyRate: BigDecimal?
    get() = hourlyRate?.takeIf { it > BigDecimal.ZERO }

  /**
   * @param projectExists Whether a project with the given kost (three parts) exists.
   */
  fun validate(projectExists: (List<Int>) -> Boolean): List<Error> {
    val errors = mutableListOf<Error>()
    if (ContributionMarginCalculator.accountRanges(revenueAccounts) == null) {
      errors += Error(REVENUE_ACCOUNTS, "$ERROR_PREFIX.revenueAccounts")
    }
    if (hourlyRate?.let { it < BigDecimal.ZERO } == true) {
      errors += Error(HOURLY_RATE, "$ERROR_PREFIX.negative")
    }
    if (targetPercentage !in 0..100) {
      errors += Error(TARGET_PERCENTAGE, "$ERROR_PREFIX.percentage")
    }
    if (redThreshold !in 0..100) {
      errors += Error(RED_THRESHOLD, "$ERROR_PREFIX.percentage")
    } else if (redThreshold > targetPercentage) {
      errors += Error(RED_THRESHOLD, "$ERROR_PREFIX.redAboveTarget")
    }
    kost2Assignments.forEachIndexed { index, assignment ->
      if (assignment.isBlank) {
        return@forEachIndexed
      }
      if (parseKostNumber(assignment.kost2) == null) {
        errors += Error(KOST2_ASSIGNMENTS, "$ERROR_PREFIX.kostNumber", index, assignment.kost2 ?: "")
      }
      val project = parseKostNumber(assignment.project)
      if (project == null) {
        errors += Error(KOST2_ASSIGNMENTS, "$ERROR_PREFIX.kostNumber", index, assignment.project ?: "")
      } else if (!projectExists(project)) {
        errors += Error(KOST2_ASSIGNMENTS, "$ERROR_PREFIX.projectNotFound", index, assignment.project)
      }
    }
    return errors
  }

  companion object {
    const val DEFAULT_REVENUE_ACCOUNTS = "4000-4799"
    const val DEFAULT_TARGET_PERCENTAGE = 65
    const val DEFAULT_RED_THRESHOLD = 50

    const val REVENUE_ACCOUNTS = "revenueAccounts"
    const val HOURLY_RATE = "hourlyRate"
    const val TARGET_PERCENTAGE = "targetPercentage"
    const val RED_THRESHOLD = "redThreshold"
    const val KOST2_ASSIGNMENTS = "kost2Assignments"

    /** The i18n prefix of the labels of the fields, see [Error.field]. */
    const val I18N_PREFIX = "fibu.auftrag.contributionMargin.config"
    private const val ERROR_PREFIX = "$I18N_PREFIX.error"

    /** The settings of the given JSON value; the defaults if blank or not readable. */
    fun parse(json: String?): ContributionMarginConfig {
      if (json.isNullOrBlank()) {
        return ContributionMarginConfig()
      }
      return try {
        JsonUtils.fromJson(json, ContributionMarginConfig::class.java, failOnUnknownProps = false)
          ?: ContributionMarginConfig()
      } catch (ex: Exception) {
        log.warn { "Contribution margin: configuration not readable, using the defaults: ${ex.message}" }
        ContributionMarginConfig()
      }
    }

    /** Parses a kost number of three parts like `6.000.10`, or null. */
    fun parseKostNumber(value: String?): List<Int>? {
      val parts = value?.trim()?.split('.')?.map { it.trim().toIntOrNull() ?: return null } ?: return null
      return if (parts.size == 3) parts else null
    }
  }
}
