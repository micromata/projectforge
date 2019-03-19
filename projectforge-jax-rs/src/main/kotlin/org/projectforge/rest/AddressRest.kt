package org.projectforge.rest

import org.projectforge.business.DOUtils
import org.projectforge.business.address.AddressDO
import org.projectforge.business.address.AddressDao
import org.projectforge.business.address.AddressFilter
import org.projectforge.business.address.AddressbookDO
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Controller
import javax.ws.rs.Path

@Controller
@Path("addresses")
open class AddressRest() : AbstractDORest<AddressDO, AddressDao, AddressFilter>() {
    private val log = org.slf4j.LoggerFactory.getLogger(AddressRest::class.java)

    @Autowired
    open var addressDao: AddressDao? = null

    override fun getBaseDao(): AddressDao {
        return addressDao!!
    }

    override fun newBaseDO(): AddressDO {
        return AddressDO()
    }

    override fun processItemBeforeExport(item: AddressDO) {
        super.processItemBeforeExport(item)
        val addressbookList: MutableSet<AddressbookDO> = mutableSetOf();
        item.addressbookList.forEach {
            val addressbook = DOUtils.cloneMinimal(it)
            if (!addressbook.isDeleted) // Don't proceed deleted addressbooks...
                addressbookList.add(addressbook)
        }
        item.addressbookList = addressbookList
    }

    /**
     * Clone is supported by addresses.
     */
    override fun prepareClone(obj: AddressDO): Boolean {
        // TODO: Enter here the PersonalAddressDO stuff etc.
        return true
    }
}