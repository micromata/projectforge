package org.projectforge.ui

import com.google.gson.annotations.SerializedName

data class UILabel(
        override var label: String? = null,
        /**
         * i18n key will be automatically translated. For protecting the auto-translation, please set [protectLabels] = true
         * or simply use trailing '^' char, e. g. "^My untranslated label".
         */
        @SerializedName("for")
        var labelFor: String? = null,
        @Transient
        override val layoutSettings: LayoutSettings? = null,
        @Transient
        var reference: UIElement? = null,
        @SerializedName("additional-label")
        override var additionalLabel: String? = null,
        override var tooltip: String? = null)
    : UIElement(UIElementType.LABEL), UILabelledElement
