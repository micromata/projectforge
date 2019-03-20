package org.projectforge.ui

/**
 * A named container represents a bunch of UIElements usable by the UI referenced by the given name (id). It may contain
 * e. g. filter settings in a list view.
 */
data class UINamedContainer(
        /** The name of the container. */
        val id: String,
        /**
         * The key is an unique id, used e. g. by React for lists.
         */
        var key : String? = null,
        val content: MutableList<UIElement> = mutableListOf()) {
    val type : String = "named-container"

    fun add(element: UIElement): UINamedContainer {
        content.add(element)
        return this
    }
}