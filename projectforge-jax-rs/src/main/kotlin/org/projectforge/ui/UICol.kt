package org.projectforge.ui

open class UICol(val length: Int,
                 val content: MutableList<UIElement> = mutableListOf(),
                 type : UIElementType = UIElementType.COL)
    : UIElement(type) {
    fun add(element: UIElement): UICol {
        content.add(element)
        return this
    }

    /**
     * Convenient method for adding a bunch of UIInput fields with the given ids.
     */
    fun add(layoutSettings: LayoutContext, vararg ids: String): UICol {
        ids.forEach {
            val element = LayoutUtils.buildLabelInputElement(layoutSettings, it)
            if (element != null)
                add(element)
        }
        return this
    }
}