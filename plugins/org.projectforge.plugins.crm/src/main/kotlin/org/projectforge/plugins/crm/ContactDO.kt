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

package org.projectforge.plugins.crm

import org.apache.commons.lang3.StringUtils
import org.hibernate.annotations.ListIndexBase
import org.hibernate.search.annotations.*
import org.projectforge.business.address.AddressStatus
import org.projectforge.business.address.ContactStatus
import org.projectforge.business.address.FormOfAddress
import org.projectforge.business.address.InstantMessagingType
import org.projectforge.business.task.TaskDO
import org.projectforge.common.StringHelper
import org.projectforge.common.anots.PropertyInfo
import org.projectforge.framework.persistence.api.PFPersistancyBehavior
import org.projectforge.framework.persistence.entities.DefaultBaseDO
import org.projectforge.framework.persistence.user.api.ThreadLocalUserContext
import org.projectforge.framework.utils.LabelValueBean
import java.sql.Date
import java.util.*
import javax.persistence.*

/**
 * @author Werner Feder (werner.feder@t-online.de)
 */
@Entity
@Indexed
@Table(name = "T_CONTACT", indexes = [javax.persistence.Index(name = "idx_fk_t_contact_tenant_id", columnList = "tenant_id")])
class ContactDO : DefaultBaseDO() {

    private val log = org.slf4j.LoggerFactory.getLogger(ContactDO::class.java)

    /**
     * Not used as object due to performance reasons.
     */
    @get:ManyToOne(fetch = FetchType.LAZY)
    @get:JoinColumn(name = "task_id")
    var task: TaskDO? = null

    @PropertyInfo(i18nKey = "name")
    @Field
    @get:Column(length = 255, nullable = false)
    var name: String? = null // 255 not null

    @PropertyInfo(i18nKey = "firstName")
    @Field
    @get:Column(name = "first_name", length = 255)
    var firstName: String? = null // 255

    @PropertyInfo(i18nKey = "form")
    @Field
    @get:Enumerated(EnumType.STRING)
    @get:Column(name = "form", length = 10)
    var form: FormOfAddress? = null

    @PropertyInfo(i18nKey = "title")
    @Field
    @get:Column(length = 255)
    var title: String? = null // 255

    @PropertyInfo(i18nKey = "birthday")
    @Field(analyze = Analyze.NO)
    @DateBridge(resolution = Resolution.DAY, encoding = EncodingType.STRING)
    @get:Column
    var birthday: Date? = null

    @PropertyInfo(i18nKey = "contact.imValues")
    @Field
    @get:Column
    var socialMediaValues: String? = null

    @PropertyInfo(i18nKey = "contact.emailValues")
    @Field
    @get:Column
    var emailValues: String? = null

    @PropertyInfo(i18nKey = "contact.phoneValues")
    @Field
    @get:Column
    var phoneValues: String? = null

    @PropertyInfo(i18nKey = "contact.contacts")
    @PFPersistancyBehavior(autoUpdateCollectionEntries = true)
    @IndexedEmbedded(depth = 1)
    @get:OneToMany(cascade = [CascadeType.ALL], fetch = FetchType.EAGER, orphanRemoval = true, mappedBy = "contact", targetEntity = ContactEntryDO::class)
    @get:OrderColumn(name = "number")
    @get:ListIndexBase(1)
    var contactEntries: MutableSet<ContactEntryDO>? = null

    @get:Enumerated(EnumType.STRING)
    @get:Column(name = "contact_status", length = 20, nullable = false)
    var contactStatus = ContactStatus.ACTIVE

    @get:Enumerated(EnumType.STRING)
    @get:Column(name = "address_status", length = 20, nullable = false)
    var addressStatus = AddressStatus.UPTODATE

    @Field
    @get:Column(name = "public_key", length = 7000)
    var publicKey: String? = null // 7000

    @Field
    @get:Column(length = 255)
    var fingerprint: String? = null // 255

    /**
     * The communication will take place in this language.
     */
    @get:Column(name = "communication_language")
    var communicationLanguage: Locale? = null

    @Field
    @get:Column(length = 255)
    var website: String? = null // 255

    @Field
    @get:Column(length = 255)
    var organization: String? = null // 255

    @Field
    @get:Column(length = 255)
    var division: String? = null // 255

    @Field
    @get:Column(length = 255)
    var positionText: String? = null // 255

    @Field
    @get:Column(name = "comment", length = 5000)
    var comment: String? = null // 5000;

    /**
     * Instant messaging settings as property file.
     */
    // @FieldBridge(impl = HibernateSearchInstantMessagingBridge.class)
    // @Field(index = Index.YES /*TOKENIZED*/, store = Store.NO)
    // TODO: Prepared for hibernate search.
    @get:Transient
    var socialMedia: MutableList<LabelValueBean<InstantMessagingType, String>>? = null

    /**
     * List of instant messaging contacts in the form of a property file: {skype=hugo.mustermann\naim=12345dse}. Only for
     * data base access, use getter an setter of instant messaging instead.
     *
     * @return
     */
    // @Column(name = "instant_messaging", length = 4000)
    // TODO: Prepared for data base persistence.
    var socialMedia4DB: String?
        @Transient
        get() = getSocialMediaAsString(socialMedia)
        set(properties) = if (StringUtils.isBlank(properties)) {
            this.socialMedia = null
        } else {
            val tokenizer = StringTokenizer(properties, "\n")
            while (tokenizer.hasMoreTokens()) {
                val line = tokenizer.nextToken()
                if (StringUtils.isBlank(line)) {
                    continue
                }
                val idx = line.indexOf('=')
                if (idx <= 0) {
                    log.error("Wrong social media entry format in data base: $line")
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
                    log.error("Ignoring unknown social media entry: $label", ex)
                    continue
                }

                setSocialMedia(type, value)
            }
        }

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

    val taskId: Int?
        @Transient
        get() = if (this.task == null) {
            null
        } else task!!.id

    fun setSocialMedia(type: InstantMessagingType?, value: String) {
        if (this.socialMedia == null) {
            this.socialMedia = ArrayList()
        } else {
            for (entry in this.socialMedia!!) {
                if (entry.label == type) {
                    // Entry found;
                    if (StringUtils.isBlank(value)) {
                        // Remove this entry:
                        this.socialMedia!!.remove(entry)
                    } else {
                        // Modify existing entry:
                        entry.setValue(value)
                    }
                    return
                }
            }
        }
        this.socialMedia!!.add(LabelValueBean<InstantMessagingType, String>(type, value))
    }

    /**
     * @param number
     * @return ContactEntryDO with given position number or null (iterates through the list of contacts and compares the
     * number), if not exist.
     */
    fun getContactEntry(number: Short): ContactEntryDO? {
        if (contactEntries == null) {
            return null
        }
        for (contact in this.contactEntries!!) {
            if (contact.number == number) {
                return contact
            }
        }
        return null
    }

    fun addContactEntry(contactEntry: ContactEntryDO): ContactDO {
        ensureAndGetContactEntries()
        var number: Short = 1
        for (pos in contactEntries!!) {
            if (pos.number >= number) {
                number = pos.number
                number++
            }
        }
        contactEntry.number = number
        contactEntry.contact = this
        this.contactEntries!!.add(contactEntry)
        return this
    }

    fun ensureAndGetContactEntries(): Set<ContactEntryDO>? {
        if (this.contactEntries == null) {
            this.contactEntries = LinkedHashSet()
        }
        return this.contactEntries
    }

    companion object {
        /**
         * Used for representation in the data base and for hibernate search (lucene).
         */
        internal fun getSocialMediaAsString(list: List<LabelValueBean<InstantMessagingType, String>>?): String? {
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
            return if (first) {
                null // No entry was written.
            } else buf.toString()
        }
    }

}
