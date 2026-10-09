/////////////////////////////////////////////////////////////////////////////
//
// Project ProjectForge Community Edition
//         www.projectforge.org
//
// Copyright (C) 2001-2026 Micromata GmbH, Germany (www.micromata.com)
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


package org.projectforge.business.notification

import org.projectforge.business.user.ProjectForgeGroup
import org.projectforge.framework.access.AccessException
import org.projectforge.framework.access.OperationType
import org.projectforge.framework.persistence.api.BaseDao
import org.projectforge.framework.persistence.api.BaseSearchFilter
import org.projectforge.framework.persistence.api.QueryFilter
import org.projectforge.framework.persistence.api.SortProperty
import org.projectforge.framework.persistence.user.entities.PFUserDO
import org.springframework.stereotype.Service
import java.util.Date

/**
 * The rules of the notification system, edited by admins and finance members. A rule may restrict its editors to
 * the members of some groups ([NotificationRuleDO.editableByGroupIds]): they must be finance members anyway, admins
 * are always allowed (so nobody locks out everybody).
 */
@Service
open class NotificationRuleDao : BaseDao<NotificationRuleDO>(NotificationRuleDO::class.java) {
    override val defaultSortProperties: Array<SortProperty>
        get() = DEFAULT_SORT_PROPERTIES

    override fun select(filter: BaseSearchFilter): List<NotificationRuleDO> {
        return select(QueryFilter(filter))
    }

    override fun newInstance(): NotificationRuleDO {
        return NotificationRuleDO()
    }

    override fun hasAccess(
        user: PFUserDO, obj: NotificationRuleDO?, oldObj: NotificationRuleDO?,
        operationType: OperationType,
        throwException: Boolean,
    ): Boolean {
        if (userGroupCache.isUserMemberOfAdminGroup(user.id)) {
            return true
        }
        val allowed = userGroupCache.isUserMemberOfFinanceGroup(user.id)
                && (operationType == OperationType.SELECT || (isEditor(user, obj) && isEditor(user, oldObj)))
        if (!allowed && throwException) {
            throw AccessException(user, "access.exception.userHasNotRight", ProjectForgeGroup.FINANCE_GROUP.key, operationType)
        }
        return allowed
    }

    override fun hasInsertAccess(user: PFUserDO): Boolean {
        return userGroupCache.isUserMemberOfAdminGroup(user.id) || userGroupCache.isUserMemberOfFinanceGroup(user.id)
    }

    /** The last run is maintained by the rule job only ([updateLastRun]), never by an edit (maybe stale). */
    override fun onUpdate(obj: NotificationRuleDO, dbObj: NotificationRuleDO) {
        obj.lastRun = dbObj.lastRun
    }

    /** Is the user an admin or a finance member allowed by [NotificationRuleDO.editableByGroupIds]? */
    open fun isEditor(user: PFUserDO, rule: NotificationRuleDO?): Boolean {
        if (userGroupCache.isUserMemberOfAdminGroup(user.id)) {
            return true
        }
        if (!userGroupCache.isUserMemberOfFinanceGroup(user.id)) {
            return false
        }
        val groupIds = rule?.readEditableByGroupIds().orEmpty()
        return groupIds.isEmpty() || userGroupCache.isUserMemberOfAtLeastOneGroup(user.id, *groupIds.toTypedArray())
    }

    /** All active rules, without access check (for the jobs). */
    open fun selectActive(): List<NotificationRuleDO> {
        return persistenceService.executeQuery(
            "from NotificationRuleDO r where r.active = true and r.deleted = false order by r.id",
            NotificationRuleDO::class.java,
        )
    }

    /** All not deleted rules (active or not), without access check (for the resolution of their notifications). */
    open fun selectAllInternal(): List<NotificationRuleDO> {
        return persistenceService.executeQuery(
            "from NotificationRuleDO r where r.deleted = false order by r.id",
            NotificationRuleDO::class.java,
        )
    }

    /** Without access check and without history (for the jobs). */
    open fun findInternal(id: Long?): NotificationRuleDO? {
        id ?: return null
        return find(id, checkAccess = false)
    }

    /** Stores the last run by schedule without history and without changing lastUpdate. */
    open fun updateLastRun(ruleId: Long, lastRun: Date) {
        persistenceService.runInTransaction { context ->
            context.executeUpdate(
                "update NotificationRuleDO r set r.lastRun = :lastRun where r.id = :id",
                Pair("lastRun", lastRun),
                Pair("id", ruleId),
            )
        }
    }

    companion object {
        private val DEFAULT_SORT_PROPERTIES = arrayOf(SortProperty("name"))
    }
}
