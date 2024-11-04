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

package org.projectforge.framework.persistence.user.entities

import com.fasterxml.jackson.annotation.JsonIdentityInfo
import com.fasterxml.jackson.annotation.ObjectIdGenerators
import jakarta.persistence.*
import org.apache.commons.lang3.builder.HashCodeBuilder
import org.apache.commons.lang3.builder.ToStringBuilder
import org.hibernate.search.mapper.pojo.mapping.definition.annotation.GenericField
import org.hibernate.search.mapper.pojo.mapping.definition.annotation.Indexed
import org.hibernate.search.mapper.pojo.mapping.definition.annotation.IndexedEmbedded
import org.projectforge.business.user.UserRightId
import org.projectforge.business.user.UserRightValue
import org.projectforge.framework.DisplayNameCapable
import org.projectforge.framework.persistence.api.IUserRightId
import org.projectforge.framework.persistence.entities.DefaultBaseDO
import java.io.Serializable

@Entity
@Indexed
@Table(
    name = "T_USER_RIGHT",
    uniqueConstraints = [UniqueConstraint(columnNames = ["user_fk", "right_id"])],
    indexes = [Index(name = "idx_fk_t_user_right_user_fk", columnList = "user_fk")]
)
@NamedQueries(
    NamedQuery(name = UserRightDO.FIND_ALL_ORDERED, query = "from UserRightDO order by user.id, rightIdString"),
    NamedQuery(name = UserRightDO.FIND_ALL_BY_USER_ID, query = "from UserRightDO where user.id=:userId"),
)
@JsonIdentityInfo(generator = ObjectIdGenerators.PropertyGenerator::class, property = "id")
class UserRightDO : DefaultBaseDO, Comparable<UserRightDO>, Serializable, DisplayNameCapable {
    /**
     * Only for storing the right id in the data base.
     */
    @get:Column(name = "right_id", length = 40, nullable = false)
    @GenericField // was: @Field(index = Index.YES, analyze = Analyze.NO)
    var rightIdString: String? = null

    @get:Column(length = 40)
    @get:Enumerated(EnumType.STRING)
    @GenericField // was:@Field(index = Index.YES, analyze = Analyze.NO /* UN_TOKENIZED */)
    var value: UserRightValue? = null

    @get:JoinColumn(name = "user_fk", nullable = false)
    @get:ManyToOne(fetch = FetchType.LAZY)
    @IndexedEmbedded(includeDepth = 1)
    var user: PFUserDO? = null

    constructor()

    constructor(rightId: UserRightId?) : this(null, rightId, null)

    constructor(rightId: IUserRightId?, value: UserRightValue?) : this(null, rightId, value)

    @JvmOverloads
    constructor(user: PFUserDO?, rightId: IUserRightId?, value: UserRightValue? = null) {
        this.user = user
        this.rightIdString = rightId?.id
        this.value = value
    }

    fun withValue(value: UserRightValue?): UserRightDO {
        this.value = value
        return this
    }

    @get:Transient
    val userId: Long?
        get() {
            if (this.user == null) {
                return null
            }
            return user!!.id
        }

    fun withUser(user: PFUserDO?): UserRightDO {
        this.user = user
        return this
    }

    /*
     * @see java.lang.Comparable#compareTo(java.lang.Object)
     */
    override fun compareTo(other: UserRightDO): Int {
        if (rightIdString == null) {
            if (other.rightIdString == null) {
                return 0
            }
            return -1
        } else if (other.rightIdString == null) {
            return 1
        }
        return rightIdString!!.compareTo(other.rightIdString!!)
    }

    override fun equals(other: Any?): Boolean {
        if (other is UserRightDO) {
            if (this.rightIdString != other.rightIdString) {
                return false
            }
            if (this.id != other.id) {
                return false
            }
            return true
        }
        return false
    }

    override fun hashCode(): Int {
        val hcb = HashCodeBuilder()
        if (rightIdString != null) {
            hcb.append(rightIdString.hashCode())
        }
        hcb.append(id)
        return hcb.toHashCode()
    }

    @get:Transient
    override val displayName: String
        /**
         * @see org.projectforge.framework.DisplayNameCapable.getDisplayName
         */
        get() = rightIdString.toString()

    override fun toString(): String {
        val sb = ToStringBuilder(this)
        sb.append("id", id)
        sb.append("userId", this.userId)
        sb.append("rightId", this.rightIdString)
        sb.append("value", this.value)
        return sb.toString()
    }

    companion object {
        const val FIND_ALL_ORDERED: String = "UserRightDO_FindAllOrdered"

        const val FIND_ALL_BY_USER_ID: String = "UserRightDO_FindAllByUserId"
    }
}
