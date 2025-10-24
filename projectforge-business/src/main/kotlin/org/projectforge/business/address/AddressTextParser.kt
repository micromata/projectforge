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

package org.projectforge.business.address

import mu.KotlinLogging

private val log = KotlinLogging.logger {}

/**
 * Parser for extracting address information from free text (e.g., email signatures).
 */
object AddressTextParser {

    // Common German and English academic/professional titles
    private val TITLE_PATTERNS = listOf(
        "Dr\\.",
        "Prof\\.",
        "Dipl\\.-Kfm\\.",
        "Dipl\\.-Ing\\.",
        "Dipl\\.-Inf\\.",
        "Dipl\\.",
        "B\\.Sc\\.",
        "M\\.Sc\\.",
        "B\\.A\\.",
        "M\\.A\\.",
        "Ph\\.D\\.",
        "MBA"
    )

    // Company suffixes
    private val COMPANY_SUFFIXES = listOf(
        "GmbH",
        "AG",
        "e\\.V\\.",
        "KG",
        "OHG",
        "UG",
        "SE",
        "Ltd\\.",
        "Inc\\.",
        "Corp\\.",
        "LLC",
        "PLC"
    )

    // Email regex
    private val EMAIL_REGEX = Regex(
        """[a-zA-Z0-9._%+-]+@[a-zA-Z0-9.-]+\.[a-zA-Z]{2,}""",
        RegexOption.IGNORE_CASE
    )

    // Website regex
    private val WEBSITE_REGEX = Regex(
        """(?:https?://)?(?:www\.)?[a-zA-Z0-9.-]+\.[a-zA-Z]{2,}(?:/[^\s]*)?""",
        RegexOption.IGNORE_CASE
    )

    // Phone regex (various formats with flexible separators)
    // Matches phone numbers with digits and common separators (spaces, -, /, ., parentheses)
    // Supports both "Tel:" and "Tel" (with/without colon)
    private val PHONE_REGEX = Regex(
        """(?:Tel\.?:?|Telefon:?|Phone:?|Fon:?|Mobil:?|Mobile:?|Fax:?)\s*(\+?(?:\d+[\s\-./()]*)+\d)""",
        RegexOption.IGNORE_CASE
    )

    // ZIP + City (4-5 digits + city name, optionally with "D-" or "CH-" prefix)
    // Supports German (5 digits), Swiss (4 digits), and other formats
    private val ZIP_CITY_REGEX = Regex(
        """(?:D-|CH-)?(\d{4,5})\s+([A-ZÄÖÜ][a-zäöüß]+(?:[\s-][A-ZÄÖÜ]?[a-zäöüß]+)*)""",
    )

    // Street address (street name + house number)
    private val STREET_REGEX = Regex(
        """([A-ZÄÖÜ][a-zäöüß]+(?:[\s-][A-ZÄÖÜ]?[a-zäöüß]+)*\.?(?:\s+|-)(?:\d+[a-zA-Z]?(?:\s*-\s*\d+[a-zA-Z]?)?))""",
    )

    // Country name (common countries in multiple languages, optionally with second name after /)
    private val COUNTRY_REGEX = Regex(
        """^(Deutschland|Germany|Schweiz|Switzerland|Österreich|Austria|France|Frankreich|Italia?|Italy|UK|USA|United States|United Kingdom|Nederland|Netherlands|Belgique|Belgium|España|Spain|Portugal|Sverige|Sweden|Norge|Norway|Danmark|Denmark|Polska|Poland|Česko|Czech Republic|Slovensko|Slovakia|Magyarország|Hungary|România|Romania|Bulgarien|Bulgaria|Ellinikí Demokratía|Greece|Türkiye|Turkey|Россия|Russia)(?:\s*/\s*(?:Deutschland|Germany|Schweiz|Switzerland|Österreich|Austria|France|Frankreich|Italia?|Italy|UK|USA|United States|United Kingdom|Nederland|Netherlands|Belgique|Belgium|España|Spain|Portugal|Sverige|Sweden|Norge|Norway|Danmark|Denmark|Polska|Poland|Česko|Czech Republic|Slovensko|Slovakia|Magyarország|Hungary|România|Romania|Bulgarien|Bulgaria|Ellinikí Demokratía|Greece|Türkiye|Turkey|Россия|Russia))?$""",
        RegexOption.IGNORE_CASE
    )

    /**
     * Parses free text and extracts address information.
     */
    fun parseAddressText(text: String): ParsedAddressData {
        val result = ParsedAddressData()

        // Normalize text
        val lines = text.lines()
            .map { it.trim() }
            .filter { it.isNotBlank() }

        if (lines.isEmpty()) {
            return result
        }

        // First pass: Extract obvious patterns (email, phone, website, zip+city)
        val remainingLines = mutableListOf<String>()
        val phoneNumbers = mutableListOf<String>()

        for (line in lines) {
            var processed = false

            // Extract email
            EMAIL_REGEX.find(line)?.let {
                if (result.email == null) {
                    result.email = it.value
                    processed = true
                }
            }

            // Extract website (but not email)
            if (!line.contains("@")) {
                WEBSITE_REGEX.find(line)?.let {
                    if (result.website == null && !it.value.contains("@")) {
                        result.website = it.value
                        processed = true
                    }
                }
            }

            // Extract phone numbers
            if (line.matches(Regex(""".*(?:Tel\.?:?|Telefon:?|Phone:?|Fon:?|Mobil:?|Mobile:?|Fax:?).*""", RegexOption.IGNORE_CASE))) {
                val phoneMatch = PHONE_REGEX.find(line)
                if (phoneMatch != null) {
                    val phone = phoneMatch.groupValues[1].trim()
                    phoneNumbers.add(phone)

                    // Normalize phone number
                    val normalizedPhone = org.projectforge.framework.utils.PhoneNumberUtils.normalizePhoneNumber(phone)

                    // Determine phone type
                    when {
                        line.contains(Regex("""Mobil|Mobile""", RegexOption.IGNORE_CASE)) -> {
                            if (result.mobilePhone == null) result.mobilePhone = normalizedPhone
                        }
                        line.contains(Regex("""Fax""", RegexOption.IGNORE_CASE)) -> {
                            if (result.fax == null) result.fax = normalizedPhone
                        }
                        else -> {
                            if (result.businessPhone == null) result.businessPhone = normalizedPhone
                        }
                    }
                    processed = true
                }
            }

            // Extract ZIP + City (might be combined with street in same line)
            ZIP_CITY_REGEX.find(line)?.let {
                if (result.zipCode == null) {
                    result.zipCode = it.groupValues[1]
                    result.city = it.groupValues[2]

                    // Check if street is in the same line (before ZIP+City, separated by comma)
                    val beforeZip = line.substring(0, it.range.first).trim()
                    if (beforeZip.isNotEmpty()) {
                        // Remove trailing comma if present
                        val street = beforeZip.removeSuffix(",").trim()
                        if (result.addressText == null && street.isNotEmpty()) {
                            result.addressText = street
                        }
                    }

                    processed = true
                }
            }

            // Extract country name
            COUNTRY_REGEX.find(line)?.let {
                if (result.country == null) {
                    // Extract first country name (before optional /)
                    result.country = it.groupValues[1].trim()
                    processed = true
                }
            }

            if (!processed) {
                remainingLines.add(line)
            }
        }

        // Second pass: Extract name, position, organization, address from remaining lines
        if (remainingLines.isNotEmpty()) {
            // First line: usually name (with or without title)
            val firstLine = remainingLines[0]
            parseName(firstLine, result)

            // Subsequent lines: position, organization, address
            var lineIndex = 1
            while (lineIndex < remainingLines.size) {
                val line = remainingLines[lineIndex]

                // Skip ignorable lines (logo, member info, etc.)
                if (isIgnorableLine(line)) {
                    lineIndex++
                    continue
                }

                // Check if line contains company suffix -> organization
                if (result.organization == null && containsCompanySuffix(line)) {
                    result.organization = line
                    lineIndex++
                    continue
                }

                // Check if line looks like street address (with house number)
                // Street addresses have priority and override previous addressText
                if (STREET_REGEX.find(line) != null) {
                    result.addressText = line
                    lineIndex++
                    continue
                }

                // If no organization yet and line doesn't look like address, assume it's position
                if (result.positionText == null && result.organization == null) {
                    result.positionText = line
                    lineIndex++
                    continue
                }

                // If we have position but no organization yet, this might be organization
                if (result.organization == null) {
                    result.organization = line
                    lineIndex++
                    continue
                }

                // If we have organization but no address, this might be address
                if (result.addressText == null) {
                    result.addressText = line
                    lineIndex++
                    continue
                }

                lineIndex++
            }
        }

        log.debug { "Parsed address: $result" }
        return result
    }

    /**
     * Parses name (with optional title) from a line.
     */
    private fun parseName(line: String, result: ParsedAddressData) {
        var remainingLine = line

        // Check if line contains comma (might separate name from position)
        val commaParts = remainingLine.split(",").map { it.trim() }
        if (commaParts.size == 2) {
            // First part: title + name, second part: position
            remainingLine = commaParts[0]
            result.positionText = commaParts[1]
        }

        // Extract "i. A." or "i.A." prefix (meaning "in Auftrag" / "on behalf of")
        val iARegex = Regex("""^i\.\s*A\.\s*""", RegexOption.IGNORE_CASE)
        val iAMatch = iARegex.find(remainingLine)
        if (iAMatch != null) {
            // Skip "i. A." prefix - it's not part of the name
            remainingLine = remainingLine.substring(iAMatch.value.length).trim()
        }

        // Extract title if present
        for (titlePattern in TITLE_PATTERNS) {
            val titleRegex = Regex("""^($titlePattern)\s*""")
            val match = titleRegex.find(remainingLine)
            if (match != null) {
                result.title = match.groupValues[1]
                remainingLine = remainingLine.substring(match.value.length).trim()
                break
            }
        }

        // Split remaining into first name and last name
        val nameParts = remainingLine.split(Regex("""\s+"""))
        when {
            nameParts.size >= 2 -> {
                result.firstName = nameParts[0]
                result.name = nameParts.drop(1).joinToString(" ")
            }
            nameParts.size == 1 -> {
                result.name = nameParts[0]
            }
        }
    }

    /**
     * Checks if a line contains a company suffix.
     */
    private fun containsCompanySuffix(line: String): Boolean {
        return COMPANY_SUFFIXES.any { suffix ->
            line.contains(Regex("""\b$suffix\b"""))
        }
    }

    /**
     * Checks if a line should be ignored (e.g., logo text, member info, country names).
     */
    private fun isIgnorableLine(line: String): Boolean {
        // Patterns that must match the entire line
        val fullLinePatterns = listOf(
            """(?i)^germany$""",          // Country name
            """(?i)^deutschland$""",      // Country name (German)
            """(?i)^usa$""",              // Country name
            """(?i)^uk$"""                // Country name
        )

        // Patterns that can appear anywhere in the line
        val containsPatterns = listOf(
            """(?i)\blogo\b""",           // Logo text
            """(?i)\bmember\s+of\b""",    // "Member of ..."
            """(?i)\bHRB\s+\d+""",        // Register number
            """(?i)\bAG\s+\w+\s+HRB"""    // Register court
        )

        return fullLinePatterns.any { pattern -> line.matches(Regex(pattern)) } ||
                containsPatterns.any { pattern -> line.contains(Regex(pattern)) }
    }
}
