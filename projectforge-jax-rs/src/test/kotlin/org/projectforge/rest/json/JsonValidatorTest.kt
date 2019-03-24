package org.projectforge.rest.json

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertThrows
import org.junit.jupiter.api.Test
import org.opentest4j.AssertionFailedError

class JsonValidatorTest {

    @Test
    fun parseJson() {
        val jsonValidator = JsonValidator("{'fruit1':'apple','fruit2':'orange','basket':{'fruit3':'cherry','fruit4':'banana'},'actions':[{'id':'cancel','title':'Abbrechen','style':'danger','type':'button','key':'el-20'},{'id':'create','title':'Anlegen','style':'primary','type':'button','key':'el-21'}]}")
        jsonValidator.assert("apple", "fruit1")
        jsonValidator.assert("orange", "fruit2")
        jsonValidator.assert("cherry", "basket.fruit3")

        jsonValidator.assert(null, "fruit3")
        jsonValidator.assert(null, "basket.fruit1")

        var ex1 = assertThrows(IllegalArgumentException::class.java) {
            jsonValidator.assert("...", "basket.unknown.fruit1")
        }
        assertEquals("Can't step so deep: 'basket.unknown.fruit1'. 'fruit1' doesn't exist.", ex1.message)

        jsonValidator.assert(null, "basket.unknown")
        var ex2 = assertThrows(AssertionFailedError::class.java) {
            jsonValidator.assert("...", "basket.unknown")
        }
        assertEquals("Expected '...' but found null for path 'basket.unknown'. ==> expected: <null> but was: <...>", ex2.message)

        jsonValidator.assert("cancel", "actions[0].id")
        jsonValidator.assert("Anlegen", "actions[1].title")
    }
}