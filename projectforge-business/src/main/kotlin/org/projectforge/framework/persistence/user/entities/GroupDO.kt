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

package org.projectforge.framework.persistence.user.entities

import org.apache.commons.lang3.builder.HashCodeBuilder
import org.hibernate.Hibernate
import org.hibernate.search.annotations.ContainedIn
import org.hibernate.search.annotations.Field
import org.hibernate.search.annotations.Indexed
import org.hibernate.search.annotations.IndexedEmbedded
import org.projectforge.common.StringHelper
import org.projectforge.common.anots.PropertyInfo
import org.projectforge.framework.persistence.api.AUserRightId
import org.projectforge.framework.persistence.api.ShortDisplayNameCapable
import org.projectforge.framework.persistence.entities.DefaultBaseDO
import java.util.*
import javax.persistence.*

/**
 * @author Kai Reinhard (k.reinhard@micromata.de)
 */
@Entity
@Indexed
@Table(name = "T_GROUP", uniqueConstraints = [UniqueConstraint(columnNames = ["name", "tenant_id"])], indexes = [javax.persistence.Index(name = "idx_fk_t_group_tenant_id", columnList = "tenant_id")])
@AUserRightId("ADMIN_CORE")
@NamedQueries(
        NamedQuery(name = GroupDO.FIND_BY_NAME, query = "from GroupDO where name=:name"),
        NamedQuery(name = GroupDO.FIND_OTHER_GROUP_BY_NAME, query = "from GroupDO where name=:name and id<>:id"))
open class GroupDO : DefaultBaseDO(), ShortDisplayNameCapable {

    @PropertyInfo(i18nKey = "name")
    @Field
    @get:Column(length = 100)
    open var name: String? = null

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
    @Field
    @get:Column(length = 100)
    open var organization: String? = null

    @PropertyInfo(i18nKey = "description")
    @Field
    @get:Column(length = 1000)
    open var description: String? = null

    private var usernames: String? = null

    @PropertyInfo(i18nKey = "ldap")
    @Field
    @get:Column(name = "ldap_values", length = 4000)
    open var ldapValues: String? = null

    // TODO: Type Set not yet supported
    @PropertyInfo(i18nKey = "group.assignedUsers")
    @ContainedIn
    @IndexedEmbedded(depth = 1)
    @get:ManyToMany(targetEntity = PFUserDO::class, cascade = [CascadeType.MERGE], fetch = FetchType.EAGER)
    @get:JoinTable(name = "T_GROUP_USER", joinColumns = [JoinColumn(name = "GROUP_ID")], inverseJoinColumns = [JoinColumn(name = "USER_ID")], indexes = [javax.persistence.Index(name = "idx_fk_t_group_user_group_id", columnList = "group_id"), javax.persistence.Index(name = "idx_fk_t_group_user_user_id", columnList = "user_id")])
    open var assignedUsers: MutableSet<PFUserDO>? = null

    /**
     * Returns the collection of assigned users only if initialized. Avoids a LazyInitializationException.
     *
     * @return
     */
    val safeAssignedUsers: Set<PFUserDO>?
        @Transient
        get() = if (this.assignedUsers == null || Hibernate.isInitialized(this.assignedUsers) == false) {
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

    @Transient
    override fun getShortDisplayName(): String? {
        return this.name
    }

    companion object {
        internal const val FIND_BY_NAME = "GroupDO_FindByName"
        /**
         * For detecting other groups with same groupname.
         */
        internal const val FIND_OTHER_GROUP_BY_NAME = "GroupDO_FindOtherGroupByName"
    }
}
