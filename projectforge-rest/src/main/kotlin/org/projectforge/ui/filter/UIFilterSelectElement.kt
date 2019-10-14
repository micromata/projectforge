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

package org.projectforge.ui.filter

import org.projectforge.common.i18n.I18nEnum
import org.projectforge.framework.i18n.translate
import org.projectforge.rest.core.log
import org.projectforge.ui.UISelectValue

open class UIFilterSelectElement(
        id: String,
        var multi: Boolean = true,
        var values: List<UISelectValue<String>>? = null
) : UIFilterElement(id, FilterType.SELECT) {

    fun buildValues(i18nEnum: Class<out Enum<*>>): UIFilterSelectElement {
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
}
