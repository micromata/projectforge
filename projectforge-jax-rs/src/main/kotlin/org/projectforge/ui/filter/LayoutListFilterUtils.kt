package org.projectforge.ui.filter

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
            container.add(UIFilterObjectElement("modifiedByUser",
                    autoCompletion = AutoCompletion<Int>(2,
                            //recent = listOf(AutoCompletion.Entry(42,"Fin Reinhard"), AutoCompletion.Entry(43, "Kai Reinhard")),
                            url = "user/ac")))
            container.add(UIFilterTimestampElement("modifiedInterval",
                    true,
                    listOf(UIFilterTimestampElement.QuickSelector.YEAR,
                            UIFilterTimestampElement.QuickSelector.MONTH,
                            UIFilterTimestampElement.QuickSelector.WEEK,
                            UIFilterTimestampElement.QuickSelector.DAY,
                            UIFilterTimestampElement.QuickSelector.UNTIL_NOW)))
            val searchFields = baseDao.searchFields
            searchFields.forEach {
                val elInfo = ElementsRegistry.getElementInfo(lc, it)
                if (elInfo == null) {
                    log.warn("Search field '${baseDao.doClass}.$it' not found. Ignoring it.")
                } else {
                    if (elInfo.propertyType.isEnum) {
                        @Suppress("UNCHECKED_CAST")
                        val element = UISelect<String>(it, required = elInfo.required, layoutContext = lc,
                                multi = true)
                                .buildValues(i18nEnum = elInfo.propertyType as Class<out Enum<*>>)
                        container.add(element)
                    } else {
                        container.add(UIFilterElement(it))
                    }
                }
            }
            return container
        }
    }
}
