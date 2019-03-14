package org.projectforge.ui

data class UIRow(val content: MutableList<UICol> = mutableListOf()) : UIElement(UIElementType.ROW) {
    fun add(col: UICol): UIRow {
        content?.add(col)
        return this
    }
}