/////////////////////////////////////////////////////////////////////////////
//
// Project ProjectForge Community Edition
//         www.projectforge.org
//
// Copyright (C) 2001-2024 Micromata GmbH, Germany (www.micromata.com)
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

import jakarta.persistence.*
import org.hibernate.annotations.JdbcTypeCode
import org.hibernate.search.mapper.pojo.bridge.mapping.annotation.TypeBinderRef
import org.hibernate.search.mapper.pojo.mapping.definition.annotation.FullTextField
import org.hibernate.search.mapper.pojo.mapping.definition.annotation.GenericField
import org.hibernate.search.mapper.pojo.mapping.definition.annotation.Indexed
import org.hibernate.search.mapper.pojo.mapping.definition.annotation.TypeBinding
import org.hibernate.type.SqlTypes
import org.projectforge.Constants
import org.projectforge.business.teamcal.admin.model.HibernateSearchUsersGroupsTypeBinder
import org.projectforge.common.anots.PropertyInfo
import org.projectforge.framework.persistence.entities.DefaultBaseDO
import org.projectforge.framework.persistence.history.NoHistory
import org.projectforge.framework.persistence.utils.ReflectionToString
import java.time.LocalDate

/**
 * @author Kai Reinhard (k.reinhard@micromata.de)
 */
@Entity
@Indexed
// TODO: Migration of ClassBridge to TypeBridge.
//@TypeBinding(binder = TypeBinderRef(type = HibernateSearchUsersGroupsTypeBinder::class))
//@ClassBridge(name = "owners", impl = HibernateSearchUsersBridge::class)
@Table(name = "T_PLUGIN_LM_LICENSE")
open class LicenseDO : DefaultBaseDO() {

    @PropertyInfo(i18nKey = "organization")
    @FullTextField
    @get:Column(length = 1000)
    open var organization: String? = null

    @PropertyInfo(i18nKey = "plugins.licensemanagement.product")
    @FullTextField
    @get:Column(length = 1000)
    open var product: String? = null

    @PropertyInfo(i18nKey = "plugins.licensemanagement.version")
    @FullTextField
    @get:Column(length = 1000)
    open var version: String? = null

    @PropertyInfo(i18nKey = "plugins.licensemanagement.updateFromVersion")
    @FullTextField
    @get:Column(name = "update_from_version", length = 1000)
    open var updateFromVersion: String? = null

    @PropertyInfo(i18nKey = "plugins.licensemanagement.licenseHolder")
    @FullTextField
    @get:Column(length = 10000, name = "license_holder")
    open var licenseHolder: String? = null

    @PropertyInfo(i18nKey = "plugins.licensemanagement.key")
    @FullTextField
    @get:Column(length = 10000)
    open var key: String? = null

    @PropertyInfo(i18nKey = "plugins.licensemanagement.numberOfLicenses")
    @get:Column(name = "number_of_licenses")
    open var numberOfLicenses: Int? = null

    /**
     * Comma separated id's of owners (user id's).
     */
    @PropertyInfo(i18nKey = "plugins.licensemanagement.owner")
    @get:Column(length = 4000)
    open var ownerIds: String? = null

    @PropertyInfo(i18nKey = "plugins.licensemanagement.device")
    @FullTextField
    @get:Column(length = 4000)
    open var device: String? = null

    @PropertyInfo(i18nKey = "comment")
    @FullTextField
    @get:Column(length = Constants.LENGTH_TEXT)
    open var comment: String? = null

    @GenericField
    @get:Enumerated(EnumType.STRING)
    @get:Column(length = 20)
    open var status: LicenseStatus? = null

    @PropertyInfo(i18nKey = "plugins.licensemanagement.validSince")
    @get:Column(name = "valid_since")
    open var validSince: LocalDate? = null

    @PropertyInfo(i18nKey = "plugins.licensemanagement.validUntil")
    @get:Column(name = "valid_until")
    open var validUntil: LocalDate? = null

    @field:NoHistory
    @get:Basic(fetch = FetchType.LAZY)
    @get:Column(name = "file1")
    @JdbcTypeCode(SqlTypes.BLOB)
    open var file1: ByteArray? = null

    @FullTextField
    @get:Column(name = "file_name1", length = 255)
    open var filename1: String? = null

    @field:NoHistory
    @get:Basic(fetch = FetchType.LAZY)
    @get:Column(name = "file2")
    @JdbcTypeCode(SqlTypes.BLOB)
    open var file2: ByteArray? = null

    @FullTextField
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
