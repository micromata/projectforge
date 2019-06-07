/////////////////////////////////////////////////////////////////////////////
//
// Project ProjectForge Community Edition
//         www.projectforge.org
//
// Copyright (C) 2001-2019 Micromata GmbH, Germany (www.micromata.com)
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

package org.projectforge.rest

import org.projectforge.framework.i18n.translate
import org.projectforge.framework.i18n.translateMsg
import org.projectforge.ui.UIStyle

/**
 * Contains response information for the client, such as the result info an information for displaying e. g. in a
 * client's toast.
 */
class ResponseData(
        val i18nKey: String? = null,
        /**
         * The message to display for the user.
         */
        var message: String? = null,
        /** The (technical) message. */
        var technicalMessage: String? = null,
        var messageType: MessageType = MessageType.TEXT,
        var style: UIStyle? = null,
        vararg messageParams: String) {
    init {
        if (message == null && i18nKey != null) {
            if (messageParams.isNotEmpty())
                message = translateMsg(i18nKey, messageParams)
            else
                message = translate(i18nKey)
        }
    }
}

enum class MessageType { TEXT, TOAST }
