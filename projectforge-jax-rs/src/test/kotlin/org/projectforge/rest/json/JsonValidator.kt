package org.projectforge.rest.json

import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNull

class JsonValidator {
    private val map: Map<String, Any?>

    constructor(json: String) {
        map = parseJson(json)
    }

    fun assert(expected: String?, path: String) {
        var currentMap: Map<String, Any?>? = map
        var result: String? = null
        val pathValues = path.split('.')
        pathValues.forEach {
            if (currentMap == null) {
                throw IllegalArgumentException("Can't step so deep: '${path}'. '${it}' doesn't exist.")
            }
            val value = currentMap!!.get(it)
            if (value == null) {
                currentMap = null
                result = null
            } else {
                if (value is Map<*, *>) {
                    currentMap = value as Map<String, Any?>
                } else if (value is String) {
                    result = value
                    currentMap = null
                } else {
                    throw IllegalArgumentException("Found unexpected type '${value::class.java}' in path '§{path}'")
                }
            }
        }
        if (result == null) {
            assertNull(expected, "Expected '${expected}' but found null for path '${path}'.")
        } else if (result is String) {
            assertEquals(expected, result, "Expected '${expected}' but found '${result}' for path '${path}'.")
        } else {
            throw IllegalArgumentException("Can't step so deep: '${path}'")
        }
    }

    private fun parseJson(json: String): Map<String, Any?> {
        val mapType = object : TypeToken<Map<String, Any?>>() {}.type
        return Gson().fromJson<Map<String, String>>(json, mapType)
    }
}