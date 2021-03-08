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

package org.projectforge.ui.filter

import mu.KotlinLogging
import org.projectforge.common.i18n.I18nEnum
import org.projectforge.framework.i18n.translate
import org.projectforge.ui.UISelectValue

private val log = KotlinLogging.logger {}

open class UIFilterListElement(
        id: String,
        var values: List<UISelectValue<String>>? = null,
        label: String? = null,
        additionalLabel: String? = null,
        tooltip: String? = null,
        /**
         * If true, multi values may selectable, otherwise only one value is selectable (DropDownChoice).
         */
        var multi: Boolean? = true,
        defaultFilter: Boolean? = null)
    : UIFilterElement(id, FilterType.LIST, label = label, additionalLabel = additionalLabel, tooltip = tooltip, defaultFilter = defaultFilter) {

    fun buildValues(i18nEnum: Class<out Enum<*>>): UIFilterListElement {
        val newValues = mutableListOf<UISelectValue<String>>()
        i18nEnum.enumConstants.forEach { enum ->
            if (enum is I18nEnum) {
                newValues.add(UISelectValue(enum.name, translate(enum.i18nKey)))
            } else {
                log.error("UIFilterSelectElement supports only enums of type I18nEnum, not '$enum': '${this}'")
            }
        }
        values = newValues
        return this
    }

    fun buildValues(vararg i18nEnum: Enum<*>): UIFilterListElement {
        val newValues = mutableListOf<UISelectValue<String>>()
        i18nEnum.forEach { enum ->
            if (enum is I18nEnum) {
                newValues.add(UISelectValue(enum.name, translate(enum.i18nKey)))
            } else {
                log.error("UIFilterSelectElement supports only enums of type I18nEnum, not '$enum': '${this}'")
            }
        }
        values = newValues
        return this
    }
}
