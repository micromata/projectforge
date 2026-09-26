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
import org.projectforge.business.configuration.ConfigurationService
import org.projectforge.business.sipgate.SipgateConfiguration
import org.projectforge.business.user.service.UserPrefService
import org.projectforge.common.StringHelper
import org.projectforge.framework.i18n.translate
import org.projectforge.framework.i18n.translateMsg
import org.projectforge.framework.persistence.jpa.impl.HibernateSearchFilterUtils
import org.projectforge.framework.persistence.user.api.ThreadLocalUserContext
import org.projectforge.framework.time.DateTimeFormatter
import org.projectforge.framework.utils.NumberHelper
import org.projectforge.framework.utils.RecentQueue
import org.projectforge.rest.config.Rest
import org.projectforge.rest.core.PagesResolver
import org.projectforge.rest.sipgate.SipgateDirectCallService
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
 * The "Direct call" page ("Direktwahl Telefonanlage"), successor of Wicket's `PhoneCallPage` (`wa/phoneCall`).
 *
 * Like [SendTextMessageRest] it is a non-entity, standalone page: it exposes plain JSON so the next frontend
 * can render a hand-built form (receiver phone number + "my phone" / "my caller id" selects, plus the resolved
 * address with its clickable numbers) and place the call through the reused [SipgateDirectCallService]. Calling
 * is unchanged from the Wicket page: the number is extracted with the configured country prefix and the leading
 * telephone-system number, guarded by [SipgateConfiguration.isConfigured], and mapped to a localized result.
 *
 * CSRF is inherited via `RestAuthenticationUtils`/`RestCsrfProtection` for all `/rs` endpoints; the legacy page
 * was not 2FA-gated, so no 2FA registration is needed. The Wicket page stays mounted as the "classic version"
 * escape hatch; only the menu entry ([org.projectforge.menu.builder.MenuItemDefId.PHONE_CALL]) points here now.
 *
 * The Wicket special inputs `id:<addressId>` and `<number> | … #<id>` were an autocomplete convenience of that
 * page; since this frontend passes address and number structurally (deep-link `addressId`/`number`), they are
 * not reimplemented here.
 */
@RestController
@RequestMapping("${Rest.URL}/phoneCall")
class PhoneCallRest {
    @Autowired
    private lateinit var addressDao: AddressDao

    @Autowired
    private lateinit var configurationService: ConfigurationService

    @Autowired
    private lateinit var sipgateConfiguration: SipgateConfiguration

    @Autowired
    private lateinit var sipgateDirectCallService: SipgateDirectCallService

    @Autowired
    private lateinit var userPrefService: UserPrefService

    @Autowired
    private lateinit var dateTimeFormatter: DateTimeFormatter

    /** One phone number of the resolved address, for the clickable numbers of the address panel. */
    class AddressPhoneNumber(
        val number: String,
        val phoneType: PhoneType,
    )

    /** The resolved address (deep-link `addressId`), shown in the address panel with its clickable numbers. */
    class AddressInfo(
        val id: Long,
        val fullName: String,
        val numbers: List<AddressPhoneNumber>,
        /** The address' view page, so the panel name links to it (address is not migrated to next yet). */
        val viewUrl: String,
    )

    /** Initial form data (deep-link via `addressId`, or a raw `number`). */
    class InitialData(
        val phoneNumber: String? = null,
        val address: AddressInfo? = null,
        val myPhoneIds: List<String> = emptyList(),
        val callerIds: List<String> = emptyList(),
        val recentMyPhoneId: String? = null,
        val recentMyCallerId: String? = null,
        val sipgateConfigured: Boolean = false,
        val callerPage: String? = null,
        /** Where the "back" link leads (the address list or view the user came from); null when opened plain. */
        val backUrl: String? = null,
    )

    /**
     * One auto-completion entry: [display] is what the user sees and the free-text box holds, [number] is the
     * clean number to dial, and [addressId] (when the entry came from an address, not a recent) lets the client
     * refresh the address panel to the picked contact.
     */
    class AcItem(
        val addressId: Long? = null,
        val number: String,
        val display: String,
    )

    /** Body of [call]. */
    class CallRequest(
        val phoneNumber: String? = null,
        val myPhoneId: String? = null,
        val myCallerId: String? = null,
    )

    /** Result of [call]: `success` drives a success vs. error toast on the client, `message` is localized. */
    class CallResult(
        val success: Boolean,
        val message: String,
    )

    /**
     * @param addressId Optional address to resolve (prefills the number and fills the address panel).
     * @param number    A raw receiver number, used verbatim (extracted) when given.
     * @param callerPage Where the user came from (`addressList` / `addressView`), passed through for the "back" link.
     */
    @GetMapping
    fun getInitialData(
        @RequestParam("addressId", required = false) addressId: Long?,
        @RequestParam("number", required = false) number: String?,
        @RequestParam("callerPage", required = false) callerPage: String?,
    ): InitialData {
        val deepLinkAddress = addressId?.let { addressDao.find(it) }
        // Opened plain from the menu (no deep link): restore the last shown address, but only if its remembered
        // number still belongs to it (the address may have changed its numbers meanwhile).
        val restored = if (deepLinkAddress == null && number.isNullOrBlank()) restoreLastAddress() else null
        val address = deepLinkAddress ?: restored?.first
        val phoneNumber = when {
            deepLinkAddress != null -> prefillNumber(deepLinkAddress, number)
            restored != null -> restored.second
            else -> prefillNumber(null, number)
        }
        // Remember a freshly resolved deep-link address, so re-opening the page restores it.
        deepLinkAddress?.let { rememberLastAddress(it.id, phoneNumber) }
        val user = ThreadLocalUserContext.loggedInUser
        val sipgateConfigured = sipgateConfiguration.isConfigured()
        val myPhoneIds = if (sipgateConfigured && user != null) sipgateDirectCallService.getCallerNumbers(user) else emptyList()
        val callerIds = if (sipgateConfigured && user != null) sipgateDirectCallService.getCallerIds(user) else emptyList()
        return InitialData(
            phoneNumber = phoneNumber,
            address = address?.let { toAddressInfo(it) },
            myPhoneIds = myPhoneIds,
            callerIds = callerIds,
            recentMyPhoneId = userPrefService.getEntry(PREF_AREA, PREF_RECENT_PHONE_ID, String::class.java),
            recentMyCallerId = userPrefService.getEntry(PREF_AREA, PREF_RECENT_CALLER_ID, String::class.java),
            sipgateConfigured = sipgateConfigured,
            callerPage = callerPage,
            backUrl = backUrl(callerPage, address),
        )
    }

    /**
     * The "back" link the [PhoneCallForm] offers, mirroring the Wicket page's `backToCaller`: to the address'
     * view page when the user came from there, else to the address list; null when opened plain (no caller).
     * Address is not migrated to next yet, so both targets are the legacy React app (see [PagesResolver]).
     */
    private fun backUrl(callerPage: String?, address: AddressDO?): String? {
        return when (callerPage) {
            "addressView" -> address?.id?.let { AddressViewPageRest.getPageUrl(it) }
            "addressList" -> PagesResolver.getListPageUrl(AddressPagesRest::class.java, absolute = true)
            else -> null
        }
    }

    /**
     * Auto-completion of the number field: one entry per non-blank business / mobile / private / private-mobile
     * number of the matching addresses, formatted `"<number>: <name>, <firstName>, <organization>"` (as the Wicket
     * page) and carrying the address id so the client can follow the pick in the address panel. An empty search
     * offers the numbers recently called (persisted per user), which have no address behind them.
     */
    @GetMapping("ac")
    fun autoComplete(@RequestParam("search", required = false) search: String?): List<AcItem> {
        if (search.isNullOrBlank()) {
            return recentReceivers().recentList.orEmpty().map { AcItem(number = extractPhonenumber(it) ?: it, display = it) }
        }
        val filter = AddressFilter()
        // AND search: "Kai Reinhard" → "+Kai* +Reinhard*", so all typed words must match, not just any one.
        filter.searchString = HibernateSearchFilterUtils.modifySearchString(search, true)
        val items = LinkedHashMap<String, AcItem>()
        addressDao.select(filter).forEach { address ->
            addEntry(items, address, address.businessPhone)
            addEntry(items, address, address.mobilePhone)
            addEntry(items, address, address.privatePhone)
            addEntry(items, address, address.privateMobilePhone)
        }
        return items.values.toList()
    }

    /**
     * The resolved address for the [id] picked in the auto-completion, used to refresh the address panel. When a
     * [number] is given, the address is only returned if that number still belongs to it (else `null`) and the
     * pair is remembered as the last shown address, so re-opening the page without a deep link restores it.
     */
    @GetMapping("address")
    fun address(
        @RequestParam("id") id: Long,
        @RequestParam("number", required = false) number: String?,
    ): AddressInfo? {
        val address = addressDao.find(id) ?: return null
        if (number.isNullOrBlank()) {
            return toAddressInfo(address)
        }
        if (!matchesNumber(address, number)) {
            return null
        }
        rememberLastAddress(id, extractPhonenumber(number))
        return toAddressInfo(address)
    }

    @PostMapping("call")
    fun call(@RequestBody postData: CallRequest): CallResult {
        if (!sipgateConfiguration.isConfigured()) {
            log.error("Sipgate isn't configured. Phone calls not supported.")
            return CallResult(false, translate("address.phoneCall.result.callingError"))
        }
        val number = NumberHelper.extractPhonenumber(postData.phoneNumber)
        if (number.isNullOrBlank() || !isValidDialNumber(number)) {
            return CallResult(false, translate("address.phoneCall.number.invalid"))
        }
        val user = ThreadLocalUserContext.loggedInUser
            ?: return CallResult(false, translate("address.phoneCall.result.callingError"))
        val callee = extractPhonenumber(number) ?: number
        log.info {
            "User initiates direct call from phone with id '${postData.myPhoneId}' with caller-id " +
                    "'${postData.myCallerId}' to destination number: ${StringHelper.hideStringEnding(callee, 'x', 3)}"
        }
        if (!sipgateDirectCallService.initCall(user, postData.myPhoneId ?: "", postData.myCallerId, callee)) {
            return CallResult(false, translate("address.phoneCall.result.callingError"))
        }
        // Remember the user's phone / caller id choice and the dialed number for next time.
        postData.myPhoneId?.let { userPrefService.putEntry(PREF_AREA, PREF_RECENT_PHONE_ID, it, true) }
        postData.myCallerId?.let { userPrefService.putEntry(PREF_AREA, PREF_RECENT_CALLER_ID, it, true) }
        recentReceivers().append(callee)
        return CallResult(
            true,
            "${dateTimeFormatter.getFormattedDateTime(Date())}: ${translate("address.phoneCall.result.successful")}",
        )
    }

    private fun prefillNumber(address: AddressDO?, number: String?): String? {
        if (!number.isNullOrBlank()) {
            return extractPhonenumber(number)
        }
        address ?: return null
        return extractPhonenumber(getFirstPhoneNumber(address))
    }

    /** Find a phone number, search order is business, mobile, private mobile and private (as the Wicket page). */
    private fun getFirstPhoneNumber(address: AddressDO): String? {
        return address.businessPhone?.takeIf { it.isNotBlank() }
            ?: address.mobilePhone?.takeIf { it.isNotBlank() }
            ?: address.privateMobilePhone?.takeIf { it.isNotBlank() }
            ?: address.privatePhone?.takeIf { it.isNotBlank() }
    }

    private fun toAddressInfo(address: AddressDO): AddressInfo {
        val numbers = mutableListOf<AddressPhoneNumber>()
        addNumber(numbers, address.businessPhone, PhoneType.BUSINESS)
        addNumber(numbers, address.mobilePhone, PhoneType.MOBILE)
        addNumber(numbers, address.privatePhone, PhoneType.PRIVATE)
        addNumber(numbers, address.privateMobilePhone, PhoneType.PRIVATE_MOBILE)
        // fullName joins name, first name and organization, so an entry with only an organization (a company
        // support line) shows the organization instead of a bare salutation ("Frau") from fullNameWithTitleAndForm.
        val displayName = address.fullName?.takeIf { it.isNotBlank() } ?: address.fullNameWithTitleAndForm.trim()
        return AddressInfo(address.id!!, displayName, numbers, AddressViewPageRest.getPageUrl(address.id))
    }

    private fun addNumber(list: MutableList<AddressPhoneNumber>, number: String?, phoneType: PhoneType) {
        if (!number.isNullOrBlank()) {
            list.add(AddressPhoneNumber(number, phoneType))
        }
    }

    private fun addEntry(entries: MutableMap<String, AcItem>, address: AddressDO, number: String?) {
        if (number.isNullOrBlank()) {
            return
        }
        val cleanNumber = NumberHelper.extractPhonenumber(number) ?: return
        val display = StringHelper.listToString(
            ", ",
            "$cleanNumber: ${address.name}",
            address.firstName,
            address.organization,
        )
        // Keyed by display so duplicate numbers of the same contact collapse, as the Wicket page's set did.
        entries.putIfAbsent(display, AcItem(addressId = address.id, number = cleanNumber, display = display))
    }

    /**
     * Extracts the number with the configured country prefix and strips a leading telephone-system number, as
     * [org.projectforge.web.address.PhoneCallPage.extractPhonenumber] did.
     */
    private fun extractPhonenumber(number: String?): String? {
        return stripSystemNumber(NumberHelper.extractPhonenumber(number), configurationService.telephoneSystemNumber)
    }

    @Suppress("UNCHECKED_CAST")
    private fun recentReceivers(): RecentQueue<String> {
        return userPrefService.ensureEntry(PREF_AREA, PREF_RECENTS, RecentQueue<String>(), true) as RecentQueue<String>
    }

    /** Persists the last shown address (id + already extracted number) for [restoreLastAddress]; needs both. */
    private fun rememberLastAddress(addressId: Long?, number: String?) {
        if (addressId == null || number.isNullOrBlank()) {
            return
        }
        userPrefService.putEntry(PREF_AREA, PREF_LAST_ADDRESS_ID, addressId.toString(), true)
        userPrefService.putEntry(PREF_AREA, PREF_LAST_NUMBER, number, true)
    }

    /**
     * The last shown address and its number, but only if the remembered number still belongs to the address (its
     * numbers may have changed meanwhile). Returned as a pair so [getInitialData] can render both; else `null`.
     */
    private fun restoreLastAddress(): Pair<AddressDO, String>? {
        val addressId = userPrefService.getEntry(PREF_AREA, PREF_LAST_ADDRESS_ID, String::class.java)?.toLongOrNull()
            ?: return null
        val number = userPrefService.getEntry(PREF_AREA, PREF_LAST_NUMBER, String::class.java)?.takeIf { it.isNotBlank() }
            ?: return null
        val address = addressDao.find(addressId) ?: return null
        return if (matchesNumber(address, number)) address to number else null
    }

    /** Whether [number] still matches one of the address' four phone numbers, both normalized via [extractPhonenumber]. */
    private fun matchesNumber(address: AddressDO, number: String): Boolean {
        val normalized = extractPhonenumber(number) ?: return false
        return listOf(address.businessPhone, address.mobilePhone, address.privatePhone, address.privateMobilePhone)
            .any { extractPhonenumber(it) == normalized }
    }

    companion object {
        private const val PREF_AREA = "phoneCall"
        private const val PREF_RECENTS = "recentReceivers"
        private const val PREF_RECENT_PHONE_ID = "recentPhoneId"
        private const val PREF_RECENT_CALLER_ID = "recentCallerId"
        private const val PREF_LAST_ADDRESS_ID = "lastAddressId"
        private const val PREF_LAST_NUMBER = "lastNumber"

        /**
         * Strips a leading telephone-system number from an already extracted number (see [extractPhonenumber]).
         * Pure so it can be unit-tested without the [ConfigurationService].
         */
        internal fun stripSystemNumber(extractedNumber: String?, systemNumber: String?): String? {
            val result = extractedNumber ?: return null
            return if (!systemNumber.isNullOrEmpty() && result.startsWith(systemNumber)) {
                result.substring(systemNumber.length)
            } else {
                result
            }
        }

        /** Guard from the Wicket page: only digits and the usual dialing characters are a valid number. */
        internal fun isValidDialNumber(number: String): Boolean {
            return number.all { it in "0123456789+-/() " }
        }
    }
}
