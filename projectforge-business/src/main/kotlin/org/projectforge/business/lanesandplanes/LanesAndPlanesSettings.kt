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
     * Users of Lanes & Planes who aren't employees (e.g. an external accountant), sent in addition to the
     * employees: otherwise the full replace would deactivate them, and a missing user with responsibilities lets
     * the whole push fail. Not the service admin of L&P and no guest profiles: they aren't touched by the import.
     */
    var additionalUsers: List<AdditionalUser> = emptyList(),
    /**
     * The names of the cost centers and cost units already existing in Lanes & Planes (e.g. entered by hand),
     * starting with the number without dots, e.g. `50123401 Project - Travel costs` (as in the user export). A Kost
     * with this number is sent with the existing name as ident and name, so L&P reuses the entry: L&P refuses a
     * new ident with an already used name (deleted entries included) and the import of the user fails.
     */
    var existingCostObjects: List<String> = emptyList(),
    /**
     * The emails of the users existing in Lanes & Planes (the `email` column of the user export, the ExtAPI can't
     * read users). An employee whose email isn't listed, but one of the alternative or former emails of the
     * user, is sent with that one (held back like a changed email, see [LanesAndPlanesEmailHolds]), so L&P doesn't
     * create the user anew. Empty: not checked.
     */
    var existingUserEmails: List<String> = emptyList(),
    /**
     * New emails of employees which were already changed in Lanes & Planes by hand. L&P identifies users by their
     * email only, so a changed email is held back (the old one is sent) until it's listed here, see
     * [LanesAndPlanesEmailHolds].
     */
    var confirmedEmailChanges: List<String> = emptyList(),
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

    /** The existing names of [existingCostObjects] by their number (without dots). */
    @get:JsonIgnore
    val existingNamesByNumber: Map<String, String>
        get() = existingCostObjects.map { it.trim() }.mapNotNull { name ->
            EXISTING_NAME_REGEX.find(name)?.let { it.groupValues[1] to name }
        }.distinctBy { it.first }.toMap()

    /** The emails of [existingUserEmails], lower case. */
    @get:JsonIgnore
    val existingEmails: Set<String>
        get() = existingUserEmails.map { it.trim().lowercase() }.filter { it.isNotEmpty() }.toSet()

    /** The confirmed new emails, lower case. */
    @get:JsonIgnore
    val confirmedEmails: Set<String>
        get() = confirmedEmailChanges.map { it.trim().lowercase() }.filter { it.isNotEmpty() }.toSet()

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
            } else if (email.lowercase().endsWith(LANES_AND_PLANES_DOMAIN)) {
                errors += Error(ADDITIONAL_USERS, "$ERROR_PREFIX.additionalUserLanesAndPlanes", index, email)
            } else if (!emails.add(email.lowercase())) {
                errors += Error(ADDITIONAL_USERS, "$ERROR_PREFIX.additionalUserDuplicate", index, email)
            }
            if (user.firstName.isNullOrBlank() || user.lastName.isNullOrBlank()) {
                errors += Error(ADDITIONAL_USERS, "$ERROR_PREFIX.additionalUserName", index)
            }
        }
        val numbers = mutableSetOf<String>()
        existingCostObjects.forEachIndexed { index, entry ->
            val name = entry.trim()
            if (name.isEmpty()) {
                return@forEachIndexed
            }
            val number = EXISTING_NAME_REGEX.find(name)?.groupValues?.get(1)
            if (number == null) {
                errors += Error(EXISTING_COST_OBJECTS, "$ERROR_PREFIX.existingCostObjectNumber", index, name)
            } else if (!numbers.add(number)) {
                errors += Error(EXISTING_COST_OBJECTS, "$ERROR_PREFIX.existingCostObjectDuplicate", index, number)
            }
            if (name.length > LanesAndPlanesPayloadBuilder.MAX_NAME_LENGTH) {
                errors += Error(EXISTING_COST_OBJECTS, "$ERROR_PREFIX.existingCostObjectLength", index, name)
            }
        }
        existingUserEmails.forEachIndexed { index, entry ->
            val email = entry.trim()
            if (email.isNotEmpty() && !EMAIL_REGEX.matches(email)) {
                errors += Error(EXISTING_USER_EMAILS, "$ERROR_PREFIX.existingUserEmail", index, email)
            }
        }
        confirmedEmailChanges.forEachIndexed { index, entry ->
            val email = entry.trim()
            if (email.isNotEmpty() && !EMAIL_REGEX.matches(email)) {
                errors += Error(CONFIRMED_EMAIL_CHANGES, "$ERROR_PREFIX.confirmedEmail", index, email)
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
        const val EXISTING_COST_OBJECTS = "existingCostObjects"
        const val EXISTING_USER_EMAILS = "existingUserEmails"
        const val CONFIRMED_EMAIL_CHANGES = "confirmedEmailChanges"

        /** The i18n prefix of the labels of the fields, see [Error.field]. */
        const val I18N_PREFIX = "lanesAndPlanes.config"
        private const val ERROR_PREFIX = "$I18N_PREFIX.error"

        private val PATTERN_REGEX = Regex("[0-9.*?]+")

        /** The number without dots at the start of an existing name, e.g. `50123401` of `50123401 Project`. */
        private val EXISTING_NAME_REGEX = Regex("^(\\d{8})(?!\\d)")

        /** Users of this domain (the service admin, guest profiles) aren't touched by the import of L&P. */
        private const val LANES_AND_PLANES_DOMAIN = "@lanes-planes.com"

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
