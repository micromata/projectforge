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

package org.projectforge.plugins.liquidityplanning

import org.projectforge.business.fibu.*
import org.projectforge.framework.time.DayHolder
import org.projectforge.framework.time.PFDay.Companion.from
import org.projectforge.framework.time.PFDay.Companion.fromOrNull
import org.projectforge.statistics.IntAggregatedValues
import org.projectforge.web.WicketSupport
import java.io.Serializable
import java.time.LocalDate
import java.util.*

/**
 * @author Kai Reinhard (k.reinhard@micromata.de)
 */
class LiquidityForecast() : Serializable {
    private val entries = mutableListOf<LiquidityEntry>()
    private var liquiEntries = mutableListOf<LiquidityEntry>()

    /**
     * @return the invoices
     */
    var invoices: Collection<RechnungDO>? = null
        private set

    private var invoicesLiquidityEntries = mutableListOf<LiquidityEntry>()

    /**
     * Used for calculating the expected date of payment for future invoices.
     */
    private val aggregatedDebitorInvoicesValuesMap: MutableMap<String, IntAggregatedValues> = HashMap()

    /**
     * Used for calculating the expected date of payment for future invoices.
     */
    private val aggregatedCreditorInvoicesValuesMap: MutableMap<String, IntAggregatedValues> = HashMap()

    /**
     * @return the creditorInvoices
     */
    var creditorInvoices: Collection<EingangsrechnungDO>? = null
        private set
    private var creditorInvoicesLiquidityEntries = mutableListOf<LiquidityEntry>()

    var baseDate: LocalDate? = null
        get() {
            field?.let {
                if (it.isBefore(LocalDate.now())) {
                    return it
                }
            }
            return null
        }

    /**
     * Refresh forecast from stored liqui-entries, invoices and creditor invoices and sort the entries.
     *
     * @return this for chaining.
     * @see .sort
     */
    fun build(): LiquidityForecast {
        entries.clear()
        entries.addAll(liquiEntries)
        entries.addAll(invoicesLiquidityEntries)
        entries.addAll(creditorInvoicesLiquidityEntries)
        sort()
        return this
    }

    /**
     * @return this for chaining.
     */
    private fun sort(): LiquidityForecast {
        entries.sortWith(Comparator { o1, o2 ->
            if (o1.dateOfPayment == null) {
                if (o2.dateOfPayment != null) {
                    return@Comparator -1
                }
            } else if (o2.dateOfPayment == null) {
                return@Comparator 1
            } else {
                val compare = o1.dateOfPayment.compareTo(o2.dateOfPayment)
                if (compare != 0) {
                    return@Comparator compare
                }
            }
            val s1 = if (o1.subject != null) o1.subject else ""
            val s2 = if (o2.subject != null) o2.subject else ""
            s1.compareTo(s2)
        })
        return this
    }

    /**
     * @return the entries
     */
    fun getEntries(): List<LiquidityEntry> {
        return entries
    }

    fun set(list: Collection<LiquidityEntryDO>?): LiquidityForecast {
        liquiEntries = LinkedList()
        if (list == null) {
            return this
        }
        val ignorePaidStatus = baseDate?.isBefore(LocalDate.now()) ?: false
        for (liquiEntry in list) {
            val entry = LiquidityEntry()
            entry.dateOfPayment = liquiEntry.dateOfPayment
            entry.amount = liquiEntry.amount
            if (ignorePaidStatus) {
                entry.isPaid = liquiEntry.dateOfPayment!!.isBefore(baseDate)
            } else {
                entry.isPaid = liquiEntry.paid
            }
            entry.subject = liquiEntry.subject
            entry.type = LiquidityEntryType.LIQUIDITY
            liquiEntries.add(entry)
        }
        return this
    }

    /**
     * For calculating the expected date of payment of future invoices. <br></br>
     * Should be called before [.setInvoices]!
     *
     * @param list
     */
    fun calculateExpectedTimeOfPayments(list: Collection<RechnungDO>?): LiquidityForecast {
        if (list == null) {
            return this
        }
        for (invoice in list) {
            val date = fromOrNull(invoice.datum)
            val dateOfPayment = fromOrNull(invoice.bezahlDatum)
            if (date == null || dateOfPayment == null) {
                continue
            }
            val timeForPayment = date.daysBetween(dateOfPayment).toInt()
            val amount: Int = invoice.info.grossSum.toInt()
            // Store values for different groups:
            val projectId = invoice.projekt?.id
            if (projectId != null) {
                ensureAndAddDebitorPaymentValue("project#$projectId", timeForPayment, amount)
            }
            val customerId = invoice.kunde?.nummer
            if (customerId != null) {
                ensureAndAddDebitorPaymentValue("customer#$customerId", timeForPayment, amount)
            }
            val account = WicketSupport.get(KontoCache::class.java).getKonto(invoice)
            val accountId = account?.id
            if (accountId != null) {
                ensureAndAddDebitorPaymentValue("account#$accountId", timeForPayment, amount)
            }
            var customerText = invoice.kundeText
            if (customerText != null) {
                customerText = customerText.lowercase()
                ensureAndAddDebitorPaymentValue("customer:$customerText", timeForPayment, amount)
                if (customerText.length > 5) {
                    customerText = customerText.substring(0, 5)
                }
                ensureAndAddDebitorPaymentValue("shortCustomer:$customerText", timeForPayment, amount)
            }
        }
        return this
    }

    private fun setExpectedTimeOfPayment(entry: LiquidityEntry, invoice: RechnungDO) {
        var dateOfInvoice = invoice.datum
        if (dateOfInvoice == null) {
            dateOfInvoice = DayHolder().localDate
        }
        val project = invoice.projekt
        if (project != null
                && setExpectedDateOfPayment(entry, dateOfInvoice, "project#" + project.id,
                        ProjektFormatter.formatProjektKundeAsString(project))) {
            return
        }
        val customer = invoice.kunde
        if (customer != null
                && setExpectedDateOfPayment(entry, dateOfInvoice, "customer#" + customer.nummer,
                        KundeFormatter.formatKundeAsString(customer, null))) {
            return
        }
        val account = WicketSupport.get(KontoCache::class.java).getKonto(invoice)
        if (account != null
                && setExpectedDateOfPayment(entry, dateOfInvoice, "account#" + account.id,
                        "" + account.nummer + " - " + account.bezeichnung)) {
            return
        }
        var customerText = invoice.kundeText
        if (customerText != null) {
            customerText = customerText.lowercase()
            if (setExpectedDateOfPayment(entry, dateOfInvoice, "customer:$customerText", customerText)) {
                return
            }
            if (customerText.length > 5) {
                customerText = customerText.substring(0, 5)
            }
            if (setExpectedDateOfPayment(entry, dateOfInvoice, "shortCustomer:$customerText", customerText)) {
                return
            }
        }
    }

    private fun setExpectedDateOfPayment(entry: LiquidityEntry, dateOfInvoice: LocalDate?, mapKey: String,
                                         area: String): Boolean {
        val values = aggregatedDebitorInvoicesValuesMap[mapKey]
        return if (values != null && values.numberOfValues >= 1) {
            entry.expectedDateOfPayment = getDate(dateOfInvoice, values.weightedAverage)
            entry.comment = (mapKey
                    + ": "
                    + area
                    + ": "
                    + values.weightedAverage
                    + " days ("
                    + values.numberOfValues
                    + " paid invoices)")
            true
        } else {
            false
        }
    }

    private fun ensureAndAddDebitorPaymentValue(mapId: String, timeForPayment: Int, amount: Int) {
        var values = aggregatedDebitorInvoicesValuesMap[mapId]
        if (values == null) {
            values = IntAggregatedValues()
            aggregatedDebitorInvoicesValuesMap[mapId] = values
        }
        values.add(timeForPayment, amount)
    }

    /**
     * For calculating the expected date of payment of future invoices. <br></br>
     * Should be called before [.setInvoices]!
     *
     * @param list
     */
    fun calculateExpectedTimeOfCreditorPayments(list: Collection<EingangsrechnungDO>?): LiquidityForecast {
        if (list == null) {
            return this
        }
        for (invoice in list) {
            val date = fromOrNull(invoice.datum)
            val dateOfPayment = fromOrNull(invoice.bezahlDatum)
            if (date == null || dateOfPayment == null) {
                continue
            }
            val timeForPayment = date.daysBetween(dateOfPayment).toInt()
            val amount: Int = invoice.info.grossSum.toInt()
            val account = invoice.konto
            val accountId = account?.id
            if (accountId != null) {
                ensureAndAddCreditorPaymentValue("account#$accountId", timeForPayment, amount)
            }
            var creditorText = invoice.kreditor
            if (creditorText != null) {
                creditorText = creditorText.lowercase()
                ensureAndAddCreditorPaymentValue("creditor:$creditorText", timeForPayment, amount)
                if (creditorText.length > 5) {
                    creditorText = creditorText.substring(0, 5)
                }
                ensureAndAddCreditorPaymentValue("shortCreditor:$creditorText", timeForPayment, amount)
            }
        }
        return this
    }

    private fun setExpectedTimeOfPayment(entry: LiquidityEntry, invoice: EingangsrechnungDO) {
        var dateOfInvoice = invoice.datum
        if (dateOfInvoice == null) {
            dateOfInvoice = DayHolder().localDate
        }
        val account = WicketSupport.get(KontoCache::class.java).getKontoIfNotInitialized(invoice.konto)
        if (account != null
                && setExpectedDateOfCreditorPayment(entry, dateOfInvoice, "account#" + account.id,
                        "" + account.nummer + " - " + account.bezeichnung)) {
            return
        }
        var creditorText = invoice.kreditor
        if (creditorText != null) {
            creditorText = creditorText.lowercase()
            if (setExpectedDateOfCreditorPayment(entry, dateOfInvoice, "creditor:$creditorText", creditorText)) {
                return
            }
            if (creditorText.length > 5) {
                creditorText = creditorText.substring(0, 5)
            }
            if (setExpectedDateOfCreditorPayment(entry, dateOfInvoice, "shortCreditor:$creditorText",
                            creditorText)) {
                return
            }
        }
    }

    private fun setExpectedDateOfCreditorPayment(entry: LiquidityEntry, dateOfInvoice: LocalDate?,
                                                 mapKey: String,
                                                 area: String): Boolean {
        val values = aggregatedCreditorInvoicesValuesMap[mapKey]
        return if (values != null && values.numberOfValues >= 1) {
            entry.expectedDateOfPayment = getDate(dateOfInvoice, values.weightedAverage)
            entry.comment = (mapKey
                    + ": "
                    + area
                    + ": "
                    + values.weightedAverage
                    + " days ("
                    + values.numberOfValues
                    + " paid invoices)")
            true
        } else {
            false
        }
    }

    private fun ensureAndAddCreditorPaymentValue(mapId: String, timeForPayment: Int, amount: Int) {
        var values = aggregatedCreditorInvoicesValuesMap[mapId]
        if (values == null) {
            values = IntAggregatedValues()
            aggregatedCreditorInvoicesValuesMap[mapId] = values
        }
        values.add(timeForPayment, amount)
    }

    private fun getDate(date: LocalDate?, timeOfPayment: Int): LocalDate {
        val day = from(date!!).plusDays(timeOfPayment.toLong())
        return day.localDate
    }

    /**
     * Should be called after [.calculateExpectedTimeOfPayments]-
     *
     * @param list
     * @return
     */
    fun setInvoices(list: Collection<RechnungDO>?): LiquidityForecast {
        invoices = list
        invoicesLiquidityEntries = LinkedList()
        if (list == null) {
            return this
        }
        val ignorePaidStatus = baseDate?.isBefore(LocalDate.now()) == true
        for (invoice in list) {
            val entry = LiquidityEntry()
            if (invoice.bezahlDatum != null) {
                entry.dateOfPayment = invoice.bezahlDatum
            } else {
                entry.dateOfPayment = invoice.faelligkeit
            }
            entry.amount = invoice.info.grossSum
            if (ignorePaidStatus) {
                entry.isPaid = invoice.faelligkeit!!.isBefore(baseDate)
            } else {
                entry.isPaid = invoice.info.isBezahlt
            }
            entry.subject = "#" + invoice.nummer + ": " + invoice.kundeAsString + ": " + invoice.betreff
            entry.type = LiquidityEntryType.DEBITOR
            setExpectedTimeOfPayment(entry, invoice)
            invoicesLiquidityEntries.add(entry)
        }
        return this
    }

    fun setCreditorInvoices(list: Collection<EingangsrechnungDO>?): LiquidityForecast {
        creditorInvoices = list
        creditorInvoicesLiquidityEntries = LinkedList()
        if (list == null) {
            return this
        }
        val ignorePaidStatus = baseDate?.isBefore(LocalDate.now()) == true
        for (invoice in list) {
            val entry = LiquidityEntry()
            if (invoice.bezahlDatum != null) {
                entry.dateOfPayment = invoice.bezahlDatum
            } else {
                entry.dateOfPayment = invoice.faelligkeit
            }
            entry.amount = invoice.info.grossSum.negate()
            if (ignorePaidStatus) {
                entry.isPaid = invoice.faelligkeit!!.isBefore(baseDate)
            } else {
                entry.isPaid = invoice.info.isBezahlt
            }
            entry.subject = invoice.kreditor + ": " + invoice.betreff
            entry.type = LiquidityEntryType.CREDITOR
            setExpectedTimeOfPayment(entry, invoice)
            creditorInvoicesLiquidityEntries.add(entry)
        }
        return this
    }

    /**
     * @return the invoices
     */
    fun getInvoicesLiquidityEntries(): Collection<LiquidityEntry>? {
        return invoicesLiquidityEntries
    }

    /**
     * @return the creditorInvoices
     */
    fun getCreditorInvoicesLiquidityEntries(): Collection<LiquidityEntry>? {
        return creditorInvoicesLiquidityEntries
    }
}
