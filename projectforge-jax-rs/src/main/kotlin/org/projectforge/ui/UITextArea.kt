package org.projectforge.ui

data class UITextArea(val id: String,
                      @Transient
                      override val layoutSettings: LayoutContext? = null,
                      var maxLength: Int? = null,
                      /**
                       * Number of rows to display initially.
                       */
                      var rows:Int? = 3,
                      /**
                       * Max rows to auto-extend for long content.
                       */
                      var maxRows:Int? = 10,
                      override var label: String? = null,
                      override var additionalLabel: String? = null,
                      override var tooltip: String? = null)
    : UIElement(UIElementType.TEXTAREA), UILabelledElement
