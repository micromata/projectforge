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


package org.projectforge.business.fibu.customergroup

import jakarta.annotation.PostConstruct
import org.projectforge.business.user.ProjectForgeGroup
import org.projectforge.business.user.UserGroupCache
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Service
import java.util.concurrent.ConcurrentHashMap

/**
 * What a user's order access grants ([OrderAccessGrant]) cover, joined over all grants naming the user directly
 * or through one of their groups.
 */
class OrderGrantTargets(
    val kundeIds: Set<Long> = emptySet(),
    val groupKeys: Set<String> = emptySet(),
    val businessUnitKeys: Set<String> = emptySet(),
) {
    val isEmpty: Boolean
        get() = kundeIds.isEmpty() && groupKeys.isEmpty() && businessUnitKeys.isEmpty()
}

/**
 * The order access grants of [index] applied to the group memberships [groupIdsOfUser] (user id -> group ids, as
 * [UserGroupCache.getUserGroupIdMap]). Immutable apart from the per-user targets, computed on first use.
 *
 * Only members of one of [eligibleGroupIds] (the project managers and project assistants) are granted. A
 * deactivated user has no memberships in the cache, so their grants have no effect either.
 */
class OrderAccessGrants(
    val index: CustomerGroupIndex,
    private val groupIdsOfUser: Map<Long, Set<Long>>,
    private val eligibleGroupIds: Set<Long>,
) {
    private val grants = index.config.orderAccess

    private val targetsByUser = ConcurrentHashMap<Long, OrderGrantTargets>()

    /** The user's targets, empty if the user isn't eligible or named by no grant. */
    fun targetsOf(userId: Long?): OrderGrantTargets {
        userId ?: return EMPTY_TARGETS
        if (grants.isEmpty()) {
            return EMPTY_TARGETS
        }
        return targetsByUser.computeIfAbsent(userId) { computeTargets(it) }
    }

    /** Whether the user is a project manager or project assistant, the precondition of any grant. */
    fun isEligible(userId: Long?): Boolean {
        userId ?: return false
        return groupIdsOfUser[userId]?.any { it in eligibleGroupIds } == true
    }

    /** Whether the grant names the user, directly or through one of their groups (eligible or not). */
    fun isNamed(grant: OrderAccessGrant, userId: Long?): Boolean {
        userId ?: return false
        if (userId in grant.userIds) {
            return true
        }
        val groups = groupIdsOfUser[userId] ?: return false
        return grant.groupIds.any { it in groups }
    }

    /**
     * Whether the user is granted an order of the given customer entity (by number) or free-text customer and
     * project. The order's group and business unit are found as by the order list's filters
     * ([CustomerGroupIndex.groupOf], [CustomerGroupIndex.businessUnitOf]): an order without a customer
     * reaches a business unit only by its project's task.
     */
    fun isGranted(userId: Long?, kundeId: Long?, kundeText: String?, projektId: Long?): Boolean {
        val targets = targetsOf(userId)
        if (targets.isEmpty) {
            return false
        }
        if (kundeId != null && kundeId in targets.kundeIds) {
            return true
        }
        if (targets.groupKeys.isNotEmpty() && index.groupOf(kundeId, kundeText)?.key in targets.groupKeys) {
            return true
        }
        return targets.businessUnitKeys.isNotEmpty() &&
                index.businessUnitOf(kundeId, kundeText, projektId)?.key in targets.businessUnitKeys
    }

    private fun computeTargets(userId: Long): OrderGrantTargets {
        if (!isEligible(userId)) {
            return EMPTY_TARGETS
        }
        val named = grants.filter { isNamed(it, userId) }
        if (named.isEmpty()) {
            return EMPTY_TARGETS
        }
        return OrderGrantTargets(
            named.flatMap { it.customers }.toSet(),
            named.flatMap { it.groups }.toSet(),
            named.flatMap { it.businessUnits }.toSet(),
        )
    }

    companion object {
        private val EMPTY_TARGETS = OrderGrantTargets()

        val EMPTY = OrderAccessGrants(CustomerGroupIndex.EMPTY, emptyMap(), emptySet())
    }
}

/**
 * The order access grants of the customer-groups configuration ([CustomerGroupConfig.orderAccess]), for the
 * read access to orders (`AuftragRight`, `RechnungDao`, `ContributionMarginService`).
 *
 * The access checks ask once per order of a list, so [grants] keeps the last [OrderAccessGrants] and only asks
 * the sources ([CustomerGroupService.index], [UserGroupCache.getUserGroupIdMap]) again after [RECHECK_MILLIS]:
 * a revoked grant or a user leaving the project managers takes effect after that time, a grant saved on this
 * node at once ([invalidate]).
 */
@Service
class OrderAccessGrantService {
    @Autowired
    private lateinit var customerGroupService: CustomerGroupService

    @Autowired
    private lateinit var userGroupCache: UserGroupCache

    private class Snapshot(val grants: OrderAccessGrants, val groupIdMap: Map<Long, Set<Long>>, val checkedMillis: Long)

    @Volatile
    private var snapshot: Snapshot? = null

    @PostConstruct
    private fun init() {
        instanceOrNull = this
    }

    val grants: OrderAccessGrants
        get() {
            val now = System.currentTimeMillis()
            val current = snapshot
            if (current != null && now - current.checkedMillis < RECHECK_MILLIS) {
                return current.grants
            }
            val index = customerGroupService.index
            val groupIdMap = userGroupCache.getUserGroupIdMap()
            val grants = if (current != null && current.grants.index === index && current.groupIdMap === groupIdMap) {
                current.grants
            } else {
                val eligible = listOf(ProjectForgeGroup.PROJECT_MANAGER, ProjectForgeGroup.PROJECT_ASSISTANT)
                    .mapNotNull { userGroupCache.getGroup(it)?.id }.toSet()
                OrderAccessGrants(index, groupIdMap, eligible)
            }
            snapshot = Snapshot(grants, groupIdMap, now)
            return grants
        }

    /** See [OrderAccessGrants.isGranted]. */
    fun isGranted(userId: Long?, kundeId: Long?, kundeText: String?, projektId: Long?): Boolean =
        grants.isGranted(userId, kundeId, kundeText, projektId)

    /** After a save of the configuration: the next check reads the sources again. */
    fun invalidate() {
        snapshot = null
    }

    companion object {
        const val RECHECK_MILLIS = 1000L

        /** Null outside a Spring context (unit tests), where no order is granted. */
        @JvmStatic
        var instanceOrNull: OrderAccessGrantService? = null
            private set
    }
}
