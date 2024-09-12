package org.projectforge.framework.persistence.jpa.candh

import org.projectforge.framework.json.JsonUtils

internal class DebugContext {
    val entries = mutableListOf<Entry>()

    fun add(
        field: String? = null,
        srcVal: Any? = null,
        destVal: Any? = null,
        msg: String? = "copied",
    ) {
        entries.add(Entry(fieldName = field, srcValue = srcVal, destValue = destVal, message = msg))
    }

    override fun toString(): String {
        return JsonUtils.toJson(this)
    }

    class Entry(
        val fieldName: String? = null,
        val srcValue: Any? = null,
        val destValue: Any? = null,
        val message: String? = null,
    )
}
