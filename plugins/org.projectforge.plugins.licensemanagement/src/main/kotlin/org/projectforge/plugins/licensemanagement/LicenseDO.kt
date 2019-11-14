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

package org.projectforge.plugins.licensemanagement

import de.micromata.genome.db.jpa.history.api.NoHistory
import org.hibernate.annotations.Type
import org.hibernate.search.annotations.*
import org.projectforge.common.anots.PropertyInfo
import org.projectforge.framework.persistence.api.Constants
import org.projectforge.framework.persistence.entities.DefaultBaseDO
import org.projectforge.framework.persistence.utils.ReflectionToString
import java.sql.Date
import javax.persistence.*

/**
 * @author Kai Reinhard (k.reinhard@micromata.de)
 */
@Entity
@Indexed
@ClassBridge(name = "owners", impl = HibernateSearchUsersBridge::class)
@Table(name = "T_PLUGIN_LM_LICENSE", indexes = [javax.persistence.Index(name = "idx_fk_t_plugin_lm_license_tenant_id", columnList = "tenant_id")])
open class LicenseDO : DefaultBaseDO() {

    @PropertyInfo(i18nKey = "organization")
    @Field
    @get:Column(length = 1000)
    open var organization: String? = null

    @PropertyInfo(i18nKey = "plugins.licensemanagement.product")
    @Field
    @get:Column(length = 1000)
    open var product: String? = null

    @PropertyInfo(i18nKey = "plugins.licensemanagement.version")
    @Field
    @get:Column(length = 1000)
    open var version: String? = null

    @PropertyInfo(i18nKey = "plugins.licensemanagement.updateFromVersion")
    @Field
    @get:Column(name = "update_from_version", length = 1000)
    open var updateFromVersion: String? = null

    @PropertyInfo(i18nKey = "plugins.licensemanagement.licenseHolder")
    @Field
    @get:Column(length = 10000, name = "license_holder")
    open var licenseHolder: String? = null

    @PropertyInfo(i18nKey = "plugins.licensemanagement.key")
    @Field
    @get:Column(length = 10000)
    open var key: String? = null

    @PropertyInfo(i18nKey = "plugins.licensemanagement.numberOfLicenses")
    @Field
    @get:Column(name = "number_of_licenses")
    open var numberOfLicenses: Int? = null

    /**
     * Comma separated id's of owners (user id's).
     */
    @PropertyInfo(i18nKey = "plugins.licensemanagement.owner")
    @get:Column(length = 4000)
    open var ownerIds: String? = null

    @PropertyInfo(i18nKey = "plugins.licensemanagement.device")
    @Field
    @get:Column(length = 4000)
    open var device: String? = null

    @PropertyInfo(i18nKey = "comment")
    @Field
    @get:Column(length = Constants.LENGTH_TEXT)
    open var comment: String? = null

    @Field(analyze = Analyze.NO)
    @get:Enumerated(EnumType.STRING)
    @get:Column(length = 20)
    open var status: LicenseStatus? = null

    @PropertyInfo(i18nKey = "plugins.licensemanagement.validSince")
    @DateBridge(resolution = Resolution.DAY, encoding = EncodingType.STRING)
    @get:Column(name = "valid_since")
    open var validSince: Date? = null

    @PropertyInfo(i18nKey = "plugins.licensemanagement.validUntil")
    @DateBridge(resolution = Resolution.DAY, encoding = EncodingType.STRING)
    @get:Column(name = "valid_until")
    open var validUntil: Date? = null

    @NoHistory
    @get:Basic(fetch = FetchType.LAZY)
    @get:Column(name = "file1")
    @get:Type(type = "binary")
    open var file1: ByteArray? = null

    @Field
    @get:Column(name = "file_name1", length = 255)
    open var filename1: String? = null

    @NoHistory
    @get:Basic(fetch = FetchType.LAZY)
    @get:Column(name = "file2")
    @get:Type(type = "binary")
    open var file2: ByteArray? = null

    @Field
    @get:Column(name = "file_name2", length = 255)
    open var filename2: String? = null

    val orderString: String
        @Transient
        get() = "$organization-$product-$version"

    /**
     * Returns string containing all fields (except the file1/file2) of given object (via ReflectionToStringBuilder).
     *
     * @param user
     * @return
     */
    override fun toString(): String {
        return object : ReflectionToString(this) {
            override fun accept(f: java.lang.reflect.Field): Boolean {
                return super.accept(f) && "file1" != f.name && "file2" != f.name
            }
        }.toString()
    }
}
