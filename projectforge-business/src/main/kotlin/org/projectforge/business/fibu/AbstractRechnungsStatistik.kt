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

package org.projectforge.business.fibu

import mu.KotlinLogging
import org.projectforge.business.utils.CurrencyFormatter
import org.projectforge.common.extensions.formatForUser
import org.projectforge.framework.time.PFDay.Companion.fromOrNow
import org.projectforge.framework.utils.MarkdownBuilder
import org.projectforge.framework.utils.NumberHelper.add
import org.projectforge.statistics.IntAggregatedValues
import java.io.Serializable
import java.math.BigDecimal

private val log = KotlinLogging.logger {}

abstract class AbstractRechnungsStatistik<T : AbstractRechnungDO?> : Serializable {
    var brutto: BigDecimal
        protected set
    var bruttoMitSkonto: BigDecimal
        protected set
    var netto: BigDecimal
        protected set
    var gezahlt: BigDecimal
        protected set
    var offen: BigDecimal
        protected set
    var ueberfaellig: BigDecimal
        protected set

    /**
     * Fehlbeträge, die der Kunde weniger überwiesen hat und die akzeptiert wurden, d. h. die Rechnung gilt als bezahlt.
     */
    var skonto: BigDecimal
        protected set
    protected var zahlungsZielSum: Long = 0
    protected var tatsaechlichesZahlungsZiel = IntAggregatedValues()
    var counterBezahlt: Int
        protected set
    var counter: Int
        protected set

    /**
     * Set of invoice references with currency conversion warnings.
     * Format: "invoiceRef (date): fromCurrency → toCurrency"
     */
    var currencyConversionWarnings: MutableSet<String>
        protected set

    init {
        skonto = BigDecimal.ZERO
        ueberfaellig = skonto
        offen = ueberfaellig
        gezahlt = offen
        netto = gezahlt
        brutto = netto
        bruttoMitSkonto = netto
        counterBezahlt = 0
        counter = counterBezahlt
        currencyConversionWarnings = mutableSetOf()
    }

    fun add(rechnung: T) {
        val rechnungInfo = rechnungCache.getRechnungInfo(rechnung)
        if (rechnung == null || rechnungInfo == null) {
            log.warn { "RechnungInfo not found for rechnungId=${rechnung?.id}." }
            return
        }

        // Get system default currency
        val systemCurrency = configurationService.currency ?: "EUR"
        val rechnungCurrency = rechnung.currency ?: systemCurrency
        val validAtDate = rechnung.datum // Use invoice date for conversion

        // Convert amounts to system currency if needed
        val netto = convertToSystemCurrency(rechnung, rechnungInfo.netSum, rechnungCurrency, systemCurrency, validAtDate) // Das dauert
        val brutto = convertToSystemCurrency(rechnung, rechnungInfo.grossSum, rechnungCurrency, systemCurrency, validAtDate)
        val bruttoMitSkonto = convertToSystemCurrency(rechnung, rechnungInfo.grossSumWithDiscount, rechnungCurrency, systemCurrency, validAtDate)
        val gezahlt = convertToSystemCurrency(rechnung, rechnungInfo.zahlBetrag, rechnungCurrency, systemCurrency, validAtDate)

        this.netto = add(this.netto, netto)
        this.brutto = add(this.brutto, brutto)
        this.bruttoMitSkonto = add(this.bruttoMitSkonto, bruttoMitSkonto)
        if (gezahlt != null) {
            this.gezahlt = add(this.gezahlt, gezahlt)
            if (gezahlt.compareTo(brutto!!) < 0) {
                skonto = add(skonto, brutto.subtract(gezahlt))
            }
        } else {
            offen = add(offen, brutto)
            if (rechnungInfo.isUeberfaellig) {
                ueberfaellig = add(ueberfaellig, brutto)
            }
        }
        val datum = fromOrNow(rechnung.datum)
        val faelligDatum = fromOrNow(rechnung.faelligkeit)
        zahlungsZielSum += datum.daysBetween(faelligDatum)
        if (rechnung.bezahlDatum != null && brutto != null) {
            val bezahlDatum = fromOrNow(rechnung.bezahlDatum)
            tatsaechlichesZahlungsZiel.add(datum.daysBetween(bezahlDatum).toInt(), brutto.toInt())
            counterBezahlt++
        }
        counter++
    }

    /**
     * Converts an amount to system currency if the source currency differs.
     * Tracks conversion failures for display to user.
     * Returns the original amount if currencies are the same or conversion fails.
     *
     * @param rechnung The invoice for error tracking
     * @param amount Amount to convert
     * @param fromCurrency Source currency (e.g. "USD")
     * @param toCurrency Target currency (e.g. "EUR")
     * @param validAtDate Date for exchange rate lookup
     * @return Converted amount, or original amount if conversion not possible
     */
    private fun convertToSystemCurrency(
        rechnung: T,
        amount: BigDecimal?,
        fromCurrency: String,
        toCurrency: String,
        validAtDate: java.time.LocalDate?
    ): BigDecimal? {
        amount ?: return null

        // No conversion needed if currencies are the same
        if (fromCurrency.uppercase() == toCurrency.uppercase()) {
            return amount
        }

        // Attempt conversion
        val converted = currencyConversionService.convert(
            amount = amount,
            sourceCurrency = fromCurrency,
            targetCurrency = toCurrency,
            validAtDate = validAtDate ?: java.time.LocalDate.now(),
            useFallbackToOldestRate = true // Use fallback for historical invoices
        )

        if (converted == null) {
            // Track warning for display
            val invoiceRef = "${rechnung?.displayName} (${rechnung?.datum?.formatForUser() ?: "?"})"
            currencyConversionWarnings.add("$invoiceRef: $fromCurrency → $toCurrency")

            log.warn { "Could not convert $amount $fromCurrency to $toCurrency for date $validAtDate. Using original amount." }
            return amount
        }

        return converted
    }

    val zahlungszielAverage: Int
        get() = if (counter == 0) {
            0
        } else (zahlungsZielSum / counter).toInt()
    val asMarkdown: String
        get() {
            val md = MarkdownBuilder()
            md.appendPipedValue("fibu.common.brutto", CurrencyFormatter.format(brutto))
            md.appendPipedValue("fibu.common.netto", CurrencyFormatter.format(netto))
            if (bruttoMitSkonto.compareTo(brutto) != 0) {
                md.appendPipedValue("fibu.rechnung.mitSkonto", CurrencyFormatter.format(bruttoMitSkonto))
            }
            md.appendPipedValue("fibu.rechnung.offen", CurrencyFormatter.format(offen), MarkdownBuilder.Color.BLUE)
            md.appendPipedValue(
                "fibu.rechnung.filter.ueberfaellig",
                CurrencyFormatter.format(ueberfaellig),
                MarkdownBuilder.Color.RED
            )
            md.appendPipedValue("fibu.rechnung.skonto", CurrencyFormatter.format(skonto))
            md.appendPipedValue("fibu.rechnung.zahlungsZiel", "$zahlungszielAverage")
            md.appendPipedValue("fibu.rechnung.zahlungsZiel.actual", "Ø$tatsaechlichesZahlungzielAverage")
            return md.toString()
        }

    val tatsaechlichesZahlungzielAverage: Int
        get() = tatsaechlichesZahlungsZiel.weightedAverage

    /**
     * Returns true if there were currency conversion warnings during statistics calculation.
     */
    val hasCurrencyConversionWarnings: Boolean
        get() = currencyConversionWarnings.isNotEmpty()

    /**
     * Returns formatted warning message list for Wicket display.
     * @return List of warning strings, one per invoice
     */
    val currencyConversionWarningsList: List<String>
        get() = currencyConversionWarnings.toList()

    companion object {
        private const val serialVersionUID = 3695426728243488756L
        lateinit var rechnungCache: RechnungCache
            internal set
        lateinit var currencyConversionService: CurrencyConversionService
            internal set
        lateinit var configurationService: org.projectforge.business.configuration.ConfigurationService
            internal set
    }
}
