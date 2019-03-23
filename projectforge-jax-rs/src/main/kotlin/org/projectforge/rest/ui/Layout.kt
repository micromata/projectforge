package org.projectforge.rest.ui

import org.projectforge.business.address.AddressDO
import org.projectforge.business.address.AddressDao
import org.projectforge.business.book.BookDO
import org.projectforge.business.book.BookDao
import org.projectforge.framework.persistence.api.BaseDao
import org.projectforge.framework.persistence.api.ExtendedBaseDO
import org.projectforge.ui.UILayout

/**
 * Here all layouts of list and edit pages are registered.
 */
class Layout {
    companion object {
        fun getListLayout(baseDao: BaseDao<*>): UILayout? {
            var layout = when (baseDao) {
                is AddressDao -> AddressLayout.createListLayout()
                is BookDao -> BookLayout.createListLayout()
                else -> null
            }
            return layout
        }

        fun getEditLayout(data: ExtendedBaseDO<Int>, inlineLabels : Boolean = true): UILayout? {
            var layout = when (data) {
                is AddressDO -> AddressLayout.createEditLayout(data, inlineLabels)
                is BookDO -> BookLayout.createEditLayout(data, inlineLabels)
                else -> null
            }
            return layout
        }
    }
}