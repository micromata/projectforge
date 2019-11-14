/////////////////////////////////////////////////////////////////////////////
//
// Project ProjectForge Community Edition
//         www.projectforge.org
//
// Copyright (C) 2001-2019 Micromata GmbH, Germany (www.micromata.com)
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

import de.micromata.genome.db.jpa.history.api.HistoryProperty
import de.micromata.genome.db.jpa.history.api.NoHistory
import de.micromata.genome.db.jpa.history.impl.TabAttrHistoryPropertyConverter
import de.micromata.genome.db.jpa.tabattr.entities.JpaTabAttrBaseDO
import de.micromata.genome.db.jpa.tabattr.entities.JpaTabAttrDataBaseDO
import org.apache.commons.lang3.StringUtils
import org.hibernate.annotations.Cache
import org.hibernate.annotations.CacheConcurrencyStrategy
import org.hibernate.search.annotations.*
import org.hibernate.search.annotations.Index
import org.projectforge.common.StringHelper
import org.projectforge.common.anots.PropertyInfo
import org.projectforge.framework.persistence.attr.entities.DefaultBaseWithAttrDO
import org.projectforge.framework.persistence.history.HibernateSearchPhoneNumberBridge
import org.projectforge.framework.persistence.user.api.ThreadLocalUserContext
import org.projectforge.framework.utils.LabelValueBean
import java.sql.Date
import java.util.*
import javax.persistence.*

/**
 * @author Kai Reinhard (k.reinhard@micromata.de)
 */
@Entity
@Indexed
@Table(name = "T_ADDRESS",
        uniqueConstraints = [UniqueConstraint(name = "unique_t_address_uid_tenant",
                columnNames = ["uid", "tenant_id"])],
        indexes = [javax.persistence.Index(name = "idx_fk_t_address_tenant_id",
                columnList = "tenant_id"), javax.persistence.Index(name = "idx_fk_t_address_uid_tenant_id",
                columnList = "uid, tenant_id")])
open class AddressDO : DefaultBaseWithAttrDO<AddressDO>() {

    @PropertyInfo(i18nKey = "address.contactStatus")
    @get:Enumerated(EnumType.STRING)
    @get:Column(name = "contact_status", length = 20, nullable = false)
    @Field
    open var contactStatus = ContactStatus.ACTIVE

    @PropertyInfo(i18nKey = "address.addressStatus")
    @get:Enumerated(EnumType.STRING)
    @get:Column(name = "address_status", length = 20, nullable = false)
    @Field
    open var addressStatus = AddressStatus.UPTODATE

    @get:Column(name = "uid")
    open var uid: String? = null

    @PropertyInfo(i18nKey = "name", required = true)
    @Field
    @get:Column(length = 255)
    open var name: String? = null

    @PropertyInfo(i18nKey = "firstName")
    @Field
    @get:Column(name = "first_name", length = 255)
    open var firstName: String? = null

    @PropertyInfo(i18nKey = "gender", required = true)
    @Field
    @get:Enumerated(EnumType.STRING)
    @get:Column(name = "form", length = 10)
    open var form: FormOfAddress? = null

    @PropertyInfo(i18nKey = "address.title")
    @Field
    @get:Column(length = 255)
    open var title: String? = null

    @PropertyInfo(i18nKey = "address.positionText")
    @Field
    @get:Column(length = 255)
    open var positionText: String? = null

    @PropertyInfo(i18nKey = "organization")
    @Field
    @get:Column(length = 255)
    open var organization: String? = null

    @PropertyInfo(i18nKey = "address.division")
    @Field
    @get:Column(length = 255)
    open var division: String? = null

    @PropertyInfo(i18nKey = "address.phone", additionalI18nKey = "address.business")
    @FieldBridge(impl = HibernateSearchPhoneNumberBridge::class)
    @Field
    @get:Column(name = "business_phone", length = 255)
    open var businessPhone: String? = null

    @PropertyInfo(i18nKey = "address.phoneType.mobile", additionalI18nKey = "address.business")
    @FieldBridge(impl = HibernateSearchPhoneNumberBridge::class)
    @Field
    @get:Column(name = "mobile_phone", length = 255)
    open var mobilePhone: String? = null

    @PropertyInfo(i18nKey = "address.phoneType.fax", additionalI18nKey = "address.business")
    @FieldBridge(impl = HibernateSearchPhoneNumberBridge::class)
    @Field
    @get:Column(length = 255)
    open var fax: String? = null

    @PropertyInfo(i18nKey = "address.addressText", additionalI18nKey = "address.business")
    @Field
    @get:Column(length = 255)
    open var addressText: String? = null

    @PropertyInfo(i18nKey = "address.zipCode", additionalI18nKey = "address.business")
    @Field
    @get:Column(name = "zip_code", length = 255)
    open var zipCode: String? = null

    @PropertyInfo(i18nKey = "address.city", additionalI18nKey = "address.business")
    @Field
    @get:Column(length = 255)
    open var city: String? = null

    @PropertyInfo(i18nKey = "address.country", additionalI18nKey = "address.business")
    @Field
    @get:Column(length = 255)
    open var country: String? = null

    @PropertyInfo(i18nKey = "address.state", additionalI18nKey = "address.business")
    @Field
    @get:Column(length = 255)
    open var state: String? = null

    @PropertyInfo(i18nKey = "email", additionalI18nKey = "address.business")
    @Field
    @get:Column(length = 255)
    open var email: String? = null

    @PropertyInfo(i18nKey = "address.addressText", additionalI18nKey = "address.postal")
    @Field
    @get:Column(length = 255, name = "postal_addresstext")
    open var postalAddressText: String? = null

    @PropertyInfo(i18nKey = "address.zipCode", additionalI18nKey = "address.postal")
    @Field
    @get:Column(name = "postal_zip_code", length = 255)
    open var postalZipCode: String? = null

    @PropertyInfo(i18nKey = "address.city", additionalI18nKey = "address.postal")
    @Field
    @get:Column(length = 255, name = "postal_city")
    open var postalCity: String? = null

    @PropertyInfo(i18nKey = "address.country", additionalI18nKey = "address.postal")
    @Field
    @get:Column(name = "postal_country", length = 255)
    open var postalCountry: String? = null

    @PropertyInfo(i18nKey = "address.state", additionalI18nKey = "address.postal")
    @Field
    @get:Column(name = "postal_state", length = 255)
    open var postalState: String? = null

    @PropertyInfo(i18nKey = "address.website")
    @Field
    @get:Column(length = 255)
    open var website: String? = null

    /**
     * @return The communication will take place in this language.
     */
    @PropertyInfo(i18nKey = "address.communicationLanguage")
    @get:Column(name = "communication_language")
    open var communicationLanguage: Locale? = null

    @PropertyInfo(i18nKey = "address.phone", additionalI18nKey = "address.private")
    @FieldBridge(impl = HibernateSearchPhoneNumberBridge::class)
    @Field
    @get:Column(name = "private_phone", length = 255)
    open var privatePhone: String? = null

    @PropertyInfo(i18nKey = "address.phoneType.mobile", additionalI18nKey = "address.private")
    @FieldBridge(impl = HibernateSearchPhoneNumberBridge::class)
    @Field
    @get:Column(name = "private_mobile_phone", length = 255)
    open var privateMobilePhone: String? = null

    @PropertyInfo(i18nKey = "address.addressText", additionalI18nKey = "address.private")
    @Field
    @get:Column(length = 255, name = "private_addresstext")
    open var privateAddressText: String? = null

    @PropertyInfo(i18nKey = "address.zipCode", additionalI18nKey = "address.private")
    @Field
    @get:Column(name = "private_zip_code", length = 255)
    open var privateZipCode: String? = null

    @PropertyInfo(i18nKey = "address.city", additionalI18nKey = "address.private")
    @Field
    @get:Column(length = 255, name = "private_city")
    open var privateCity: String? = null

    @PropertyInfo(i18nKey = "address.country", additionalI18nKey = "address.private")
    @Field
    @get:Column(name = "private_country", length = 255)
    open var privateCountry: String? = null

    @PropertyInfo(i18nKey = "address.state", additionalI18nKey = "address.private")
    @Field
    @get:Column(name = "private_state", length = 255)
    open var privateState: String? = null

    @PropertyInfo(i18nKey = "email", additionalI18nKey = "address.private")
    @Field
    @get:Column(length = 255, name = "private_email")
    open var privateEmail: String? = null

    @PropertyInfo(i18nKey = "address.publicKey")
    @Field
    @get:Column(name = "public_key", length = 20000)
    open var publicKey: String? = null

    @PropertyInfo(i18nKey = "address.fingerprint")
    @Field
    @get:Column(length = 255)
    open var fingerprint: String? = null

    @PropertyInfo(i18nKey = "comment")
    @Field
    @get:Column(name = "comment", length = 5000)
    open var comment: String? = null

    @PropertyInfo(i18nKey = "address.birthday")
    @Field(index = Index.YES, analyze = Analyze.NO)
    @DateBridge(resolution = Resolution.DAY, encoding = EncodingType.STRING)
    @get:Column
    open var birthday: Date? = null

    @PropertyInfo(i18nKey = "address.image")
    @field:NoHistory
    @get:Column
    open var imageData: ByteArray? = null

    @PropertyInfo(i18nKey = "address.image")
    @field:NoHistory
    @get:Column(name = "image_data_preview")
    open var imageDataPreview: ByteArray? = null

    /**
     * The substitutions.
     */
    @PropertyInfo(i18nKey = "address.addressbooks")
    @get:ManyToMany(fetch = FetchType.LAZY)
    @get:JoinTable(name = "t_addressbook_address",
            joinColumns = [JoinColumn(name = "address_id", referencedColumnName = "PK")],
            inverseJoinColumns = [JoinColumn(name = "addressbook_id", referencedColumnName = "PK")],
            indexes = [javax.persistence.Index(name = "idx_fk_t_addressbook_address_address_id", columnList = "address_id"),
                javax.persistence.Index(name = "idx_fk_t_addressbook_address_addressbook_id", columnList = "addressbook_id")])
    open var addressbookList: MutableSet<AddressbookDO>? = HashSet()

    // @FieldBridge(impl = HibernateSearchInstantMessagingBridge.class)
    // @Field(index = Index.YES /*TOKENIZED*/, store = Store.NO)
    // TODO: Prepared for hibernate search.
    private var instantMessaging: MutableList<LabelValueBean<InstantMessagingType, String>>? = null

    val fullName: String?
        @Transient
        get() = StringHelper.listToString(", ", name, firstName)

    val fullNameWithTitleAndForm: String
        @Transient
        get() {
            val buf = StringBuffer()
            if (form != null) {
                buf.append(ThreadLocalUserContext.getLocalizedString(form!!.i18nKey)).append(" ")
            }
            if (title != null) {
                buf.append(title).append(" ")
            }
            if (firstName != null) {
                buf.append(firstName).append(" ")
            }
            if (name != null) {
                buf.append(name)
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
        get() = if (hasPostalAddress() == true) {
            postalAddressText
        } else if (hasDefaultAddress() == true) {
            addressText
        } else {
            privateAddressText
        }

    /**
     * @return zip code of mailing address (in order: postal, default or private address).
     * @see .hasPostalAddress
     * @see .hasDefaultAddress
     */
    val mailingZipCode: String?
        @Transient
        get() = if (hasPostalAddress() == true) {
            postalZipCode
        } else if (hasDefaultAddress() == true) {
            zipCode
        } else {
            privateZipCode
        }

    /**
     * @return city of mailing address (in order: postal, default or private address).
     * @see .hasPostalAddress
     * @see .hasDefaultAddress
     */
    val mailingCity: String?
        @Transient
        get() = if (hasPostalAddress() == true) {
            postalCity
        } else if (hasDefaultAddress() == true) {
            city
        } else {
            privateCity
        }

    /**
     * @return country of mailing address (in order: postal, default or private address).
     * @see .hasPostalAddress
     * @see .hasDefaultAddress
     */
    val mailingCountry: String?
        @Transient
        get() = if (hasPostalAddress() == true) {
            postalCountry
        } else if (hasDefaultAddress() == true) {
            country
        } else {
            privateCountry
        }

    /**
     * @return state of mailing address (in order: postal, default or private address).
     * @see .hasPostalAddress
     * @see .hasDefaultAddress
     */
    val mailingState: String?
        @Transient
        get() = if (hasPostalAddress()) {
            postalState
        } else if (hasDefaultAddress()) {
            state
        } else {
            privateState
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
        return StringHelper.isNotBlank(postalAddressText, postalZipCode, postalCity, postalCountry)
    }

    /**
     * @return true, if default addressText, zip code, city or country is given.
     */
    @Transient
    fun hasDefaultAddress(): Boolean {
        return StringHelper.isNotBlank(addressText, zipCode, city, country)
    }

    /**
     * @return true, if private addressText, zip code, city or country is given.
     */
    @Transient
    fun hasPrivateAddress(): Boolean {
        return StringHelper.isNotBlank(privateAddressText, privateZipCode, privateCity, privateCountry)
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

    /**
     * @see org.projectforge.framework.persistence.attr.entities.DefaultBaseWithAttrDO.getAttrEntityClass
     */
    @Transient
    override fun getAttrEntityClass(): Class<out JpaTabAttrBaseDO<AddressDO, Int>> {
        return AddressAttrDO::class.java
    }

    /**
     * @see org.projectforge.framework.persistence.attr.entities.DefaultBaseWithAttrDO.getAttrEntityWithDataClass
     */
    @Transient
    override fun getAttrEntityWithDataClass(): Class<out JpaTabAttrBaseDO<AddressDO, Int>> {
        return AddressAttrWithDataDO::class.java
    }

    /**
     * @see org.projectforge.framework.persistence.attr.entities.DefaultBaseWithAttrDO.getAttrDataEntityClass
     */
    @Transient
    override fun getAttrDataEntityClass(): Class<out JpaTabAttrDataBaseDO<out JpaTabAttrBaseDO<AddressDO, Int>, Int>> {
        return AddressAttrDataDO::class.java
    }

    /**
     * @see org.projectforge.framework.persistence.attr.entities.DefaultBaseWithAttrDO.createAttrEntity
     */
    override fun createAttrEntity(key: String, type: Char, value: String): JpaTabAttrBaseDO<AddressDO, Int> {
        return AddressAttrDO(this, key, type, value)
    }

    /**
     * @see org.projectforge.framework.persistence.attr.entities.DefaultBaseWithAttrDO.createAttrEntityWithData
     */
    override fun createAttrEntityWithData(key: String, type: Char, value: String): JpaTabAttrBaseDO<AddressDO, Int> {
        return AddressAttrWithDataDO(this, key, type, value)
    }

    @OneToMany(cascade = [CascadeType.ALL], mappedBy = "parent", targetEntity = AddressAttrDO::class, orphanRemoval = true, fetch = FetchType.LAZY)
    @MapKey(name = "propertyName")
    @HistoryProperty(converter = TabAttrHistoryPropertyConverter::class)
    override fun getAttrs(): Map<String, JpaTabAttrBaseDO<AddressDO, Int>> {
        return super.getAttrs()
    }

    companion object {
        private val log = org.slf4j.LoggerFactory.getLogger(AddressDO::class.java)

        /**
         * Used for representation in the data base and for hibernate search (lucene).
         */
        fun getInstantMessagingAsString(list: List<LabelValueBean<InstantMessagingType, String>>?): String? {
            if (list == null || list.isEmpty()) {
                return null
            }
            val buf = StringBuffer()
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
            return if (first == true) {
                null // No entry was written.
            } else buf.toString()
        }
    }
}
