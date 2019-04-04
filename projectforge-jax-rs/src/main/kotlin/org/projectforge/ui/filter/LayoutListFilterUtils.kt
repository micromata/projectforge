package org.projectforge.ui.filter

import org.projectforge.framework.persistence.api.BaseDao
import org.projectforge.ui.LayoutContext
import org.projectforge.ui.UINamedContainer

/**
 * Utils for the Layout classes for handling filter settings in list views.
 */
class LayoutListFilterUtils {
    companion object {
        fun createNamedContainer(baseDao: BaseDao<*>, lc: LayoutContext): UINamedContainer {
            val container = UINamedContainer("searchFilter")
            container.add(UIFilterElement("modifiedByUser", filterType = UIFilterElement.FilterType.OBJECT))
            container.add(UIFilterTimestampElement("modifiedInterval",
                    true,
                    listOf(UIFilterTimestampElement.QuickSelector.YEAR,
                            UIFilterTimestampElement.QuickSelector.MONTH,
                            UIFilterTimestampElement.QuickSelector.WEEK,
                            UIFilterTimestampElement.QuickSelector.DAY,
                            UIFilterTimestampElement.QuickSelector.UNTIL_NOW)))
            val searchFields = baseDao.searchFields
            searchFields.forEach {
                container.add(UIFilterElement(it))
            }
            return container
        }
    }
}