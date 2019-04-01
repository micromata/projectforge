package org.projectforge.ui

data class UIInput(val id: String,
                   @Transient
                   override val layoutSettings: LayoutContext? = null,
                   var maxLength: Int? = null,
                   var required: Boolean? = null,
                   var focus: Boolean? = null,
                   var dataType : UIDataType = UIDataType.STRING,
                   override var label: String? = null,
                   override var additionalLabel: String? = null,
                   override var tooltip: String? = null)
    : UIElement(UIElementType.INPUT), UILabelledElement
