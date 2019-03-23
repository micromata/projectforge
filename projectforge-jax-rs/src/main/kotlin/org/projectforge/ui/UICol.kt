package org.projectforge.ui

data class UICol(val length: Int,
                 val content: MutableList<UIElement> = mutableListOf()) : UIElement(UIElementType.COL) {
    fun add(element: UIElement): UICol {
        content.add(element)
        return this
    }

    /**
     * Convenient method for adding a bunch of UIInput fields with the given ids.
     */
    fun add(layoutSettings: LayoutSettings, vararg ids: String): UICol {
        ids.forEach {
            val group = UIGroup()
            group.add(it, UIElementsRegistry.buildElement(layoutSettings, it))
            add(group)
        }
        return this
    }
}