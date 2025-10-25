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

package org.projectforge.rest.address.importer

import mu.KotlinLogging
import org.projectforge.business.address.AddressDO
import org.projectforge.business.address.AddressImageDO
import org.projectforge.business.address.FormOfAddress
import org.projectforge.common.StringMatchUtils
import org.projectforge.rest.dto.BaseDTO
import org.projectforge.rest.importer.ImportPairEntry
import java.time.LocalDate
import java.util.*
import kotlin.reflect.KProperty

private val log = KotlinLogging.logger {}

/**
 * Data Transfer Object for importing addresses from VCF files.
 * Supports all major address fields including business and private contact information.
 */
class AddressImportDTO(
    var name: String? = null,
    var firstName: String? = null,
    var organization: String? = null,
    var division: String? = null,
    var positionText: String? = null,
    var title: String? = null,
    var form: FormOfAddress? = null,
    var birthName: String? = null,
    var birthday: LocalDate? = null,

    // Business contact
    var email: String? = null,
    var businessPhone: String? = null,
    var mobilePhone: String? = null,
    var fax: String? = null,

    // Private contact
    var privateEmail: String? = null,
    var privatePhone: String? = null,
    var privateMobilePhone: String? = null,

    // Business address
    var addressText: String? = null,
    var addressText2: String? = null,
    var zipCode: String? = null,
    var city: String? = null,
    var country: String? = null,
    var state: String? = null,

    // Private address
    var privateAddressText: String? = null,
    var privateAddressText2: String? = null,
    var privateZipCode: String? = null,
    var privateCity: String? = null,
    var privateCountry: String? = null,
    var privateState: String? = null,

    // Postal address
    var postalAddressText: String? = null,
    var postalAddressText2: String? = null,
    var postalZipCode: String? = null,
    var postalCity: String? = null,
    var postalCountry: String? = null,
    var postalState: String? = null,

    // Other fields
    var website: String? = null,
    var communicationLanguage: Locale? = null,
    var fingerprint: String? = null,
    var publicKey: String? = null,
    var comment: String? = null,
    var uid: String? = null,
) : BaseDTO<AddressDO>(), ImportPairEntry.Modified<AddressImportDTO> {

    /**
     * Import errors that occurred during parsing.
     * Stored in DTO to survive the reconcile process (which recreates PairEntries).
     */
    private val errors = mutableListOf<String>()

    /**
     * Transient attributes for storing temporary data (e.g., images).
     */
    private val transientAttributes = mutableMapOf<String, Any>()

    /**
     * Adds an error message to this import entity.
     */
    fun addError(errorMessage: String) {
        errors.add(errorMessage)
    }

    /**
     * Gets all error messages as a list.
     */
    fun getErrors(): List<String> = errors.toList()

    /**
     * Sets a transient attribute.
     */
    fun setTransientAttribute(key: String, value: Any) {
        transientAttributes[key] = value
    }

    /**
     * Gets a transient attribute.
     */
    fun getTransientAttribute(key: String): Any? {
        return transientAttributes[key]
    }

    override val properties: Array<KProperty<*>>
        get() = arrayOf(
            AddressDO::name,
            AddressDO::firstName,
            AddressDO::organization,
            AddressDO::division,
            AddressDO::positionText,
            AddressDO::title,
            AddressDO::form,
            AddressDO::birthName,
            AddressDO::birthday,
            AddressDO::email,
            AddressDO::businessPhone,
            AddressDO::mobilePhone,
            AddressDO::fax,
            AddressDO::privateEmail,
            AddressDO::privatePhone,
            AddressDO::privateMobilePhone,
            AddressDO::addressText,
            AddressDO::addressText2,
            AddressDO::zipCode,
            AddressDO::city,
            AddressDO::country,
            AddressDO::state,
            AddressDO::privateAddressText,
            AddressDO::privateAddressText2,
            AddressDO::privateZipCode,
            AddressDO::privateCity,
            AddressDO::privateCountry,
            AddressDO::privateState,
            AddressDO::postalAddressText,
            AddressDO::postalAddressText2,
            AddressDO::postalZipCode,
            AddressDO::postalCity,
            AddressDO::postalCountry,
            AddressDO::postalState,
            AddressDO::website,
            AddressDO::communicationLanguage,
            AddressDO::fingerprint,
            AddressDO::publicKey,
            AddressDO::comment,
        )

    override fun copyFrom(src: AddressDO) {
        super.copyFrom(src)
        this.name = src.name
        this.firstName = src.firstName
        this.organization = src.organization
        this.division = src.division
        this.positionText = src.positionText
        this.title = src.title
        this.form = src.form
        this.birthName = src.birthName
        this.birthday = src.birthday
        this.email = src.email
        this.businessPhone = src.businessPhone
        this.mobilePhone = src.mobilePhone
        this.fax = src.fax
        this.privateEmail = src.privateEmail
        this.privatePhone = src.privatePhone
        this.privateMobilePhone = src.privateMobilePhone
        this.addressText = src.addressText
        this.addressText2 = src.addressText2
        this.zipCode = src.zipCode
        this.city = src.city
        this.country = src.country
        this.state = src.state
        this.privateAddressText = src.privateAddressText
        this.privateAddressText2 = src.privateAddressText2
        this.privateZipCode = src.privateZipCode
        this.privateCity = src.privateCity
        this.privateCountry = src.privateCountry
        this.privateState = src.privateState
        this.postalAddressText = src.postalAddressText
        this.postalAddressText2 = src.postalAddressText2
        this.postalZipCode = src.postalZipCode
        this.postalCity = src.postalCity
        this.postalCountry = src.postalCountry
        this.postalState = src.postalState
        this.website = src.website
        this.communicationLanguage = src.communicationLanguage
        this.fingerprint = src.fingerprint
        this.publicKey = src.publicKey
        this.comment = src.comment
        this.uid = src.uid
    }

    override fun copyTo(obj: AddressDO) {
        if (this.id != null) obj.id = this.id
        obj.name = this.name
        obj.firstName = this.firstName
        obj.organization = this.organization
        obj.division = this.division
        obj.positionText = this.positionText
        obj.title = this.title
        obj.form = this.form
        obj.birthName = this.birthName
        obj.birthday = this.birthday
        obj.email = this.email
        obj.businessPhone = this.businessPhone
        obj.mobilePhone = this.mobilePhone
        obj.fax = this.fax
        obj.privateEmail = this.privateEmail
        obj.privatePhone = this.privatePhone
        obj.privateMobilePhone = this.privateMobilePhone
        obj.addressText = this.addressText
        obj.addressText2 = this.addressText2
        obj.zipCode = this.zipCode
        obj.city = this.city
        obj.country = this.country
        obj.state = this.state
        obj.privateAddressText = this.privateAddressText
        obj.privateAddressText2 = this.privateAddressText2
        obj.privateZipCode = this.privateZipCode
        obj.privateCity = this.privateCity
        obj.privateCountry = this.privateCountry
        obj.privateState = this.privateState
        obj.postalAddressText = this.postalAddressText
        obj.postalAddressText2 = this.postalAddressText2
        obj.postalZipCode = this.postalZipCode
        obj.postalCity = this.postalCity
        obj.postalCountry = this.postalCountry
        obj.postalState = this.postalState
        obj.website = this.website
        obj.communicationLanguage = this.communicationLanguage
        obj.fingerprint = this.fingerprint
        obj.publicKey = this.publicKey
        obj.comment = this.comment
        obj.uid = this.uid
    }

    override fun buildOldDiffValues(map: MutableMap<String, Any>, old: AddressImportDTO) {
        // No custom diff values needed for addresses
    }

    /**
     * Calculate matching score with an existing AddressDO for import reconciliation.
     * Higher score means better match. Score 0 means no match.
     *
     * Matching criteria (in order of importance):
     * - **Required**: Name + FirstName must match
     *   - Exact match: +50 points
     *   - Normalized match: +40 points
     *   - High similarity (>80%): +30 points
     * - **Additional scoring** (when multiple candidates exist):
     *   - Organization match: +20 points
     *   - Email match: +15 points
     *   - Phone match: +10 points
     *
     * Minimum score threshold: 50 points for a valid match
     *
     * @param logErrors If true, logs errors when calculation fails
     */
    fun matchScore(dbAddress: AddressDO, logErrors: Boolean = false): Int {
        var score = 0

        // Stage 1: Name + FirstName matching (REQUIRED)
        val nameScore = calculateNameMatchScore(dbAddress)
        if (nameScore < 30) {
            // Name doesn't match sufficiently, no match possible
            if (logErrors && nameScore > 0) {
                log.debug { "Name match too weak: Import='${this.name}/${this.firstName}' vs DB='${dbAddress.name}/${dbAddress.firstName}', score=$nameScore" }
            }
            return 0
        }
        score += nameScore

        // Stage 2: Additional scoring for disambiguation
        val organizationScore = calculateOrganizationMatchScore(dbAddress)
        val emailScore = calculateEmailMatchScore(dbAddress)
        val phoneScore = calculatePhoneMatchScore(dbAddress)

        score += organizationScore
        score += emailScore
        score += phoneScore

        // Log score calculation for debugging (only for significant scores)
        if (score >= 50) {
            log.debug {
                "MATCH SCORE: Import='${this.name}/${this.firstName}' vs DB='${dbAddress.name}/${dbAddress.firstName}' | " +
                        "ImportOrg='${this.organization}' vs DBOrg='${dbAddress.organization}' | " +
                        "ImportEmail='${this.email}' vs DBEmail='${dbAddress.email}' | " +
                        "Scores: name=$nameScore, org=$organizationScore, email=$emailScore, phone=$phoneScore | " +
                        "TOTAL=$score"
            }
        }

        return score
    }

    /**
     * Calculate name + firstName match score.
     * This is the primary matching criterion and must score at least 30 points.
     */
    private fun calculateNameMatchScore(dbAddress: AddressDO): Int {
        val thisName = this.name
        val thisFirstName = this.firstName
        val dbName = dbAddress.name
        val dbFirstName = dbAddress.firstName

        // Name (lastName) must exist in both addresses
        if (thisName.isNullOrBlank() || dbName.isNullOrBlank()) {
            return 0
        }

        val thisFirstNameBlank = thisFirstName.isNullOrBlank()
        val dbFirstNameBlank = dbFirstName.isNullOrBlank()

        // Case 1: Both firstNames are empty - match only on lastName
        if (thisFirstNameBlank && dbFirstNameBlank) {
            // Exact match (case-insensitive)
            if (thisName.equals(dbName, ignoreCase = true)) {
                return 45  // Slightly lower than full name match
            }

            // Normalized match using StringMatchUtils
            val similarity = StringMatchUtils.calculateSimilarity(thisName, dbName)
            return when {
                similarity >= 1.0 -> 35   // Perfect normalized match
                similarity >= 0.8 -> 30   // High similarity
                similarity >= 0.6 -> 20   // Medium similarity (below threshold)
                else -> 0                  // No meaningful similarity
            }
        }

        // Case 2: One firstName is empty, the other is not - no match
        // (Different person - one has firstName, the other doesn't)
        if (thisFirstNameBlank != dbFirstNameBlank) {
            return 0
        }

        // Case 3: Both firstNames exist - match on full name
        // Exact match (case-insensitive)
        if (thisName.equals(dbName, ignoreCase = true) &&
            thisFirstName.equals(dbFirstName, ignoreCase = true)
        ) {
            return 50
        }

        // Normalized match using StringMatchUtils
        val fullName = "$thisName $thisFirstName"
        val dbFullName = "$dbName $dbFirstName"
        val similarity = StringMatchUtils.calculateSimilarity(fullName, dbFullName)

        return when {
            similarity >= 1.0 -> 40   // Perfect normalized match
            similarity >= 0.8 -> 30   // High similarity
            similarity >= 0.6 -> 20   // Medium similarity (below threshold)
            else -> 0                  // No meaningful similarity
        }
    }

    /**
     * Calculate organization match score.
     */
    private fun calculateOrganizationMatchScore(dbAddress: AddressDO): Int {
        val thisOrg = this.organization
        val dbOrg = dbAddress.organization

        if (thisOrg.isNullOrBlank() || dbOrg.isNullOrBlank()) {
            return 0
        }

        // Exact match
        if (thisOrg.equals(dbOrg, ignoreCase = true)) {
            return 20
        }

        // Company similarity matching
        val similarity = StringMatchUtils.calculateCompanySimilarity(thisOrg, dbOrg)
        return when {
            similarity >= 0.8 -> 15   // High similarity
            similarity >= 0.6 -> 10   // Medium similarity
            else -> 0                  // No meaningful similarity
        }
    }

    /**
     * Calculate email match score (business or private).
     */
    private fun calculateEmailMatchScore(dbAddress: AddressDO): Int {
        val thisBusinessEmail = this.email?.trim()?.lowercase()
        val thisPrivateEmail = this.privateEmail?.trim()?.lowercase()
        val dbBusinessEmail = dbAddress.email?.trim()?.lowercase()
        val dbPrivateEmail = dbAddress.privateEmail?.trim()?.lowercase()

        // Check business email match
        if (!thisBusinessEmail.isNullOrBlank() && thisBusinessEmail == dbBusinessEmail) {
            return 15
        }

        // Check private email match
        if (!thisPrivateEmail.isNullOrBlank() && thisPrivateEmail == dbPrivateEmail) {
            return 15
        }

        // Cross-check: import business email matches DB private email (or vice versa)
        if (!thisBusinessEmail.isNullOrBlank() && thisBusinessEmail == dbPrivateEmail) {
            return 10
        }
        if (!thisPrivateEmail.isNullOrBlank() && thisPrivateEmail == dbBusinessEmail) {
            return 10
        }

        return 0
    }

    /**
     * Calculate phone match score (any phone number).
     * Normalizes phone numbers before comparison.
     */
    private fun calculatePhoneMatchScore(dbAddress: AddressDO): Int {
        val importPhones = listOfNotNull(
            this.businessPhone,
            this.mobilePhone,
            this.privatePhone,
            this.privateMobilePhone
        ).map { normalizePhoneNumber(it) }

        val dbPhones = listOfNotNull(
            dbAddress.businessPhone,
            dbAddress.mobilePhone,
            dbAddress.privatePhone,
            dbAddress.privateMobilePhone
        ).map { normalizePhoneNumber(it) }

        // Check if any import phone matches any DB phone
        importPhones.forEach { importPhone ->
            if (importPhone.isNotBlank() && dbPhones.contains(importPhone)) {
                return 10
            }
        }

        return 0
    }

    /**
     * Normalize phone number for comparison (remove spaces, dashes, parentheses).
     */
    private fun normalizePhoneNumber(phone: String?): String {
        return phone?.replace(Regex("[\\s\\-()]+"), "") ?: ""
    }
}
