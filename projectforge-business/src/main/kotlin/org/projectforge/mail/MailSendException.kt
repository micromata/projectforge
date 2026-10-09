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

package org.projectforge.mail

import org.projectforge.common.logging.LogEvent
import org.projectforge.common.logging.LogEventAware
import org.projectforge.framework.i18n.InternalErrorException

/**
 * A mail couldn't be created or sent; [SendMail] has already reported it as [MailLogEvents.SEND_FAILED]. The user gets
 * the message of `mail.error.exception`. If a caller logs it again (`log.error(ex) { ... }`), it's classified as
 * [MailLogEvents.SEND_FAILED_REPORTED] instead of an unclassified error, so the failure isn't reported twice.
 */
class MailSendException(cause: Throwable) : InternalErrorException("mail.error.exception"), LogEventAware {
    init {
        initCause(cause)
    }

    override val logEvent: LogEvent
        get() = MailLogEvents.SEND_FAILED_REPORTED
}
