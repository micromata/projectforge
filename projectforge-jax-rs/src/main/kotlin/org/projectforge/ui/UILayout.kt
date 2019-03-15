package org.projectforge.ui

import com.google.gson.annotations.SerializedName

class UILayout(val title: String? = null) {
    val layout: MutableList<UIElement> = mutableListOf()
    @SerializedName("named-containers")
    val namedContainers : MutableList<UINamedContainer> = mutableListOf()
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
}