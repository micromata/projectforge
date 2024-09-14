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

package org.projectforge.business.teamcal.admin.model

import com.fasterxml.jackson.annotation.JsonCreator
import com.fasterxml.jackson.annotation.JsonIgnore
import org.apache.commons.lang3.StringUtils
import org.apache.commons.lang3.builder.HashCodeBuilder
import org.hibernate.annotations.Type
import org.projectforge.business.common.BaseUserGroupRightsDO
import org.projectforge.common.anots.PropertyInfo
import org.projectforge.Constants
import org.projectforge.framework.persistence.user.entities.PFUserDO
import jakarta.persistence.*
import org.hibernate.annotations.JdbcTypeCode
import org.hibernate.search.mapper.pojo.automaticindexing.ReindexOnUpdate
import org.hibernate.search.mapper.pojo.bridge.mapping.annotation.TypeBinderRef
import org.hibernate.search.mapper.pojo.mapping.definition.annotation.*
import org.hibernate.type.SqlTypes
import org.projectforge.framework.persistence.history.NoHistory
import org.projectforge.framework.persistence.search.ClassBridge

/**
 * @author Kai Reinhard (k.reinhard@micromata.de)
 * @author M. Lauterbach (m.lauterbach@micromata.de)
 */
@Entity
@Indexed
@TypeBinding(binder = TypeBinderRef(type = HibernateSearchUsersGroupsTypeBinder::class))
@ClassBridge(name = "usersgroups") // usersgroups should be used in HibernateSearchUsersGroupsBridge as field name.
//@ClassBridge(name = "usersgroups", index = Index.YES, store = Store.NO, impl = HibernateSearchUsersGroupsBridge::class)
@Table(name = "T_CALENDAR", indexes = [jakarta.persistence.Index(name = "idx_fk_t_calendar_owner_fk", columnList = "owner_fk")])
open class TeamCalDO : BaseUserGroupRightsDO() {

    companion object {
        @JsonCreator(mode = JsonCreator.Mode.DELEGATING)
        @JvmStatic
        fun createFrom(value: Long): TeamCalDO {
            val cal = TeamCalDO()
            cal.id = value
            return cal
        }

        val TEAMCALRESTBLACKLIST = "teamCalRestBlackList"
    }

    @PropertyInfo(i18nKey = "plugins.teamcal.title")
    @FullTextField
    @get:Column(length = Constants.LENGTH_TITLE)
    open var title: String? = null

    @PropertyInfo(i18nKey = "plugins.teamcal.owner")
    @IndexedEmbedded(includeDepth = 1)
    @IndexingDependency(reindexOnUpdate = ReindexOnUpdate.SHALLOW)
    @get:ManyToOne(fetch = FetchType.LAZY)
    @get:JoinColumn(name = "owner_fk")
    override var owner: PFUserDO? = null

    @PropertyInfo(i18nKey = "plugins.teamcal.description")
    @FullTextField
    @get:Column(length = Constants.LENGTH_TEXT)
    open var description: String? = null

    /**
     * Optional list of user id's (csv). For these users the leave days will be included as events.
     */
    @PropertyInfo(i18nKey = "calendar.filter.vacation.users", tooltip = "calendar.filter.vacation.users.tooltip")
    @get:Column(name = "include_leave_days_for_users", length = Constants.LENGTH_TEXT)
    open var includeLeaveDaysForUsers: String? = null

    /**
     * Optional list of group id's (csv). For all users of these groups the leave days will be included as events.
     */
    @PropertyInfo(i18nKey = "calendar.filter.vacation.groups", tooltip = "calendar.filter.vacation.groups.tooltip")
    @get:Column(name = "include_leave_days_for_groups", length = Constants.LENGTH_TEXT)
    open var includeLeaveDaysForGroups: String? = null

    @PropertyInfo(i18nKey = "plugins.teamcal.externalsubscription.url")
    @get:Column(name = "ext_subscription", nullable = false, columnDefinition = "BOOLEAN DEFAULT 'false'")
    open var externalSubscription: Boolean = false

    /**
     * This hash value is used for detecting changes of an subscribed calendar.
     */
    @JsonIgnore
    @NoHistory
    @get:Column(length = 255, name = "ext_subscription_hash")
    open var externalSubscriptionHash: String? = null

    /**
     * This calendar is a subscription of an external calendar. This URL shouldn't be visible for users without
     * full access, because this field may contain personal user settings of a cloud calendar.
     *
     * @return The subscription url.
     */
    @PropertyInfo(i18nKey = "plugins.teamcal.externalsubscription.label")
    @get:Column(name = "ext_subscription_url")
    open var externalSubscriptionUrl: String? = null

    /**
     * This calendar is a subscription of an external calendar. This is the time in seconds after which this calendar
     * should be refreshed.
     *
     * @return externalSubscriptionUpdateInterval
     */
    @PropertyInfo(i18nKey = "plugins.teamcal.externalsubscription.updateInterval")
    @get:Column(name = "ext_subscription_update_interval")
    open var externalSubscriptionUpdateInterval: Int? = null

    /**
     * This binary contains all the events of a subscribed calendar and might be large. Don't export this field to
     * any client because it may contain private data.
     */
    @JsonIgnore
    @NoHistory
    @get:Basic(fetch = FetchType.LAZY)
    @get:Column(name = "ext_subscription_calendar_binary")
    @JdbcTypeCode(SqlTypes.BLOB)
    open var externalSubscriptionCalendarBinary: ByteArray? = null

    /**
     * Shorten the url or avoiding logging of user credentials as part of the url.<br></br>
     * Example: Shorten http://www.projectforge.org/cal/... -> http://www.projectforge.org
     *
     * @return
     */
    // Slash after domain found
    // Shorten http://www.projectforge.org/cal/... -> http://www.projectforge.org
    @get:PropertyInfo(i18nKey = "plugins.teamcal.externalsubscription.label")
    val externalSubscriptionUrlAnonymized: String
        @Transient
        get() {
            if (this.externalSubscriptionUrl == null) {
                return ""
            }
            val buf = StringBuffer()
            var dotRead = false
            for (i in 0 until externalSubscriptionUrl!!.length) {
                val ch = externalSubscriptionUrl!![i]
                if (dotRead == true && ch == '/') {
                    buf.append("/...")
                    break
                } else if (ch == '?') {
                    buf.append("?...")
                    break
                } else if (ch == '.') {
                    dotRead = true
                }
                buf.append(ch)
            }
            return buf.toString()
        }

    /**
     * @see java.lang.Object.hashCode
     */
    override fun hashCode(): Int {
        val hcb = HashCodeBuilder().append(this.id)
        hcb.append(this.title)
        return hcb.hashCode()
    }

    /**
     * @see java.lang.Object.equals
     */
    override fun equals(other: Any?): Boolean {
        if (other !is TeamCalDO) {
            return false
        }
        if (this === other) {
            return true
        }
        return if (this.id == other.id) {
            true
        } else StringUtils.equals(title, other.title)
    }
}
