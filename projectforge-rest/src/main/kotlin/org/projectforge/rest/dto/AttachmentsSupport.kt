/////////////////////////////////////////////////////////////////////////////
//
// Project ProjectForge Community Edition
//         www.projectforge.org
//
// Copyright (C) 2001-2023 Micromata GmbH, Germany (www.micromata.com)
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

package org.projectforge.rest.dto

import com.fasterxml.jackson.annotation.JsonProperty
import org.projectforge.framework.jcr.Attachment
import org.projectforge.framework.jcr.AttachmentsInfo
import org.projectforge.framework.utils.NumberHelper
import javax.persistence.Transient

interface AttachmentsSupport {
    var attachments: List<Attachment>?
    var attachmentsSize: Long?
    var attachmentsCounter: Int?
    /**
     * The number and soue of attachments attached to this data object.
     */
    val attachmentsSizeFormatted: String
        @JsonProperty
        @Transient
        get() = AttachmentsInfo.getAttachmentsSizeFormatted(attachmentsCounter, attachmentsSize)
}
