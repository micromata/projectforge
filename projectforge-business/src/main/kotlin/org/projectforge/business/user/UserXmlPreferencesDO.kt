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

package org.projectforge.business.user

import de.micromata.genome.db.jpa.xmldump.api.JpaXmlPersist
import de.micromata.genome.jpa.DbRecord
import org.projectforge.framework.persistence.user.entities.PFUserDO
import org.projectforge.framework.persistence.user.entities.TenantDO

import javax.persistence.*
import java.io.Serializable
import java.util.Date

/**
 * For persistency of UserPreferencesData (stores them serialized).
 * The data are stored as xml.
 *
 * @author Kai Reinhard (k.reinhard@micromata.de)
 */
@Entity
@Table(name = "T_USER_XML_PREFS", uniqueConstraints = [UniqueConstraint(columnNames = ["user_id", "key", "tenant_id"])], indexes = [javax.persistence.Index(name = "idx_fk_t_user_xml_prefs_user_id", columnList = "user_id"), javax.persistence.Index(name = "idx_fk_t_user_xml_prefs_tenant_id", columnList = "tenant_id")])
@JpaXmlPersist(beforePersistListener = [UserXmlPreferenceXmlBeforePersistListener::class])
class UserXmlPreferencesDO : Serializable, DbRecord<Int> {

    @get:Id
    @get:GeneratedValue
    @get:Column(name = "pk")
    var id: Int? = null

    @get:ManyToOne(fetch = FetchType.LAZY)
    @get:JoinColumn(name = "tenant_id")
    var tenant: TenantDO? = null

    /**
     * The owner of this preference.
     */
    @get:ManyToOne(fetch = FetchType.LAZY)
    @get:JoinColumn(name = "user_id", nullable = false)
    var user: PFUserDO? = null

    /**
     * Contains the serialized settings, stored in the database.
     */
    @get:Column(length = MAX_SERIALIZED_LENGTH)
    var serializedSettings: String? = null

    /**
     * Optional if the user preference should be stored in its own data base entry.
     */
    @get:Column(length = 1000)
    var key: String? = null

    @get:Basic
    var created: Date? = null

    /**
     *
     * Last update will be modified automatically for every update of the database object.
     *
     * @return
     */
    @get:Basic
    @get:Column(name = "last_update")
    var lastUpdate: Date? = null

    /**
     * For migrating older entries the version for every entry is given.
     */
    @get:Column
    var version: Int = 0

    /**
     * @see org.projectforge.framework.persistence.api.BaseDO.getTenantId
     */
    val tenantId: Int?
        @Transient
        get() = if (tenant != null) tenant!!.id else null

    val userId: Int?
        @Transient
        get() = if (this.user == null) {
            null
        } else user!!.id

    fun setCreated() {
        this.created = Date()
    }

    fun setLastUpdate() {
        this.lastUpdate = Date()
    }

    /**
     * Sets CURRENT_VERSION as version.
     *
     * @see .CURRENT_VERSION
     *
     * @return this for chaining.
     */
    @Column
    fun setVersion(): UserXmlPreferencesDO {
        this.version = CURRENT_VERSION
        return this
    }

    @Transient
    override fun getPk(): Int? {
        return this.id
    }

    override fun setPk(pk: Int?) {
        this.id = pk
    }

    companion object {
        const val MAX_SERIALIZED_LENGTH = 10000

        /**
         * Don't forget to increase, if any changes in the object stored in user data are made. If not, the user preferences
         * will be lost because of unsupported (de)serialization.
         */
        const val CURRENT_VERSION = 4

        fun getCURRENT_VERSION(): Int {
            return CURRENT_VERSION
        }
    }
}
