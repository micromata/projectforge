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


package org.projectforge.rest.notification

import org.projectforge.business.PfCaches
import org.projectforge.business.notification.NotificationChannel
import org.projectforge.business.notification.NotificationDO
import org.projectforge.business.notification.NotificationDao
import org.projectforge.business.notification.NotificationRuleDO
import org.projectforge.business.notification.NotificationRuleDao
import org.projectforge.business.notification.NotificationRuleType
import org.projectforge.business.notification.NotificationService
import org.projectforge.business.notification.NotificationStatus
import org.projectforge.framework.access.AccessException
import org.projectforge.framework.i18n.translate
import org.projectforge.framework.i18n.translateMsg
import org.projectforge.framework.persistence.user.api.ThreadLocalUserContext
import org.projectforge.menu.builder.MenuItemDefId
import org.projectforge.rest.config.Rest
import org.projectforge.rest.core.AbstractDTOEntityRest
import org.projectforge.rest.core.AccessChecked
import org.projectforge.ui.ValidationError
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.RestController
import java.util.Date

/**
 * The layout-free REST endpoint of the rules of the notification system, serving the hand-built projectforge-next
 * page (components/features/notification-rule), for admins and finance (see [NotificationRuleDao.hasAccess]).
 *
 * Besides the standard list/edit endpoints: the preview, a test to oneself (both of the maybe unsaved rule of the
 * form), the trigger of a saved rule and the log of its notifications.
 */
@RestController
@RequestMapping("${Rest.URL}/notificationRule")
class NotificationRuleEntityRest : AbstractDTOEntityRest<NotificationRuleDO, NotificationRule, NotificationRuleDao>(
    NotificationRuleDao::class.java, "notification.rule.title",
) {
    /** A variable `{{key}}` of the rule texts with its translated label. */
    class Variable(val key: String, val label: String)

    /** A menu item a rule may show its notifications on as a counter. */
    class MenuBadgeOption(val id: String, val label: String)

    class PreviewResult(
        val recipientCount: Int,
        /** The first recipients (names), at most [MAX_PREVIEW_RECIPIENTS]. */
        val recipients: List<String>,
        val alreadyNotified: Int,
        val periodKey: String?,
        val subject: String,
        val text: String,
    )

    class TestResult(val delivered: List<NotificationChannel>, val failed: Map<NotificationChannel, String>)

    class TriggerResult(val recipients: Int, val created: Int, val errors: Int, val lastError: String?)

    /** One notification in the log: who got it and who confirmed it, when. */
    class NotificationEntry(
        val id: Long?,
        /** The name of the rule, null for an ad hoc notification or a deleted rule. */
        val ruleName: String?,
        val recipient: String?,
        val status: NotificationStatus,
        val periodKey: String?,
        val title: String?,
        val created: Date?,
        val inAppSince: Date?,
        val acknowledgedAt: Date?,
        val acknowledgedBy: String?,
        val doneAt: Date?,
        val resolvedAt: Date?,
        val deliverySteps: List<DeliveryStepEntry>,
    )

    class DeliveryStepEntry(
        val channel: NotificationChannel,
        val dueAt: Date?,
        val sentAt: Date?,
        val skipped: Boolean,
        val failed: Boolean,
        val attempts: Int,
        val error: String?,
    )

    @Autowired
    private lateinit var caches: PfCaches

    @Autowired
    private lateinit var notificationDao: NotificationDao

    @Autowired
    private lateinit var notificationService: NotificationService

    override fun transformFromDB(obj: NotificationRuleDO, editMode: Boolean): NotificationRule {
        val rule = NotificationRule()
        rule.copyFrom(obj)
        return rule
    }

    override fun transformForDB(dto: NotificationRule): NotificationRuleDO {
        val rule = NotificationRuleDO()
        dto.copyTo(rule)
        return rule
    }

    override fun validate(validationErrors: MutableList<ValidationError>, dto: NotificationRule) {
        if (dto.ruleType == null) {
            validationErrors += required("ruleType", "notification.rule.ruleType")
        }
        dto.schedule.validate().forEach { (field, i18nKey) ->
            // schedule.day -> scheduleDay: the form edits the schedule flat.
            val fieldId = field.split('.').mapIndexed { i, part -> if (i == 0) part else part.replaceFirstChar { it.uppercase() } }
                .joinToString("")
            validationErrors += ValidationError(translate(i18nKey), fieldId = fieldId)
        }
        if (dto.subject.isNullOrBlank()) {
            validationErrors += required("subject", "notification.subject")
        }
        if (dto.text.isNullOrBlank()) {
            validationErrors += required("text", "notification.text")
        }
        if (dto.deliverySteps.isEmpty()) {
            validationErrors += ValidationError(translate("notification.rule.error.noDeliveryStep"), fieldId = "deliverySteps")
        }
        if (dto.deliverySteps.any { it.delayMinutes < 0 }) {
            validationErrors += ValidationError(translate("notification.rule.error.delay"), fieldId = "deliverySteps")
        }
        if (dto.daysBeforeExpiry < 0) {
            validationErrors += ValidationError(translate("notification.rule.error.daysBeforeExpiry"), fieldId = "daysBeforeExpiry")
        }
    }

    /** The variables of the texts of the given rule type (the common ones for none). */
    @AccessChecked("DAO: NotificationRuleDao insert access (admin/finance, checkInsertAccess)")
    @GetMapping("variables")
    fun getVariables(@RequestParam("ruleType", required = false) ruleType: NotificationRuleType?): List<Variable> {
        checkInsertAccess()
        return notificationService.variables(ruleType).map { Variable(it, translate("notification.variable.$it")) }
    }

    /** The menu items selectable as menu badge, sorted by their translated labels. */
    @AccessChecked("DAO: NotificationRuleDao insert access (admin/finance, checkInsertAccess)")
    @GetMapping("menuBadges")
    fun getMenuBadges(): List<MenuBadgeOption> {
        checkInsertAccess()
        return MenuItemDefId.entries.filter { it.url != null }
            .map { MenuBadgeOption(it.id, translate(it.i18nKey)) }
            .sortedBy { it.label.lowercase() }
    }

    /** The recipients and the texts rendered for the logged-in user of the given (maybe unsaved) rule. */
    @AccessChecked("DAO: NotificationRuleDao insert access (admin/finance, checkInsertAccess)")
    @PostMapping("preview")
    fun preview(@RequestBody dto: NotificationRule): PreviewResult {
        checkInsertAccess()
        val user = ThreadLocalUserContext.requiredLoggedInUser
        val preview = notificationService.preview(transformForDB(dto), user)
        return PreviewResult(
            recipientCount = preview.recipients.size,
            recipients = preview.recipients.take(MAX_PREVIEW_RECIPIENTS).map { it.getFullname() },
            alreadyNotified = preview.alreadyNotified,
            periodKey = preview.periodKey,
            subject = preview.subject,
            text = preview.text,
        )
    }

    /** Sends the given (maybe unsaved) rule to the logged-in user only, through all channels of its cascade. */
    @AccessChecked("DAO: NotificationRuleDao insert access (admin/finance, checkInsertAccess) + not restricted/demo")
    @PostMapping("testToMe")
    fun testToMe(@RequestBody dto: NotificationRule): TestResult {
        checkInsertAccess()
        accessChecker.checkRestrictedOrDemoUser()
        val result = notificationService.sendTestToMe(transformForDB(dto), ThreadLocalUserContext.requiredLoggedInUser)
        return TestResult(result.delivered, result.failed)
    }

    /** Runs the saved rule now, regardless of its schedule (recipients already notified aren't notified twice). */
    @AccessChecked("DAO: NotificationRuleDao.find (select access) + isEditor of the rule + not restricted/demo")
    @PostMapping("{id}/trigger")
    fun trigger(@PathVariable("id") id: Long): TriggerResult {
        val rule = findAsEditor(id)
        accessChecker.checkRestrictedOrDemoUser()
        val result = notificationService.trigger(rule)
        return TriggerResult(result.recipients, result.created, result.errors, result.lastError)
    }

    /** The log of the notifications of the rule, the newest first. */
    @AccessChecked("DAO: NotificationRuleDao.find (select access)")
    @GetMapping("{id}/notifications")
    fun getNotifications(@PathVariable("id") id: Long): List<NotificationEntry> {
        val rule = baseDao.find(id) ?: return emptyList()
        return notificationDao.select(NotificationDao.Filter(ruleId = rule.id, maxResults = MAX_LOG_ENTRIES))
            .map { toEntry(it, rule.name) }
    }

    /**
     * The log of the notifications of all rules, the newest first (at most [MAX_LOG_ENTRIES]), optionally of one
     * recipient and of the given statuses only.
     */
    @AccessChecked("DAO: NotificationRuleDao.checkLoggedInUserSelectAccess (admins and finance)")
    @GetMapping("log")
    fun getLog(
        @RequestParam("recipientId", required = false) recipientId: Long?,
        @RequestParam("status", required = false) statuses: List<NotificationStatus>?,
    ): List<NotificationEntry> {
        baseDao.checkLoggedInUserSelectAccess()
        val filter = NotificationDao.Filter(recipientId = recipientId, statuses = statuses, maxResults = MAX_LOG_ENTRIES)
        val ruleNames = baseDao.selectAllInternal().associate { it.id to it.name }
        return notificationDao.select(filter).map { toEntry(it, ruleNames[it.ruleId]) }
    }

    private fun toEntry(notification: NotificationDO, ruleName: String?): NotificationEntry {
        return NotificationEntry(
            id = notification.id,
            ruleName = ruleName,
            recipient = caches.getUser(notification.recipientId)?.getFullname(),
            status = notification.status,
            periodKey = notification.periodKey,
            title = notification.title,
            created = notification.created,
            inAppSince = notification.inAppSince,
            acknowledgedAt = notification.acknowledgedAt,
            acknowledgedBy = caches.getUser(notification.acknowledgedById)?.getFullname(),
            doneAt = notification.doneAt,
            resolvedAt = notification.resolvedAt,
            deliverySteps = notification.readDeliveryState().steps.map {
                DeliveryStepEntry(it.channel, it.dueAt, it.sentAt, it.skipped, it.failed, it.attempts, it.error)
            },
        )
    }

    private fun required(field: String, i18nKey: String): ValidationError {
        return ValidationError(
            translateMsg("validation.error.fieldRequired", translate(i18nKey)),
            fieldId = field,
        )
    }

    private fun checkInsertAccess() {
        baseDao.checkLoggedInUserInsertAccess(NotificationRuleDO())
    }

    private fun findAsEditor(id: Long): NotificationRuleDO {
        val rule = baseDao.find(id) ?: throw IllegalArgumentException("Notification rule #$id not found.")
        val user = ThreadLocalUserContext.requiredLoggedInUser
        if (!baseDao.isEditor(user, rule)) {
            throw AccessException(user, "access.exception.noAccess")
        }
        return rule
    }

    companion object {
        private const val MAX_PREVIEW_RECIPIENTS = 50
        private const val MAX_LOG_ENTRIES = 500
    }
}
