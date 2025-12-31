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

package org.projectforge.business.address

import mu.KotlinLogging
import org.projectforge.framework.i18n.translate
import org.springframework.stereotype.Service

private val log = KotlinLogging.logger {}

/**
 * Service for parsing address text (e.g., email signatures) and converting
 * to structured field mappings with confidence levels.
 */
@Service
class AddressTextImportService {

    /**
     * Parses free text and returns structured field mapping with confidence levels.
     *
     * @param text The raw text to parse (email signature, contact info, etc.)
     * @return ParsedFieldMapping with fields, confidence levels, and warnings
     */
    fun parseText(text: String): ParsedFieldMapping {
        if (text.isBlank()) {
            return ParsedFieldMapping(
                fields = emptyMap(),
                warnings = listOf(translate("address.parseText.error.emptyInput"))
            )
        }

        val parsed = AddressTextParser.parseAddressText(text)
        log.debug { "Parsed text: $parsed" }

        val fieldMapping = createFieldMapping(parsed)
        val warnings = validateParsedData(parsed)

        return ParsedFieldMapping(
            fields = fieldMapping,
            warnings = warnings
        )
    }

    /**
     * Converts ParsedAddressData to field mapping with confidence levels.
     */
    private fun createFieldMapping(parsed: ParsedAddressData): Map<String, ParsedField> {
        val fields = mutableMapOf<String, ParsedField>()

        // Name fields
        addField(fields, "title", parsed.title, determineConfidence(parsed.title))
        addField(fields, "form", parsed.form, determineConfidence(parsed.form))
        addField(fields, "firstName", parsed.firstName, determineConfidence(parsed.firstName))
        addField(fields, "name", parsed.name, determineConfidence(parsed.name))

        // Organization fields
        addField(fields, "organization", parsed.organization, determineConfidence(parsed.organization))
        addField(fields, "division", parsed.division, determineConfidence(parsed.division))
        addField(fields, "positionText", parsed.positionText, ConfidenceLevel.MEDIUM)

        // Phone numbers (high confidence due to PhoneNumberUtils normalization)
        addField(fields, "businessPhone", parsed.businessPhone, ConfidenceLevel.HIGH)
        addField(fields, "mobilePhone", parsed.mobilePhone, ConfidenceLevel.HIGH)
        addField(fields, "fax", parsed.fax, ConfidenceLevel.HIGH)
        addField(fields, "privatePhone", parsed.privatePhone, ConfidenceLevel.HIGH)
        addField(fields, "privateMobilePhone", parsed.privateMobilePhone, ConfidenceLevel.HIGH)

        // Email addresses (high confidence due to regex validation)
        addField(fields, "email", parsed.email, ConfidenceLevel.HIGH)
        addField(fields, "privateEmail", parsed.privateEmail, ConfidenceLevel.HIGH)

        // Business address fields
        addField(fields, "addressText", parsed.addressText, determineConfidence(parsed.addressText))
        addField(fields, "addressText2", parsed.addressText2, ConfidenceLevel.MEDIUM)
        addField(fields, "zipCode", parsed.zipCode, ConfidenceLevel.HIGH)
        addField(fields, "city", parsed.city, ConfidenceLevel.HIGH)
        addField(fields, "state", parsed.state, ConfidenceLevel.MEDIUM)
        addField(fields, "country", parsed.country, ConfidenceLevel.MEDIUM)

        // Website
        addField(fields, "website", parsed.website, ConfidenceLevel.HIGH)

        return fields
    }

    /**
     * Adds field to mapping only if value is not null/blank.
     */
    private fun addField(
        fields: MutableMap<String, ParsedField>,
        fieldName: String,
        value: String?,
        confidence: ConfidenceLevel
    ) {
        if (!value.isNullOrBlank()) {
            fields[fieldName] = ParsedField(
                value = value,
                confidence = confidence,
                selected = confidence != ConfidenceLevel.LOW  // Auto-select high/medium confidence fields
            )
        }
    }

    /**
     * Determines confidence level based on whether value exists and looks valid.
     */
    private fun determineConfidence(value: String?): ConfidenceLevel {
        return if (value.isNullOrBlank()) {
            ConfidenceLevel.LOW
        } else {
            // Simple heuristic: if value exists, we're confident
            ConfidenceLevel.HIGH
        }
    }

    /**
     * Validates parsed data and returns list of warnings.
     */
    private fun validateParsedData(parsed: ParsedAddressData): List<String> {
        val warnings = mutableListOf<String>()

        // Warn if no name or organization
        if (parsed.firstName == null && parsed.name == null && parsed.organization == null) {
            warnings.add(translate("address.parseText.warning.noNameOrOrg"))
        }

        // Warn if no email
        if (parsed.email == null && parsed.privateEmail == null) {
            warnings.add(translate("address.parseText.warning.noEmail"))
        }

        // Warn if no address
        if (parsed.addressText == null && parsed.zipCode == null && parsed.city == null) {
            warnings.add(translate("address.parseText.warning.noAddress"))
        }

        return warnings
    }
}

/**
 * Result of parsing address text, containing fields with confidence levels.
 */
data class ParsedFieldMapping(
    val fields: Map<String, ParsedField>,
    val warnings: List<String> = emptyList()
)

/**
 * A single parsed field with its value, confidence level, and selection state.
 */
data class ParsedField(
    val value: String?,
    val confidence: ConfidenceLevel,
    val selected: Boolean = value != null && confidence != ConfidenceLevel.LOW
)

/**
 * Confidence level for parsed fields.
 */
enum class ConfidenceLevel {
    HIGH,    // Green indicator - very confident in parsing
    MEDIUM,  // Yellow indicator - somewhat confident
    LOW      // Red indicator - low confidence or missing
}
