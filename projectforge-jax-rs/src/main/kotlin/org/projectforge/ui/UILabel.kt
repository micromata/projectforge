package org.projectforge.ui

data class UILabel(
        override var label: String? = null,
        /**
         * i18n key will be automatically translated. For protecting the auto-translation, please set [protectLabels] = true
         * or simply use trailing '^' char, e. g. "^My untranslated label".
         */
        var labelFor: String? = null,
        @Transient
        override val layoutSettings: LayoutContext? = null,
        @Transient
        var reference: UIElement? = null,
        override var additionalLabel: String? = null,
        override var tooltip: String? = null)
    : UIElement(UIElementType.LABEL), UILabelledElement
