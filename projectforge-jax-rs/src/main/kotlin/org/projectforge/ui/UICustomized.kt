package org.projectforge.ui

data class UICustomized(val id: String,
                        var values: MutableMap<String, Any>? = null) : UIElement(UIElementType.CUSTOMIZED) {
    fun add(property: String, value : String): UICustomized {
        if (values == null) {
            values = mutableMapOf()
        }
        values?.put(property, value);
        return this
    }
}
