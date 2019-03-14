package org.projectforge.ui

data class UIMultiSelect(val id: String,
                         val required: Boolean? = null) : UIElement(UIElementType.MULTI_SELECT) {
}
