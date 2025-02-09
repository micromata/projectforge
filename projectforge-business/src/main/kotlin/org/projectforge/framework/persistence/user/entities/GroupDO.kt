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

package org.projectforge.framework.persistence.user.entities

import com.fasterxml.jackson.databind.annotation.JsonSerialize
import jakarta.persistence.*
import org.apache.commons.lang3.builder.HashCodeBuilder
import org.hibernate.search.mapper.pojo.automaticindexing.ReindexOnUpdate
import org.hibernate.search.mapper.pojo.mapping.definition.annotation.FullTextField
import org.hibernate.search.mapper.pojo.mapping.definition.annotation.Indexed
import org.hibernate.search.mapper.pojo.mapping.definition.annotation.IndexedEmbedded
import org.hibernate.search.mapper.pojo.mapping.definition.annotation.IndexingDependency
import org.projectforge.common.StringHelper
import org.projectforge.common.anots.PropertyInfo
import org.projectforge.framework.DisplayNameCapable
import org.projectforge.framework.json.IdOnlySerializer
import org.projectforge.framework.json.IdsOnlySerializer
import org.projectforge.framework.persistence.api.AUserRightId
import org.projectforge.framework.persistence.api.HibernateUtils
import org.projectforge.framework.persistence.entities.DefaultBaseDO
import org.projectforge.framework.persistence.entities.HistoryUserCommentSupport
import java.util.*

/**
 * @author Kai Reinhard (k.reinhard@micromata.de)
 */
@Entity
@Indexed
@Table(name = "T_GROUP", uniqueConstraints = [UniqueConstraint(columnNames = ["name"])])
@AUserRightId("ADMIN_CORE")
@NamedQueries(
    NamedQuery(name = GroupDO.FIND_BY_NAME, query = "from GroupDO where name=:name"),
    NamedQuery(name = GroupDO.FIND_OTHER_GROUP_BY_NAME, query = "from GroupDO where name=:name and id<>:id")
)
open class GroupDO : DefaultBaseDO(), DisplayNameCapable, HistoryUserCommentSupport {

    override val displayName: String
        @Transient
        get() = "$name"

    @PropertyInfo(i18nKey = "name")
    @FullTextField
    @get:Column(length = 100)
    open var name: String? = null

    @PropertyInfo(i18nKey = "group.localGroup")
    @get:Column(name = "local_group", nullable = false)
    open var localGroup: Boolean = false

    // private boolean nestedGroupsAllowed = true;

    // private String nestedGroupIds;

    // /**
    // * Default is true.
    // * @return the nestedGroupsAllowed
    // */
    // @Column(name = "nested_groups_allowed", nullable = false)
    // public boolean isNestedGroupsAllowed()
    // {
    // return nestedGroupsAllowed;
    // }
    //
    // /**
    // * @param nestedGroupsAllowed the nestedGroupsAllowed to set
    // * @return this for chaining.
    // */
    // public GroupDO setNestedGroupsAllowed(final boolean nestedGroupsAllowed)
    // {
    // this.nestedGroupsAllowed = nestedGroupsAllowed;
    // return this;
    // }
    //
    // /**
    // * Comma separated id's of nested groups.
    // * @return the nestedGroups
    // */
    // @Column(name = "nested_group_ids", length = 1000)
    // public String getNestedGroupIds()
    // {
    // return nestedGroupIds;
    // }
    //
    // /**
    // * @param nestedGroupIds the nestedGroups to set
    // * @return this for chaining.
    // */
    // public GroupDO setNestedGroupIds(final String nestedGroupIds)
    // {
    // this.nestedGroupIds = nestedGroupIds;
    // return this;
    // }

    @PropertyInfo(i18nKey = "organization")
    @FullTextField
    @get:Column(length = 100)
    open var organization: String? = null

    @PropertyInfo(i18nKey = "description")
    @FullTextField
    @get:Column(length = 1000)
    open var description: String? = null

    private var usernames: String? = null

    @PropertyInfo(i18nKey = "ldap")
    @FullTextField
    @get:Column(name = "ldap_values", length = 4000)
    open var ldapValues: String? = null

    // TODO: Type Set not yet supported
    @PropertyInfo(i18nKey = "group.assignedUsers")
    // @ContainedIn
    @IndexedEmbedded(includeDepth = 1)
    @IndexingDependency(reindexOnUpdate = ReindexOnUpdate.SHALLOW)
    @get:ManyToMany(targetEntity = PFUserDO::class, fetch = FetchType.LAZY)
    @get:JoinTable(
        name = "T_GROUP_USER",
        joinColumns = [JoinColumn(name = "GROUP_ID")],
        inverseJoinColumns = [JoinColumn(name = "USER_ID")],
        indexes = [jakarta.persistence.Index(
            name = "idx_fk_t_group_user_group_id",
            columnList = "group_id"
        ), jakarta.persistence.Index(name = "idx_fk_t_group_user_user_id", columnList = "user_id")]
    )
    @JsonSerialize(using = IdsOnlySerializer::class)
    open var assignedUsers: MutableSet<PFUserDO>? = null

    @PropertyInfo(i18nKey = "group.owner")
    @IndexedEmbedded(includeDepth = 1)
    @IndexingDependency(reindexOnUpdate = ReindexOnUpdate.SHALLOW)
    @get:ManyToOne(fetch = FetchType.LAZY)
    @get:JoinColumn(name = "group_owner_fk")
    @JsonSerialize(using = IdOnlySerializer::class)
    open var groupOwner: PFUserDO? = null

    /**
     * Returns the collection of assigned users only if initialized. Avoids a LazyInitializationException.
     *
     * @return
     */
    val safeAssignedUsers: Set<PFUserDO>?
        @Transient
        get() = if (this.assignedUsers == null || !HibernateUtils.isFullyInitialized(this.assignedUsers)) {
            null
        } else this.assignedUsers


    @Transient
    fun getUsernames(): String {
        if (usernames != null) {
            return usernames as String
        }
        if (safeAssignedUsers == null) {
            return ""
        }
        val list = ArrayList<String>()
        assignedUsers?.forEach {
            val username = it.username
            if (username != null) list.add(username)
        }
        usernames = StringHelper.listToString(list, ", ", true)
        return usernames as String
    }

    fun addUser(user: PFUserDO) {
        if (this.assignedUsers == null) {
            this.assignedUsers = HashSet()
        }
        this.assignedUsers!!.add(user)
        this.usernames = null
    }

    override fun equals(other: Any?): Boolean {
        if (other is GroupDO) {
            return Objects.equals(name, other.name)
        }
        return false
    }

    override fun hashCode(): Int {
        val hcb = HashCodeBuilder()
        hcb.append(this.name)
        return hcb.toHashCode()
    }

    companion object {
        /**
         * Converts group list to comma separated long values.
         * @return String with csv or null, if list of group's is null.
         */
        fun toLongList(users: List<GroupDO>?): String? {
            return users?.filter { it.id != null }?.joinToString { "${it.id}" }
        }

        internal const val FIND_BY_NAME = "GroupDO_FindByName"

        /**
         * For detecting other groups with same groupname.
         */
        internal const val FIND_OTHER_GROUP_BY_NAME = "GroupDO_FindOtherGroupByName"
    }
}
