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

import io.github.oshai.kotlinlogging.KotlinLogging
import jakarta.servlet.http.HttpServletRequest
import org.projectforge.business.PfCaches
import org.projectforge.business.notification.NotificationChannel
import org.projectforge.business.notification.NotificationDO
import org.projectforge.business.notification.NotificationDao
import org.projectforge.business.notification.NotificationParams
import org.projectforge.business.notification.NotificationRuleDO
import org.projectforge.business.notification.NotificationRuleDao
import org.projectforge.business.notification.NotificationRuleType
import org.projectforge.business.notification.NotificationService
import org.projectforge.business.notification.NotificationStatus
import org.projectforge.framework.access.AccessException
import org.projectforge.framework.configuration.Configuration
import org.projectforge.framework.i18n.translate
import org.projectforge.framework.i18n.translateMsg
import org.projectforge.framework.persistence.user.api.ThreadLocalUserContext
import org.projectforge.menu.builder.MenuItemDefId
import org.projectforge.rest.config.Rest
import org.projectforge.rest.core.AbstractDTOEntityRest
import org.projectforge.rest.core.AccessChecked
import org.projectforge.rest.core.RestButtonEvent
import org.projectforge.rest.dto.PostData
import org.projectforge.rest.dto.User
import org.projectforge.sms.SmsSenderConfig
import org.projectforge.ui.ResponseAction
import org.projectforge.ui.UIColor
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

private val log = KotlinLogging.logger {}

/**
 * The layout-free REST endpoint of the rules of the notification system, serving the hand-built projectforge-next
 * page (components/features/notification-rule), for admins and finance (see [NotificationRuleDao.hasAccess]).
 *
 * Besides the standard list/edit endpoints: the preview, a test to oneself (both of the maybe unsaved rule of the
 * form), the trigger of a saved rule and the log of its notifications, in which editors may end pending ones.
 */
@RestController
@RequestMapping("${Rest.URL}/notificationRule")
class NotificationRuleEntityRest : AbstractDTOEntityRest<NotificationRuleDO, NotificationRule, NotificationRuleDao>(
    NotificationRuleDao::class.java, "notification.rule",
) {
    /** A variable `{{key}}` of the rule texts with its translated label. */
    /**
     * [common]: available for every rule type (names of the recipient), otherwise only for the given one. [example]:
     * how a value looks (e.g. a list of invoices), from synthetic data.
     */
    class Variable(val key: String, val label: String, val common: Boolean, val example: String?)

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
        /** Subject and text rendered with example values. */
        val exampleSubject: String,
        val exampleText: String,
        /** The SMS as sent (own values and example values), null if the cascade has no SMS step. */
        val sms: String?,
        val exampleSms: String?,
    )

    /** Is SMS configured (otherwise SMS steps are skipped), and the max length of an SMS. */
    class SmsInfo(val configured: Boolean, val maxLength: Int)

    class TestResult(val delivered: List<NotificationChannel>, val failed: Map<NotificationChannel, String>)

    class TriggerResult(val recipients: Int, val created: Int, val errors: Int, val lastError: String?)

    /** One notification in the log: who got it and who confirmed it, when. */
    class NotificationEntry(
        val id: Long?,
        /** The name of the rule, null for an ad hoc notification or a deleted rule. */
        val ruleName: String?,
        val recipient: String?,
        /** The recipient is no longer active (deactivated or deleted): the name is struck through. */
        val recipientDeactivated: Boolean,
        val status: NotificationStatus,
        val periodKey: String?,
        val title: String?,
        val created: Date?,
        val inAppSince: Date?,
        val acknowledgedAt: Date?,
        val acknowledgedBy: String?,
        /** As [recipientDeactivated], for the confirming user. */
        val acknowledgedByDeactivated: Boolean,
        val doneAt: Date?,
        val resolvedAt: Date?,
        val deliverySteps: List<DeliveryStepEntry>,
        /** May the logged-in user end it: pending and editor of its rule? */
        val finishable: Boolean = false,
    )

    class FinishResult(val finished: Int)

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

    @Autowired
    private lateinit var smsSenderConfig: SmsSenderConfig

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
        if ((dto.smsText?.length ?: 0) > MAX_SMS_TEXT_LENGTH) {
            validationErrors += ValidationError(
                translateMsg("notification.rule.error.smsTextLength", MAX_SMS_TEXT_LENGTH), fieldId = "smsText",
            )
        }
        if (dto.deliverySteps.isEmpty()) {
            validationErrors += ValidationError(translate("notification.rule.error.noDeliveryStep"), fieldId = "deliverySteps")
        }
        if (dto.deliverySteps.any { it.delayMinutes < 0 }) {
            validationErrors += ValidationError(translate("notification.rule.error.delay"), fieldId = "deliverySteps")
        }
        if (dto.linkMode == NotificationParams.LinkMode.MENU_ITEM &&
            MenuItemDefId.entries.none { it.id == dto.linkMenuItem && it.url != null }
        ) {
            validationErrors += required("linkMenuItem", "notification.rule.link")
        }
        if (dto.daysBeforeExpiry < 0) {
            validationErrors += ValidationError(translate("notification.rule.error.daysBeforeExpiry"), fieldId = "daysBeforeExpiry")
        }
        if (dto.ruleType == NotificationRuleType.PASSWORD_NOT_CHANGED && dto.passwordMaxAgeDays < 1) {
            validationErrors += ValidationError(translate("notification.rule.error.passwordMaxAgeDays"), fieldId = "passwordMaxAgeDays")
        }
        if (dto.ruleType == NotificationRuleType.TIMESHEETS_WITHOUT_KOST2 && dto.kost2MissingMonths !in 1..12) {
            validationErrors += ValidationError(translate("notification.rule.error.kost2MissingMonths"), fieldId = "kost2MissingMonths")
        }
        if (dto.ruleType == NotificationRuleType.CREDITOR_INVOICES_DUE) {
            if (dto.dueWorkingDays !in 0..30) {
                validationErrors += ValidationError(translate("notification.rule.error.dueWorkingDays"), fieldId = "dueWorkingDays")
            }
            // Nobody is concerned by an invoice, so "only those concerned" without recipients would notify nobody.
            if (dto.recipientGroups.isEmpty() && dto.recipientUsers.isEmpty() && !dto.allEmployees && dto.employeeStatus.isEmpty()) {
                validationErrors += ValidationError(translate("notification.rule.error.recipientsRequired"), fieldId = "recipientGroups")
            }
        }
    }

    /** A rule of time sheets without cost 2 unit notifies nobody, as long as these aren't reported at all. */
    override fun onAfterEdit(
        request: HttpServletRequest,
        obj: NotificationRuleDO,
        postData: PostData<NotificationRule>,
        event: RestButtonEvent,
    ): ResponseAction {
        val responseAction = super.onAfterEdit(request, obj, postData, event)
        if (obj.ruleType == NotificationRuleType.TIMESHEETS_WITHOUT_KOST2 && !Configuration.instance.isWarnMissingKost2) {
            responseAction.message =
                ResponseAction.Message("notification.rule.warning.warnMissingKost2Off", color = UIColor.WARNING)
        }
        return responseAction
    }

    /** The variables of the texts of the given rule type (the common ones for none). */
    @AccessChecked("DAO: NotificationRuleDao insert access (admin/finance, checkInsertAccess)")
    @GetMapping("variables")
    fun getVariables(@RequestParam("ruleType", required = false) ruleType: NotificationRuleType?): List<Variable> {
        checkInsertAccess()
        val examples = notificationService.exampleVariables(ruleType, ThreadLocalUserContext.requiredLoggedInUser)
        return notificationService.variables(ruleType).map {
            Variable(it, translate("notification.variable.$it"), it in NotificationService.COMMON_VARIABLES, examples[it])
        }
    }

    /** Is SMS configured? The editor offers SMS steps only then. */
    @AccessChecked("DAO: NotificationRuleDao insert access (admin/finance, checkInsertAccess)")
    @GetMapping("smsInfo")
    fun getSmsInfo(): SmsInfo {
        checkInsertAccess()
        return SmsInfo(smsSenderConfig.isSmsConfigured(), smsSenderConfig.smsMaxMessageLength)
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
            exampleSubject = preview.exampleSubject,
            exampleText = preview.exampleText,
            sms = preview.sms,
            exampleSms = preview.exampleSms,
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

    /**
     * Runs the saved rule now, regardless of its schedule (recipients already notified aren't notified twice).
     * An inactive rule isn't run: its refusal is reported as the error of the result (shown by the client).
     */
    @AccessChecked("DAO: NotificationRuleDao.find (select access) + isEditor of the rule + not restricted/demo")
    @PostMapping("{id}/trigger")
    fun trigger(@PathVariable("id") id: Long): TriggerResult {
        val rule = findAsEditor(id)
        accessChecker.checkRestrictedOrDemoUser()
        if (!rule.active) {
            return TriggerResult(0, 0, 1, translate("notification.rule.trigger.inactive"))
        }
        val result = notificationService.trigger(rule)
        return TriggerResult(result.recipients, result.created, result.errors, result.lastError)
    }

    /** The log of the notifications of the rule, the newest first. */
    @AccessChecked("DAO: NotificationRuleDao.find (select access)")
    @GetMapping("{id}/notifications")
    fun getNotifications(@PathVariable("id") id: Long): List<NotificationEntry> {
        val rule = baseDao.find(id) ?: return emptyList()
        val editor = baseDao.isEditor(ThreadLocalUserContext.requiredLoggedInUser, rule)
        return notificationDao.select(NotificationDao.Filter(ruleId = rule.id, maxResults = MAX_LOG_ENTRIES))
            .map { toEntry(it, rule.name, editor) }
    }

    /**
     * Ends all pending notifications of the rule (expired), e.g. banners nobody can mark as done. For editors of the
     * rule.
     */
    @AccessChecked("DAO: NotificationRuleDao.find (select access) + isEditor of the rule + not restricted/demo")
    @PostMapping("{id}/finishPending")
    fun finishPending(@PathVariable("id") id: Long): FinishResult {
        val rule = findAsEditor(id)
        accessChecker.checkRestrictedOrDemoUser()
        val finished = baseDao.expirePending(rule)
        log.info { "Notification rule #${rule.id} '${rule.name}': $finished pending notification(s) ended by hand." }
        return FinishResult(finished)
    }

    /**
     * Ends the pending notification (expired). For editors of its rule (also of a deleted one), for a notification
     * without rule (a test) for all rule editors.
     */
    @AccessChecked("DAO: NotificationRuleDao.checkLoggedInUserInsertAccess (admin/finance) + isEditor of the rule + not restricted/demo")
    @PostMapping("notifications/{notificationId}/finish")
    fun finish(@PathVariable("notificationId") notificationId: Long): NotificationEntry {
        checkInsertAccess()
        accessChecker.checkRestrictedOrDemoUser()
        val user = ThreadLocalUserContext.requiredLoggedInUser
        val notification = notificationDao.findById(notificationId)
            ?: throw IllegalArgumentException("Notification #$notificationId not found.")
        val rule = baseDao.findInternal(notification.ruleId)
        if (!baseDao.isEditor(user, rule)) {
            throw AccessException(user, "access.exception.noAccess")
        }
        if (notification.status.isPending) {
            notificationDao.finish(notification, NotificationStatus.EXPIRED)
            log.info { "Notification #${notification.id} of rule #${notification.ruleId} ended by hand." }
        }
        return toEntry(notification, rule?.name, editor = true)
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
        val user = ThreadLocalUserContext.requiredLoggedInUser
        val rules = baseDao.selectAllInternal().associateBy { it.id }
        // Rules not found (deleted) restrict nobody: their notifications may be ended by every rule editor.
        val editorOfOthers = baseDao.isEditor(user, null)
        return notificationDao.select(filter).map {
            val rule = rules[it.ruleId]
            toEntry(it, rule?.name, editor = if (rule == null) editorOfOthers else baseDao.isEditor(user, rule))
        }
    }

    private fun toEntry(notification: NotificationDO, ruleName: String?, editor: Boolean): NotificationEntry {
        return NotificationEntry(
            id = notification.id,
            ruleName = ruleName,
            recipient = caches.getUser(notification.recipientId)?.getFullname(),
            recipientDeactivated = User.isInactive(caches.getUser(notification.recipientId)),
            status = notification.status,
            periodKey = notification.periodKey,
            title = notification.title,
            created = notification.created,
            inAppSince = notification.inAppSince,
            acknowledgedAt = notification.acknowledgedAt,
            acknowledgedBy = caches.getUser(notification.acknowledgedById)?.getFullname(),
            acknowledgedByDeactivated = User.isInactive(caches.getUser(notification.acknowledgedById)),
            doneAt = notification.doneAt,
            resolvedAt = notification.resolvedAt,
            deliverySteps = notification.readDeliveryState().steps.map {
                DeliveryStepEntry(it.channel, it.dueAt, it.sentAt, it.skipped, it.failed, it.attempts, it.error)
            },
            finishable = editor && notification.status.isPending,
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
        private const val MAX_SMS_TEXT_LENGTH = 1000
    }
}
