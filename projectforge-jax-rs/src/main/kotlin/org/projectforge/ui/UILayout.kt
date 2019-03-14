package org.projectforge.ui

class UILayout(val title: String? = null) {
    val layout: MutableList<UIElement> = mutableListOf()
    fun add(element: UIElement): UILayout {
        layout.add(element)
        return this
    }
}