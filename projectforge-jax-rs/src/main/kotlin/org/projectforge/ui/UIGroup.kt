package org.projectforge.ui

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
     * Adds a UILabel and the given element. UILabel.labelFor is set with id of given element.
     * @param label The value of the label to add as UILabel.
     * @param element The element (of type UIInput, UITextArea or UISelect).
     */
    fun add(id: String, element: UIElement?): UIGroup? {
        if (element == null)
            return null
        val label = when (element) {
            is UILabelledElement -> element.label
            else -> "??? ${id} ???"
        }
        return add(UILabel(label = label, protectLabels = true), element)
    }

    /**
     * Convenient method:
     * Adds a UILabel and the given element. UILabel.labelFor is set with id of given element.
     * @param label The label to add.
     * @param element The element (of type UIInput, UITextArea or UISelect).
     */
    fun add(label: UILabel, element: UIElement?): UIGroup? {
        if (element == null)
            return null
        label.reference = element
        label.labelFor =
                when (element) {
                    is UIInput -> element.id
                    is UITextarea -> element.id
                    is UISelect -> element.id
                    is UIMultiSelect -> element.id
                    else -> null
                }
        if (label.labelFor == null)
            log.error("Unsupported element for method add(UILabel, UIElement): '${element}'. Supported elements are UIInput, UISelect and UITextArea")
        content.add(label)
        content.add(element)
        return this;
    }
}