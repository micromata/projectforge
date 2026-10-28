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

package org.projectforge.business.chat

/**
 * A file of a chat message, stored in the data transfer ([ChatFileService]): in the synthetic area of the conversation
 * or, for direct messages, in the personal box of the recipient. Kept with the message as JSON
 * ([ChatMessageDO.attachments]); the file itself may expire or be deleted in the data transfer.
 */
class ChatAttachment(
    var areaId: Long = 0,
    var fileId: String = "",
    var name: String = "",
    var size: Long = 0,
    /** A PNG, JPEG, GIF or WebP image (checked by its content), shown as thumbnail. */
    var image: Boolean = false,
)
