package org.projectforge.ui.filter

import org.projectforge.framework.persistence.api.BaseDao
import org.projectforge.ui.LayoutContext
import org.projectforge.ui.UIInput
import org.projectforge.ui.UINamedContainer

/**
 * Utils for the Layout classes for handling filter settings in list views.
 */
class LayoutFilterUtils {
    companion object {
        fun createNamedContainer(baseDao: BaseDao<*>, lc : LayoutContext): UINamedContainer {
            val container = UINamedContainer("searchFilter")
            val searchFields = baseDao.searchFields
            searchFields.forEach {
                container.add(UIFilterElement(it))
            }
            return container
        }
    }
}