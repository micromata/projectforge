package org.projectforge.ui

class UILayout(val title: String? = null) {
    val layout: MutableList<UIElement> = mutableListOf()
    val actions: MutableList<UIElement> = mutableListOf()

    fun add(element: UIElement): UILayout {
        layout.add(element)
        return this
    }

    fun addAction(element: UIElement): UILayout {
        actions.add(element)
        return this
    }
}