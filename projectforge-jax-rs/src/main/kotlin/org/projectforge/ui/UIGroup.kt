package org.projectforge.ui

data class UIGroup(val content: MutableList<UIElement> = mutableListOf()) : UIElement(UIElementType.GROUP) {
    fun add(element: UIElement): UIGroup {
        content?.add(element)
        return this
    }
}