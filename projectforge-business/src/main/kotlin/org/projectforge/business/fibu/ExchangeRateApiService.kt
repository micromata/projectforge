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

package org.projectforge.business.fibu

import com.fasterxml.jackson.databind.JsonNode
import com.fasterxml.jackson.databind.ObjectMapper
import io.github.oshai.kotlinlogging.KotlinLogging
import org.projectforge.common.logging.warn
import org.springframework.stereotype.Service
import java.math.BigDecimal
import java.net.URI
import java.net.http.HttpClient
import java.net.http.HttpRequest
import java.net.http.HttpResponse
import java.time.Duration

private val log = KotlinLogging.logger {}

/**
 * Service for fetching current exchange rates from external API.
 * Uses Frankfurter API v2 (https://frankfurter.dev/) - free, open source, no API key required.
 *
 * @author Kai Reinhard
 */
@Service
class ExchangeRateApiService {
    private val httpClient: HttpClient = HttpClient.newBuilder()
        .connectTimeout(Duration.ofSeconds(5))
        // The API already moved its domain once (frankfurter.app -> frankfurter.dev, answered with 301).
        .followRedirects(HttpClient.Redirect.NORMAL)
        .build()

    private val objectMapper = ObjectMapper()

    /**
     * Fetches the current exchange rate from external API.
     * @param sourceCurrency Source currency code (e.g. "USD")
     * @param targetCurrency Target currency code (e.g. "EUR")
     * @return Current exchange rate or null if unavailable
     */
    fun fetchCurrentRate(sourceCurrency: String, targetCurrency: String): BigDecimal? {
        return fetchRateForDate(sourceCurrency, targetCurrency, null)
    }

    /**
     * Fetches both direction exchange rates from external API.
     * @param sourceCurrency Source currency code (e.g. "USD")
     * @param targetCurrency Target currency code (e.g. "EUR")
     * @param date The date for which to fetch the rate (null = today/latest)
     * @return Pair of (conversionRate, inverseConversionRate) fetched from API
     */
    fun fetchBothRates(
        sourceCurrency: String,
        targetCurrency: String,
        date: java.time.LocalDate?
    ): Pair<BigDecimal?, BigDecimal?> {
        // Fetch both directions from API (not calculated, as actual rates may differ due to spreads/fees)
        val rate = fetchRateForDate(sourceCurrency, targetCurrency, date)
        val inverseRate = fetchRateForDate(targetCurrency, sourceCurrency, date)
        return Pair(rate, inverseRate)
    }

    /**
     * Fetches the exchange rate for a specific date from external API.
     * @param sourceCurrency Source currency code (e.g. "USD")
     * @param targetCurrency Target currency code (e.g. "EUR")
     * @param date The date for which to fetch the rate (null = today/latest)
     * @return Exchange rate for the given date or null if unavailable
     */
    fun fetchRateForDate(sourceCurrency: String, targetCurrency: String, date: java.time.LocalDate?): BigDecimal? {
        if (sourceCurrency.isBlank() || targetCurrency.isBlank()) {
            log.warn { "Source or target currency is blank" }
            return null
        }

        // If same currency, return 1.0
        if (sourceCurrency.uppercase() == targetCurrency.uppercase()) {
            return BigDecimal.ONE
        }

        val dateString = date?.toString() ?: "latest"
        return try {
            val url = buildUrl(sourceCurrency.uppercase(), targetCurrency.uppercase(), date)
            val request = HttpRequest.newBuilder()
                .uri(URI.create(url))
                .timeout(Duration.ofSeconds(5))
                .GET()
                .build()

            val response = httpClient.send(request, HttpResponse.BodyHandlers.ofString())

            if (response.statusCode() == 200) {
                parseRate(response.body(), targetCurrency.uppercase())
            } else {
                log.warn(ExchangeRateLogEvents.UNEXPECTED_RESPONSE) {
                    "Exchange rate API returned status ${response.statusCode()} for $sourceCurrency -> $targetCurrency on $dateString ($url)"
                }
                null
            }
        } catch (ex: Exception) {
            log.warn(ExchangeRateLogEvents.FETCH_FAILED, ex) { "Failed to fetch exchange rate for $sourceCurrency -> $targetCurrency on $dateString" }
            null
        }
    }

    internal fun buildUrl(sourceCurrency: String, targetCurrency: String, date: java.time.LocalDate?): String {
        val url = "$BASE_URL?base=$sourceCurrency&quotes=$targetCurrency"
        return if (date != null) "$url&date=$date" else url
    }

    /**
     * Parses the JSON response from Frankfurter API v2.
     * Example response: [{"date":"2025-10-03","base":"USD","quote":"EUR","rate":0.85}]
     */
    internal fun parseRate(jsonResponse: String, targetCurrency: String): BigDecimal? {
        return try {
            val root: JsonNode = objectMapper.readTree(jsonResponse)
            val rate = root.takeIf { it.isArray }
                ?.firstOrNull { it.get("quote")?.asText() == targetCurrency }
                ?.get("rate")
                ?.takeIf { it.isNumber }
            if (rate != null) {
                rate.decimalValue()
            } else {
                log.warn(ExchangeRateLogEvents.UNEXPECTED_RESPONSE) { "No rate found for $targetCurrency in API response: $jsonResponse" }
                null
            }
        } catch (ex: Exception) {
            log.warn(ExchangeRateLogEvents.UNEXPECTED_RESPONSE, ex) { "Failed to parse exchange rate API response" }
            null
        }
    }

    companion object {
        internal const val BASE_URL = "https://api.frankfurter.dev/v2/rates"
    }
}
