/////////////////////////////////////////////////////////////////////////////
//
// Project ProjectForge Community Edition
//         www.projectforge.org
//
// Copyright (C) 2001-2019 Micromata GmbH, Germany (www.micromata.com)
//
// ProjectForge is dual-licensed.
//
// This community edition is free software; you can redistribute it and/or
// modify it under the terms of the GNU General Public License as published
// by the Free Software Foundation; version 3 of the License.
//
// This community edition is distributed in the hope that it will be useful,
// but WITHOUT ANY WARRANTY; without even the implied warranty of
// MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU General
// Public License for more details.
//
// You should have received a copy of the GNU General Public License along
// with this program; if not, see http://www.gnu.org/licenses/.
//
/////////////////////////////////////////////////////////////////////////////

package org.projectforge.framework.json

import com.google.gson.Gson
import com.google.gson.reflect.TypeToken

class JsonValidator(val json: String) {
    private val map: Map<String, Any?>

    private val attrPattern = """[a-z_0-9-]*"""
    private val attrReqex = attrPattern.toRegex(RegexOption.IGNORE_CASE)
    private val attrPatternWithIndex = """[a-z_0-9-]*\[([0-9]+)]"""
    private val attrRegexWithIndex = attrPatternWithIndex.toRegex(RegexOption.IGNORE_CASE)

    init {
        map = parseJson(json)
    }

    /**
     * Finds the first sub element of path (or from root) with matching the field value.
     * @param path: Path must represent a map.
     * @return The map where the element is in.
     */
    fun findParentMap(field: String, value: String, path: String? = null): Map<String, Any?>? {
        val start = if (path != null) getMap(path) else map
        start?.forEach {
            val result = find(field, value, it.value)
            if (result != null)
                return result // Found, return the map the found element is in.
        }
        return null
    }

    private fun find(field: String, value: String, element: Any?): Map<String, Any?>? {
        if (element == null)
            return null
        if (element is Map<*, *>) {
            if (element[field] == value) {
                @Suppress("UNCHECKED_CAST")
                return element as Map<String, Any?> // Found
            }
            element.forEach {
                val result = find(field, value, it.value)
                if (result != null)
                    return result // Found!
            }
            return null
        } else if (element is List<*>) { // Array
            element.forEach {
                val result = find(field, value, it)
                if (result != null)
                    return result // Found!
            }
        }
        return null
    }

    fun get(path: String): String? {
        val result = getElement(path)
        if (result == null)
            return null
        if (result is String) {
            return result
        }
        throw java.lang.IllegalArgumentException("Requested element of path '${path}' isn't of type String: '${result::class.java}'.")
    }

    fun getDouble(path: String): Double? {
        val result = getElement(path)
        if (result == null)
            return null
        if (result is Double) {
            return result
        }
        throw java.lang.IllegalArgumentException("Requested element of path '${path}' isn't of type Double: '${result::class.java}'.")
    }

    fun getBoolean(path: String): Boolean? {
        val result = getElement(path)
        if (result == null)
            return null
        if (result is Boolean) {
            return result
        }
        throw java.lang.IllegalArgumentException("Requested element of path '${path}' isn't of type Boolean: '${result::class.java}'.")
    }

    fun getList(path: String): List<*>? {
        val result = getElement(path)
        if (result == null)
            return null
        if (result is List<*>) {
            return result
        }
        throw java.lang.IllegalArgumentException("Requested element of path '${path}' isn't of type List<?>: '${result::class.java}'.")
    }

    fun getMap(path: String): Map<String, *>? {
        val result = getElement(path)
        if (result == null)
            return null
        if (result is Map<*, *>) {
            @Suppress("UNCHECKED_CAST")
            return result as Map<String, *>
        }
        throw java.lang.IllegalArgumentException("Requested element of path '${path}' isn't of type Map<?,?>: '${result::class.java}'.")
    }

    fun getElement(path: String): Any? {
        var currentMap: Map<*, *>? = map
        var result: Any? = null
        val pathValues = path.split('.')
        pathValues.forEach {
            if (currentMap == null) {
                throw IllegalArgumentException("Can't step so deep: '${path}'. '${it}' doesn't exist.")
            }
            if (it.isNullOrBlank())
                throw IllegalArgumentException("Illegal path: '${path}' contains empty attributes such as 'a..b'.")

            var idx: Int?;
            var attr = it
            var value: Any?
            if (it.indexOf('[') > 0) {
                // Array found:
                if (!it.matches(attrRegexWithIndex)) {
                    throw IllegalArgumentException("Illegal path: '${path}' contains illegal attribute ('${attrPattern}' or '${attrPatternWithIndex}' are supported: '${it}'.")
                }
                attr = it.substring(0, it.indexOf('['))
                idx = attrRegexWithIndex.find(it)!!.groups[1]?.value?.toInt()
                if (idx == null)
                    throw IllegalArgumentException("Illegal path: '${path}' contains illegal attribute ('${attrPattern}' or '${attrPatternWithIndex}' are supported: '${it}'.")

                val arr = currentMap?.get(attr)
                if (arr == null || !(arr is List<*>)) {
                    throw IllegalArgumentException("Illegal path: '${attr}' not found as array: '${path}': '${arr}'")
                }
                value = arr[idx]
            } else if (!it.matches(attrReqex)) {
                throw IllegalArgumentException("Illegal path: '${path}' contains illegal attribute ('${attrPattern}' or '${attrPatternWithIndex}' are supported: '${it}'.")
            } else {
                value = currentMap?.get(attr)
            }
            if (value == null) {
                currentMap = null
            } else {
                if (value is Map<*, *>) {
                    @Suppress("UNCHECKED_CAST")
                    currentMap = value as Map<String, Any?>
                } else {
                    currentMap = null
                }
            }
            result = value
        }
        return result
    }

    private fun parseJson(json: String): Map<String, Any?> {
        val mapType = object : TypeToken<Map<String, Any?>>() {}.type
        return Gson().fromJson<Map<String, String>>(json, mapType)
    }
}
