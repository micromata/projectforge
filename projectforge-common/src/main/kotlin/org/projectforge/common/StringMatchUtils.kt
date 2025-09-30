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

package org.projectforge.common

import kotlin.math.max
import kotlin.math.min

/**
 * Universal string similarity utilities for matching different string formats.
 * Provides normalization, similarity calculation, and flexible matching algorithms.
 *
 * Use cases:
 * - Invoice number matching: "325124610" ↔ "3251246-10 / Az.: IS-0017-10/KSR"
 * - Company name matching: "Microsoft Corp." ↔ "Microsoft Corporation"
 * - Reference code matching: "ABC-123/XYZ" ↔ "abc123xyz"
 */
object StringMatchUtils {

    /**
     * Calculate similarity between two strings using multiple algorithms.
     * Returns a value between 0.0 (no similarity) and 1.0 (identical).
     *
     * Algorithm combines:
     * - Exact normalized match (1.0)
     * - Jaccard similarity of word tokens
     * - Substring containment bonus
     * - Levenshtein distance for similar strings
     *
     * @param minSubstringLength Minimum length for substring extraction (default: 6)
     */
    fun calculateSimilarity(str1: String?, str2: String?, minSubstringLength: Int = 6): Double {
        if (str1.isNullOrBlank() && str2.isNullOrBlank()) return 1.0
        if (str1.isNullOrBlank() || str2.isNullOrBlank()) return 0.0

    val normalized1 = normalizeString(str1)
        val normalized2 = normalizeString(str2)

        // Exact match after normalization
        if (normalized1 == normalized2) return 1.0

        // Extract significant parts for token-based comparison
        val tokens1 = extractSignificantParts(normalized1, minSubstringLength)
        val tokens2 = extractSignificantParts(normalized2, minSubstringLength)

        // Jaccard similarity of tokens
        val jaccardSimilarity = calculateJaccardSimilarity(tokens1, tokens2)

        // Substring containment bonus
        val containmentBonus = calculateContainmentBonus(normalized1, normalized2)

        // Levenshtein similarity for similar-length strings
        val levenshteinSimilarity = if (shouldUseLevenshtein(normalized1, normalized2)) {
            calculateLevenshteinSimilarity(normalized1, normalized2)
        } else 0.0

        // Combine similarities with weights
        return max(
            jaccardSimilarity + containmentBonus * 0.3,
            levenshteinSimilarity
        ).coerceAtMost(1.0)
    }

    /**
     * Normalize a string by removing special characters, whitespace, and converting to lowercase.
     *
     * Examples:
     * - "3251246-10 / Az.: IS-0017-10/KSR" → "32512461010is001710ksr"
     * - "Microsoft Corp." → "microsoftcorp"
     * - "ABC-123/XYZ" → "abc123xyz"
     */
    fun normalizeString(input: String): String {
        return input
            .lowercase()
            .replace(Regex("[^a-z0-9]"), "") // Remove all non-alphanumeric characters
            .trim()
    }

    /**
     * Extract significant parts from a normalized string for token-based comparison.
     * Splits on common boundaries and filters out very short tokens.
     *
     * Examples:
     * - "microsoftcorp" → ["microsoft", "corp"]
     * - "32512461010is001710ksr" → ["3251246", "1010", "is", "0017", "10", "ksr"]
     *
     * @param minSubstringLength Minimum length for substring extraction (default: 6)
     */
    fun extractSignificantParts(normalized: String, minSubstringLength: Int = 6): Set<String> {
        if (normalized.length < 2) return emptySet()

        val parts = mutableSetOf<String>()

        // Add the full string
        parts.add(normalized)

        // Split numeric and alphabetic sequences
        val sequences = normalized.split(Regex("(?<=\\d)(?=\\D)|(?<=\\D)(?=\\d)"))
            .filter { it.length >= 2 } // Filter out single characters
        parts.addAll(sequences)

        // For longer strings, also add overlapping substrings
        val minLength = minSubstringLength.coerceAtLeast(4) // Ensure at least 4
        if (normalized.length >= minLength + 2) {
            for (i in 0..normalized.length - minLength) {
                for (j in i + minLength..min(i + minLength + 4, normalized.length)) {
                    parts.add(normalized.substring(i, j))
                }
            }
        }

        return parts
    }

    /**
     * Calculate Jaccard similarity between two sets of tokens.
     */
    private fun calculateJaccardSimilarity(tokens1: Set<String>, tokens2: Set<String>): Double {
        if (tokens1.isEmpty() && tokens2.isEmpty()) return 1.0
        if (tokens1.isEmpty() || tokens2.isEmpty()) return 0.0

        val intersection = tokens1.intersect(tokens2)
        val union = tokens1.union(tokens2)

        return intersection.size.toDouble() / union.size
    }

    /**
     * Calculate containment bonus when one string is contained in another.
     */
    private fun calculateContainmentBonus(str1: String, str2: String): Double {
        val shorter = if (str1.length <= str2.length) str1 else str2
        val longer = if (str1.length > str2.length) str1 else str2

        return if (longer.contains(shorter) && shorter.length >= 4) {
            shorter.length.toDouble() / longer.length
        } else 0.0
    }

    /**
     * Determine if Levenshtein distance should be used based on string characteristics.
     */
    private fun shouldUseLevenshtein(str1: String, str2: String): Boolean {
        val minLength = min(str1.length, str2.length)
        val maxLength = max(str1.length, str2.length)

        // Use Levenshtein for similar-length strings (within 50% size difference)
        return minLength >= 4 && maxLength <= minLength * 1.5
    }

    /**
     * Calculate similarity based on Levenshtein distance.
     */
    private fun calculateLevenshteinSimilarity(str1: String, str2: String): Double {
        val distance = levenshteinDistance(str1, str2)
        val maxLength = max(str1.length, str2.length)

        return if (maxLength == 0) 1.0 else 1.0 - (distance.toDouble() / maxLength)
    }

    /**
     * Calculate Levenshtein distance between two strings.
     */
    private fun levenshteinDistance(str1: String, str2: String): Int {
        val len1 = str1.length
        val len2 = str2.length

        val dp = Array(len1 + 1) { IntArray(len2 + 1) }

        for (i in 0..len1) dp[i][0] = i
        for (j in 0..len2) dp[0][j] = j

        for (i in 1..len1) {
            for (j in 1..len2) {
                val cost = if (str1[i - 1] == str2[j - 1]) 0 else 1
                dp[i][j] = minOf(
                    dp[i - 1][j] + 1,      // deletion
                    dp[i][j - 1] + 1,      // insertion
                    dp[i - 1][j - 1] + cost // substitution
                )
            }
        }

        return dp[len1][len2]
    }

    /**
     * Enhanced company name similarity matching with support for legal forms and abbreviations.
     * This method provides domain-specific logic for company names.
     */
    fun calculateCompanySimilarity(company1: String?, company2: String?): Double {
        if (company1.isNullOrBlank() && company2.isNullOrBlank()) return 1.0
        if (company1.isNullOrBlank() || company2.isNullOrBlank()) return 0.0

        // Exact match gets highest score
        if (company1.equals(company2, ignoreCase = true)) {
            return 1.0
        }

        // Extract and normalize words from both company names
        val words1 = normalizeAndExtractCompanyWords(company1)
        val words2 = normalizeAndExtractCompanyWords(company2)

        if (words1.isEmpty() || words2.isEmpty()) {
            // Fallback to simple contains check if normalization fails
            return if (company1.contains(company2, ignoreCase = true) ||
                      company2.contains(company1, ignoreCase = true)) 0.4 else 0.0
        }

        // Calculate word-based similarity
        val similarity = calculateCompanyWordSimilarity(words1, words2)

        return similarity
    }

    /**
     * Normalize company name and extract meaningful words.
     * Removes legal forms, handles abbreviations, and filters stop words.
     */
    private fun normalizeAndExtractCompanyWords(company: String): Set<String> {
        // Common legal forms to ignore
        val legalForms = setOf("gmbh", "ag", "e.k.", "ltd", "inc", "corp", "co", "kg", "ohg",
                              "llc", "plc", "sa", "srl", "bv", "oy", "ab", "as", "aps")

        // Common abbreviations for company types
        val abbreviations = mapOf(
            "f." to "firma",
            "fa." to "firma",
            "co." to "company",
            "corp." to "corporation"
        )

        return company.lowercase()
            .replace(Regex("[^a-zA-Z0-9\\s]"), " ")  // Replace special chars with spaces
            .split(Regex("\\s+"))  // Split on whitespace
            .map { word -> abbreviations[word] ?: word }  // Expand abbreviations
            .filter { word ->
                word.length > 1 &&  // Skip single characters
                !legalForms.contains(word)  // Skip legal forms
            }
            .toSet()
    }

    /**
     * Calculate similarity between two sets of company words using Jaccard similarity.
     * Also considers partial word matches for better flexibility.
     */
    private fun calculateCompanyWordSimilarity(words1: Set<String>, words2: Set<String>): Double {
        if (words1.isEmpty() && words2.isEmpty()) return 1.0
        if (words1.isEmpty() || words2.isEmpty()) return 0.0

        // Exact word matches (Jaccard similarity)
        val intersection = words1.intersect(words2)
        val union = words1.union(words2)
        val jaccardSimilarity = intersection.size.toDouble() / union.size

        // Bonus for partial word matches (e.g., "microsoft" matches "microsystems")
        var partialMatches = 0
        val totalComparisons = words1.size * words2.size

        words1.forEach { word1 ->
            words2.forEach { word2 ->
                if (word1.contains(word2) || word2.contains(word1)) {
                    partialMatches++
                }
            }
        }

        val partialSimilarity = if (totalComparisons > 0) {
            partialMatches.toDouble() / totalComparisons * 0.3  // Weight partial matches lower
        } else 0.0

        // Combine Jaccard similarity with partial match bonus
        return jaccardSimilarity + partialSimilarity
    }
}
