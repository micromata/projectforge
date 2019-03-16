package org.projectforge.ui

/**
 * A group represents a group of UI elements, such as a pair of a Label and an Input field.
 */
data class UIGroup(val content: MutableList<UIElement> = mutableListOf()) : UIElement(UIElementType.GROUP) {
    fun add(element: UIElement): UIGroup {
        content.add(element)
        return this
    }

    /**
     * Adds a UILabel and the given UIInput. UILabel.labelFor is set with id of given input.
     */
    fun add(label: String, input: UIInput): UIGroup {
        content.add(UILabel(label, input.id))
        content.add(input)
        return this;
    }

    /**
     * Adds a UILabel and the given UISelect. UILabel.labelFor is set with id of given input.
     */
    fun add(label: String, textArea: UITextarea): UIGroup {
        content.add(UILabel(label, textArea.id))
        content.add(textArea)
        return this;
    }

    /**
     * Adds a UILabel and the given UISelect. UILabel.labelFor is set with id of given input.
     */
    fun add(label: String, select: UISelect): UIGroup {
        content.add(UILabel(label, select.id))
        content.add(select)
        return this;
    }
}