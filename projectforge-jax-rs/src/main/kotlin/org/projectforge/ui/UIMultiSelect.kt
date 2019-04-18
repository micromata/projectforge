package org.projectforge.ui

data class UIMultiSelect(val id: String,
                         @Transient
                         override val layoutSettings: LayoutContext? = null,
                         val required: Boolean? = null,
                         override var label: String? = null,
                         override var additionalLabel: String? = null,
                         override var tooltip: String? = null,
                         var values: List<UISelectValue<*>>? = null,
                         var valueProperty : String? = null,
                         var labelProperty : String? = null,
                         var autoCompletion : AutoCompletion<*>? = null)
    : UIElement(UIElementType.MULTI_SELECT), UILabelledElement
