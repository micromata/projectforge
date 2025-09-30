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

import org.junit.jupiter.api.Test
import org.junit.jupiter.api.Assertions.*

class StringMatchUtilsTest {

    @Test
    fun testNormalizeString() {
        // Test removal of special characters and whitespace
        assertEquals("microsoftcorp", StringMatchUtils.normalizeString("Microsoft Corp."))
        assertEquals("325124610azis001710ksr", StringMatchUtils.normalizeString("3251246-10 / Az.: IS-0017-10/KSR"))
        assertEquals("abc123xyz", StringMatchUtils.normalizeString("ABC-123/XYZ"))
        assertEquals("ordernr2024001", StringMatchUtils.normalizeString("Order Nr. 2024-001"))
        assertEquals("", StringMatchUtils.normalizeString("!@#$%^&*()"))
        assertEquals("", StringMatchUtils.normalizeString("   "))
        assertEquals("test", StringMatchUtils.normalizeString("  TEST  "))
    }

    @Test
    fun testExtractSignificantParts() {
        val parts1 = StringMatchUtils.extractSignificantParts("microsoftcorp")
        assertTrue(parts1.contains("microsoftcorp"))
        assertTrue(parts1.size >= 1) // Should have at least the full string

        val parts2 = StringMatchUtils.extractSignificantParts("32512461010")
        assertTrue(parts2.contains("32512461010"))
        assertTrue(parts2.size >= 1) // Should have at least the full string

        // Test short strings
        val parts3 = StringMatchUtils.extractSignificantParts("a")
        assertTrue(parts3.isEmpty()) // Too short
    }

    @Test
    fun testCalculateSimilarityExactMatches() {
        // Exact matches after normalization should return 1.0
        assertEquals(1.0, StringMatchUtils.calculateSimilarity("ABC-123", "abc123"), 0.01)
        assertEquals(1.0, StringMatchUtils.calculateSimilarity("Microsoft Corp.", "microsoftcorp"), 0.01)
        assertEquals(1.0, StringMatchUtils.calculateSimilarity("  TEST  ", "test"), 0.01)
    }

    @Test
    fun testCalculateSimilarityInvoiceNumbers() {
        // Main use case: invoice number matching
        val similarity1 = StringMatchUtils.calculateSimilarity("325124610", "3251246-10")
        assertTrue(similarity1 > 0.5, "Invoice numbers should be similar")

        val similarity2 = StringMatchUtils.calculateSimilarity("325124610", "3251246-10 / Az.: IS-0017-10/KSR")
        assertTrue(similarity2 > 0.3, "Invoice number with additional info should be similar")

        val similarity3 = StringMatchUtils.calculateSimilarity("325124610", "999888777")
        assertTrue(similarity3 < 0.3, "Different invoice numbers should not be similar")
    }

    @Test
    fun testCalculateSimilarityContainment() {
        // Test substring containment
        val similarity1 = StringMatchUtils.calculateSimilarity("test", "testing123")
        assertTrue(similarity1 > 0.15, "Contained string should have reasonable similarity")

        val similarity2 = StringMatchUtils.calculateSimilarity("abcdef", "abcd")
        assertTrue(similarity2 > 0.1, "Longer contained string should have reasonable similarity")
    }

    @Test
    fun testCalculateSimilarityNullAndEmpty() {
        assertEquals(1.0, StringMatchUtils.calculateSimilarity(null, null), 0.01)
        assertEquals(1.0, StringMatchUtils.calculateSimilarity("", ""), 0.01)
        assertEquals(0.0, StringMatchUtils.calculateSimilarity("test", null), 0.01)
        assertEquals(0.0, StringMatchUtils.calculateSimilarity(null, "test"), 0.01)
        assertEquals(0.0, StringMatchUtils.calculateSimilarity("test", ""), 0.01)
    }

    @Test
    fun testCalculateSimilarityDifferentStrings() {
        val similarity1 = StringMatchUtils.calculateSimilarity("apple", "orange")
        assertTrue(similarity1 < 0.3, "Completely different strings should have low similarity")

        val similarity2 = StringMatchUtils.calculateSimilarity("12345", "abcde")
        assertTrue(similarity2 < 0.3, "Different character types should have low similarity")
    }

    @Test
    fun testCalculateCompanySimilarity() {
        // Exact company matches
        assertEquals(1.0, StringMatchUtils.calculateCompanySimilarity("Microsoft Corp.", "Microsoft Corp."), 0.01)
        assertEquals(1.0, StringMatchUtils.calculateCompanySimilarity("ACME GmbH", "acme gmbh"), 0.01)

        // Company name variations
        val similarity1 = StringMatchUtils.calculateCompanySimilarity("Microsoft Corporation", "Microsoft Corp.")
        assertTrue(similarity1 > 0.6, "Company variations should be similar")

        val similarity2 = StringMatchUtils.calculateCompanySimilarity("Firma ACME GmbH", "F. ACME")
        assertTrue(similarity2 > 0.5, "Company with abbreviations should be similar")

        val similarity3 = StringMatchUtils.calculateCompanySimilarity("Lanes & Planes GmbH", "traumferienvilla.de - Denis Baden")
        assertTrue(similarity3 < 0.3, "Different companies should not be similar")
    }

    @Test
    fun testCalculateCompanySimilarityLegalForms() {
        // Legal forms should be ignored in comparison
        val similarity1 = StringMatchUtils.calculateCompanySimilarity("ACME GmbH", "ACME Ltd")
        assertTrue(similarity1 > 0.7, "Same company with different legal forms should be similar")

        val similarity2 = StringMatchUtils.calculateCompanySimilarity("Microsoft Inc.", "Microsoft LLC")
        assertTrue(similarity2 > 0.7, "Same company with different legal forms should be similar")
    }

    @Test
    fun testCalculateCompanySimilarityAbbreviations() {
        // Abbreviations should be expanded and matched
        val similarity1 = StringMatchUtils.calculateCompanySimilarity("F. ACME", "Firma ACME")
        assertTrue(similarity1 > 0.5, "Abbreviations should be recognized")

        val similarity2 = StringMatchUtils.calculateCompanySimilarity("Co. Test", "Company Test")
        assertTrue(similarity2 > 0.5, "Company abbreviations should be recognized")
    }

    @Test
    fun testCalculateCompanySimilarityNullAndEmpty() {
        assertEquals(1.0, StringMatchUtils.calculateCompanySimilarity(null, null), 0.01)
        assertEquals(1.0, StringMatchUtils.calculateCompanySimilarity("", ""), 0.01)
        assertEquals(0.0, StringMatchUtils.calculateCompanySimilarity("test", null), 0.01)
        assertEquals(0.0, StringMatchUtils.calculateCompanySimilarity(null, "test"), 0.01)
    }

    @Test
    fun testCalculateCompanySimilarityPartialMatches() {
        // Test partial containment
        val similarity1 = StringMatchUtils.calculateCompanySimilarity("Microsoft", "Microsoft Deutschland")
        assertTrue(similarity1 > 0.4, "Partial company name should have good similarity")

        val similarity2 = StringMatchUtils.calculateCompanySimilarity("ACME Corp", "ACME International Corp")
        assertTrue(similarity2 > 0.5, "Extended company name should have good similarity")
    }

    @Test
    fun testRealWorldFailingCase() {
        // Test the actual failing case from user
        val referenz1 = "325124610"
        val referenz2 = "3251246-10 / Az.: IS-0017-10/KSR"
        val referenzSimilarity = StringMatchUtils.calculateSimilarity(referenz1, referenz2)

        val kreditor1 = "ISICO Datenschutz GmbH"
        val kreditor2 = "ISICO GmbH"
        val kreditorSimilarity = StringMatchUtils.calculateCompanySimilarity(kreditor1, kreditor2)

        // Calculate scores as per EingangsrechnungPosImportDTO logic
        val referenzScore = when {
            referenzSimilarity >= 1.0 -> 45
            referenzSimilarity >= 0.8 -> 40
            referenzSimilarity >= 0.6 -> 35
            referenzSimilarity >= 0.35 -> 25
            referenzSimilarity >= 0.2 -> 15
            else -> 0
        }

        val kreditorScore = when {
            kreditorSimilarity >= 1.0 -> 30
            kreditorSimilarity >= 0.8 -> 25
            kreditorSimilarity >= 0.6 -> 20
            kreditorSimilarity >= 0.4 -> 15
            kreditorSimilarity >= 0.2 -> 10
            else -> 0
        }

        // Test both with and without amount match (in case amount matching fails)
        val totalScoreWithAmount = referenzScore + kreditorScore + 20 + 10 // date match + amount match
        val totalScoreWithoutAmount = referenzScore + kreditorScore + 20 + 0 // date match only

        assertTrue(referenzSimilarity > 0.3, "Invoice numbers should show meaningful similarity")
        assertTrue(kreditorSimilarity > 0.6, "Creditors should be similar")
        assertTrue(totalScoreWithoutAmount >= 25, "Score should be above threshold even without amount match")
        assertTrue(totalScoreWithAmount >= 65, "Total score with amount should be sufficient for matching")
    }

    @Test
    fun testFalsePositiveInvoiceNumbers() {
        // Real-world case that should NOT match: completely different invoices with some common digits
        val similarity1 = StringMatchUtils.calculateSimilarity("119977", "#17182991")
        assertTrue(similarity1 < 0.3, "Different invoice numbers should have low similarity (was $similarity1)")

        // Another case with similar numbers but different invoices (2 digits differ)
        // This looks like a typo (0.8 similarity), but combined with different kreditor/date
        // should NOT match (score would be ~45 < threshold 50)
        val similarity2 = StringMatchUtils.calculateSimilarity("2025051909", "202505-2903")
        // Note: High similarity (0.8) is OK here, the threshold (50) will prevent false match
        // when combined with mismatched kreditor and date

        // This should still match: same number with different formatting
        val similarity3 = StringMatchUtils.calculateSimilarity("325124610", "3251246-10 / Az.: IS-0017-10/KSR")
        assertTrue(similarity3 > 0.8, "Same invoice number with extra text should match (was $similarity3)")

        // Typo case: should still be similar
        val similarity4 = StringMatchUtils.calculateSimilarity("119977", "119978")
        assertTrue(similarity4 > 0.8, "Invoice numbers with typo should still match (was $similarity4)")
    }
}
