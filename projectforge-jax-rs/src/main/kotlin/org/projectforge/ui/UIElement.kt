package org.projectforge.ui

import com.google.gson.annotations.SerializedName

data class UIElement(
        val type: UIElementType,
        val value: String? = null,
        @SerializedName("for")
        val forField: String? = null,
        val id: String? = null,
        @SerializedName("max-length")
        val maxLength: Int? = null,
        val required: Boolean? = null,
        val focus: Boolean? = null,
        val length: Int? = null) {

    var content: MutableList<UIElement>? = null

    var values: MutableList<UISelectValue>? = null

    fun add(element: UIElement): UIElement {
        if (content == null) {
            content = mutableListOf()
        }
        content?.add(element)
        return this
    }

    fun add(selectValue: UISelectValue): UIElement {
        if (values == null) {
            values = mutableListOf()
        }
        values?.add(selectValue)
        return this
    }

    data class Builder(
            var type: UIElementType,
            var value: String? = null,
            var forField: String? = null,
            var id: String? = null,
            var maxLength: Int? = null,
            var required: Boolean? = null,
            var focus: Boolean? = null,
            var length: Int? = null
    ) {
        fun type(type: UIElementType) = apply { this.type = type }
        fun value(value: String) = apply { this.value = value }
        fun forField(forField: String) = apply { this.forField = forField }
        fun id(id: String) = apply { this.id = id }
        fun maxLength(maxLength: Int) = apply { this.maxLength = maxLength }
        fun required() = apply { this.required = true }
        fun focus() = apply { this.focus = true }
        fun length(length: Int) = apply { this.length = length }
        fun build(): UIElement {
            val element =
                    UIElement(type, value, forField, id, maxLength, required, focus, length)
            element.check()
            return element
        }
    }

    fun check() {
        require("type", type)
        if (!this.type.contentAllowed() && !content.isNullOrEmpty()) {
            error("It's not allowed to add elements to non-groups.")
        }
        if (type != UIElementType.SELECT && !values.isNullOrEmpty()) {
            error("It's not allowed to add values to non-select fields.")
        }

        when (type) {
            UIElementType.LABEL -> {
                require("value", value)
            }
        }
    }

    fun require(field: String, fieldValue: Any?) {
        if (fieldValue == null) {
            error("Field '${field}' required.")
        }
    }


    fun notAllowed(field: String, fieldValue: Any?) {
        if (fieldValue != null) {
            error("Field '${field}' not allowed.")
        }
    }

    fun error(msg: String) {
        throw IllegalArgumentException("${msg}: ${this}")
    }
}