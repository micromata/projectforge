package org.projectforge.ui

open class UICol(val length: Int? = null,
                 val content: MutableList<UIElement> = mutableListOf(),
                 type: UIElementType = UIElementType.COL)
    : UIElement(type) {
    fun add(element: UIElement): UICol {
        content.add(element)
        return this
    }

    /**
     * Convenient method for adding a bunch of UIInput fields with the given ids.
     * @param createRowCol If true (default), the elements will be surrounded with [UIRow] and [UICol] each, otherwise not.
     */
    fun add(layoutSettings: LayoutContext, vararg ids: String, createRowCol: Boolean = true): UICol {
        ids.forEach {
            val element = LayoutUtils.buildLabelInputElement(layoutSettings, it)
            if (element != null) {
                add(LayoutUtils.prepareElementToAdd(element, createRowCol))
            }
        }
        return this
    }
}
