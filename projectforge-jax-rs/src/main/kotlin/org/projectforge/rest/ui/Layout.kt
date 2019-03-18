package org.projectforge.rest.ui

import org.bouncycastle.asn1.x509.X509ObjectIdentifiers.id
import org.projectforge.business.address.AddressDO
import org.projectforge.business.address.AddressDao
import org.projectforge.business.book.BookDO
import org.projectforge.business.book.BookDao
import org.projectforge.framework.persistence.api.BaseDao
import org.projectforge.framework.persistence.api.ExtendedBaseDO
import org.projectforge.framework.persistence.entities.DefaultBaseDO
import org.projectforge.rest.JsonUtils
import org.projectforge.ui.UILayout
import org.springframework.stereotype.Controller
import javax.ws.rs.*
import javax.ws.rs.core.MediaType
import javax.ws.rs.core.Response

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

        fun getEditLayout(data: ExtendedBaseDO<Int>): UILayout? {
            var layout = when (data) {
                is AddressDO -> AddressLayout.createEditLayout(data)
                is BookDO -> BookLayout.createEditLayout(data)
                else -> null
            }
            return layout
        }
    }
}