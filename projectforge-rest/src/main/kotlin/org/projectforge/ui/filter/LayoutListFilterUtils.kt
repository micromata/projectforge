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

package org.projectforge.ui.filter

import mu.KotlinLogging
import org.projectforge.framework.i18n.translate
import org.projectforge.framework.persistence.api.BaseDao
import org.projectforge.framework.persistence.api.ExtendedBaseDO
import org.projectforge.framework.persistence.api.MagicFilter
import org.projectforge.framework.persistence.api.MagicFilterEntry
import org.projectforge.framework.persistence.user.api.ThreadLocalUserContext
import org.projectforge.framework.utils.NumberFormatter
import org.projectforge.rest.core.AbstractEntityRest
import org.projectforge.rest.core.AttachmentsFilterSupport
import org.projectforge.ui.*

private val log = KotlinLogging.logger {}

val PAGINATION_PAGE_SIZES = intArrayOf(25, 50, 100, 200, 500, 1000)

/**
 * Utils for the Layout classes for handling filter settings in list views.
 */
object LayoutListFilterUtils {
    fun createNamedSearchFilterContainer(
        pagesRest: AbstractEntityRest<out ExtendedBaseDO<Long>, *, out BaseDao<*>>,
        lc: LayoutContext
    ): UINamedContainer {
        val container = UINamedContainer("searchFilter")
        val elements = mutableListOf<UILabelledElement>()
        elements.add(
            UIFilterObjectElement(
                MagicFilterEntry.HistorySearch.MODIFIED_BY_USER.fieldName,
                label = translate(MagicFilterEntry.HistorySearch.MODIFIED_BY_USER.i18nKey),
                autoCompletion = AutoCompletion.getAutoCompletion4Users()
            )
        )
        elements.add(
            UIFilterTimestampElement(
                MagicFilterEntry.HistorySearch.MODIFIED_INTERVAL.fieldName,
                label = translate(MagicFilterEntry.HistorySearch.MODIFIED_INTERVAL.i18nKey),
                openInterval = true,
                selectors = listOf(
                    UIFilterTimestampElement.QuickSelector.YEAR,
                    UIFilterTimestampElement.QuickSelector.MONTH,
                    UIFilterTimestampElement.QuickSelector.WEEK,
                    UIFilterTimestampElement.QuickSelector.DAY,
                    UIFilterTimestampElement.QuickSelector.UNTIL_NOW
                )
            )
        )
        elements.add(
            UIFilterElement(
                MagicFilterEntry.HistorySearch.MODIFIED_HISTORY_VALUE.fieldName,
                label = translate(MagicFilterEntry.HistorySearch.MODIFIED_HISTORY_VALUE.i18nKey)
            )
        )
        elements.add(UIFilterElement("deleted", UIFilterElement.FilterType.BOOLEAN, translate("deleted")))
        if (AttachmentsFilterSupport.supported(pagesRest)) {
            AttachmentsFilterSupport.addFilterElement(elements)
        }

        val baseDao = pagesRest.baseDao
        val searchFields = baseDao.searchFields
        searchFields.forEach {
            val elInfo = ElementsRegistry.getElementInfo(lc, it)
            if (elInfo == null) {
                log.warn("Search field '${baseDao.doClass}.$it' not found. Ignoring it.")
            } else {
                val element: UIElement
                if (elInfo.propertyClass.isEnum) {
                    @Suppress("UNCHECKED_CAST")
                    element = UIFilterListElement(it)
                        .buildValues(
                            i18nEnum = elInfo.propertyClass as Class<out Enum<*>>,
                            // Only where the column allows null: for a mandatory field the option would
                            // be an option that never matches (see ElementsRegistry.getElementInfo).
                            addNullValue = elInfo.required != true,
                        )
                    element.label = element.id // Default label if no translation will be found below.
                } else {
                    element = UIFilterElement(it)
                    element.label = element.id // Default label if no translation will be found below.
                    element.determine(elInfo.propertyClass)
                }
                element as UILabelledElement
                element.label = getLabel(elInfo)
                if (element is UIFilterElement) {
                    // Nested fields carry their parents in the label ("Kunde - Name"), which is what the
                    // client groups them by; it shows the leaf alone under the group's heading.
                    groupLabel(elInfo)?.let { group ->
                        element.group = group
                        element.shortLabel = leafLabel(elInfo)
                    }
                    // No @PropertyInfo, so no translation: getLabel fell back to the property name above
                    // (attachmentsIds). Indexed plumbing, searchable but not a field a user looks for.
                    if (elInfo.i18nKey.isNullOrBlank()) {
                        element.technical = true
                    }
                }
                elements.add(element)
            }
        }
        pagesRest.addMagicFilterElements(elements)

        elements.sortWith(compareBy(ThreadLocalUserContext.localeComparator) { it.label })
        elements.forEach { container.add(it as UIElement) }
        return container
    }

    /**
     * The full label of a field, its parents first: "Projekt - Kunde - Name".
     */
    fun getLabel(elInfo: ElementInfo): String {
        val sb = StringBuilder()
        addLabel(sb, elInfo)
        return sb.toString()
    }

    /**
     * The parents of a field alone ("Projekt - Kunde"), or null for a field of the entity itself.
     *
     * Together with [leafLabel] this is [getLabel] split in two, for a client that shows the parents once
     * as a group heading instead of in every field's label (see [UIFilterElement.group]).
     */
    fun groupLabel(elInfo: ElementInfo): String? {
        val parent = elInfo.parent ?: return null
        val sb = StringBuilder()
        addLabel(sb, parent)
        return sb.toString()
    }

    /** The field's own label, without its parents: "Name". */
    fun leafLabel(elInfo: ElementInfo): String {
        val sb = StringBuilder()
        addOwnLabel(sb, elInfo)
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
        addOwnLabel(sb, elInfo)
    }

    private fun addOwnLabel(sb: StringBuilder, elInfo: ElementInfo) {
        if (!elInfo.i18nKey.isNullOrBlank()) {
            sb.append(translate(elInfo.i18nKey))
        } else {
            sb.append(elInfo.simplePropertyName)
        }
        if (!elInfo.additionalI18nKey.isNullOrBlank()) {
            sb.append(" (").append(translate(elInfo.additionalI18nKey)).append(")")
        }
    }

    private val pageValues = PAGINATION_PAGE_SIZES.map { UISelectValue("$it", NumberFormatter.format(it)) }
}
