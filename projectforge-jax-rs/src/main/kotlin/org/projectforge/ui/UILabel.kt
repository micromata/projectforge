package org.projectforge.ui

import com.google.gson.annotations.SerializedName

data class UILabel(
        /**
         * i18n key will be automatically translated. For protecting the auto-translation, please set [protectLabels] = true
         * or simply use trailing '^' char, e. g. "^My untranslated label".
         */
        @SerializedName("for")
        var labelFor: String? = null,
        @Transient
        var reference: UIElement? = null,
        override var label: String? = null,
        @SerializedName("additional-label")
        override var additionalLabel: String? = null,
        override var tooltip: String? = null,
        @Transient
        override var protectLabels: Boolean = false)
    : UIElement(UIElementType.LABEL), UILabelledElement
