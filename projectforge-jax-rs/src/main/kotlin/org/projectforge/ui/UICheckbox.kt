package org.projectforge.ui

data class UICheckbox(val id: String,
                      @Transient
                      override val layoutSettings: LayoutContext? = null,
                      override var tooltip: String? = null,
                      override var label: String? = null,
                      override var additionalLabel: String? = null)
    : UIElement(UIElementType.CHECKBOX), UILabelledElement