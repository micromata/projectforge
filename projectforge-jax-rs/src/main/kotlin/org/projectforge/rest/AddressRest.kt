package org.projectforge.rest

import org.projectforge.business.address.AddressDO
import org.projectforge.business.address.AddressDao
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Controller
import javax.ws.rs.Path

@Controller
@Path("addresses")
open class AddressRest() : AbstractDORest<AddressDO, AddressDao>() {
    private val log = org.slf4j.LoggerFactory.getLogger(AddressRest::class.java)

    @Autowired
    open var addressDao: AddressDao? = null

    override fun getBaseDao() : AddressDao {
        return addressDao!!
    }

    override fun processItemBeforeExport(item: AddressDO) {
        super.processItemBeforeExport(item)
        item.addressbookList = null
    }
}