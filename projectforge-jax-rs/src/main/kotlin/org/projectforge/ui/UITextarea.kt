package org.projectforge.ui

data class UITextarea(val id: String,
                      @Transient
                      override val layoutSettings: LayoutContext? = null,
                      var maxLength: Int? = null,
                      override var label: String? = null,
                      override var additionalLabel: String? = null,
                      override var tooltip: String? = null)
    : UIElement(UIElementType.TEXTAREA), UILabelledElement
