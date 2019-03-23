package org.projectforge.ui

import com.google.gson.annotations.SerializedName

class UILayout {
    constructor(title: String) {
        this.title = LayoutUtils.getLabelTransformation(title)
    }

    var title: String?
    val layout: MutableList<UIElement> = mutableListOf()
    @SerializedName("named-containers")
    val namedContainers: MutableList<UINamedContainer> = mutableListOf()
    val actions: MutableList<UIElement> = mutableListOf()

    fun add(element: UIElement): UILayout {
        layout.add(element)
        return this
    }

    fun addAction(element: UIElement): UILayout {
        actions.add(element)
        return this
    }

    fun add(namedContainer: UINamedContainer): UILayout {
        namedContainers.add(namedContainer)
        return this
    }

    fun getAllElements(): List<Any> {
        val list = mutableListOf<Any>()
        addAllElements(list, layout)
        namedContainers.forEach { addAllElements(list, it.content) }
        addAllElements(list, actions)
        return list
    }

    fun getElementById(id: String): UIElement? {
        var element = getElementById(id, layout)
        if (element != null)
            return element
        return null
    }

    private fun getElementById(id: String, elements: List<UIElement>): UIElement? {
        elements.forEach {
            if (LayoutUtils.getId(it, followLabelReference = false) == id)
                return it
            val element = when (it) {
                is UIGroup -> getElementById(id, it.content)
                is UIRow -> getElementById(id, it.content)
                is UICol -> getElementById(id, it.content)
                is UITable -> getElementById(id, it.columns)
                else -> null
            }
            if (element != null)
                return element
        }
        return null
    }

    private fun addAllElements(list: MutableList<Any>, elements: MutableList<UIElement>) {
        elements.forEach { addAllElements(list, it) }
    }

    private fun addAllCols(list: MutableList<Any>, cols: MutableList<UICol>) {
        cols.forEach { addAllElements(list, it) }
    }

    private fun addAllElements(list: MutableList<Any>, element: UIElement) {
        list.add(element)
        when (element) {
            is UIGroup -> addAllElements(list, element.content)
            is UIRow -> addAllCols(list, element.content)
            is UICol -> addAllElements(list, element.content)
            is UISelect -> element.values.forEach { list.add(it) }
            is UITable -> element.columns.forEach { list.add(it) }
        }
    }
}