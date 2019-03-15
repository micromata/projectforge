package org.projectforge.ui

data class UICol(val length: Int,
                 val content: MutableList<UIElement> = mutableListOf()) : UIElement(UIElementType.COL) {
    fun add(element: UIElement): UICol {
        content.add(element)
        return this
    }
}