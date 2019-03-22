package org.projectforge.ui

import org.projectforge.rest.ui.LayoutUtils

/**
 * A group represents a group of UI elements, such as a pair of a Label and an Input field.
 */
data class UIGroup(val content: MutableList<UIElement> = mutableListOf()) : UIElement(UIElementType.GROUP) {
    @Transient
    private val log = org.slf4j.LoggerFactory.getLogger(UIGroup::class.java)

    fun add(element: UIElement): UIGroup {
        content.add(element)
        return this
    }

    /**
     * Convenient method:
     * Adds a UILabel and the given input. UILabel.labelFor is set with id of given input.
     * @param label The value of the label to add as UILabel.
     * @param input The element (of type UIInput, UITextArea or UISelect).
     */
    fun add(label: String, input: UIElement): UIGroup {
        return add(UILabel(label = label), input)
    }

    /**
     * Convenient method:
     * Adds a UILabel and the given input. UILabel.labelFor is set with id of given input.
     * @param label The label to add.
     * @param input The element (of type UIInput, UITextArea or UISelect).
     */
    fun add(label: UILabel, input: UIElement): UIGroup {
        label.reference = input
        label.labelFor =
                when (input) {
                    is UIInput -> input.id
                    is UITextarea -> input.id
                    is UISelect -> input.id
                    is UIMultiSelect -> input.id
                    else -> null
                }
        if (label.labelFor == null)
            log.error("Unsupported element for method add(UILabel, UIElement): '${input}'. Supported elements are UIInput, UISelect and UITextArea")
        content.add(label)
        content.add(input)
        return this;
    }
}