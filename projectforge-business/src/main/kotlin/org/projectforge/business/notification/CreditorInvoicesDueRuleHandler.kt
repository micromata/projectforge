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

package org.projectforge.business.notification

import org.projectforge.business.configuration.ConfigurationService
import org.projectforge.business.fibu.EingangsrechnungDO
import org.projectforge.business.fibu.EingangsrechnungDao
import org.projectforge.business.fibu.RechnungCache
import org.projectforge.business.fibu.RechnungCalculator
import org.projectforge.business.fibu.RechnungInfo
import org.projectforge.business.user.UserLocale
import org.projectforge.framework.persistence.api.QueryFilter
import org.projectforge.framework.persistence.user.entities.PFUserDO
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Service
import java.math.BigDecimal
import java.text.NumberFormat
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.time.format.FormatStyle

/**
 * Unpaid creditor invoices due within the next working days ([NotificationParams.dueWorkingDays], holidays aware),
 * overdue ones included (up to [MAX_OVERDUE_DAYS], so old unpaid ones don't trigger the rule forever), so they are
 * paid in time. Nobody is concerned by an invoice: the configured recipients (e.g. the accounting group) are notified,
 * but only those with read access to the creditor invoices.
 *
 * Meant to run daily: the period key is the day of the run, so a notification marked as done comes back with the next
 * run as long as such invoices are left. An older pending notification of the same recipient expires once a newer
 * one exists; all of them are resolved, as soon as no such invoice is left.
 */
@Service
class CreditorInvoicesDueRuleHandler : NotificationRuleHandler {
    @Autowired
    private lateinit var configurationService: ConfigurationService

    @Autowired
    private lateinit var eingangsrechnungDao: EingangsrechnungDao

    @Autowired
    private lateinit var notificationLinks: NotificationLinks

    @Autowired
    private lateinit var rechnungCache: RechnungCache

    override val ruleType = NotificationRuleType.CREDITOR_INVOICES_DUE

    override val variables = listOf(
        "dueWorkingDays",
        "creditorInvoicesDueCount",
        "creditorInvoicesDueSum",
        "creditorInvoicesDueList",
        "creditorInvoiceLink",
    )

    override fun evaluate(rule: NotificationRuleDO, today: LocalDate): NotificationRuleHandler.Evaluation? {
        val invoices = dueInvoices(rule, today)
        if (invoices.isEmpty()) {
            return null
        }
        // No user is concerned by an invoice: all recipients get the notification (filtered by isRelevant). The
        // invoices are loaded once per run, not per recipient.
        return NotificationRuleHandler.Evaluation(
            today.toString(), emptyMap(), CREDITOR_INVOICE_PATH, restrictsRecipients = false,
            variablesFor = { user -> variables(rule, invoices, user) },
        )
    }

    override fun variables(rule: NotificationRuleDO, today: LocalDate, user: PFUserDO): Map<String, String> {
        return variables(rule, dueInvoices(rule, today), user)
    }

    private fun variables(rule: NotificationRuleDO, invoices: List<DueInvoice>, user: PFUserDO): Map<String, String> {
        val workingDays = rule.readParams().dueWorkingDays
        val locale = UserLocale.determineUserLocale(user)
        val dateFormatter = DateTimeFormatter.ofLocalizedDate(FormatStyle.MEDIUM).withLocale(locale)
        val numberFormat = NumberFormat.getNumberInstance(locale).also {
            it.minimumFractionDigits = 2
            it.maximumFractionDigits = 2
        }
        val systemCurrency = configurationService.currency ?: "EUR"
        // One sum per currency: amounts of different currencies don't add up.
        val sums = invoices.groupBy { it.invoice.currency ?: systemCurrency }
            .map { (currency, list) -> "${numberFormat.format(list.sumOf { it.info.grossSum })} $currency" }
        val list = invoices.take(MAX_INVOICES).map { (invoice, info) ->
            listOfNotNull(
                invoice.kreditor,
                invoice.referenz,
                info.faelligkeit?.format(dateFormatter),
                "${numberFormat.format(info.grossSum)} ${invoice.currency ?: systemCurrency}",
            ).joinToString(", ")
        }.toMutableList()
        if (invoices.size > MAX_INVOICES) {
            list += "…"
        }
        return mapOf(
            "dueWorkingDays" to workingDays.toString(),
            "creditorInvoicesDueCount" to invoices.size.toString(),
            "creditorInvoicesDueSum" to sums.joinToString("; ").ifEmpty { numberFormat.format(BigDecimal.ZERO) },
            "creditorInvoicesDueList" to list.joinToString("; ").ifEmpty { "–" },
            "creditorInvoiceLink" to notificationLinks.absolute(CREDITOR_INVOICE_PATH),
        )
    }

    /** Only recipients who may read the creditor invoices: the notification lists them. */
    override fun isRelevant(rule: NotificationRuleDO, today: LocalDate, user: PFUserDO): Boolean {
        return eingangsrechnungDao.hasUserSelectAccess(user, false)
    }

    /**
     * Resolved if no invoice is due any more (paid or the due date moved); expired if a newer notification of the
     * recipient is pending (the one of a later run).
     */
    override fun resolve(
        rule: NotificationRuleDO,
        notifications: List<NotificationDO>,
        today: LocalDate,
    ): Map<Long, NotificationStatus> {
        val result = mutableMapOf<Long, NotificationStatus>()
        val noneDue = dueInvoices(rule, today).isEmpty()
        notifications.groupBy { it.recipientId }.forEach { (_, list) ->
            val newest = list.maxWith(compareBy<NotificationDO> { it.periodKey ?: "" }.thenBy { it.id ?: 0 })
            list.forEach { notification ->
                if (noneDue) {
                    result[notification.id!!] = NotificationStatus.RESOLVED
                } else if (notification !== newest) {
                    result[notification.id!!] = NotificationStatus.EXPIRED
                }
            }
        }
        return result
    }

    internal data class DueInvoice(val invoice: EingangsrechnungDO, val info: RechnungInfo)

    /** The invoices due, the earliest due date first. */
    internal fun dueInvoices(rule: NotificationRuleDO, today: LocalDate): List<DueInvoice> {
        val workingDays = rule.readParams().dueWorkingDays
        // A generous upper bound of the due date for the query (weekends and holidays in between); the exact check,
        // paid or not included, is RechnungCalculator's.
        val filter = QueryFilter().also {
            it.deleted = false
            it.add(QueryFilter.ge("faelligkeit", today.minusDays(MAX_OVERDUE_DAYS)))
            it.add(QueryFilter.le("faelligkeit", today.plusDays(workingDays * 2L + 14)))
        }
        return eingangsrechnungDao.select(filter, checkAccess = false)
            .mapNotNull { invoice ->
                val info = rechnungCache.getEingangsrechnungInfo(invoice.id) ?: return@mapNotNull null
                if (RechnungCalculator.isDueWithinWorkingDays(info, workingDays, today)) DueInvoice(invoice, info) else null
            }
            .sortedWith(compareBy<DueInvoice> { it.info.faelligkeit }.thenBy { it.invoice.kreditor })
    }

    companion object {
        /** The invoices listed at most in [variables]. */
        private const val MAX_INVOICES = 10

        /** Invoices overdue for longer are left out: old unpaid ones (e.g. disputed) would be reported every day. */
        const val MAX_OVERDUE_DAYS = 90L

        /** The creditor invoice list of the next app. */
        const val CREDITOR_INVOICE_PATH = "/creditor-invoice"
    }
}
