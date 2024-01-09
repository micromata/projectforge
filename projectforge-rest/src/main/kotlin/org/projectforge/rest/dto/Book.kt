/////////////////////////////////////////////////////////////////////////////
//
// Project ProjectForge Community Edition
//         www.projectforge.org
//
// Copyright (C) 2001-2024 Micromata GmbH, Germany (www.micromata.com)
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
import org.projectforge.business.book.BookDO
import org.projectforge.business.book.BookStatus
import org.projectforge.business.book.BookType
import org.projectforge.framework.i18n.translate
import org.projectforge.framework.jcr.Attachment
import java.time.LocalDate

class Book(
  id: Int? = null,
  var title: String? = null,
  var keywords: String? = null,
  var lendOutBy: User? = null,
  var lendOutDate: LocalDate? = null,
  var lendOutComment: String? = null,
  var isbn: String? = null,
  var signature: String? = null,
  var publisher: String? = null,
  var editor: String? = null,
  var yearOfPublishing: String? = null,
  var authors: String? = null,
  var abstractText: String? = null,
  var comment: String? = null,
  var status: BookStatus? = null,
  var type: BookType? = null,
  override var attachmentsCounter: Int? = null,
  override var attachmentsSize: Long? = null,
  override var attachments: List<Attachment>? = null
) : BaseDTO<BookDO>(id), AttachmentsSupport {
  @get:JsonProperty
  val statusAsString: String?
    get() {
      status?.let { return translate(it.i18nKey) }
      return null
    }

  @get:JsonProperty
  val typeAsString: String?
    get() {
      type?.let { return translate(it.i18nKey) }
      return null
    }
}
