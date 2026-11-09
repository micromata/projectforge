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

/**
 * Replaces the generic text of a notification by a preview of what it is about, when shown in the app or a push to its
 * recipient (e.g. the beginning of a chat message). Built when shown, not stored: the mail of the same notification
 * keeps the generic text, and a changed or deleted source is never shown out of date. A Spring bean.
 */
interface NotificationPreview {
    /** The text shown instead of [NotificationDO.body] (plain text), null to keep the body. */
    fun preview(notification: NotificationDO, recipient: PFUserDO): String?
}
