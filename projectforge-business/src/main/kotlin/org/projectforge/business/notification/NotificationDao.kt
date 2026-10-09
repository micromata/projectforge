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

import org.projectforge.framework.access.AccessException
import org.projectforge.framework.persistence.jpa.PfPersistenceService
import org.projectforge.framework.persistence.user.entities.PFUserDO
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Service
import java.util.Date

/**
 * The notifications ([NotificationDO]). No [org.projectforge.framework.persistence.api.BaseDao]: the jobs write them
 * without a logged-in user, a recipient may only read the own ones and acknowledge or finish them (checked here),
 * the log of all of them is for admins and finance only (checked by the caller).
 */
@Service
class NotificationDao {
    @Autowired
    private lateinit var persistenceService: PfPersistenceService

    /** The filter of the log of all notifications. */
    class Filter(
        val ruleId: Long? = null,
        val recipientId: Long? = null,
        val statuses: Collection<NotificationStatus>? = null,
        val maxResults: Int = 1000,
    )

    /** The summary of the pending notifications of a user, shown in the app. */
    class Summary(val openCount: Int, val maxSeverity: NotificationSeverity?, val latestId: Long?)

    fun findById(id: Long?): NotificationDO? {
        id ?: return null
        return persistenceService.runReadOnly { context -> context.find(NotificationDO::class.java, id, attached = false) }
    }

    fun findByDedupKey(dedupKey: String): NotificationDO? {
        return persistenceService.selectNamedSingleResult(
            NotificationDO.FIND_BY_DEDUP_KEY,
            NotificationDO::class.java,
            Pair("dedupKey", dedupKey),
        )
    }

    fun insert(notification: NotificationDO): NotificationDO {
        persistenceService.runInTransaction { context ->
            context.em.persist(notification)
            context.em.flush()
        }
        return notification
    }

    /**
     * Writes the delivery fields only (state, next due time, in-app visibility): the status may be changed by the
     * recipient at the same time.
     */
    fun updateDelivery(notification: NotificationDO) {
        persistenceService.runInTransaction { context ->
            context.executeUpdate(
                "update NotificationDO n set n.deliveryState = :deliveryState, n.nextDeliveryAt = :nextDeliveryAt," +
                        " n.inAppSince = :inAppSince where n.id = :id",
                Pair("deliveryState", notification.deliveryState),
                Pair("nextDeliveryAt", notification.nextDeliveryAt),
                Pair("inAppSince", notification.inAppSince),
                Pair("id", notification.id),
            )
        }
    }

    /** Writes the status fields (and the delivery state with the skipped steps). */
    private fun updateStatus(notification: NotificationDO) {
        persistenceService.runInTransaction { context ->
            context.executeUpdate(
                "update NotificationDO n set n.status = :status, n.acknowledgedAt = :acknowledgedAt," +
                        " n.acknowledgedById = :acknowledgedById, n.doneAt = :doneAt, n.resolvedAt = :resolvedAt," +
                        " n.deliveryState = :deliveryState, n.nextDeliveryAt = :nextDeliveryAt where n.id = :id",
                Pair("status", notification.status),
                Pair("acknowledgedAt", notification.acknowledgedAt),
                Pair("acknowledgedById", notification.acknowledgedById),
                Pair("doneAt", notification.doneAt),
                Pair("resolvedAt", notification.resolvedAt),
                Pair("deliveryState", notification.deliveryState),
                Pair("nextDeliveryAt", notification.nextDeliveryAt),
                Pair("id", notification.id),
            )
        }
    }

    /** The notifications visible in the app for the given user, the newest first. */
    fun findVisible(userId: Long, now: Date = Date()): List<NotificationDO> {
        return persistenceService.executeNamedQuery(
            NotificationDO.FIND_PENDING_BY_RECIPIENT,
            NotificationDO::class.java,
            Pair("recipientId", userId),
            Pair("statuses", NotificationStatus.PENDING),
            Pair("now", now),
        ).filter { it.isVisibleInApp }
    }

    fun summary(userId: Long, now: Date = Date()): Summary {
        val visible = findVisible(userId, now)
        return Summary(
            openCount = visible.size,
            maxSeverity = visible.maxOfOrNull { it.severity },
            latestId = visible.mapNotNull { it.id }.maxOrNull(),
        )
    }

    /**
     * The number of pending notifications of the given user per menu item ([NotificationDO.menuBadge]): the open ones
     * and the acknowledged banners (not yet done), regardless of the in-app visibility (e.g. mail only).
     */
    fun countByMenuBadge(userId: Long): Map<String, Int> {
        return persistenceService.executeQuery(
            "from NotificationDO n where n.recipientId = :recipientId and n.menuBadge is not null and n.status in :statuses",
            NotificationDO::class.java,
            Pair("recipientId", userId),
            Pair("statuses", NotificationStatus.PENDING),
        ).filter { it.status == NotificationStatus.OPEN || it.display == NotificationDisplay.BANNER }
            .groupingBy { it.menuBadge!! }.eachCount()
    }

    /** The pending notifications of the given rules. */
    fun findPendingByRules(ruleIds: Collection<Long>): List<NotificationDO> {
        return persistenceService.executeQueryBatched(
            "from NotificationDO n where n.ruleId in :ruleIds and n.status in :statuses",
            NotificationDO::class.java,
            batchParam = "ruleIds",
            batchValues = ruleIds,
            Pair("statuses", NotificationStatus.PENDING),
        )
    }

    /** The pending notifications whose rule isn't given (any more) by the given ids. */
    fun findPendingOfOtherRules(ruleIds: Collection<Long>): List<NotificationDO> {
        val ids = ruleIds.toSet()
        return persistenceService.executeQuery(
            "from NotificationDO n where n.ruleId is not null and n.status in :statuses",
            NotificationDO::class.java,
            Pair("statuses", NotificationStatus.PENDING),
        ).filter { it.ruleId !in ids }
    }

    fun findDueDeliveries(now: Date): List<NotificationDO> {
        return persistenceService.executeNamedQuery(
            NotificationDO.FIND_DUE_DELIVERIES,
            NotificationDO::class.java,
            Pair("now", now),
        )
    }

    fun countDueDeliveries(now: Date): Long {
        return persistenceService.selectNamedSingleResult(
            NotificationDO.COUNT_DUE_DELIVERIES,
            Long::class.java,
            Pair("now", now),
        ) ?: 0L
    }

    fun countPending(): Long {
        return persistenceService.selectNamedSingleResult(
            NotificationDO.COUNT_PENDING,
            Long::class.java,
            Pair("statuses", NotificationStatus.PENDING),
        ) ?: 0L
    }

    /** The log of the notifications, the newest first. Without access check. */
    fun select(filter: Filter): List<NotificationDO> {
        val where = mutableListOf<String>()
        val params = mutableListOf<Pair<String, Any?>>()
        filter.ruleId?.let {
            where += "n.ruleId = :ruleId"
            params += Pair("ruleId", it)
        }
        filter.recipientId?.let {
            where += "n.recipientId = :recipientId"
            params += Pair("recipientId", it)
        }
        filter.statuses?.takeIf { it.isNotEmpty() }?.let {
            where += "n.status in :statuses"
            params += Pair("statuses", it)
        }
        val sql = "from NotificationDO n" + (if (where.isEmpty()) "" else " where ${where.joinToString(" and ")}") +
                " order by n.created desc, n.id desc"
        return persistenceService.executeQuery(
            sql,
            NotificationDO::class.java,
            *params.toTypedArray(),
            maxResults = filter.maxResults,
        )
    }

    /**
     * The recipient confirms the notification: no further escalation steps. Only the recipient may do so. Does nothing
     * if it isn't open (any more).
     */
    fun acknowledge(id: Long, user: PFUserDO): NotificationDO? {
        val notification = findOwn(id, user) ?: return null
        if (notification.status != NotificationStatus.OPEN) {
            return notification
        }
        notification.status = NotificationStatus.ACKNOWLEDGED
        notification.acknowledgedAt = Date()
        notification.acknowledgedById = user.id
        notification.writeDeliveryState(skipUnacknowledgedSteps(notification))
        updateStatus(notification)
        return notification
    }

    /** The recipient marks the notification as done, if allowed by its rule ([NotificationDO.manualDone]). */
    fun done(id: Long, user: PFUserDO): NotificationDO? {
        val notification = findOwn(id, user) ?: return null
        if (!notification.manualDone) {
            throw AccessException(user, "notification.error.manualDoneNotAllowed")
        }
        if (!notification.status.isPending) {
            return notification
        }
        val now = Date()
        if (notification.acknowledgedAt == null) {
            notification.acknowledgedAt = now
            notification.acknowledgedById = user.id
        }
        notification.status = NotificationStatus.DONE
        notification.doneAt = now
        notification.writeDeliveryState(skipUnacknowledgedSteps(notification))
        updateStatus(notification)
        return notification
    }

    /** Finishes the notification by the rule (resolved or expired). */
    fun finish(notification: NotificationDO, status: NotificationStatus) {
        notification.status = status
        notification.resolvedAt = Date()
        notification.writeDeliveryState(skipUnacknowledgedSteps(notification))
        updateStatus(notification)
    }

    /** The steps only to be delivered if unacknowledged are skipped, if not yet processed. */
    private fun skipUnacknowledgedSteps(notification: NotificationDO): NotificationDeliveryState {
        val state = notification.readDeliveryState()
        val delivery = notification.readDelivery()
        state.steps.filter { !it.isProcessed && delivery.steps.getOrNull(it.step)?.onlyIfUnacknowledged == true }
            .forEach { it.skipped = true }
        return state
    }

    private fun findOwn(id: Long, user: PFUserDO): NotificationDO? {
        val notification = findById(id) ?: return null
        if (notification.recipientId != user.id) {
            throw AccessException(user, "access.exception.noAccess")
        }
        return notification
    }
}
