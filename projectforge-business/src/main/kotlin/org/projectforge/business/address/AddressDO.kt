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

package org.projectforge.business.address

import com.fasterxml.jackson.databind.annotation.JsonSerialize
import jakarta.persistence.*
import mu.KotlinLogging
import org.apache.commons.lang3.StringUtils
import org.hibernate.search.mapper.pojo.bridge.mapping.annotation.ValueBridgeRef
import org.hibernate.search.mapper.pojo.mapping.definition.annotation.FullTextField
import org.hibernate.search.mapper.pojo.mapping.definition.annotation.GenericField
import org.hibernate.search.mapper.pojo.mapping.definition.annotation.Indexed
import org.projectforge.business.common.HibernateSearchPhoneNumberBridge
import org.projectforge.common.StringHelper
import org.projectforge.common.anots.PropertyInfo
import org.projectforge.framework.DisplayNameCapable
import org.projectforge.framework.i18n.translate
import org.projectforge.framework.json.IdsOnlySerializer
import org.projectforge.framework.persistence.entities.DefaultBaseDO
import org.projectforge.framework.persistence.user.api.ThreadLocalUserContext
import org.projectforge.framework.utils.LabelValueBean
import java.time.LocalDate
import java.util.*

private val log = KotlinLogging.logger {}

/**
 * @author Kai Reinhard (k.reinhard@micromata.de)
 */
@Entity
@Indexed
@Table(
    name = "T_ADDRESS",
    uniqueConstraints = [UniqueConstraint(name = "unique_t_address_uid", columnNames = ["uid"])],
    indexes = [jakarta.persistence.Index(name = "idx_fk_t_address_uid", columnList = "uid")]
)
open class AddressDO : DefaultBaseDO(), DisplayNameCapable {
    override val displayName: String
        @Transient
        get() = listOf(name, firstName, organization, city).filter { !it.isNullOrBlank() }.joinToString(", ")

    @PropertyInfo(i18nKey = "address.contactStatus")
    @get:Enumerated(EnumType.STRING)
    @get:Column(name = "contact_status", length = 20, nullable = false)
    @FullTextField
    open var contactStatus = ContactStatus.ACTIVE

    @PropertyInfo(i18nKey = "address.addressStatus")
    @get:Enumerated(EnumType.STRING)
    @get:Column(name = "address_status", length = 20, nullable = false)
    @FullTextField
    open var addressStatus = AddressStatus.UPTODATE

    @get:Column(name = "uid")
    open var uid: String? = null

    @PropertyInfo(i18nKey = "name", required = true)
    @FullTextField
    @get:Column(length = 255)
    open var name: String? = null

    @PropertyInfo(i18nKey = "address.birthName")
    @FullTextField
    @get:Column(length = 255, name = "birth_name")
    open var birthName: String? = null

    @PropertyInfo(i18nKey = "firstName")
    @FullTextField
    @get:Column(name = "first_name", length = 255)
    open var firstName: String? = null

    @PropertyInfo(i18nKey = "address.form", required = true)
    @FullTextField
    @get:Enumerated(EnumType.STRING)
    @get:Column(name = "form", length = 10)
    open var form: FormOfAddress? = null

    @PropertyInfo(i18nKey = "address.title")
    @FullTextField
    @get:Column(length = 255)
    open var title: String? = null

    @PropertyInfo(i18nKey = "address.positionText")
    @FullTextField
    @get:Column(length = 255)
    open var positionText: String? = null

    @PropertyInfo(i18nKey = "organization")
    @FullTextField
    @get:Column(length = 255)
    open var organization: String? = null

    @PropertyInfo(i18nKey = "address.division")
    @FullTextField
    @get:Column(length = 255)
    open var division: String? = null

    @PropertyInfo(i18nKey = "address.phone", additionalI18nKey = "address.business")
    @GenericField(valueBridge = ValueBridgeRef(type = HibernateSearchPhoneNumberBridge::class))
    @get:Column(name = "business_phone", length = 255)
    open var businessPhone: String? = null

    @PropertyInfo(i18nKey = "address.phoneType.mobile", additionalI18nKey = "address.business")
    @GenericField(valueBridge = ValueBridgeRef(type = HibernateSearchPhoneNumberBridge::class))
    @get:Column(name = "mobile_phone", length = 255)
    open var mobilePhone: String? = null

    @PropertyInfo(i18nKey = "address.phoneType.fax", additionalI18nKey = "address.business")
    @GenericField(valueBridge = ValueBridgeRef(type = HibernateSearchPhoneNumberBridge::class))
    @get:Column(length = 255)
    open var fax: String? = null

    @PropertyInfo(i18nKey = "address.addressText", additionalI18nKey = "address.business")
    @FullTextField
    @get:Column(length = 255)
    open var addressText: String? = null

    @PropertyInfo(i18nKey = "address.addressText2", additionalI18nKey = "address.business")
    @FullTextField
    @get:Column(length = 255)
    open var addressText2: String? = null

    @PropertyInfo(i18nKey = "address.zipCode", additionalI18nKey = "address.business")
    @FullTextField
    @get:Column(name = "zip_code", length = 255)
    open var zipCode: String? = null

    @PropertyInfo(i18nKey = "address.city", additionalI18nKey = "address.business")
    @FullTextField
    @get:Column(length = 255)
    open var city: String? = null

    @PropertyInfo(i18nKey = "address.country", additionalI18nKey = "address.business")
    @FullTextField
    @get:Column(length = 255)
    open var country: String? = null

    @PropertyInfo(i18nKey = "address.state", additionalI18nKey = "address.business")
    @FullTextField
    @get:Column(length = 255)
    open var state: String? = null

    @PropertyInfo(i18nKey = "email", additionalI18nKey = "address.business")
    @FullTextField
    @get:Column(length = 255)
    open var email: String? = null

    @PropertyInfo(i18nKey = "address.addressText", additionalI18nKey = "address.postal")
    @FullTextField
    @get:Column(length = 255, name = "postal_addresstext")
    open var postalAddressText: String? = null

    @PropertyInfo(i18nKey = "address.addressText2", additionalI18nKey = "address.postal")
    @FullTextField
    @get:Column(length = 255, name = "postal_addresstext2")
    open var postalAddressText2: String? = null

    @PropertyInfo(i18nKey = "address.zipCode", additionalI18nKey = "address.postal")
    @FullTextField
    @get:Column(name = "postal_zip_code", length = 255)
    open var postalZipCode: String? = null

    @PropertyInfo(i18nKey = "address.city", additionalI18nKey = "address.postal")
    @FullTextField
    @get:Column(length = 255, name = "postal_city")
    open var postalCity: String? = null

    @PropertyInfo(i18nKey = "address.country", additionalI18nKey = "address.postal")
    @FullTextField
    @get:Column(name = "postal_country", length = 255)
    open var postalCountry: String? = null

    @PropertyInfo(i18nKey = "address.state", additionalI18nKey = "address.postal")
    @FullTextField
    @get:Column(name = "postal_state", length = 255)
    open var postalState: String? = null

    @PropertyInfo(i18nKey = "address.website")
    @FullTextField
    @get:Column(length = 255)
    open var website: String? = null

    /**
     * @return The communication will take place in this language.
     */
    @PropertyInfo(i18nKey = "address.communicationLanguage")
    @get:Column(name = "communication_language")
    open var communicationLanguage: Locale? = null

    @PropertyInfo(i18nKey = "address.phone", additionalI18nKey = "address.private")
    @GenericField(valueBridge = ValueBridgeRef(type = HibernateSearchPhoneNumberBridge::class))
    @get:Column(name = "private_phone", length = 255)
    open var privatePhone: String? = null

    @PropertyInfo(i18nKey = "address.phoneType.mobile", additionalI18nKey = "address.private")
    @GenericField(valueBridge = ValueBridgeRef(type = HibernateSearchPhoneNumberBridge::class))
    @get:Column(name = "private_mobile_phone", length = 255)
    open var privateMobilePhone: String? = null

    @PropertyInfo(i18nKey = "address.addressText", additionalI18nKey = "address.private")
    @FullTextField
    @get:Column(length = 255, name = "private_addresstext")
    open var privateAddressText: String? = null

    @PropertyInfo(i18nKey = "address.addressText2", additionalI18nKey = "address.private")
    @FullTextField
    @get:Column(length = 255, name = "private_addresstext2")
    open var privateAddressText2: String? = null

    @PropertyInfo(i18nKey = "address.zipCode", additionalI18nKey = "address.private")
    @FullTextField
    @get:Column(name = "private_zip_code", length = 255)
    open var privateZipCode: String? = null

    @PropertyInfo(i18nKey = "address.city", additionalI18nKey = "address.private")
    @FullTextField
    @get:Column(length = 255, name = "private_city")
    open var privateCity: String? = null

    @PropertyInfo(i18nKey = "address.country", additionalI18nKey = "address.private")
    @FullTextField
    @get:Column(name = "private_country", length = 255)
    open var privateCountry: String? = null

    @PropertyInfo(i18nKey = "address.state", additionalI18nKey = "address.private")
    @FullTextField
    @get:Column(name = "private_state", length = 255)
    open var privateState: String? = null

    @PropertyInfo(i18nKey = "email", additionalI18nKey = "address.private")
    @FullTextField
    @get:Column(length = 255, name = "private_email")
    open var privateEmail: String? = null

    @PropertyInfo(i18nKey = "address.publicKey")
    @FullTextField
    @get:Column(name = "public_key", length = 20000)
    open var publicKey: String? = null

    @PropertyInfo(i18nKey = "address.fingerprint")
    @FullTextField
    @get:Column(length = 255)
    open var fingerprint: String? = null

    @PropertyInfo(i18nKey = "comment")
    @FullTextField
    @get:Column(name = "comment", length = 5000)
    open var comment: String? = null

    @PropertyInfo(i18nKey = "address.birthday")
    //@FullTextField(index = Index.YES, analyze = Analyze.NO)
    @GenericField
    @get:Column
    open var birthday: LocalDate? = null

    /**
     * Time stamp of last image modification (or deletion). Useful for history of changes.
     */
    @get:Column(name = "image_last_update")
    open var imageLastUpdate: Date? = null

    @PropertyInfo(i18nKey = "address.addressbooks")
    @get:ManyToMany(fetch = FetchType.LAZY)
    @get:JoinTable(
        name = "t_addressbook_address",
        joinColumns = [JoinColumn(name = "address_id", referencedColumnName = "PK")],
        inverseJoinColumns = [JoinColumn(name = "addressbook_id", referencedColumnName = "PK")],
        indexes = [jakarta.persistence.Index(
            name = "idx_fk_t_addressbook_address_address_id",
            columnList = "address_id"
        ),
            jakarta.persistence.Index(
                name = "idx_fk_t_addressbook_address_addressbook_id",
                columnList = "addressbook_id"
            )]
    )
    @JsonSerialize(using = IdsOnlySerializer::class)
    open var addressbookList: MutableSet<AddressbookDO>? = null

    fun add(addressbook: AddressbookDO) {
        if (addressbookList == null) {
            addressbookList = mutableSetOf()
        }
        addressbookList!!.add(addressbook)
    }

    // @FieldBridge(impl = HibernateSearchInstantMessagingBridge.class)
    // @FullTextField(index = Index.YES /*TOKENIZED*/, store = Store.NO)
    // TODO: Prepared for hibernate search.
    private var instantMessaging: MutableList<LabelValueBean<InstantMessagingType, String>>? = null

    val fullName: String?
        @Transient
        get() = listOf(fullLastName, firstName, organization).filter { !it.isNullOrBlank() }.joinToString(", ")

    val fullLastName: String?
        @Transient
        get() = if (!birthName.isNullOrBlank()) {
            "$name, ${translate("address.formerly")} $birthName"
        } else {
            name
        }

    val fullNameWithTitleAndForm: String
        @Transient
        get() {
            val buf = StringBuilder()
            if (form != null) {
                buf.append(ThreadLocalUserContext.getLocalizedString(form!!.i18nKey)).append(" ")
            }
            if (title != null) {
                buf.append(title).append(" ")
            }
            if (firstName != null) {
                buf.append(firstName).append(" ")
            }
            if (fullLastName != null) {
                buf.append(fullLastName)
            }
            return buf.toString()
        }

    /**
     * @return address text of mailing address (in order: postal, default or private address).
     * @see .hasPostalAddress
     * @see .hasDefaultAddress
     */
    val mailingAddressText: String?
        @Transient
        get() = when {
            hasPostalAddress() -> postalAddressText
            hasDefaultAddress() -> addressText
            else -> privateAddressText
        }

    /**
     * @return address text of mailing address (in order: postal, default or private address).
     * @see .hasPostalAddress
     * @see .hasDefaultAddress
     */
    val mailingAddressText2: String?
        @Transient
        get() = when {
            hasPostalAddress() -> postalAddressText2
            hasDefaultAddress() -> addressText2
            else -> privateAddressText2
        }

    /**
     * @return zip code of mailing address (in order: postal, default or private address).
     * @see .hasPostalAddress
     * @see .hasDefaultAddress
     */
    val mailingZipCode: String?
        @Transient
        get() = when {
            hasPostalAddress() -> postalZipCode
            hasDefaultAddress() -> zipCode
            else -> privateZipCode
        }

    /**
     * @return city of mailing address (in order: postal, default or private address).
     * @see .hasPostalAddress
     * @see .hasDefaultAddress
     */
    val mailingCity: String?
        @Transient
        get() = when {
            hasPostalAddress() -> postalCity
            hasDefaultAddress() -> city
            else -> privateCity
        }

    /**
     * @return country of mailing address (in order: postal, default or private address).
     * @see .hasPostalAddress
     * @see .hasDefaultAddress
     */
    val mailingCountry: String?
        @Transient
        get() = when {
            hasPostalAddress() -> postalCountry
            hasDefaultAddress() -> country
            else -> privateCountry
        }

    /**
     * @return state of mailing address (in order: postal, default or private address).
     * @see .hasPostalAddress
     * @see .hasDefaultAddress
     */
    val mailingState: String?
        @Transient
        get() = when {
            hasPostalAddress() -> postalState
            hasDefaultAddress() -> state
            else -> privateState
        }

    /**
     * List of instant messaging contacts in the form of a property file: {skype=hugo.mustermann\naim=12345dse}. Only for
     * data base access, use getter an setter of instant messaging instead.
     *
     * @return
     */
    // @Column(name = "instant_messaging", length = 4000)
    // TODO: Prepared for data base persistence.
    var instantMessaging4DB: String?
        @Transient
        get() = getInstantMessagingAsString(instantMessaging)
        set(properties) = if (StringUtils.isBlank(properties)) {
            this.instantMessaging = null
        } else {
            val tokenizer = StringTokenizer(properties, "\n")
            while (tokenizer.hasMoreTokens()) {
                val line = tokenizer.nextToken()
                if (StringUtils.isBlank(line)) {
                    continue
                }
                val idx = line.indexOf('=')
                if (idx <= 0) {
                    log.error("Wrong instant messaging entry format in data base: $line")
                    continue
                }
                var label = line.substring(0, idx)
                val value = ""
                if (idx < line.length) {
                    label = line.substring(idx)
                }
                var type: InstantMessagingType?
                try {
                    type = InstantMessagingType.get(label)
                } catch (ex: Exception) {
                    log.error("Ignoring unknown Instant Messaging entry: $label", ex)
                    continue
                }

                setInstantMessaging(type, value)
            }
        }


    /**
     * @return true, if postal addressText, zip code, city or country is given.
     */
    @Transient
    fun hasPostalAddress(): Boolean {
        return StringHelper.isNotBlank(postalAddressText, postalAddressText2, postalZipCode, postalCity, postalCountry)
    }

    /**
     * @return true, if default addressText, zip code, city or country is given.
     */
    @Transient
    fun hasDefaultAddress(): Boolean {
        return StringHelper.isNotBlank(addressText, addressText2, zipCode, city, country)
    }

    /**
     * @return true, if private addressText, zip code, city or country is given.
     */
    @Transient
    fun hasPrivateAddress(): Boolean {
        return StringHelper.isNotBlank(
            privateAddressText,
            privateAddressText2,
            privateZipCode,
            privateCity,
            privateCountry
        )
    }

    /**
     * Instant messaging settings as property file.
     *
     * @return
     */
    @Transient
    fun getInstantMessaging(): List<LabelValueBean<InstantMessagingType, String>>? {
        return instantMessaging
    }

    fun setInstantMessaging(type: InstantMessagingType?, value: String) {
        if (this.instantMessaging == null) {
            this.instantMessaging = ArrayList()
        } else {
            for (entry in this.instantMessaging!!) {
                if (entry.label == type) {
                    // Entry found;
                    if (StringUtils.isBlank(value) == true) {
                        // Remove this entry:
                        this.instantMessaging!!.remove(entry)
                    } else {
                        // Modify existing entry:
                        entry.setValue(value)
                    }
                    return
                }
            }
        }
        this.instantMessaging!!.add(LabelValueBean<InstantMessagingType, String>(type, value))
    }

    companion object {
        /**
         * Used for representation in the data base and for hibernate search (lucene).
         */
        fun getInstantMessagingAsString(list: List<LabelValueBean<InstantMessagingType, String>>?): String? {
            if (list == null || list.isEmpty()) {
                return null
            }
            val buf = StringBuilder()
            var first = true
            for (lv in list) {
                if (StringUtils.isBlank(lv.value)) {
                    continue // Do not write empty entries.
                }
                if (first) {
                    first = false
                } else {
                    buf.append("\n")
                }
                buf.append(lv.label).append("=").append(lv.value)
            }
            return if (first) {
                null // No entry was written.
            } else buf.toString()
        }
    }
}
