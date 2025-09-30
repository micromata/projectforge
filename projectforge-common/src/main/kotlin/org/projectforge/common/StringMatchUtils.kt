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
     * - Levenshtein distance for similar strings (typos)
     * - Substring containment for partial matches
     * - Jaccard similarity as fallback (weighted lower to avoid false positives)
     *
     * @param minSubstringLength Minimum length for substring extraction (default: 8 for invoice numbers)
     */
    fun calculateSimilarity(str1: String?, str2: String?, minSubstringLength: Int = 8): Double {
        if (str1.isNullOrBlank() && str2.isNullOrBlank()) return 1.0
        if (str1.isNullOrBlank() || str2.isNullOrBlank()) return 0.0

        val normalized1 = normalizeString(str1)
        val normalized2 = normalizeString(str2)

        // Exact match after normalization - highest priority
        if (normalized1 == normalized2) return 1.0

        // Priority 1: Levenshtein for typo detection (similar length strings)
        val levenshteinSimilarity = if (shouldUseLevenshtein(normalized1, normalized2)) {
            calculateLevenshteinSimilarity(normalized1, normalized2)
        } else 0.0

        // Priority 2: Substring containment for partial matches (e.g., "325124610" in "3251246-10 / Az...")
        val containmentBonus = calculateContainmentBonus(normalized1, normalized2)

        // If we have strong Levenshtein or containment, use that
        if (levenshteinSimilarity >= 0.8 || containmentBonus >= 0.5) {
            return max(levenshteinSimilarity, containmentBonus).coerceAtMost(1.0)
        }

        // Priority 3: Token-based comparison only for longer substrings (avoid single digit matches)
        val tokens1 = extractSignificantParts(normalized1, minSubstringLength)
        val tokens2 = extractSignificantParts(normalized2, minSubstringLength)

        // Only use Jaccard if we have meaningful tokens (not just single digits)
        val jaccardSimilarity = if (tokens1.isNotEmpty() && tokens2.isNotEmpty()) {
            calculateJaccardSimilarity(tokens1, tokens2)
        } else 0.0

        // Weight Jaccard lower to prevent false positives from short common substrings
        return max(
            levenshteinSimilarity,
            max(containmentBonus, jaccardSimilarity * 0.5)
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
     * For invoice numbers, only extracts meaningful long substrings to avoid false positives.
     *
     * Examples:
     * - "microsoftcorp" → ["microsoftcorp", "microsoft", "corp"]
     * - "32512461010" → ["32512461010", "3251246", "1010"] (only longer sequences)
     *
     * @param minSubstringLength Minimum length for substring extraction (default: 8 for invoice numbers)
     */
    fun extractSignificantParts(normalized: String, minSubstringLength: Int = 8): Set<String> {
        if (normalized.length < minSubstringLength) return emptySet()

        val parts = mutableSetOf<String>()

        // Always add the full string
        parts.add(normalized)

        // Split numeric and alphabetic sequences, but only keep longer ones
        val sequences = normalized.split(Regex("(?<=\\d)(?=\\D)|(?<=\\D)(?=\\d)"))
            .filter { it.length >= minSubstringLength } // Only significant length tokens
        parts.addAll(sequences)

        // For invoice numbers: extract substrings only if they're at least 50% of the full length
        // This avoids matching on short common prefixes like "202505"
        val minMeaningfulLength = max(minSubstringLength, normalized.length / 2)
        if (normalized.length >= minMeaningfulLength + 2) {
            for (length in minMeaningfulLength..normalized.length) {
                for (i in 0..normalized.length - length) {
                    parts.add(normalized.substring(i, i + length))
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
     * Returns high similarity when the shorter string is fully contained in the longer one.
     */
    private fun calculateContainmentBonus(str1: String, str2: String): Double {
        val shorter = if (str1.length <= str2.length) str1 else str2
        val longer = if (str1.length > str2.length) str1 else str2

        return if (longer.contains(shorter) && shorter.length >= 4) {
            // If shorter string is substantial, give high similarity
            // This handles cases like "325124610" (9 chars) in "32512461010is001710ksr" (22 chars)
            // Ratio: 9/22 = 0.409 → should be high similarity as it's the core invoice number
            val ratio = shorter.length.toDouble() / longer.length
            if (shorter.length >= 8) {
                // For longer invoice numbers (8+ chars), full containment means strong match
                0.9
            } else if (ratio >= 0.5) {
                0.9 // Very high containment ratio
            } else if (ratio >= 0.25) {
                0.8 // Still good containment for significant substrings
            } else {
                ratio // Lower similarity for small containment
            }
        } else 0.0
    }

    /**
     * Determine if Levenshtein distance should be used based on string characteristics.
     * Only use for very similar length strings to detect typos, not for completely different strings.
     */
    private fun shouldUseLevenshtein(str1: String, str2: String): Boolean {
        val minLength = min(str1.length, str2.length)
        val maxLength = max(str1.length, str2.length)

        // Use Levenshtein only for very similar-length strings (within 20% size difference)
        // This detects typos like "119977" vs "119978", but not different numbers like "119977" vs "17182991"
        return minLength >= 4 && maxLength <= minLength * 1.2
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
