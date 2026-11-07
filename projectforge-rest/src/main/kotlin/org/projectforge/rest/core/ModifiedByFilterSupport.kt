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

import org.projectforge.framework.i18n.translate
import org.projectforge.framework.persistence.api.ExtendedBaseDO
import org.projectforge.framework.persistence.api.MagicFilter
import org.projectforge.framework.persistence.api.impl.CustomResultFilter
import org.projectforge.ui.AutoCompletion
import org.projectforge.ui.UILabelledElement
import org.projectforge.ui.filter.UIFilterObjectElement

/** Keeps the entries a user has created or changed at some point (see [HistoryEditorsService]). */
class ModifiedByResultFilter<O : ExtendedBaseDO<Long>>(private val entityIds: Set<Long>) : CustomResultFilter<O> {
    override fun match(list: MutableList<O>, element: O): Boolean = entityIds.contains(element.id)
}

/**
 * The "edited by" filter of a list page: a user picker that shows the entries the chosen user has created or
 * changed. Opt-in per list, with the column `editorsColumn` of the client showing everybody who did.
 *
 * The entry is synthetic: its field isn't a property of the data object, so
 * [org.projectforge.framework.persistence.api.MagicFilterProcessor] must not look for one.
 */
object ModifiedByFilterSupport {
    const val FILTER_ID = "modifiedBy"

    fun addFilterElement(elements: MutableList<UILabelledElement>) {
        elements.add(
            UIFilterObjectElement(
                FILTER_ID,
                label = translate("history.filter.modifiedBy"),
                autoCompletion = AutoCompletion.getAutoCompletion4Users(),
            ).also {
                it.tooltip = translate("history.filter.modifiedBy.tooltip")
                it.defaultFilter = true
            }
        )
    }

    /** The result filter of the entry, if the filter is set. */
    fun <O : ExtendedBaseDO<Long>> preProcessMagicFilter(
        source: MagicFilter,
        doClass: Class<O>,
        service: HistoryEditorsService,
    ): CustomResultFilter<O>? {
        val entry = source.entries.find { it.field == FILTER_ID } ?: return null
        entry.synthetic = true
        // The pickers send the user as `value.id`, which the generic processor doesn't read.
        val userId = entry.value.id ?: entry.value.value?.toLongOrNull() ?: return null
        return ModifiedByResultFilter(service.entityIdsModifiedBy(doClass, userId))
    }
}
