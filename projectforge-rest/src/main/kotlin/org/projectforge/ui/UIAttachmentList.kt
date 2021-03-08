/////////////////////////////////////////////////////////////////////////////
//
// Project ProjectForge Community Edition
//         www.projectforge.org
//
// Copyright (C) 2001-2021 Micromata GmbH, Germany (www.micromata.com)
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

package org.projectforge.ui

import org.projectforge.framework.ToStringUtil
import org.projectforge.rest.AttachmentsServicesRest
import org.projectforge.rest.core.RestResolver

/**
 * List of attachments including upload, download and remove functionality.
 */
class UIAttachmentList(
        /**
         * Simply use [org.projectforge.rest.core.AbstractPagesRest.category] if using in pages rest. Otherwise
         * the category/jcrPath is required here.
         */
        var category: String,
        /**
         * The id of the object (pk) where the attachments are belonging to.
         * Null for new objects, meaning, that now upload is available and an hint is shown.
         */
        val id: Int?,
        /**
         * id to identify attachments list by server, especially if multiple lists of attachments are
         * used for one page.
         * Default is 'attachments'.
         */
        val listId: String = "attachments",
        /**
         * If true, only download of attachments is allowed.
         */
        val readOnly: Boolean = false) :
        UIElement(type = UIElementType.ATTACHMENT_LIST)
