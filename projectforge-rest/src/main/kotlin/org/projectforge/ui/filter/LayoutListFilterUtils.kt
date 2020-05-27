/////////////////////////////////////////////////////////////////////////////
//
// Project ProjectForge Community Edition
//         www.projectforge.org
//
// Copyright (C) 2001-2020 Micromata GmbH, Germany (www.micromata.com)
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
import org.projectforge.framework.i18n.translate
import org.projectforge.framework.persistence.api.BaseDO
import org.projectforge.framework.persistence.api.BaseDao
import org.projectforge.framework.persistence.api.ExtendedBaseDO
import org.projectforge.framework.persistence.api.MagicFilterEntry
import org.projectforge.framework.persistence.user.api.ThreadLocalUserContext
import org.projectforge.framework.utils.NumberFormatter
import org.projectforge.rest.core.AbstractPagesRest
import org.projectforge.ui.*
import java.time.LocalDate

private val log = KotlinLogging.logger {}

private val PAGE_SIZES = intArrayOf(25, 50, 100, 250, 500, 1000)

/**
 * Utils for the Layout classes for handling filter settings in list views.
 */
class LayoutListFilterUtils {
    companion object {

        fun createNamedSearchFilterContainer(pagesRest: AbstractPagesRest<out ExtendedBaseDO<Int>, *, out BaseDao<*>>,
                                             lc: LayoutContext): UINamedContainer {
            val container = UINamedContainer("searchFilter")
            val elements = mutableListOf<UILabelledElement>()
            elements.add(UIFilterObjectElement(MagicFilterEntry.HistorySearch.MODIFIED_BY_USER.fieldName,
                    label = translate(MagicFilterEntry.HistorySearch.MODIFIED_BY_USER.i18nKey),
                    autoCompletion = AutoCompletion.getAutoCompletion4Users()))
            elements.add(UIFilterTimestampElement(MagicFilterEntry.HistorySearch.MODIFIED_INTERVAL.fieldName,
                    label = translate(MagicFilterEntry.HistorySearch.MODIFIED_INTERVAL.i18nKey),
                    openInterval = true,
                    selectors = listOf(UIFilterTimestampElement.QuickSelector.YEAR,
                            UIFilterTimestampElement.QuickSelector.MONTH,
                            UIFilterTimestampElement.QuickSelector.WEEK,
                            UIFilterTimestampElement.QuickSelector.DAY,
                            UIFilterTimestampElement.QuickSelector.UNTIL_NOW)))
            elements.add(UIFilterElement(MagicFilterEntry.HistorySearch.MODIFIED_HISTORY_VALUE.fieldName,
                    label = translate(MagicFilterEntry.HistorySearch.MODIFIED_HISTORY_VALUE.i18nKey)))
            elements.add(UIFilterElement("deleted", UIFilterElement.FilterType.BOOLEAN, translate("deleted")))

            elements.add(UIFilterListElement("pageSize", pageValues, translate("label.pageSize"), multi = false, defaultFilter = true))

            val baseDao = pagesRest.baseDao
            val searchFields = baseDao.searchFields
            searchFields.forEach {
                val elInfo = ElementsRegistry.getElementInfo(lc, it)
                if (elInfo == null) {
                    log.warn("Search field '${baseDao.doClass}.$it' not found. Ignoring it.")
                } else {
                    val element: UIElement
                    if (elInfo.propertyType.isEnum) {
                        @Suppress("UNCHECKED_CAST")
                        element = UIFilterListElement(it)
                                .buildValues(i18nEnum = elInfo.propertyType as Class<out Enum<*>>)
                        element.label = element.id // Default label if no translation will be found below.
                    } else {
                        element = UIFilterElement(it)
                        element.label = element.id // Default label if no translation will be found below.
                        element.determine(elInfo.propertyType)
                    }
                    element as UILabelledElement
                    element.label = getLabel(elInfo)
                    elements.add(element)
                }
            }
            pagesRest.addMagicFilterElements(elements)

            elements.sortWith(compareBy(ThreadLocalUserContext.getLocaleComparator()) { it.label })
            elements.forEach { container.add(it as UIElement) }
            return container
        }

        fun getLabel(elInfo: ElementInfo): String {
            val sb = StringBuilder()
            addLabel(sb, elInfo)
            return sb.toString()
        }

        private fun addLabel(sb: StringBuilder, elInfo: ElementInfo?) {
            if (elInfo == null) return
            if (sb.length > 1000) { // Paranoia test for endless loops
                log.error("Oups, paranoia test detects endless loop in ElementInfo.parent '$sb'!")
                return
            }
            addLabel(sb, elInfo.parent)
            if (elInfo.parent != null) sb.append(" - ")
            if (!elInfo.i18nKey.isNullOrBlank()) {
                sb.append(translate(elInfo.i18nKey))
            } else {
                sb.append(elInfo.simplePropertyName)
            }
            if (!elInfo.additionalI18nKey.isNullOrBlank()) {
                sb.append(" (").append(translate(elInfo.additionalI18nKey)).append(")")
            }
        }

        private val pageValues = PAGE_SIZES.map { UISelectValue("$it", NumberFormatter.format(it)) }
    }
}
