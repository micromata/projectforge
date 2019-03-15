package org.projectforge.ui

/**
 * A group represents a group of UI elements, such as a pair of a Label and an Input field.
 */
data class UIGroup(val content: MutableList<UIElement> = mutableListOf()) : UIElement(UIElementType.GROUP) {
    fun add(element: UIElement): UIGroup {
        content.add(element)
        return this
    }
}