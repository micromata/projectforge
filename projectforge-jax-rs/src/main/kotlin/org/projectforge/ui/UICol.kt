package org.projectforge.ui

import org.apache.xalan.xsltc.runtime.CallFunction.clazz
import org.projectforge.rest.ui.LayoutSettings
import org.projectforge.rest.ui.UIElementsRegistry

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
            group.add(it, UIElementsRegistry.getElement(layoutSettings.dataObjectClazz, it))
            add(group)
        }
        return this
    }
}