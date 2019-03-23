package org.projectforge.rest

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
class LayoutRegistry {
    companion object {
        fun getListLayout(baseDao: BaseDao<*>): UILayout? {
            var layout = when (baseDao) {
                is AddressDao -> AddressRest.createListLayout()
                is BookDao -> BookRest.createListLayout()
                else -> null
            }
            return layout
        }

        fun getEditLayout(data: ExtendedBaseDO<Int>, inlineLabels : Boolean = true): UILayout? {
            var layout = when (data) {
                is AddressDO -> AddressRest.createEditLayout(data, inlineLabels)
                is BookDO -> BookRest.createEditLayout(data, inlineLabels)
                else -> null
            }
            return layout
        }
    }
}