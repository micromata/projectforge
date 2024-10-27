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

package org.projectforge.business.user

import org.projectforge.framework.persistence.user.entities.PFUserDO
import java.util.*
import jakarta.persistence.*

/**
 * For persistency of UserPreferencesData (stores them serialized).
 * The data are stored as xml.
 *
 * @author Kai Reinhard (k.reinhard@micromata.de)
 */
@Entity
@Table(name = "T_USER_XML_PREFS", uniqueConstraints = [UniqueConstraint(columnNames = ["user_id", "key"])], indexes = [Index(name = "idx_fk_t_user_xml_prefs_user_id", columnList = "user_id")])
class UserXmlPreferencesDO : IUserPref {

    @get:Id
    @get:GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "hibernate_sequence")
    @get:Column(name = "pk")
    override var id: Long? = null

    /**
     * The owner of this preference.
     */
    @get:ManyToOne(fetch = FetchType.LAZY)
    @get:JoinColumn(name = "user_id", nullable = false)
    override var user: PFUserDO? = null

    /**
     * Contains the serialized settings, stored in the database.
     */
    @get:Column(length = MAX_SERIALIZED_LENGTH)
    override var serializedValue: String? = null

    /**
     * Not in use. area is global for entries per user.
     */
    @get:Transient
    override var area: String? = null

    override var identifier: String?
        get() = key
        set(value) {
            key = value
        }

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

    override fun equals(other: Any?): Boolean {
        return IUserPref.equals(this, other)
    }

    override fun hashCode(): Int {
        return IUserPref.hashCode()
    }

    companion object {
        const val MAX_SERIALIZED_LENGTH = 10000

        /**
         * Don't forget to increase, if any changes in the object stored in user data are made. If not, the user preferences
         * will be lost because of unsupported (de)serialization.
         */
        const val CURRENT_VERSION = 4

        fun getCurrentVersion(): Int {
            return CURRENT_VERSION
        }
    }
}
