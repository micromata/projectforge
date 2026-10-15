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
import jakarta.persistence.*
import org.hibernate.search.mapper.pojo.mapping.definition.annotation.FullTextField
import org.hibernate.search.mapper.pojo.mapping.definition.annotation.Indexed
import org.projectforge.common.anots.PropertyInfo
import org.projectforge.framework.DisplayNameCapable
import org.projectforge.framework.json.JsonUtils
import org.projectforge.framework.persistence.entities.DefaultBaseDO
import org.projectforge.framework.persistence.history.NoHistory
import java.util.Date

private val log = KotlinLogging.logger {}

/**
 * A rule of the notification system: what is checked ([ruleType] with its [params]), when ([schedule]), who gets the
 * notification ([recipients]), how ([display], [severity] and the delivery cascade [delivery]) and with which text
 * ([subject] and [text] with variables `{{key}}`). The options are stored as JSON, so new options need no new column.
 */
@Entity
@Indexed
@Table(name = "T_NOTIFICATION_RULE")
open class NotificationRuleDO : DefaultBaseDO(), DisplayNameCapable {

    override val displayName: String
        @Transient
        get() = name ?: ""

    @PropertyInfo(i18nKey = "notification.rule.name")
    @FullTextField
    @get:Column(length = 255, nullable = false)
    open var name: String? = null

    @PropertyInfo(i18nKey = "description")
    @FullTextField
    @get:Column(length = 4000)
    open var description: String? = null

    @PropertyInfo(i18nKey = "notification.rule.active")
    @get:Column(nullable = false)
    open var active: Boolean = false

    @PropertyInfo(i18nKey = "notification.rule.ruleType")
    @get:Enumerated(EnumType.STRING)
    @get:Column(name = "rule_type", length = 50, nullable = false)
    open var ruleType: NotificationRuleType? = null

    /** JSON of [NotificationSchedule]. */
    @PropertyInfo(i18nKey = "notification.rule.schedule")
    @get:Column(length = 1000)
    open var schedule: String? = null

    /** JSON of [NotificationParams]. */
    @PropertyInfo(i18nKey = "notification.rule.params")
    @get:Column(length = 4000)
    open var params: String? = null

    /** JSON of [NotificationRecipients]. */
    @PropertyInfo(i18nKey = "notification.rule.recipients")
    @get:Column(length = 10000)
    open var recipients: String? = null

    /** JSON of [NotificationDelivery]. */
    @PropertyInfo(i18nKey = "notification.rule.delivery")
    @get:Column(length = 4000)
    open var delivery: String? = null

    @PropertyInfo(i18nKey = "notification.severity")
    @get:Enumerated(EnumType.STRING)
    @get:Column(length = 20, nullable = false)
    open var severity: NotificationSeverity = NotificationSeverity.INFO

    @PropertyInfo(i18nKey = "notification.display")
    @get:Enumerated(EnumType.STRING)
    @get:Column(length = 20, nullable = false)
    open var display: NotificationDisplay = NotificationDisplay.BANNER

    /** May the recipients mark a notification as done? Otherwise only the rule resolves it. */
    @PropertyInfo(i18nKey = "notification.rule.manualDone")
    @get:Column(name = "manual_done", nullable = false)
    open var manualDone: Boolean = false

    /** The id of a menu item (`MenuItemDefId`), whose counter shows the pending notifications of this rule. */
    @PropertyInfo(i18nKey = "notification.rule.menuBadge")
    @get:Column(name = "menu_badge", length = 100)
    open var menuBadge: String? = null

    /** Plain text with variables `{{key}}`. */
    @PropertyInfo(i18nKey = "notification.subject")
    @FullTextField
    @get:Column(length = 1000)
    open var subject: String? = null

    /** Rich text (HTML) with variables `{{key}}`, sanitized when rendered. */
    @PropertyInfo(i18nKey = "notification.text")
    @get:Column(length = 100000)
    open var text: String? = null

    /**
     * If given, only members of these groups may edit this rule (besides admins), who must be finance members anyway.
     * CSV of group ids.
     */
    @PropertyInfo(i18nKey = "notification.rule.editableByGroups")
    @get:Column(name = "editable_by_group_ids", length = 10000)
    open var editableByGroupIds: String? = null

    /** The script of a script rule (not yet supported). */
    @get:Column(name = "script_fk")
    open var scriptId: Long? = null

    /** The last run by the schedule (the rule runs at most once per day by schedule). */
    @NoHistory
    @get:Column(name = "last_run")
    open var lastRun: Date? = null

    fun readSchedule(): NotificationSchedule = parse(schedule, NotificationSchedule::class.java) { NotificationSchedule() }

    fun readParams(): NotificationParams = parse(params, NotificationParams::class.java) { NotificationParams() }

    fun readRecipients(): NotificationRecipients =
        parse(recipients, NotificationRecipients::class.java) { NotificationRecipients() }

    fun readDelivery(): NotificationDelivery = parse(delivery, NotificationDelivery::class.java) { NotificationDelivery() }

    /** The ids of [editableByGroupIds]. */
    fun readEditableByGroupIds(): List<Long> =
        editableByGroupIds?.split(',')?.mapNotNull { it.trim().toLongOrNull() } ?: emptyList()

    private fun <T> parse(json: String?, cls: Class<T>, default: () -> T): T {
        if (json.isNullOrBlank()) {
            return default()
        }
        return try {
            JsonUtils.fromJson(json, cls, failOnUnknownProps = false) ?: default()
        } catch (ex: Exception) {
            log.warn { "Notification rule #$id: ${cls.simpleName} not readable, using the defaults: ${ex.message}" }
            default()
        }
    }
}
