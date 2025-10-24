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

package org.projectforge.framework.utils

import org.projectforge.framework.configuration.Configuration
import org.projectforge.framework.configuration.ConfigurationParam

/**
 * Utility functions for phone number normalization and formatting.
 *
 * @author Kai Reinhard (k.reinhard@micromata.de)
 */
object PhoneNumberUtils {

    // Common country codes (1-3 digits)
    private val KNOWN_COUNTRY_CODES = setOf(
        "1",   // USA, Canada
        "7",   // Russia, Kazakhstan
        "20", "27",  // Egypt, South Africa
        "30", "31", "32", "33", "34", "36", "39", "40", "41", "43", "44", "45", "46", "47", "48", "49", // Europe
        "51", "52", "53", "54", "55", "56", "57", "58", // South America
        "60", "61", "62", "63", "64", "65", "66", // Asia/Oceania
        "81", "82", "84", "86", "90", "91", "92", "93", "94", "95", "98", // Asia
        "212", "213", "216", "218", "220", "221", "222", "223", "224", "225", "226", "227", "228", "229", // Africa
        "230", "231", "232", "233", "234", "235", "236", "237", "238", "239", "240", "241", "242", "243", "244", "245", "246", "247", "248", "249", // Africa
        "250", "251", "252", "253", "254", "255", "256", "257", "258", "260", "261", "262", "263", "264", "265", "266", "267", "268", "269", // Africa
        "290", "291", "297", "298", "299", // Atlantic
        "350", "351", "352", "353", "354", "355", "356", "357", "358", "359", // Europe
        "370", "371", "372", "373", "374", "375", "376", "377", "378", "380", "381", "382", "383", "385", "386", "387", "389", // Europe
        "420", "421", "423", // Europe
        "500", "501", "502", "503", "504", "505", "506", "507", "508", "509", "590", "591", "592", "593", "594", "595", "596", "597", "598", "599", // Americas
        "670", "671", "672", "673", "674", "675", "676", "677", "678", "679", "680", "681", "682", "683", "684", "685", "686", "687", "688", "689", "690", "691", "692", // Pacific
        "850", "852", "853", "855", "856", "880", "886", // Asia
        "960", "961", "962", "963", "964", "965", "966", "967", "968", "970", "971", "972", "973", "974", "975", "976", "977", "992", "993", "994", "995", "996", "998" // Asia/Middle East
    )

    /**
     * Normalizes a phone number to a standardized, readable format.
     *
     * Format: +[Country] [Area] [Number][-Extension]
     *
     * Examples:
     * - "0561 316793-0" → "+49 561 316793-0"
     * - "+49 (0) 561 / 316793-0" → "+49 561 316793-0"
     * - "0170 1234567" → "+49 170 1234567"
     * - "+1 415 555-1234" → "+1 415 5551234"
     *
     * @param phoneNumber The phone number to normalize (may contain spaces, slashes, parentheses, etc.)
     * @param defaultCountryPrefix The default country prefix to use if none is present (default: from config or "+49")
     * @return Normalized phone number in format "+CC AAA NNNNNNN[-EXT]" or null if input is null/empty
     */
    fun normalizePhoneNumber(
        phoneNumber: String?,
        defaultCountryPrefix: String = getDefaultCountryPrefix()
    ): String? {
        if (phoneNumber.isNullOrBlank()) {
            return null
        }

        var cleaned = phoneNumber.trim()

        // Remove prefixes like "Tel:", "Phone:", "Fax:", etc.
        cleaned = cleaned.replace(Regex("^(?:Tel\\.?:|Telefon:|Phone:|Mobil:|Mobile:|Fax:)\\s*", RegexOption.IGNORE_CASE), "")

        // Remove UTF control chars
        cleaned = cleaned.replace("\\p{C}".toRegex(), "")

        // Remove (0) in +49 (0) 123456789
        cleaned = cleaned.replace(Regex("\\(0\\)"), "")

        if (cleaned.isEmpty()) {
            return null
        }

        // Extract country code
        val (countryCode, remainingPart) = extractCountryCode(cleaned, defaultCountryPrefix)

        if (remainingPart.isBlank()) {
            return null
        }

        // Clean up remaining part: remove slashes, dots, parentheses first, then normalize spaces
        var remaining = remainingPart.trim()
            .replace(Regex("[/().]"), "") // Remove slashes, parentheses, dots
            .replace(Regex("\\s+"), " ") // Normalize multiple spaces to single space
            .trim()

        // Check for extension (last hyphen followed by 1-4 digits)
        var extension: String? = null
        val lastHyphenIndex = remaining.lastIndexOf('-')
        if (lastHyphenIndex > 0) {
            val afterHyphen = remaining.substring(lastHyphenIndex + 1).trim()
            // If after hyphen is 1-4 digits, treat as extension
            if (afterHyphen.matches(Regex("\\d{1,4}"))) {
                extension = afterHyphen
                remaining = remaining.substring(0, lastHyphenIndex).trim()
            }
        }

        // Remove any remaining hyphens
        remaining = remaining.replace("-", "")

        if (remaining.isEmpty()) {
            return null
        }

        // Validate that remaining contains at least some digits
        if (!remaining.contains(Regex("\\d"))) {
            return null
        }

        // Build result: +CC remaining[-ext]
        val result = StringBuilder()
        result.append(countryCode)
        result.append(" ")
        result.append(remaining)

        if (extension != null) {
            result.append("-")
            result.append(extension)
        }

        return result.toString()
    }

    /**
     * Compares two phone numbers by normalizing them first.
     *
     * @param phone1 First phone number
     * @param phone2 Second phone number
     * @return true if both numbers are equal after normalization, false otherwise
     */
    fun phoneNumbersMatch(phone1: String?, phone2: String?): Boolean {
        val normalized1 = normalizePhoneNumber(phone1)
        val normalized2 = normalizePhoneNumber(phone2)

        if (normalized1 == null || normalized2 == null) {
            return false
        }

        return normalized1 == normalized2
    }

    private fun getDefaultCountryPrefix(): String {
        return NumberHelper.TEST_COUNTRY_PREFIX_USAGE_IN_TESTCASES_ONLY
            ?: try {
                Configuration.instance.getStringValue(ConfigurationParam.DEFAULT_COUNTRY_PHONE_PREFIX) ?: "+49"
            } catch (e: Exception) {
                "+49" // Fallback if Configuration is not initialized (e.g., in tests)
            }
    }

    /**
     * Extracts the country code from the phone number.
     * Returns pair of (countryCode, remainingDigits)
     */
    private fun extractCountryCode(cleaned: String, defaultCountryPrefix: String): Pair<String, String> {
        // Already has + prefix
        if (cleaned.startsWith("+")) {
            // Try to match known country codes (1-3 digits)
            // Check longest first (3 digits), then 2 digits, then 1 digit
            val digitsAfterPlus = cleaned.substring(1).takeWhile { it.isDigit() }

            if (digitsAfterPlus.length >= 3) {
                val code3 = digitsAfterPlus.substring(0, 3)
                if (code3 in KNOWN_COUNTRY_CODES) {
                    return Pair("+$code3", cleaned.substring(4))
                }
            }

            if (digitsAfterPlus.length >= 2) {
                val code2 = digitsAfterPlus.substring(0, 2)
                if (code2 in KNOWN_COUNTRY_CODES) {
                    return Pair("+$code2", cleaned.substring(3))
                }
            }

            if (digitsAfterPlus.isNotEmpty()) {
                val code1 = digitsAfterPlus.substring(0, 1)
                if (code1 in KNOWN_COUNTRY_CODES) {
                    return Pair("+$code1", cleaned.substring(2))
                }
            }

            // Fallback: use first 2 digits as country code
            if (digitsAfterPlus.length >= 2) {
                return Pair("+" + digitsAfterPlus.substring(0, 2), cleaned.substring(3))
            } else if (digitsAfterPlus.isNotEmpty()) {
                return Pair("+" + digitsAfterPlus.substring(0, 1), cleaned.substring(2))
            }
        }

        // Has 00 prefix (international format)
        if (cleaned.startsWith("00") && cleaned.length > 2) {
            val digitsAfter00 = cleaned.substring(2).takeWhile { it.isDigit() }

            if (digitsAfter00.length >= 3) {
                val code3 = digitsAfter00.substring(0, 3)
                if (code3 in KNOWN_COUNTRY_CODES) {
                    return Pair("+$code3", cleaned.substring(5))
                }
            }

            if (digitsAfter00.length >= 2) {
                val code2 = digitsAfter00.substring(0, 2)
                if (code2 in KNOWN_COUNTRY_CODES) {
                    return Pair("+$code2", cleaned.substring(4))
                }
            }

            if (digitsAfter00.isNotEmpty()) {
                val code1 = digitsAfter00.substring(0, 1)
                if (code1 in KNOWN_COUNTRY_CODES) {
                    return Pair("+$code1", cleaned.substring(3))
                }
            }

            // Fallback: use first 2 digits as country code
            if (digitsAfter00.length >= 2) {
                return Pair("+" + digitsAfter00.substring(0, 2), cleaned.substring(4))
            } else if (digitsAfter00.isNotEmpty()) {
                return Pair("+" + digitsAfter00.substring(0, 1), cleaned.substring(3))
            }
        }

        // Starts with 0 (national format) - add default country prefix
        if (cleaned.startsWith("0")) {
            return Pair(defaultCountryPrefix, cleaned.substring(1))
        }

        // No prefix - assume national format
        return Pair(defaultCountryPrefix, cleaned)
    }

}
