package org.projectforge.ui

import org.projectforge.rest.core.AbstractStandardRest

data class UIInput(val id: String,
                   @Transient
                   override val layoutSettings: LayoutContext? = null,
                   var maxLength: Int? = null,
                   var required: Boolean? = null,
                   var focus: Boolean? = null,
                   var dataType: UIDataType = UIDataType.STRING,
                   override var label: String? = null,
                   override var additionalLabel: String? = null,
                   override var tooltip: String? = null)
    : UIElement(UIElementType.INPUT), UILabelledElement {
    var autoCompletionUrl: String? = null

    /**
     * @return this for chaining.
     */
    fun enableAutoCompletion(services: AbstractStandardRest<*, *, *, *>):UIInput {
        autoCompletionUrl = "${services.getFullRestPath()}/ac?property=${id}&search="
        return this
    }
}
