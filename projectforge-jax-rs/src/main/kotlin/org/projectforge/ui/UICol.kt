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
    fun add(vararg ids: String): UICol {
        ids.forEach {
            val group = UIGroup()
            group.add(it, UIInput(it))
        }
        return this
    }
}