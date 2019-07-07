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

import org.projectforge.framework.i18n.translate
import org.projectforge.framework.persistence.api.BaseDao
import org.projectforge.ui.*

/**
 * Utils for the Layout classes for handling filter settings in list views.
 */
class LayoutListFilterUtils {
    companion object {
        internal val log = org.slf4j.LoggerFactory.getLogger(LayoutListFilterUtils::class.java)

        fun createNamedContainer(baseDao: BaseDao<*>, lc: LayoutContext): UINamedContainer {
            val container = UINamedContainer("searchFilter")
            val searchFields = baseDao.searchFields
            val elements = mutableListOf<UILabelledElement>()
            elements.add(UIFilterObjectElement("modifiedByUser",
                    label = translate("modifiedBy"),
                    autoCompletion = AutoCompletion<Int>(2,
                            //recent = listOf(AutoCompletion.Entry(42,"Fin Reinhard"), AutoCompletion.Entry(43, "Kai Reinhard")),
                            url = "user/ac")))
            elements.add(UIFilterTimestampElement("modifiedInterval",
                    label = translate("modificationTime"),
                    openInterval = true,
                    selectors = listOf(UIFilterTimestampElement.QuickSelector.YEAR,
                            UIFilterTimestampElement.QuickSelector.MONTH,
                            UIFilterTimestampElement.QuickSelector.WEEK,
                            UIFilterTimestampElement.QuickSelector.DAY,
                            UIFilterTimestampElement.QuickSelector.UNTIL_NOW)))
            searchFields.forEach {
                val elInfo = ElementsRegistry.getElementInfo(lc, it)
                if (elInfo == null) {
                    log.warn("Search field '${baseDao.doClass}.$it' not found. Ignoring it.")
                } else {
                    var element: UIElement
                    if (elInfo.propertyType.isEnum) {
                        @Suppress("UNCHECKED_CAST")
                        element = UISelect<String>(it, required = elInfo.required, layoutContext = lc,
                                multi = true)
                                .buildValues(i18nEnum = elInfo.propertyType as Class<out Enum<*>>)
                        element.label = element.id // Default label if no translation will be found below.
                    } else {
                        element = UIFilterElement(it)
                        element.label = element.id // Default label if no translation will be found below.
                    }
                    element as UILabelledElement
                    if (!elInfo.i18nKey.isNullOrBlank()) {
                        if (elInfo.additionalI18nKey.isNullOrBlank()) {
                            element.label = translate(elInfo.i18nKey)
                        } else {
                            element.label = "${translate(elInfo.i18nKey)}, ${translate(elInfo.additionalI18nKey)}"
                        }
                    }
                    elements.add(element)
                }
            }
            elements.sortBy { it.label }
            elements.forEach { container.add(it as UIElement) }
            return container
        }
    }
}
