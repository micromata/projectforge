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

import org.projectforge.framework.persistence.user.entities.PFUserDO
import org.springframework.stereotype.Service
import java.time.LocalDate

/**
 * No condition: all recipients are notified, by schedule or by hand. Every run is a new period (the dedup key
 * contains the time of the run), the notifications are finished by their recipients only.
 */
@Service
class ManualRuleHandler : NotificationRuleHandler {
    override val ruleType = NotificationRuleType.MANUAL

    override val variables = emptyList<String>()

    override fun evaluate(rule: NotificationRuleDO, today: LocalDate): NotificationRuleHandler.Evaluation {
        return NotificationRuleHandler.Evaluation("run-${System.currentTimeMillis()}", emptyMap(), restrictsRecipients = false)
    }

    override fun variables(rule: NotificationRuleDO, today: LocalDate, user: PFUserDO): Map<String, String> = emptyMap()
}
