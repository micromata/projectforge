package org.projectforge.ui

class UILayout(
        val title: String? = null) {

    val layout: MutableList<UIElement> = mutableListOf()
    fun add(element: UIElement): UILayout {
        layout.add(element)
        return this
    }

    data class Builder(
            var title: String? = null
    ) {
        fun title(title: String) = apply { this.title = title }
        fun build(): UILayout {
            val layout =
                    UILayout(title)
            return layout
        }
    }
}