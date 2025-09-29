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

package org.projectforge.rest.fibu.importer

import mu.KotlinLogging
import org.projectforge.business.fibu.EingangsrechnungDO
import org.projectforge.business.fibu.PaymentType
import org.projectforge.rest.dto.BaseDTO
import org.projectforge.rest.dto.Konto
import org.projectforge.rest.dto.Kost1
import org.projectforge.rest.dto.Kost2
import org.projectforge.rest.importer.ImportPairEntry
import java.math.BigDecimal
import java.time.LocalDate
import kotlin.reflect.KProperty

private val log = KotlinLogging.logger {}

/**
 * Data Transfer Object for importing incoming invoice positions.
 * Datev (Liefernantenrechnung, alle Positionen) Excel format (Blatt Blatt 1 - lieferantenrechnungen):
 * * Periode (01.05.2025-31.05.2025)
 * * Betrag (brutto)
 * * Währung (EUR, USD, ...)
 * * Datum (dd.MM.)
 * * RENR (Rechnungs-Nr.)
 * * LieferantName
 * * LieferantKonto
 * * Fällig_am
 * * gezahlt_am
 * * Ware/Leistung (Text)
 * * Steuer%
 * * KOST1
 * * KOST2
 * export (nicht alle Positionen, nur Summe), Blatt 1 - export_final:
 * * Geschäftspartner-Name
 * * Geschäftspartner-Konto
 * * Rechnungsbetrag (brutto)
 * * WKZ (Währung)
 * * Rechnungs-Nr.
 * * Rechnungsdatum
 * * Bezahlt (ja oder leeres Feld)
 * * BezahltAm
 * * Skonto-Betrag 1
 * * Fällig mit Skonto 1
 * * Skonto 1 in %
 * * Fällig ohne Skonto
 * * IBAN
 * * BIC
 * * Belegtyp (Ueberweisungen, Kreditkartenbelege, Lastschrift, ...)
 */
class EingangsrechnungPosImportDTO(
    var kreditor: String? = null, // LieferantName,
    var konto: Konto? = null, // LieferantKonto
    var referenz: String? = null, // RENR, Rechnungs-Nr.
    var betreff: String? = null, // Ware/Leistung
    var datum: LocalDate? = null,
    var faelligkeit: LocalDate? = null,
    var bezahlDatum: LocalDate? = null,
    var taxRate: BigDecimal? = null,
    var grossSum: BigDecimal? = null,
    var currency: String? = null, // Währung (EUR, USD, ...)
    var zahlBetrag: BigDecimal? = null,
    var discountMaturity: LocalDate? = null, // Fällig mit Skonto 1
    var discountPercent: BigDecimal? = null, // Skonto 1 in %
    var iban: String? = null,
    var bic: String? = null,
    var receiver: String? = null,
    var paymentType: PaymentType? = null,
    var customernr: String? = null,
    var bemerkung: String? = null,
    var kost1: Kost1? = null,
    var kost2: Kost2? = null,

    /**
     * Sequential position number within an invoice (RENR).
     * Starts with 1 and is assigned in the order of reading.
     */
    var positionNummer: Int? = null,
) : BaseDTO<EingangsrechnungDO>(), ImportPairEntry.Modified<EingangsrechnungPosImportDTO> {

    override val properties: Array<KProperty<*>>
        get() = arrayOf(
            EingangsrechnungDO::kreditor,
            EingangsrechnungDO::referenz,
            EingangsrechnungDO::betreff,
            EingangsrechnungDO::datum,
            EingangsrechnungDO::faelligkeit,
            EingangsrechnungDO::bezahlDatum,
            EingangsrechnungDO::zahlBetrag,
            EingangsrechnungDO::iban,
            EingangsrechnungDO::bic,
            EingangsrechnungDO::receiver,
            EingangsrechnungDO::paymentType,
            EingangsrechnungDO::customernr,
            EingangsrechnungDO::bemerkung
        )

    override fun copyFrom(src: EingangsrechnungDO) {
        super.copyFrom(src)
        this.kreditor = src.kreditor
        this.referenz = src.referenz
        this.betreff = src.betreff
        this.datum = src.datum
        this.faelligkeit = src.faelligkeit
        this.bezahlDatum = src.bezahlDatum
        this.zahlBetrag = src.zahlBetrag
        this.iban = src.iban
        this.bic = src.bic
        this.receiver = src.receiver
        this.paymentType = src.paymentType
        this.customernr = src.customernr
        this.bemerkung = src.bemerkung
        // this.kost1 = src.kost1
        // this.kost2 = src.kost2
    }

    override fun copyTo(obj: EingangsrechnungDO) {
        if (this.id != null) obj.id = this.id
        obj.kreditor = this.kreditor
        obj.referenz = this.referenz
        obj.betreff = this.betreff
        obj.datum = this.datum
        obj.faelligkeit = this.faelligkeit
        obj.bezahlDatum = this.bezahlDatum
        obj.zahlBetrag = this.zahlBetrag
        obj.iban = this.iban
        obj.bic = this.bic
        obj.receiver = this.receiver
        obj.paymentType = this.paymentType
        obj.customernr = this.customernr
        obj.bemerkung = this.bemerkung
    }

    /**
     * Calculate matching score with an existing EingangsrechnungDO for import reconciliation.
     * Higher score means better match. Score 0 means no match.
     *
     * Matching criteria (in order of importance):
     * - Same invoice number (referenz): +50 points
     * - Same creditor (kreditor): +30 points
     * - Same date (datum): +20 points
     * - Partial creditor match: +10 points
     * - Same amount (grossSum): +10 points
     * - Date within 7 days: +5 points
     * - Date within 30 days: +2 points
     */
    fun matchScore(dbInvoice: EingangsrechnungDO): Int {
        var score = 0
        score += calculateReferenzMatchScore(dbInvoice)
        score += calculateKreditorMatchScore(dbInvoice)
        score += calculateDateMatchScore(dbInvoice)
        score += calculateAmountMatchScore(dbInvoice)
        return score
    }

    /**
     * Header-only matching score for invoice reconciliation.
     * Focuses on header fields and ignores position-specific data.
     * Scoring:
     * - Invoice number exact match: +50 points
     * - Creditor exact match: +30 points
     * - Creditor partial match: +10 points
     * - Date exact match: +20 points
     * - Date within 7 days: +5 points
     * - Date within 30 days: +2 points
     * - Amount exact match: +10 points
     */
    fun headerMatchScore(dbInvoice: EingangsrechnungDO): Int {
        var score = 0
        score += calculateReferenzMatchScore(dbInvoice)
        score += calculateKreditorMatchScore(dbInvoice)
        score += calculateDateMatchScore(dbInvoice)
        score += calculateAmountMatchScore(dbInvoice, logErrors = true)
        return score
    }

    /**
     * Calculate referenz (invoice number) match score.
     */
    private fun calculateReferenzMatchScore(dbInvoice: EingangsrechnungDO): Int {
        val thisReferenz = this.referenz
        val dbReferenz = dbInvoice.referenz
        return if (!thisReferenz.isNullOrBlank() && !dbReferenz.isNullOrBlank()) {
            if (thisReferenz.equals(dbReferenz, ignoreCase = true)) 50 else 0
        } else 0
    }

    /**
     * Calculate kreditor (creditor) match score using enhanced similarity matching.
     */
    private fun calculateKreditorMatchScore(dbInvoice: EingangsrechnungDO): Int {
        val thisKreditor = this.kreditor
        val dbKreditor = dbInvoice.kreditor
        return if (!thisKreditor.isNullOrBlank() && !dbKreditor.isNullOrBlank()) {
            calculateKreditorSimilarity(thisKreditor, dbKreditor)
        } else 0
    }

    /**
     * Calculate date match score based on exactness and proximity.
     */
    private fun calculateDateMatchScore(dbInvoice: EingangsrechnungDO): Int {
        return if (datum != null && dbInvoice.datum != null) {
            if (datum == dbInvoice.datum) {
                20
            } else {
                val daysDiff = kotlin.math.abs(java.time.temporal.ChronoUnit.DAYS.between(datum, dbInvoice.datum))
                when {
                    daysDiff <= 7 -> 5
                    daysDiff <= 30 -> 2
                    else -> 0
                }
            }
        } else 0
    }

    /**
     * Calculate amount match score by comparing gross sums.
     */
    private fun calculateAmountMatchScore(dbInvoice: EingangsrechnungDO, logErrors: Boolean = false): Int {
        val thisGrossSum = this.grossSum
        return if (thisGrossSum != null) {
            try {
                // Try to calculate and get grossSum, skip if it fails
                org.projectforge.business.fibu.RechnungCalculator.calculate(dbInvoice, useCaches = false)
                val dbGrossSum = dbInvoice.info.grossSum
                if (thisGrossSum.compareTo(dbGrossSum) == 0) 10 else 0
            } catch (e: Exception) {
                // Info calculation failed, skip amount comparison
                if (logErrors) {
                    log.error(e.message, e)
                }
                0
            }
        } else 0
    }

    /**
     * Enhanced kreditor similarity matching with support for:
     * - Company name variations ("Firma ACME GmbH" vs "F. ACME")
     * - Legal form differences ("GmbH", "AG", "Inc", etc.)
     * - Abbreviations ("F." -> "Firma")
     * - Word-based similarity scoring
     */
    private fun calculateKreditorSimilarity(kreditor1: String, kreditor2: String): Int {
        // Exact match gets highest score
        if (kreditor1.equals(kreditor2, ignoreCase = true)) {
            return 30
        }

        // Extract and normalize words from both company names
        val words1 = normalizeAndExtractWords(kreditor1)
        val words2 = normalizeAndExtractWords(kreditor2)

        if (words1.isEmpty() || words2.isEmpty()) {
            // Fallback to simple contains check if normalization fails
            return if (kreditor1.contains(kreditor2, ignoreCase = true) ||
                      kreditor2.contains(kreditor1, ignoreCase = true)) 10 else 0
        }

        // Calculate word-based similarity
        val similarity = calculateWordSimilarity(words1, words2)

        // Return score based on similarity percentage
        return when {
            similarity >= 0.8 -> 25  // High similarity: "Firma ACME GmbH" vs "F. ACME"
            similarity >= 0.6 -> 20  // Medium similarity: "Microsoft Corp." vs "Microsoft Corporation"
            similarity >= 0.4 -> 15  // Low similarity: still worth considering
            kreditor1.contains(kreditor2, ignoreCase = true) ||
            kreditor2.contains(kreditor1, ignoreCase = true) -> 10  // Fallback contains match
            else -> 0
        }
    }

    /**
     * Normalize company name and extract meaningful words.
     * Removes legal forms, handles abbreviations, and filters stop words.
     */
    private fun normalizeAndExtractWords(company: String): Set<String> {
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
     * Calculate similarity between two sets of words using Jaccard similarity.
     * Also considers partial word matches for better flexibility.
     */
    private fun calculateWordSimilarity(words1: Set<String>, words2: Set<String>): Double {
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
