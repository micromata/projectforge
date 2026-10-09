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

package org.projectforge.framework.support

import jakarta.annotation.PostConstruct
import org.projectforge.business.fibu.ExchangeRateLogEvents
import org.projectforge.business.jobs.JobLogEvents
import org.projectforge.business.notification.NotificationLogEvents
import org.projectforge.business.user.UserLogEvents
import org.projectforge.common.logging.CommonLogEvents
import org.projectforge.common.logging.LogEventRegistry
import org.projectforge.framework.access.AccessLogEvents
import org.projectforge.framework.integration.IntegrationLogEvents
import org.projectforge.framework.persistence.jpa.PersistenceLogEvents
import org.projectforge.mail.MailLogEvents
import org.springframework.stereotype.Component

/**
 * Registers the log events of the common and business modules in the [LogEventRegistry].
 */
@Component
class BusinessLogEventCatalog {
    @PostConstruct
    internal fun init() {
        LogEventRegistry.register(*HOLDERS)
    }

    companion object {
        /** New `*LogEvents` objects of these modules belong here. */
        val HOLDERS = arrayOf(
            CommonLogEvents,
            SupportLogEvents,
            MailLogEvents,
            PersistenceLogEvents,
            ExchangeRateLogEvents,
            JobLogEvents,
            IntegrationLogEvents,
            UserLogEvents,
            AccessLogEvents,
            ThirdPartyLogEvents,
            NotificationLogEvents,
        )
    }
}
