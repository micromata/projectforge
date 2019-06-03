package org.projectforge.rest.dto

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import org.projectforge.business.address.AddressDO
import org.projectforge.business.address.AddressbookDO
import org.projectforge.framework.persistence.user.entities.PFUserDO

class AddressTest {

    @Test
    fun dtoTest() {
        val src = AddressDO()
        src.firstName = "Kai"
        src.uid = "42"
        src.id = 1
        val addressbook = AddressbookDO()
        val user = PFUserDO()
        user.id = 3
        user.username = "kai"
        user.email = "email"
        addressbook.id = 8
        addressbook.description = "Description"
        addressbook.title = "Title"
        addressbook.owner = user
        src.addressbookList!!.add(addressbook)
        val dest = Address()
        dest.copyFrom(src)

        assertEquals("Kai", dest.firstName)
        assertEquals(1, dest.id)
        assertEquals(1, dest.addressbookList?.size)

        val addressDO = AddressDO()
        dest.copyTo(addressDO)
        assertEquals("Kai", addressDO.firstName)
        assertEquals(1, addressDO.id)
        assertEquals(1, addressDO.addressbookList?.size)
    }
}
