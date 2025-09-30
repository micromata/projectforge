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
import org.projectforge.common.StringMatchUtils
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
     * - Exact invoice number (referenz): +50 points
     * - Similar invoice number (normalized): +35-45 points
     * - Exact creditor (kreditor): +30 points
     * - Similar creditor: +10-25 points
     * - Exact date (datum): +20 points
     * - Same amount (grossSum): +10 points
     * - Date within 7 days: +5 points
     * - Date within 30 days: +2 points
     *
     * Uses StringMatchUtils for advanced similarity matching of invoice numbers and creditors.
     *
     * @param logErrors If true, logs errors when amount calculation fails
     */
    fun matchScore(dbInvoice: EingangsrechnungDO, logErrors: Boolean = false): Int {
        var score = 0
        val referenzScore = calculateReferenzMatchScore(dbInvoice)
        val kreditorScore = calculateKreditorMatchScore(dbInvoice)
        val dateScore = calculateDateMatchScore(dbInvoice)
        val amountScore = calculateAmountMatchScore(dbInvoice, logErrors)

        score += referenzScore
        score += kreditorScore
        score += dateScore
        score += amountScore

        // Log score calculation for debugging (only for significant scores to reduce noise)
        if (score > 20) {
            log.debug {
                val similarity = StringMatchUtils.calculateSimilarity(this.referenz, dbInvoice.referenz)
                val kreditorSimilarity = StringMatchUtils.calculateCompanySimilarity(this.kreditor, dbInvoice.kreditor)
                "MATCH SCORE: Import='${this.referenz}' vs DB='${dbInvoice.referenz}' | " +
                        "ImportKreditor='${this.kreditor}' vs DBKreditor='${dbInvoice.kreditor}' | " +
                        "ImportDate=${this.datum} vs DBDate=${dbInvoice.datum} | " +
                        "ImportAmount=${this.grossSum} vs DBAmount=${try { dbInvoice.ensuredInfo.grossSum } catch(e: Exception) { "ERROR: ${e.message}" }} | " +
                        "Scores: referenz=$referenzScore, kreditor=$kreditorScore, date=$dateScore, amount=$amountScore | " +
                        "ReferenzSimilarity=$similarity, KreditorSimilarity=$kreditorSimilarity | " +
                        "TOTAL=$score"
            }
        }

        return score
    }

    /**
     * Calculate referenz (invoice number) match score.
     * Uses multi-level matching: exact, normalized, and similarity-based.
     */
    private fun calculateReferenzMatchScore(dbInvoice: EingangsrechnungDO): Int {
        val thisReferenz = this.referenz
        val dbReferenz = dbInvoice.referenz
        return if (!thisReferenz.isNullOrBlank() && !dbReferenz.isNullOrBlank()) {
            // Level 1: Exact match (highest score)
            if (thisReferenz.equals(dbReferenz, ignoreCase = true)) {
                50
            } else {
                // Level 2: Use StringMatchUtils for similarity matching
                val similarity = StringMatchUtils.calculateSimilarity(thisReferenz, dbReferenz)
                convertReferenzSimilarityToScore(similarity)
            }
        } else 0
    }

    /**
     * Convert similarity percentage (0.0-1.0) to scoring points for referenz matching.
     */
    private fun convertReferenzSimilarityToScore(similarity: Double): Int {
        return when {
            similarity >= 1.0 -> 45   // Perfect normalized match: "325124610" = "3251246-10"
            similarity >= 0.8 -> 40   // High similarity: significant common parts
            similarity >= 0.6 -> 35   // Medium similarity: "325124610" vs "3251246-10 / Az.: IS-0017-10/KSR"
            similarity >= 0.35 -> 25  // Low similarity: some common elements (lowered from 0.4 to 0.35)
            similarity >= 0.2 -> 15   // Very low similarity: minimal but detectable match
            else -> 0                 // No meaningful similarity
        }
    }

    /**
     * Calculate kreditor (creditor) match score using enhanced similarity matching.
     */
    private fun calculateKreditorMatchScore(dbInvoice: EingangsrechnungDO): Int {
        val thisKreditor = this.kreditor
        val dbKreditor = dbInvoice.kreditor
        return if (!thisKreditor.isNullOrBlank() && !dbKreditor.isNullOrBlank()) {
            // Use StringMatchUtils for company similarity calculation
            val similarity = StringMatchUtils.calculateCompanySimilarity(thisKreditor, dbKreditor)
            convertSimilarityToScore(similarity)
        } else 0
    }

    /**
     * Convert similarity percentage (0.0-1.0) to scoring points for kreditor matching.
     */
    private fun convertSimilarityToScore(similarity: Double): Int {
        return when {
            similarity >= 1.0 -> 30   // Perfect match
            similarity >= 0.8 -> 25   // High similarity: "Firma ACME GmbH" vs "F. ACME"
            similarity >= 0.6 -> 20   // Medium similarity: "Microsoft Corp." vs "Microsoft Corporation"
            similarity >= 0.4 -> 15   // Low similarity: still worth considering
            similarity >= 0.2 -> 10   // Very low similarity: minimal match
            else -> 0                 // No meaningful similarity
        }
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
                val dbGrossSum = dbInvoice.ensuredInfo.grossSum
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
}
