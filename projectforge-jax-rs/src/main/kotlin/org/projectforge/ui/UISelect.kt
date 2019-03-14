package org.projectforge.ui

data class UISelect(val id: String,
                    val values: MutableList<UISelectValue> = mutableListOf(),
                    val required: Boolean? = null) : UIElement(UIElementType.SELECT) {

    fun add(selectValue: UISelectValue): UISelect {
        values.add(selectValue)
        return this
    }
}
