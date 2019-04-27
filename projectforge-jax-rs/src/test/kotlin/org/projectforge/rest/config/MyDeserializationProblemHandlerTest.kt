package org.projectforge.rest.config

import com.fasterxml.jackson.databind.DeserializationFeature
import com.fasterxml.jackson.databind.JsonMappingException
import com.fasterxml.jackson.databind.ObjectMapper
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.Test
import org.projectforge.test.TestSetup

class MyDeserializationProblemHandlerTest {

    class TestObject(val stringValue: String? = null,
                     val intValue: Int? = null,
                     val longValue: Long? = null,
                     val doubleValue: Double? = null)

    class Json(val mapper:ObjectMapper, val problemHandler : MyDeserializationProblemHandler)

    companion object {
        @BeforeAll
        @JvmStatic
        fun initialize() {
            TestSetup.init()
        }
    }

    @Test
    fun deserializatioTest() {
        var json = createObjectMapper()
        var obj = json.mapper.readValue(creeateJson("\"Hello\"", "1", "1.2", "42"),
                TestObject::class.java)
        assertObject(obj, "Hello", 1, 1.2, 42)

        json = createObjectMapper()
        obj = json.mapper.readValue(creeateJson("\"Hello\"", "\"ab2\"", "\"dfsd1.2\"", "\"dfas42\""),
                TestObject::class.java)
        assertObject(obj, "Hello", 0, 0.0, 0L)
        assertValidationErrors(json.problemHandler,
                arrayOf("intValue", "validation.error.format.integer"),
                arrayOf("longValue", "validation.error.format.integer"),
                arrayOf("doubleValue", "validation.error.generic"))

        json = createObjectMapper()
        obj = json.mapper.readValue(creeateJson("5", "2147483647", "0.0", "9223372036854775807"),
                TestObject::class.java)
        assertEquals(0, json.problemHandler.validationErrors.size)
        assertObject(obj, "5", Int.MAX_VALUE, 0.0, Long.MAX_VALUE)

        json = createObjectMapper()
        try {
            obj = json.mapper.readValue(creeateJson("5", "2147483648", "9223372036854775808", "9223372036854775808"),
                    TestObject::class.java)
        } catch (ex: JsonMappingException) {
            assertTrue(ex.message?.startsWith("Numeric value (2147483648) out of range of int") ?: false)
        }
    }

    private fun createObjectMapper(): Json {
        val mapper = ObjectMapper()
        val problemHandler = MyDeserializationProblemHandler()
        mapper.addHandler(problemHandler)
        mapper.configure(DeserializationFeature.WRAP_EXCEPTIONS, true)
        return Json(mapper, problemHandler)
    }

    private fun creeateJson(stringValue: String?, intValue: String?, doubleValue: String?, longValue: String?): String {
        return "{\"stringValue\":$stringValue,\"intValue\":$intValue,\"longValue\":$longValue,\"doubleValue\":$doubleValue}"
    }

    private fun assertObject(obj: TestObject, strValue: String?, intValue: Int?, doubleValue: Double?, longValue: Long?) {
        assertEquals(strValue, obj.stringValue)
        assertEquals(intValue, obj.intValue)
        assertEquals(doubleValue, obj.doubleValue)
        assertEquals(longValue, obj.longValue)
    }

    private fun assertValidationErrors(problemHandler: MyDeserializationProblemHandler, vararg errors: Array<String>) {
        assertEquals(errors.size, problemHandler.validationErrors.size)
        for ((index, value) in errors.withIndex()) {
            assertEquals(value[0], problemHandler.validationErrors[index].fieldId)
            assertEquals(value[1], problemHandler.validationErrors[index].messageId)
        }
    }
}
