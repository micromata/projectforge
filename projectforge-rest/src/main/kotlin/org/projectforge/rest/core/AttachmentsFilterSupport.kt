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

package org.projectforge.rest.core

import org.projectforge.common.i18n.I18nEnum
import org.projectforge.framework.i18n.translate
import org.projectforge.framework.jcr.AttachmentsInfo
import org.projectforge.framework.persistence.api.BaseDao
import org.projectforge.framework.persistence.api.ExtendedBaseDO
import org.projectforge.framework.persistence.api.MagicFilter
import org.projectforge.framework.persistence.api.QueryFilter
import org.projectforge.ui.UILabelledElement
import org.projectforge.ui.filter.UIFilterListElement

/**
 * The "has attachments" filter of a list page, offered for every entity with attachment support.
 *
 * [AttachmentsInfo.attachmentsCounter] is kept up to date by
 * [org.projectforge.framework.jcr.AttachmentsService], so the filter is a plain query predicate and
 * needs no [org.projectforge.framework.persistence.api.impl.CustomResultFilter].
 *
 * The filter field isn't a searchable property of the data object (it's `@JsonIgnore` and carries no
 * `@PropertyInfo`), so it can't be detected automatically like title or authors — it is added by
 * [org.projectforge.ui.filter.LayoutListFilterUtils] and applied by [getObjectList], both for all
 * entities at once rather than per pages rest class.
 */
object AttachmentsFilterSupport {
  /** Id of the filter element; not a property of the data object. */
  const val FILTER_ID = "hasAttachments"

  private const val COUNTER_PROPERTY = "attachmentsCounter"

  /** The choices; their names travel as the filter's value. */
  enum class HasAttachments(override val i18nKey: String) : I18nEnum {
    YES("yes"), NO("no")
  }

  /**
   * Whether this list page can offer the filter: the entity has to support attachments
   * ([AbstractPagesRest.enableJcr]) and its data object has to store their number.
   */
  fun supported(pagesRest: AbstractEntityRest<out ExtendedBaseDO<Long>, *, out BaseDao<*>>): Boolean {
    return pagesRest.jcrPath != null && AttachmentsInfo::class.java.isAssignableFrom(pagesRest.baseDao.doClass)
  }

  fun addFilterElement(elements: MutableList<UILabelledElement>) {
    elements.add(
      UIFilterListElement(
        FILTER_ID,
        label = translate("attachments"),
        multi = false,
      ).buildValues(HasAttachments::class.java)
    )
  }

  /**
   * Turns the filter entry, if any, into a predicate. The entry is marked as synthetic, so
   * [org.projectforge.framework.persistence.api.MagicFilterProcessor] doesn't look for a property of
   * that name.
   */
  fun preProcessMagicFilter(target: QueryFilter, source: MagicFilter) {
    val entry = source.entries.find { it.field == FILTER_ID } ?: return
    entry.synthetic = true
    when (entry.value.values?.singleOrNull()) {
      HasAttachments.YES.name -> target.add(QueryFilter.gt(COUNTER_PROPERTY, 0))
      // An object that never had an attachment has null, one whose last attachment was deleted 0
      // (see AttachmentsService.updateAttachmentsInfo).
      HasAttachments.NO.name -> target.add(
        QueryFilter.or(
          QueryFilter.isNull(COUNTER_PROPERTY),
          QueryFilter.le(COUNTER_PROPERTY, 0),
        )
      )
    }
  }
}
