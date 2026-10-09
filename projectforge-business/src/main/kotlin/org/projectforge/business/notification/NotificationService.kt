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

import io.github.oshai.kotlinlogging.KotlinLogging
import org.projectforge.business.PfCaches
import org.projectforge.common.logging.error
import org.projectforge.common.logging.warn
import org.projectforge.framework.configuration.Configuration
import org.projectforge.framework.json.JsonUtils
import org.projectforge.framework.persistence.user.entities.PFUserDO
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.scheduling.annotation.Scheduled
import org.springframework.stereotype.Service
import java.time.LocalDate
import java.util.Date
import java.util.concurrent.locks.ReentrantLock
import kotlin.concurrent.withLock

private val log = KotlinLogging.logger {}

/**
 * The engine of the notification system:
 * - the rule job evaluates the due rules ([NotificationSchedule], each at most once per day), creates one
 *   [NotificationDO] per recipient and period (dedup key) and checks the pending ones for resolution;
 * - the delivery job delivers the due steps of the delivery cascades (e.g. a mail, if not confirmed in the app
 *   within two days). A failed step is retried ([NotificationDeliveryState.RETRY_DELAYS_MINUTES]); a crash between
 *   sending and storing the state may deliver a step twice (at least once).
 * Both jobs record their runs for the problem dashboard ([NotificationSubsystemStatusProvider]).
 */
@Service
class NotificationService {
    @Autowired
    private lateinit var caches: PfCaches

    @Autowired
    private lateinit var notificationDao: NotificationDao

    @Autowired
    private lateinit var notificationRuleDao: NotificationRuleDao

    @Autowired
    private lateinit var recipientResolver: NotificationRecipientResolver

    @Autowired
    private lateinit var handlerList: List<NotificationRuleHandler>

    @Autowired
    private lateinit var senderList: List<NotificationChannelSender>

    /** The result of a rule run. */
    class RunResult(val recipients: Int = 0, val created: Int = 0, val errors: Int = 0, val lastError: String? = null)

    /** The result of [preview]. */
    class Preview(
        /** The users who would be notified by a run today (not yet notified ones and already notified ones). */
        val recipients: List<PFUserDO>,
        /** The recipients already notified for the period (by an earlier run). */
        val alreadyNotified: Int,
        val periodKey: String?,
        /** Subject and text rendered for the given user. */
        val subject: String,
        val text: String,
    )

    /** The result of [sendTestToMe]: the channels delivered and the reasons of the ones not delivered. */
    class TestResult(val delivered: List<NotificationChannel>, val failed: Map<NotificationChannel, String>)

    val ruleJobStats = NotificationJobStats(RULE_JOB_TYPE)

    val deliveryJobStats = NotificationJobStats(DELIVERY_JOB_TYPE)

    // Serializes the jobs (and the manual triggers): a new notification must not be delivered twice.
    private val lock = ReentrantLock()

    private val handlers: Map<NotificationRuleType, NotificationRuleHandler> by lazy {
        handlerList.associateBy { it.ruleType }
    }

    private val senders: Map<NotificationChannel, NotificationChannelSender> by lazy {
        senderList.associateBy { it.channel }
    }

    @Scheduled(cron = "\${projectforge.notification.cron:0 0 7-19 * * *}")
    fun ruleJob() {
        Thread({ runRuleJob(today(), Date()) }, "NotificationRuleJob").start()
    }

    @Scheduled(cron = "\${projectforge.notification.deliveryCron:0 */15 * * * *}")
    fun deliveryJob() {
        Thread({ runDeliveryJob(Date()) }, "NotificationDeliveryJob").start()
    }

    /** The keys of the variables of the given rule type (`{{key}}`), the common ones first. */
    fun variables(ruleType: NotificationRuleType?): List<String> {
        return COMMON_VARIABLES + (ruleType?.let { handlers[it]?.variables }.orEmpty())
    }

    /** Runs the due rules, then checks the pending notifications for resolution. */
    fun runRuleJob(today: LocalDate, now: Date) {
        val start = System.currentTimeMillis()
        try {
            lock.withLock {
                var errors = 0
                var lastError: String? = null
                notificationRuleDao.selectActive().forEach { rule ->
                    if (!rule.readSchedule().isDue(today) || rule.lastRun?.let { toLocalDate(it) } == today) {
                        return@forEach
                    }
                    val result = runRule(rule, today, now)
                    notificationRuleDao.updateLastRun(rule.id!!, now)
                    errors += result.errors
                    result.lastError?.let { lastError = it }
                    log.info { "Notification rule #${rule.id} '${rule.name}': ${result.created} notification(s) created for ${result.recipients} recipient(s), ${result.errors} error(s)." }
                }
                val resolveResult = resolvePending(today)
                errors += resolveResult.errors
                resolveResult.lastError?.let { lastError = it }
                ruleJobStats.record(start, errors, lastError)
            }
        } catch (ex: Exception) {
            log.error(NotificationLogEvents.JOB_FAILED, ex) { "Notification rule job failed: ${ex.message}" }
            ruleJobStats.recordAbort(start, ex)
        }
    }

    /** Delivers the due steps of all notifications. Skipped, if a job is running (the next run catches up). */
    fun runDeliveryJob(now: Date) {
        if (!lock.tryLock()) {
            log.info { "Notification delivery job skipped: another job of the notification system is running." }
            return
        }
        val start = System.currentTimeMillis()
        try {
            var errors = 0
            var lastError: String? = null
            notificationDao.findDueDeliveries(now).forEach { notification ->
                val result = deliverDueSteps(notification, now)
                errors += result.errors
                result.lastError?.let { lastError = it }
            }
            deliveryJobStats.record(start, errors, lastError)
        } catch (ex: Exception) {
            log.error(NotificationLogEvents.JOB_FAILED, ex) { "Notification delivery job failed: ${ex.message}" }
            deliveryJobStats.recordAbort(start, ex)
        } finally {
            lock.unlock()
        }
    }

    /** Runs the rule now, regardless of its schedule (already notified recipients aren't notified twice). */
    fun trigger(rule: NotificationRuleDO): RunResult {
        lock.withLock {
            val result = runRule(rule, today(), Date())
            log.info { "Notification rule #${rule.id} '${rule.name}' triggered by hand: ${result.created} notification(s) created for ${result.recipients} recipient(s), ${result.errors} error(s)." }
            return result
        }
    }

    /**
     * Evaluates the given rule (maybe unsaved) as a run today would: the recipients, and subject and text rendered
     * for the given user.
     */
    fun preview(rule: NotificationRuleDO, user: PFUserDO): Preview {
        val today = today()
        val handler = handler(rule)
        val evaluation = handler.evaluate(rule, today)
        val recipients = evaluation?.let { recipientsOf(rule, handler, it, today) }.orEmpty()
        val alreadyNotified = if (rule.id != null && evaluation != null) {
            recipients.count { notificationDao.findByDedupKey(dedupKey(rule, it, evaluation.periodKey)) != null }
        } else 0
        val variables = variablesOf(rule, handler, evaluation, today, user)
        return Preview(
            recipients = recipients,
            alreadyNotified = alreadyNotified,
            periodKey = evaluation?.periodKey,
            subject = NotificationTemplate.renderSubject(rule.subject, variables),
            text = NotificationTemplate.renderText(rule.text, variables),
        )
    }

    /**
     * Sends the given rule (maybe unsaved) to the user only, with the user's own values, through all channels of its
     * delivery cascade at once (regardless of the delays). The in-app notification has no rule (it's no rule run).
     */
    fun sendTestToMe(rule: NotificationRuleDO, user: PFUserDO): TestResult {
        require(!rule.subject.isNullOrBlank() && !rule.text?.replace(TAG_REGEX, "").isNullOrBlank()) {
            "Subject and text are required for a test."
        }
        val today = today()
        val handler = handler(rule)
        val evaluation = handler.evaluate(rule, today)
        val now = Date()
        val notification = build(rule, user, variablesOf(rule, handler, evaluation, today, user), evaluation, now)
        notification.ruleId = null
        notification.manualDone = true // No rule resolves or expires a test notification: the recipient removes it.
        notification.dedupKey = "test:${user.id}:${now.time}"
        notification.periodKey = null
        val delivered = mutableListOf<NotificationChannel>()
        val failed = mutableMapOf<NotificationChannel, String>()
        val channels = rule.readDelivery().steps.map { it.channel }.distinct()
        notification.delivery = JsonUtils.toJson(NotificationDelivery(emptyList()))
        notification.writeDeliveryState(NotificationDeliveryState())
        channels.forEach { channel ->
            val sender = senders[channel]
            if (sender == null) {
                failed[channel] = "Not supported."
                return@forEach
            }
            try {
                val reason = sender.deliver(notification, user, now)
                if (reason == null) delivered += channel else failed[channel] = reason
            } catch (ex: Exception) {
                failed[channel] = ex.message ?: ex.javaClass.simpleName
            }
        }
        if (notification.inAppSince != null) {
            notificationDao.insert(notification)
        }
        return TestResult(delivered, failed)
    }

    /** Evaluates the rule and creates the notifications of the recipients not yet notified for the period. */
    internal fun runRule(rule: NotificationRuleDO, today: LocalDate, now: Date): RunResult {
        val evaluation: NotificationRuleHandler.Evaluation
        val handler: NotificationRuleHandler
        val recipients: List<PFUserDO>
        try {
            handler = handler(rule)
            evaluation = handler.evaluate(rule, today) ?: return RunResult()
            recipients = recipientsOf(rule, handler, evaluation, today)
        } catch (ex: Exception) {
            log.error(NotificationLogEvents.RULE_FAILED, ex) { "Notification rule #${rule.id} '${rule.name}' failed: ${ex.message}" }
            return RunResult(errors = 1, lastError = ex.message)
        }
        var created = 0
        var errors = 0
        var lastError: String? = null
        recipients.forEach { user ->
            try {
                val dedupKey = dedupKey(rule, user, evaluation.periodKey)
                if (notificationDao.findByDedupKey(dedupKey) != null) {
                    return@forEach
                }
                val notification = build(rule, user, variablesOf(rule, handler, evaluation, today, user), evaluation, now)
                notification.dedupKey = dedupKey
                notificationDao.insert(notification)
                ++created
                val result = deliverDueSteps(notification, now)
                errors += result.errors
                result.lastError?.let { lastError = it }
            } catch (ex: Exception) {
                log.error(NotificationLogEvents.RULE_FAILED, ex) { "Notification rule #${rule.id} '${rule.name}': notification of user #${user.id} not created: ${ex.message}" }
                ++errors
                lastError = ex.message
            }
        }
        return RunResult(recipients = recipients.size, created = created, errors = errors, lastError = lastError)
    }

    /** Checks the pending notifications of all rules: finished by the rule, or expired with a deleted rule. */
    internal fun resolvePending(today: LocalDate): RunResult {
        var errors = 0
        var lastError: String? = null
        val rules = notificationRuleDao.selectAllInternal()
        rules.forEach { rule ->
            try {
                val pending = notificationDao.findPendingByRules(listOf(rule.id!!))
                if (pending.isEmpty()) {
                    return@forEach
                }
                handler(rule).resolve(rule, pending, today).forEach { (id, status) ->
                    pending.find { it.id == id }?.let { notificationDao.finish(it, status) }
                }
            } catch (ex: Exception) {
                log.error(NotificationLogEvents.RULE_FAILED, ex) { "Notification rule #${rule.id} '${rule.name}': resolution of the pending notifications failed: ${ex.message}" }
                ++errors
                lastError = ex.message
            }
        }
        notificationDao.findPendingOfOtherRules(rules.mapNotNull { it.id }).forEach {
            notificationDao.finish(it, NotificationStatus.EXPIRED)
        }
        return RunResult(errors = errors, lastError = lastError)
    }

    /** Delivers the due, not yet processed steps of the notification (re-read: the recipient may have confirmed it). */
    internal fun deliverDueSteps(given: NotificationDO, now: Date): RunResult {
        val notification = notificationDao.findById(given.id) ?: return RunResult()
        val state = notification.readDeliveryState()
        val delivery = notification.readDelivery()
        val recipient = caches.getUser(notification.recipientId)
        var errors = 0
        var lastError: String? = null
        state.steps.filter { !it.isProcessed && it.dueAt?.after(now) != true }.sortedBy { it.step }.forEach { step ->
            val onlyIfUnacknowledged = delivery.steps.getOrNull(step.step)?.onlyIfUnacknowledged == true
            if (!notification.status.isPending || onlyIfUnacknowledged && notification.status != NotificationStatus.OPEN) {
                step.skipped = true
                return@forEach
            }
            val sender = senders[step.channel]
            if (sender == null || recipient == null || recipient.deleted || !recipient.hasSystemAccess()) {
                step.skipped = true
                step.error = if (sender == null) "Channel ${step.channel} not supported." else "Recipient not available."
                return@forEach
            }
            try {
                val reason = sender.deliver(notification, recipient, now)
                if (reason == null) {
                    step.sentAt = now
                } else {
                    step.skipped = true
                    step.error = reason
                }
            } catch (ex: Exception) {
                val error = ex.message ?: ex.javaClass.simpleName
                val attempt = step.attempts + 1
                if (step.recordFailure(error, now)) {
                    log.warn(NotificationLogEvents.DELIVERY_FAILED, ex) { "Notification #${notification.id}: ${step.channel} to user #${recipient.id} failed (attempt $attempt of ${NotificationDeliveryState.MAX_ATTEMPTS}), retry at ${step.dueAt}: $error" }
                } else {
                    log.error(NotificationLogEvents.DELIVERY_FAILED, ex) { "Notification #${notification.id}: ${step.channel} to user #${recipient.id} failed finally after $attempt attempts: $error" }
                }
                ++errors
                lastError = error
            }
        }
        notification.writeDeliveryState(state)
        notificationDao.updateDelivery(notification)
        given.deliveryState = notification.deliveryState
        given.nextDeliveryAt = notification.nextDeliveryAt
        given.inAppSince = notification.inAppSince
        return RunResult(errors = errors, lastError = lastError)
    }

    private fun recipientsOf(
        rule: NotificationRuleDO,
        handler: NotificationRuleHandler,
        evaluation: NotificationRuleHandler.Evaluation,
        today: LocalDate,
    ): List<PFUserDO> {
        val affected = if (evaluation.restrictsRecipients) evaluation.affected.keys else null
        return recipientResolver.resolve(rule.readRecipients(), affected)
            .filter { handler.isRelevant(rule, today, it) }
    }

    private fun variablesOf(
        rule: NotificationRuleDO,
        handler: NotificationRuleHandler,
        evaluation: NotificationRuleHandler.Evaluation?,
        today: LocalDate,
        user: PFUserDO,
    ): Map<String, String> {
        val common = mapOf(
            "firstName" to (user.firstname ?: ""),
            "lastName" to (user.lastname ?: ""),
            "fullName" to user.getFullname(),
            // The first name, if the user has no nickname, so "Hi {{nickname}}," always works.
            "nickname" to (user.nickname?.takeIf { it.isNotBlank() } ?: user.firstname ?: ""),
        )
        return common + (evaluation?.affected?.get(user.id) ?: handler.variables(rule, today, user))
    }

    private fun build(
        rule: NotificationRuleDO,
        user: PFUserDO,
        variables: Map<String, String>,
        evaluation: NotificationRuleHandler.Evaluation?,
        now: Date,
    ): NotificationDO {
        val delivery = rule.readDelivery()
        return NotificationDO().also {
            it.ruleId = rule.id
            it.recipientId = user.id
            it.severity = rule.severity
            it.display = rule.display
            it.manualDone = rule.manualDone
            it.menuBadge = rule.menuBadge?.takeIf { badge -> badge.isNotBlank() }
            it.title = NotificationTemplate.renderSubject(rule.subject, variables)
            it.body = NotificationTemplate.renderText(rule.text, variables)
            it.link = evaluation?.linkFor(user)
            it.periodKey = evaluation?.periodKey
            it.status = NotificationStatus.OPEN
            it.created = now
            it.delivery = JsonUtils.toJson(delivery)
            it.writeDeliveryState(NotificationDeliveryState.of(delivery, now))
        }
    }

    private fun handler(rule: NotificationRuleDO): NotificationRuleHandler {
        val ruleType = rule.ruleType ?: throw IllegalArgumentException("Notification rule #${rule.id} has no type.")
        return handlers[ruleType] ?: throw IllegalArgumentException("No handler for notification rule type $ruleType.")
    }

    companion object {
        const val RULE_JOB_TYPE = "notification-rules"
        const val DELIVERY_JOB_TYPE = "notification-delivery"

        /** The variables of all rule types. */
        val COMMON_VARIABLES = listOf("firstName", "lastName", "fullName", "nickname")

        /** HTML tags, removed to check whether a rich text is empty (e.g. `<p></p>`). */
        private val TAG_REGEX = Regex("<[^>]*>")

        fun dedupKey(rule: NotificationRuleDO, user: PFUserDO, periodKey: String): String =
            "${rule.id}:${user.id}:$periodKey"

        fun today(): LocalDate = LocalDate.now(zoneId())

        internal fun toLocalDate(date: Date): LocalDate = date.toInstant().atZone(zoneId()).toLocalDate()

        private fun zoneId() = Configuration.instance.defaultTimeZone.toZoneId()
    }
}
