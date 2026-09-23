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

package org.projectforge.rest

import mu.KotlinLogging
import org.projectforge.business.address.AddressDO
import org.projectforge.business.address.AddressDao
import org.projectforge.business.address.AddressFilter
import org.projectforge.business.address.PhoneType
import org.projectforge.business.user.service.UserPrefService
import org.projectforge.common.StringHelper
import org.projectforge.framework.i18n.translate
import org.projectforge.framework.i18n.translateMsg
import org.projectforge.framework.persistence.jpa.impl.HibernateSearchFilterUtils
import org.projectforge.framework.persistence.user.api.ThreadLocalUserContext
import org.projectforge.framework.time.DateTimeFormatter
import org.projectforge.framework.utils.NumberHelper
import org.projectforge.framework.utils.RecentQueue
import org.projectforge.messaging.SmsSender
import org.projectforge.rest.config.Rest
import org.projectforge.sms.SmsSenderConfig
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.RestController
import java.util.Date

private val log = KotlinLogging.logger {}

/**
 * The "Send text message" page ("SMS senden"), successor of Wicket's `SendSmsPage` (`wa/sendSms`).
 *
 * Like [org.projectforge.rest.fibu.MonthlyEmployeeReportRest] it is a non-entity, standalone page: it
 * exposes plain JSON so the next frontend can render a two-field form (receiver phone number + message)
 * and send the message through the reused business layer ([SmsSender] / [SmsSenderConfig]). Sending is
 * unchanged from the Wicket page: the number is extracted with the configured country prefix, guarded by
 * [SmsSenderConfig.isSmsConfigured], and the [SmsSender.HttpResponseCode] is mapped to a localized result.
 *
 * CSRF is inherited via `RestAuthenticationUtils`/`RestCsrfProtection` for all `/rs` endpoints; the legacy
 * page was not 2FA-gated, so no 2FA registration is needed.
 */
@RestController
@RequestMapping("${Rest.URL}/sendTextMessage")
class SendTextMessageRest {
    @Autowired
    private lateinit var addressDao: AddressDao

    @Autowired
    private lateinit var smsSenderConfig: SmsSenderConfig

    @Autowired
    private lateinit var userPrefService: UserPrefService

    @Autowired
    private lateinit var dateTimeFormatter: DateTimeFormatter

    /** Initial form data (deep-link via `addressId` + `phoneType`, or a raw `number`). */
    class InitialData(
        val phoneNumber: String? = null,
        val message: String = "",
        val maxMessageSize: Int = 160,
        val smsConfigured: Boolean = false,
    )

    /** Body of [send]. */
    class SendRequest(
        val phoneNumber: String? = null,
        val message: String? = null,
    )

    /** Result of [send]: `success` drives a success vs. error toast on the client, `message` is localized. */
    class SendResult(
        val success: Boolean,
        val message: String,
    )

    /**
     * @param addressId Optional address to prefill the receiver from (together with [phoneType]).
     * @param phoneType MOBILE or PRIVATE_MOBILE — which number of the address to prefill.
     * @param number    A raw receiver number, used verbatim when no address is given.
     */
    @GetMapping
    fun getInitialData(
        @RequestParam("addressId", required = false) addressId: Long?,
        @RequestParam("phoneType", required = false) phoneType: String?,
        @RequestParam("number", required = false) number: String?,
    ): InitialData {
        return InitialData(
            phoneNumber = prefillNumber(addressId, phoneType, number),
            message = initialMessageText(),
            maxMessageSize = smsSenderConfig.smsMaxMessageLength,
            smsConfigured = smsSenderConfig.isSmsConfigured(),
        )
    }

    /**
     * Auto-completion of the receiver field: one entry per non-blank mobile / private mobile number of the
     * matching addresses, formatted `"<number>: <name>, <firstName>, <organization>"` (as the Wicket page).
     * An empty search offers the numbers recently sent to (persisted per user).
     */
    @GetMapping("ac")
    fun autoComplete(@RequestParam("search", required = false) search: String?): List<String> {
        if (search.isNullOrBlank()) {
            return recentReceivers().recentList ?: emptyList()
        }
        val filter = AddressFilter()
        // AND search: when the user types multiple words (e.g. first name and last name) all of them
        // must match, not just any one of them (the full-text default is OR). modifySearchString turns
        // "Kai Reinhard" into "+Kai* +Reinhard*", which the downstream query layer keeps verbatim.
        filter.searchString = HibernateSearchFilterUtils.modifySearchString(search, true)
        val entries = LinkedHashSet<String>()
        addressDao.select(filter).forEach { address ->
            addEntry(entries, address, address.mobilePhone)
            addEntry(entries, address, address.privateMobilePhone)
        }
        return entries.toList()
    }

    @PostMapping("send")
    fun send(@RequestBody postData: SendRequest): SendResult {
        val number = NumberHelper.extractPhonenumber(postData.phoneNumber)
        if (!smsSenderConfig.isSmsConfigured()) {
            log.error("Servlet url for sending sms not configured. SMS not supported.")
            return SendResult(false, translate("address.sendSms.sendMessage.result.unknownError"))
        }
        val smsSender = SmsSender(smsSenderConfig)
        val responseCode = smsSender.send(number, postData.message)
        val errorKey = smsSender.getErrorMessage(responseCode)
        if (errorKey != null) {
            return SendResult(false, translate(errorKey))
        }
        recentReceivers().append(postData.phoneNumber)
        return SendResult(
            true,
            translateMsg(
                "address.sendSms.sendMessage.result.successful",
                number,
                dateTimeFormatter.getFormattedDateTime(Date()),
            ),
        )
    }

    private fun prefillNumber(addressId: Long?, phoneType: String?, number: String?): String? {
        if (!number.isNullOrBlank()) {
            return number
        }
        addressId ?: return null
        val address = addressDao.find(addressId) ?: return null
        val type = phoneType?.let { runCatching { PhoneType.valueOf(it) }.getOrNull() }
        val raw = when (type) {
            PhoneType.MOBILE -> address.mobilePhone
            PhoneType.PRIVATE_MOBILE -> address.privateMobilePhone
            else -> null
        } ?: return null
        return getPhoneNumberAndPerson(address, raw)
    }

    private fun addEntry(entries: MutableSet<String>, address: AddressDO, number: String?) {
        if (number.isNullOrBlank()) {
            return
        }
        entries.add(getPhoneNumberAndPerson(address, number))
    }

    /** `"<extracted number>: <name>, <firstName>, <organization>"`, as the Wicket page composed it. */
    private fun getPhoneNumberAndPerson(address: AddressDO, number: String): String {
        return StringHelper.listToString(
            ", ",
            "${NumberHelper.extractPhonenumber(number)}: ${address.name}",
            address.firstName,
            address.organization,
        )
    }

    private fun initialMessageText(): String {
        val fullname = ThreadLocalUserContext.loggedInUser?.getFullname() ?: ""
        return "$fullname. ${translate("address.sendSms.doNotReply")}"
    }

    @Suppress("UNCHECKED_CAST")
    private fun recentReceivers(): RecentQueue<String> {
        return userPrefService.ensureEntry(PREF_AREA, PREF_RECENTS, RecentQueue<String>(), true) as RecentQueue<String>
    }

    companion object {
        private const val PREF_AREA = "sendTextMessage"
        private const val PREF_RECENTS = "recentReceivers"
    }
}
