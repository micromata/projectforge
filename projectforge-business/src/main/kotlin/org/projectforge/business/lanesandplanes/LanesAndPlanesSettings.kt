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


package org.projectforge.business.lanesandplanes

import com.fasterxml.jackson.annotation.JsonIgnore
import io.github.oshai.kotlinlogging.KotlinLogging
import org.projectforge.business.fibu.kost.Kost1DO
import org.projectforge.business.fibu.kost.Kost2DO
import org.projectforge.business.fibu.kost.KostentraegerStatus
import org.projectforge.framework.json.JsonUtils

private val log = KotlinLogging.logger {}

/**
 * The settings of the Lanes & Planes push, stored as JSON in the configuration parameter
 * `ConfigurationParam.LANES_AND_PLANES` and edited by finance on the configuration page. The technical settings
 * (url, API key, dry run) are in [LanesAndPlanesConfig].
 */
class LanesAndPlanesSettings(
    /**
     * Required by Lanes & Planes for every user, the ids are provided by Lanes & Planes. Empty rows of the editor
     * are stored as null, see [invoiceProfileIds].
     */
    var accountingInvoiceProfileIds: List<Long?> = emptyList(),
    /** Formatted numbers of the Kost1 sent to all users as cost centers (`cost_centers`). */
    var generalKost1: List<String> = emptyList(),
    /**
     * Formatted numbers of the Kost2 sent to all users as cost units (`cost_units`), in addition to the bookable
     * ones (e.g. internal ones like trainings or sales).
     */
    var generalKost2: List<String> = emptyList(),
    /**
     * Glob patterns of the formatted numbers (e.g. `5.*.02`): the Kost2 a user may book are only sent, if they
     * match one of them. Empty: all bookable Kost2.
     */
    var kost2Patterns: List<String> = emptyList(),
    /**
     * Users of Lanes & Planes who aren't employees (e.g. an external accountant or the service admin of L&P), sent
     * in addition to the employees: otherwise the full replace would deactivate them, and a missing user with
     * responsibilities lets the whole push fail.
     */
    var additionalUsers: List<AdditionalUser> = emptyList(),
) {
    /** A user sent as is: only the name, the email and the invoice profiles, everything else is kept in L&P. */
    class AdditionalUser(
        var email: String? = null,
        var firstName: String? = null,
        var lastName: String? = null,
    ) {
        @get:JsonIgnore
        val isEmpty: Boolean
            get() = email.isNullOrBlank() && firstName.isNullOrBlank() && lastName.isNullOrBlank()
    }


    /** An error of [validate]: the [field] of the JSON object, the [index] of the entry and the i18n key. */
    data class Error(val field: String, val i18nKey: String, val index: Int? = null, val param: String? = null)

    @get:JsonIgnore
    val invoiceProfileIds: List<Long>
        get() = accountingInvoiceProfileIds.filterNotNull()

    /** The general Kost1 numbers without the empty rows of the editor. */
    @get:JsonIgnore
    val generalKost1Numbers: List<String>
        get() = generalKost1.map { it.trim() }.filter { it.isNotEmpty() }

    /** The general Kost2 numbers without the empty rows of the editor. */
    @get:JsonIgnore
    val generalKost2Numbers: List<String>
        get() = generalKost2.map { it.trim() }.filter { it.isNotEmpty() }

    /** The additional users without the empty rows of the editor. */
    @get:JsonIgnore
    val additionalUserList: List<AdditionalUser>
        get() = additionalUsers.filter { !it.isEmpty }

    @get:JsonIgnore
    val kost2Regexes: List<Regex>
        get() = kost2Patterns.map { it.trim() }.filter { it.isNotEmpty() }.map { globToRegex(it) }

    /**
     * @param findKost1 The Kost1 of the given formatted number (or null, if unknown).
     * @param findKost2 The Kost2 of the given formatted number (or null, if unknown).
     */
    fun validate(findKost1: (String) -> Kost1DO?, findKost2: (String) -> Kost2DO?): List<Error> {
        val errors = mutableListOf<Error>()
        if (invoiceProfileIds.isEmpty()) {
            errors += Error(ACCOUNTING_INVOICE_PROFILE_IDS, "$ERROR_PREFIX.invoiceProfileRequired")
        }
        accountingInvoiceProfileIds.forEachIndexed { index, id ->
            if (id != null && id <= 0) {
                errors += Error(ACCOUNTING_INVOICE_PROFILE_IDS, "$ERROR_PREFIX.invoiceProfileId", index, "$id")
            }
        }
        validateNumbers(errors, GENERAL_KOST1, "kost1", generalKost1) { number ->
            findKost1(number)?.let { it.id!! to isActive(it) }
        }
        validateNumbers(errors, GENERAL_KOST2, "kost2", generalKost2) { number ->
            findKost2(number)?.let { it.id!! to isActive(it) }
        }
        kost2Patterns.forEachIndexed { index, pattern ->
            if (pattern.isNotBlank() && !PATTERN_REGEX.matches(pattern.trim())) {
                errors += Error(KOST2_PATTERNS, "$ERROR_PREFIX.pattern", index, pattern)
            }
        }
        val emails = mutableSetOf<String>()
        additionalUsers.forEachIndexed { index, user ->
            if (user.isEmpty) {
                return@forEachIndexed
            }
            val email = user.email?.trim() ?: ""
            if (!EMAIL_REGEX.matches(email)) {
                errors += Error(ADDITIONAL_USERS, "$ERROR_PREFIX.additionalUserEmail", index, email)
            } else if (!emails.add(email.lowercase())) {
                errors += Error(ADDITIONAL_USERS, "$ERROR_PREFIX.additionalUserDuplicate", index, email)
            }
            if (user.firstName.isNullOrBlank() || user.lastName.isNullOrBlank()) {
                errors += Error(ADDITIONAL_USERS, "$ERROR_PREFIX.additionalUserName", index)
            }
        }
        return errors
    }

    /**
     * @param kost "kost1" or "kost2", part of the i18n keys of the errors.
     * @param find The id and the active state of the given number (or null, if unknown).
     */
    private fun validateNumbers(
        errors: MutableList<Error>,
        field: String,
        kost: String,
        numbers: List<String>,
        find: (String) -> Pair<Long, Boolean>?,
    ) {
        val ids = mutableSetOf<Long>()
        numbers.forEachIndexed { index, number ->
            if (number.isBlank()) {
                return@forEachIndexed
            }
            val (id, active) = find(number.trim()) ?: run {
                errors += Error(field, "$ERROR_PREFIX.${kost}NotFound", index, number)
                return@forEachIndexed
            }
            if (!active) {
                errors += Error(field, "$ERROR_PREFIX.${kost}NotActive", index, number)
            } else if (!ids.add(id)) {
                errors += Error(field, "$ERROR_PREFIX.${kost}Duplicate", index, number)
            }
        }
    }

    companion object {
        const val ACCOUNTING_INVOICE_PROFILE_IDS = "accountingInvoiceProfileIds"
        const val GENERAL_KOST1 = "generalKost1"
        const val GENERAL_KOST2 = "generalKost2"
        const val KOST2_PATTERNS = "kost2Patterns"
        const val ADDITIONAL_USERS = "additionalUsers"

        /** The i18n prefix of the labels of the fields, see [Error.field]. */
        const val I18N_PREFIX = "lanesAndPlanes.config"
        private const val ERROR_PREFIX = "$I18N_PREFIX.error"

        private val PATTERN_REGEX = Regex("[0-9.*?]+")

        private val EMAIL_REGEX = Regex("[^@\\s]+@[^@\\s]+\\.[^@\\s]+")

        /** The settings of the given JSON value; the defaults if blank or not readable. */
        fun parse(json: String?): LanesAndPlanesSettings {
            if (json.isNullOrBlank()) {
                return LanesAndPlanesSettings()
            }
            return try {
                JsonUtils.fromJson(json, LanesAndPlanesSettings::class.java, failOnUnknownProps = false)
                    ?: LanesAndPlanesSettings()
            } catch (ex: Exception) {
                log.warn { "Lanes & Planes: configuration not readable, using the defaults: ${ex.message}" }
                LanesAndPlanesSettings()
            }
        }

        /** Not deleted and active. */
        fun isActive(kost1: Kost1DO): Boolean {
            return !kost1.deleted && (kost1.kostentraegerStatus ?: KostentraegerStatus.ACTIVE) == KostentraegerStatus.ACTIVE
        }

        /** Not deleted and effectively active. */
        fun isActive(kost2: Kost2DO): Boolean {
            return !kost2.deleted &&
                    (kost2.effectiveKostentraegerStatus ?: KostentraegerStatus.ACTIVE) == KostentraegerStatus.ACTIVE
        }

        /**
         * A glob pattern of a formatted Kost2 number as regex for the whole number: `*` matches any characters
         * (dots included), `?` a single one.
         */
        internal fun globToRegex(glob: String): Regex {
            val regex = glob.split('*').joinToString(".*") { part ->
                part.split('?').joinToString(".") { if (it.isEmpty()) "" else Regex.escape(it) }
            }
            return Regex(regex)
        }
    }
}
