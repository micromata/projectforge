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

import javax.persistence.Column
import javax.persistence.Entity
import javax.persistence.EnumType
import javax.persistence.Enumerated
import javax.persistence.FetchType
import javax.persistence.JoinColumn
import javax.persistence.ManyToOne
import javax.persistence.Table
import javax.persistence.Transient
import javax.persistence.UniqueConstraint

import org.apache.commons.lang3.builder.HashCodeBuilder
import org.hibernate.search.annotations.Field
import org.hibernate.search.annotations.Indexed
import org.projectforge.common.anots.PropertyInfo
import org.projectforge.framework.persistence.entities.DefaultBaseDO

/**
 * @author Werner Feder (werner.feder@t-online.de)
 */
@Entity
@Indexed
@Table(name = "T_CONTACTENTRY", uniqueConstraints = [UniqueConstraint(columnNames = ["contact_id", "number"])], indexes = [javax.persistence.Index(name = "idx_fk_t_contactentry_tenant_id", columnList = "tenant_id")])
class ContactEntryDO : DefaultBaseDO() {

    //private static final org.slf4j.Logger log = org.slf4j.LoggerFactory.getLogger(ContactEntryDO.class);

    @get:Column
    var number: Short = 0

    /**
     * Not used as object due to performance reasons.
     */
    @get:ManyToOne(fetch = FetchType.EAGER)
    @get:JoinColumn(name = "contact_id", nullable = false)
    var contact: ContactDO? = null

    @PropertyInfo(i18nKey = "contactType")
    @Enumerated(EnumType.STRING)
    @Field
    @get:Enumerated(EnumType.STRING)
    @get:Column(length = 15, name = "contact_type", nullable = false)
    var contactType: ContactType? = null // 15

    @PropertyInfo(i18nKey = "city")
    @Field
    @get:Column
    var city: String? = null

    @PropertyInfo(i18nKey = "country")
    @Field
    @get:Column
    var country: String? = null

    @PropertyInfo(i18nKey = "state")
    @Field
    @get:Column
    var state: String? = null

    @PropertyInfo(i18nKey = "street")
    @Field
    @get:Column
    var street: String? = null

    @PropertyInfo(i18nKey = "zipCode")
    @Field
    @get:Column
    var zipCode: String? = null

    val contactId: Int?
        @Transient
        get() = if (this.contact == null) null else contact!!.id


    override fun equals(other: Any?): Boolean {
        if (other is ContactEntryDO) {
            val o = other as ContactEntryDO?
            if (this.number != o!!.number) {
                return false
            }
            return this.contactId == o.contactId
        }
        return false
    }

    override fun hashCode(): Int {
        val hcb = HashCodeBuilder()
        hcb.append(number)
        if (contact != null) {
            hcb.append(contact!!.id)
        }
        return hcb.toHashCode()
    }
}
