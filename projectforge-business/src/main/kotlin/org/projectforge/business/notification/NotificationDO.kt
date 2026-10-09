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

import com.fasterxml.jackson.annotation.JsonIgnore
import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.EnumType
import jakarta.persistence.Enumerated
import jakarta.persistence.GeneratedValue
import jakarta.persistence.GenerationType
import jakarta.persistence.Id
import jakarta.persistence.Index
import jakarta.persistence.NamedQueries
import jakarta.persistence.NamedQuery
import jakarta.persistence.Table
import jakarta.persistence.Transient
import jakarta.persistence.UniqueConstraint
import org.projectforge.framework.json.JsonUtils
import java.util.Date

/**
 * One notification of a rule run for one recipient, also the log of who acknowledged or finished it and when.
 *
 * Deliberately not a [org.projectforge.framework.persistence.entities.DefaultBaseDO] (no history, no BaseDao): the
 * rows are written by the jobs of [NotificationService] without a logged-in user and changed by their recipients
 * only through [NotificationDao]. The references (rule, users) are plain ids, the users are taken from the caches.
 */
@Entity
@Table(
    name = "T_NOTIFICATION",
    uniqueConstraints = [UniqueConstraint(name = "unique_t_notification_dedup_key", columnNames = ["dedup_key"])],
    indexes = [
        Index(name = "idx_t_notification_recipient_status", columnList = "recipient_fk, status"),
        Index(name = "idx_t_notification_next_delivery", columnList = "next_delivery_at"),
        Index(name = "idx_t_notification_rule", columnList = "rule_fk"),
    ],
)
@NamedQueries(
    NamedQuery(
        name = NotificationDO.FIND_BY_DEDUP_KEY,
        query = "from NotificationDO n where n.dedupKey = :dedupKey",
    ),
    NamedQuery(
        name = NotificationDO.FIND_PENDING_BY_RECIPIENT,
        query = "from NotificationDO n where n.recipientId = :recipientId and n.status in :statuses and n.inAppSince is not null and n.inAppSince <= :now order by n.created desc",
    ),
    NamedQuery(
        name = NotificationDO.FIND_DUE_DELIVERIES,
        query = "from NotificationDO n where n.nextDeliveryAt is not null and n.nextDeliveryAt <= :now order by n.nextDeliveryAt",
    ),
    NamedQuery(
        name = NotificationDO.COUNT_DUE_DELIVERIES,
        query = "select count(n) from NotificationDO n where n.nextDeliveryAt is not null and n.nextDeliveryAt <= :now",
    ),
    NamedQuery(
        name = NotificationDO.COUNT_PENDING,
        query = "select count(n) from NotificationDO n where n.status in :statuses",
    ),
)
open class NotificationDO {
    @get:Id
    @get:GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "hibernate_sequence")
    @get:Column(name = "pk")
    open var id: Long? = null

    /** The [NotificationRuleDO], null for a notification without rule. */
    @get:Column(name = "rule_fk")
    open var ruleId: Long? = null

    @get:Column(name = "recipient_fk", nullable = false)
    open var recipientId: Long? = null

    @get:Enumerated(EnumType.STRING)
    @get:Column(length = 20, nullable = false)
    open var severity: NotificationSeverity = NotificationSeverity.INFO

    @get:Enumerated(EnumType.STRING)
    @get:Column(length = 20, nullable = false)
    open var display: NotificationDisplay = NotificationDisplay.BANNER

    /** Copied from the rule: may the recipient mark it as done? */
    @get:Column(name = "manual_done", nullable = false)
    open var manualDone: Boolean = false

    /** Copied from the rule: the menu item showing the pending notification as counter. */
    @get:Column(name = "menu_badge", length = 100)
    open var menuBadge: String? = null

    /** The rendered subject (plain text). */
    @get:Column(length = 1000)
    open var title: String? = null

    /** The rendered and sanitized text (HTML). */
    @get:Column(length = 100000)
    open var body: String? = null

    /** An optional link (path of the app or absolute url), e.g. to the monthly report. */
    @get:Column(length = 1000)
    open var link: String? = null

    /** The period the notification refers to, e.g. `2026-10` for the time sheets of October 2026. */
    @get:Column(name = "period_key", length = 100)
    open var periodKey: String? = null

    /** Unique per rule, recipient and period, so a rule run doesn't notify twice. */
    @get:Column(name = "dedup_key", length = 255, nullable = false)
    open var dedupKey: String? = null

    @get:Enumerated(EnumType.STRING)
    @get:Column(length = 20, nullable = false)
    open var status: NotificationStatus = NotificationStatus.OPEN

    @get:Column(nullable = false)
    open var created: Date? = null

    /** Since when the notification is visible in the app (by the first delivered in-app step), null if not (yet). */
    @get:Column(name = "in_app_since")
    open var inAppSince: Date? = null

    @get:Column(name = "acknowledged_at")
    open var acknowledgedAt: Date? = null

    @get:Column(name = "acknowledged_by_fk")
    open var acknowledgedById: Long? = null

    @get:Column(name = "done_at")
    open var doneAt: Date? = null

    @get:Column(name = "resolved_at")
    open var resolvedAt: Date? = null

    /** The copy of the rule's [NotificationDelivery] (JSON) at creation: changes of the rule don't affect it. */
    @get:Column(length = 4000)
    open var delivery: String? = null

    /** JSON of [NotificationDeliveryState]. */
    @get:Column(name = "delivery_state", length = 10000)
    open var deliveryState: String? = null

    /** The due time of the next delivery step, null if none is left (or the notification is finished). */
    @get:Column(name = "next_delivery_at")
    open var nextDeliveryAt: Date? = null

    @JsonIgnore
    fun readDelivery(): NotificationDelivery =
        delivery?.let { JsonUtils.fromJson(it, NotificationDelivery::class.java, failOnUnknownProps = false) }
            ?: NotificationDelivery(emptyList())

    @JsonIgnore
    fun readDeliveryState(): NotificationDeliveryState =
        deliveryState?.let { JsonUtils.fromJson(it, NotificationDeliveryState::class.java, failOnUnknownProps = false) }
            ?: NotificationDeliveryState()

    fun writeDeliveryState(state: NotificationDeliveryState) {
        deliveryState = JsonUtils.toJson(state)
        nextDeliveryAt = if (status == NotificationStatus.OPEN || status == NotificationStatus.ACKNOWLEDGED) {
            state.nextDueAt()
        } else {
            null
        }
    }

    /** Is it shown in the app: pending and in-app since, a toast only until acknowledged? */
    @get:Transient
    @get:JsonIgnore
    open val isVisibleInApp: Boolean
        get() = inAppSince != null && (status == NotificationStatus.OPEN
                || status == NotificationStatus.ACKNOWLEDGED && display == NotificationDisplay.BANNER)

    /**
     * May the recipient mark it as done: if allowed by the rule, or always for a test notification (without rule),
     * which no rule run resolves or expires.
     */
    @get:Transient
    @get:JsonIgnore
    open val isManualDoneAllowed: Boolean
        get() = manualDone || ruleId == null

    companion object {
        internal const val FIND_BY_DEDUP_KEY = "NotificationDO_FindByDedupKey"
        internal const val FIND_PENDING_BY_RECIPIENT = "NotificationDO_FindPendingByRecipient"
        internal const val FIND_DUE_DELIVERIES = "NotificationDO_FindDueDeliveries"
        internal const val COUNT_DUE_DELIVERIES = "NotificationDO_CountDueDeliveries"
        internal const val COUNT_PENDING = "NotificationDO_CountPending"
    }
}
