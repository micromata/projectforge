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

@file:Suppress("DEPRECATION")

package org.projectforge.framework.persistence.user.entities

import de.micromata.genome.db.jpa.xmldump.api.JpaXmlPersist
import org.hibernate.search.annotations.Field
import org.hibernate.search.annotations.Indexed
import org.hibernate.search.annotations.IndexedEmbedded
import org.projectforge.business.user.UserPrefAreaRegistry
import org.projectforge.common.StringHelper
import org.projectforge.framework.persistence.api.BaseDO
import org.projectforge.framework.persistence.api.ModificationStatus
import org.projectforge.framework.persistence.entities.AbstractBaseDO
import org.projectforge.framework.persistence.user.api.UserPrefArea
import java.io.Serializable
import java.util.*
import javax.persistence.*

/**
 * Stores preferences of the user for any objects such as list filters or templates for adding new objects (time sheets
 * etc.).
 *
 * @author Kai Reinhard (k.reinhard@micromata.de)
 */
@Entity
@Indexed
@Table(name = "T_USER_PREF",
        uniqueConstraints = [UniqueConstraint(columnNames = ["user_fk", "area", "name", "tenant_id"])],
        indexes = [Index(name = "idx_fk_t_user_pref_user_fk", columnList = "user_fk"), Index(name = "idx_fk_t_user_pref_tenant_id", columnList = "tenant_id")])
@JpaXmlPersist(beforePersistListener = [UserPrefXmlBeforePersistListener::class])
class UserPrefDO : AbstractBaseDO<Int>() {

    @IndexedEmbedded(depth = 1)
    @get:ManyToOne(fetch = FetchType.LAZY)
    @get:JoinColumn(name = "user_fk", nullable = false)
    var user: PFUserDO? = null

    @Field
    @get:Column(length = 255)
    var name: String? = null

    @get:Transient
    var areaObject: UserPrefArea? = null // 20;
        get() {
            if (field == null) {
                field = UserPrefAreaRegistry.instance().getEntry(this.area)
            }
            return field
        }
        set(area) {
            field = area
            this.area = area?.id
        }

    @get:Deprecated("Use value with json serialization instead.")
    @get:OneToMany(cascade = [CascadeType.ALL], fetch = FetchType.EAGER, orphanRemoval = true)
    @get:JoinColumn(name = "user_pref_fk")
    @set:Deprecated("Use value with json serialization instead.")
    var userPrefEntries: MutableSet<UserPrefEntryDO>? = null

    private var id: Int? = null

    /**
     * The value as string representation (e. g. json).
     */
    @get:Column(name = "value_string", length = 100000) // 100.000, should be space enough.
    var valueString: String? = null

    /**
     * The type of the value (class name). It's not of type class because types are may-be refactored or removed.
     */
    @get:Column(name = "value_type", length = 1000)
    var valueTypeString: String? = null

    /**
     * [valueTypeString] as class or null, if [valueTypeString] is null.
     */
    val valueType: Class<*>?
        @Transient
        get() =
            if (valueTypeString.isNullOrBlank())
                null
            else Class.forName(valueTypeString)

    /**
     * The value as object (deserialized from json).
     */
    @get:Transient
    var valueObject: Any? = null

    @get:Transient
    val getIntValue: Int?
        get() = valueString?.toInt()

    /**
     * User pref's ar
     */
    @get:Column(length = UserPrefArea.MAX_ID_LENGTH, nullable = false)
    var area: String? = null

    val sortedUserPrefEntries: Set<UserPrefEntryDO>
        @Transient
        get() {
            val result = TreeSet(Comparator<UserPrefEntryDO> { o1, o2 -> StringHelper.compareTo(o1.orderString, o2.orderString) })
            result.addAll(this.userPrefEntries!!)
            return result
        }

    @Id
    @GeneratedValue
    @Column(name = "pk")
    override fun getId(): Int? {
        return id
    }

    override fun setId(id: Int?) {
        this.id = id
    }

    @Deprecated("Use value with json serialization instead.")
    fun addUserPrefEntry(userPrefEntry: UserPrefEntryDO) {
        if (this.userPrefEntries == null) {
            this.userPrefEntries = HashSet()
        }
        this.userPrefEntries!!.add(userPrefEntry)
    }

    /**
     * Copies all values from the given src object excluding the values created and modified. Null values will be
     * excluded.
     */
    @Deprecated("Use value with json serialization instead.")
    override fun copyValuesFrom(source: BaseDO<out Serializable>, vararg ignoreFields: String): ModificationStatus {
        var modificationStatus = super.copyValuesFrom(source, *ignoreFields)
        val src = source as UserPrefDO
        if (src.userPrefEntries != null) {
            for (srcEntry in src.userPrefEntries!!) {
                val destEntry = ensureAndGetAccessEntry(srcEntry.parameter)
                val st = destEntry.copyValuesFrom(srcEntry)
                modificationStatus = getModificationStatus(modificationStatus, st)
            }
            val iterator = userPrefEntries!!.iterator()
            while (iterator.hasNext()) {
                val destEntry = iterator.next()
                if (src.getUserPrefEntry(destEntry.parameter) == null) {
                    iterator.remove()
                }
            }
        }
        return modificationStatus
    }

    @Deprecated("Use value with json serialization instead.")
    fun ensureAndGetAccessEntry(parameter: String): UserPrefEntryDO {
        if (this.userPrefEntries == null) {
            userPrefEntries = TreeSet()
        }
        var entry = getUserPrefEntry(parameter)
        if (entry == null) {
            entry = UserPrefEntryDO()
            entry.parameter = parameter
            this.addUserPrefEntry(entry)
        }
        return entry
    }

    @Deprecated("Use value with json serialization instead.")
    @Transient
    fun getUserPrefEntry(parameter: String): UserPrefEntryDO? {
        if (this.userPrefEntries == null) {
            return null
        }
        for (entry in this.userPrefEntries!!) {
            if (entry.parameter == parameter) {
                return entry
            }
        }
        return null
    }

    @Deprecated("Use value with json serialization instead.")
    @Transient
    fun getUserPrefEntryAsString(parameter: String): String? {
        val entry = getUserPrefEntry(parameter) ?: return null
        return entry.value
    }

    /**
     * @param parameter
     * @return A list of all parameters which depends on the given parameter or null if no dependent parameter exists for
     * this parameter.
     */
    fun getDependentUserPrefEntries(parameter: String): List<UserPrefEntryDO>? {
        var list: MutableList<UserPrefEntryDO>? = null
        for (entry in this.userPrefEntries!!) {
            if (parameter == entry.dependsOn) {
                if (list == null) {
                    list = ArrayList()
                }
                list.add(entry)
            }
        }
        return list
    }
}
