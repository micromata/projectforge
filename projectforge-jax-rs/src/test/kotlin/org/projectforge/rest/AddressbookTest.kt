package org.projectforge.rest

import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test
import org.projectforge.business.address.AddressbookDO
import org.projectforge.rest.dto.Addressbook

class AddressbookTest {

    @Test
    fun listToStringTest() {
        var bookDO = AddressbookDO()
        bookDO.fullAccessGroupIds = "1"
        bookDO.fullAccessUserIds = "1, 2, 3"
        bookDO.readonlyAccessGroupIds = ""
        bookDO.readonlyAccessUserIds = "1, ,3"

        val book = Addressbook()
        book.copyFrom(bookDO)

        checkIntList(book.fullAccessGroupIds, 1)
        checkIntList(book.fullAccessUserIds, 1, 2, 3)
        assertNull(book.readonlyAccessGroupIds)
        checkIntList(book.readonlyAccessUserIds, 1, 3)

        bookDO = AddressbookDO()
        book.copyTo(bookDO)
        assertEquals("1", bookDO.fullAccessGroupIds)
        assertEquals("1, 2, 3", bookDO.fullAccessUserIds)
        assertNull( bookDO.readonlyAccessGroupIds)
        assertEquals("1, 3", bookDO.readonlyAccessUserIds)
    }

    private fun checkIntList(intList: List<Int>?, vararg expected: Int) {
        assertNotNull(intList)
        if (intList == null) {
            return
        }
        assertEquals(expected.size, intList.size)
        intList.forEachIndexed { index, element ->
            assertEquals(expected[index], element)
        }
    }
}
