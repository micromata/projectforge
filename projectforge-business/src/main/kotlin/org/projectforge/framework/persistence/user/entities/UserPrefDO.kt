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

@file:Suppress("DEPRECATION")

package org.projectforge.framework.persistence.user.entities

import com.fasterxml.jackson.databind.annotation.JsonSerialize
import jakarta.persistence.*
import mu.KotlinLogging
import org.hibernate.search.mapper.pojo.automaticindexing.ReindexOnUpdate
import org.hibernate.search.mapper.pojo.mapping.definition.annotation.FullTextField
import org.hibernate.search.mapper.pojo.mapping.definition.annotation.Indexed
import org.hibernate.search.mapper.pojo.mapping.definition.annotation.IndexedEmbedded
import org.hibernate.search.mapper.pojo.mapping.definition.annotation.IndexingDependency
import org.projectforge.business.user.IUserPref
import org.projectforge.business.user.UserPrefAreaRegistry
import org.projectforge.common.StringHelper
import org.projectforge.framework.json.IdOnlySerializer
import org.projectforge.framework.persistence.api.BaseDO
import org.projectforge.framework.persistence.api.EntityCopyStatus
import org.projectforge.framework.persistence.entities.AbstractBaseDO
import org.projectforge.framework.persistence.history.NoHistory
import org.projectforge.framework.persistence.history.PersistenceBehavior
import org.projectforge.framework.persistence.user.api.UserPrefArea
import org.projectforge.framework.persistence.user.entities.UserPrefDO.Companion.FIND_BY_USER_AND_AREA_AND_ID
import org.projectforge.framework.persistence.user.entities.UserPrefDO.Companion.FIND_BY_USER_AND_AREA_AND_NAME
import org.projectforge.framework.persistence.user.entities.UserPrefDO.Companion.FIND_BY_USER_ID
import org.projectforge.framework.persistence.user.entities.UserPrefDO.Companion.FIND_BY_USER_ID_AND_AREA
import org.projectforge.framework.persistence.user.entities.UserPrefDO.Companion.FIND_BY_USER_ID_AND_AREA_AND_NULLNAME
import org.projectforge.framework.persistence.user.entities.UserPrefDO.Companion.FIND_IDS_AND_NAMES_BY_USER_AND_AREA
import org.projectforge.framework.persistence.user.entities.UserPrefDO.Companion.FIND_NAMES_BY_USER_AND_AREA
import org.projectforge.framework.persistence.user.entities.UserPrefDO.Companion.FIND_OTHER_BY_USER_AND_AREA_AND_NAME
import java.io.Serializable
import java.util.*

private val log = KotlinLogging.logger {}

/**
 * Stores preferences of the user for any objects such as list filters or templates for adding new objects (time sheets
 * etc.).
 *
 * @author Kai Reinhard (k.reinhard@micromata.de)
 */
@NoHistory
@Entity
@Indexed
@Table(
    name = "T_USER_PREF",
    uniqueConstraints = [UniqueConstraint(columnNames = ["user_fk", "area", "name"])],
    indexes = [Index(name = "idx_fk_t_user_pref_user_fk", columnList = "user_fk")]
)
//@JpaXmlPersist(beforePersistListener = [UserPrefXmlBeforePersistListener::class])
@NamedQueries(
    NamedQuery(name = FIND_BY_USER_ID_AND_AREA, query = "from UserPrefDO where user.id=:userId and area=:area"),
    NamedQuery(name = FIND_BY_USER_ID, query = "from UserPrefDO where user.id=:userId"),
    NamedQuery(
        name = FIND_BY_USER_AND_AREA_AND_NAME,
        query = "from UserPrefDO where user.id=:userId and area=:area and name=:name"
    ),
    NamedQuery(
        name = FIND_BY_USER_AND_AREA_AND_ID,
        query = "from UserPrefDO where user.id=:userId and area=:area and id=:id"
    ),
    NamedQuery(
        name = FIND_BY_USER_ID_AND_AREA_AND_NULLNAME,
        query = "from UserPrefDO where user.id=:userId and area=:area and name is null"
    ),
    NamedQuery(
        name = FIND_NAMES_BY_USER_AND_AREA,
        query = "select name from UserPrefDO where user.id=:userId and area=:area order by name"
    ),
    NamedQuery(
        name = FIND_IDS_AND_NAMES_BY_USER_AND_AREA,
        query = "select id, name from UserPrefDO where user.id=:userId and area=:area order by name"
    ),
    NamedQuery(
        name = FIND_OTHER_BY_USER_AND_AREA_AND_NAME,
        query = "from UserPrefDO where id<>:id and user.id=:userId and area=:area and name=:name"
    )
)
class UserPrefDO : AbstractBaseDO<Long>(), IUserPref {
    @IndexedEmbedded(includeDepth = 1)
    @IndexingDependency(reindexOnUpdate = ReindexOnUpdate.SHALLOW)
    @get:ManyToOne(fetch = FetchType.LAZY)
    @get:JoinColumn(name = "user_fk", nullable = false)
    @JsonSerialize(using = IdOnlySerializer::class)
    override var user: PFUserDO? = null

    @FullTextField
    @get:Column(length = 255, nullable = false)
    var name: String? = null

    @get:Transient
    override var identifier: String?
        get() = name
        set(value) {
            name = value
        }

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

    @PersistenceBehavior(autoUpdateCollectionEntries = true)
    @get:Deprecated("Use value with json serialization instead.")
    @get:OneToMany(cascade = [CascadeType.ALL], orphanRemoval = true)
    @get:JoinColumn(name = "user_pref_fk")
    @set:Deprecated("Use value with json serialization instead.")
    var userPrefEntries: MutableSet<UserPrefEntryDO>? = null

    @get:Id
    @get:GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "hibernate_sequence")
    @get:Column(name = "pk")
    override var id: Long? = null

    /**
     * The value as string representation (e. g. json).
     */
    @get:Column(name = "value_string", length = 100000) // 100.000, should be space enough.
    override var serializedValue: String? = null

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
        get() {
            try {
                return if (valueTypeString.isNullOrBlank())
                    null
                else Class.forName(valueTypeString)
            } catch (ex: ClassNotFoundException) {
                log.error("Can't get value type from '$valueTypeString'. Class not found (old incompatible ProjectForge version)?")
                return null
            }
        }

    /**
     * The value as object (deserialized from json).
     */
    @get:Transient
    var valueObject: Any? = null

    @get:Transient
    val getIntValue: Int?
        get() = serializedValue?.toInt()

    /**
     * User pref's area
     */
    @get:Column(length = UserPrefArea.MAX_ID_LENGTH, nullable = false)
    override var area: String? = null

    val sortedUserPrefEntries: Set<UserPrefEntryDO>
        @Transient
        get() {
            val result = TreeSet(Comparator<UserPrefEntryDO> { o1, o2 ->
                StringHelper.compareTo(
                    o1.orderString,
                    o2.orderString
                )
            })
            result.addAll(this.userPrefEntries!!)
            return result
        }

    /**
     * Adds the given userPrefEntry, if not exist. If an entry with the same parameter already exists, it will be updated.
     */
    @Deprecated("Use value with json serialization instead.")
    fun addOrUpdateUserPrefEntry(userPrefEntry: UserPrefEntryDO) {
        userPrefEntries = userPrefEntries ?: mutableSetOf()
        userPrefEntries!!.let { entries ->
            synchronized(entries) {
                val existingEntry = entries.firstOrNull { it.parameter == userPrefEntry.parameter }
                existingEntry?.copyValuesFrom(userPrefEntry) ?: entries.add(userPrefEntry)
            }
        }
    }

    /**
     * Copies all values from the given src object excluding the values created and modified. Null values will be
     * excluded.
     */
    @Deprecated("Use value with json serialization instead.")
    override fun copyValuesFrom(source: BaseDO<out Serializable>, vararg ignoreFields: String): EntityCopyStatus {
        var modificationStatus = super.copyValuesFrom(source, *ignoreFields)
        val src = source as UserPrefDO
        src.userPrefEntries?.let { srcUserPrefEntries ->
            for (srcEntry in srcUserPrefEntries) {
                srcEntry.parameter?.let { param ->
                    val destEntry = ensureAndGetAccessEntry(param)
                    val st = destEntry.copyValuesFrom(srcEntry)
                    modificationStatus = getModificationStatus(modificationStatus, st)
                }
            }
            val iterator = userPrefEntries!!.iterator()
            while (iterator.hasNext()) {
                val destEntry = iterator.next()
                destEntry.parameter?.let { param ->
                    if (src.getUserPrefEntry(param) == null) {
                        iterator.remove()
                    }
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
            this.addOrUpdateUserPrefEntry(entry)
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

    override fun equals(other: Any?): Boolean {
        return IUserPref.equals(this, other)
    }

    override fun hashCode(): Int {
        return IUserPref.hashCode()
    }

    companion object {
        internal const val FIND_BY_USER_ID = "UserPrefDO_FindByUserId"

        internal const val FIND_BY_USER_ID_AND_AREA = "UserPrefDO_FindByUserIdAndArea"

        internal const val FIND_BY_USER_AND_AREA_AND_ID = "UserPrefDO_FindByUserIdAndAreaAndId"

        internal const val FIND_BY_USER_AND_AREA_AND_NAME = "UserPrefDO_FindByUserIdAndAreaAndName"

        internal const val FIND_OTHER_BY_USER_AND_AREA_AND_NAME = "UserPrefDO_FindOtherByUserIdAndAreaAndName"

        internal const val FIND_BY_USER_ID_AND_AREA_AND_NULLNAME = "UserPrefDO_FindByUserIdAndAreaAndNullName"

        internal const val FIND_NAMES_BY_USER_AND_AREA = "UserPrefDO_FindNamesByUserIdAndArea"

        internal const val FIND_IDS_AND_NAMES_BY_USER_AND_AREA = "UserPrefDO_FindIdsAndNamesByUserIdAndArea"
    }
}
