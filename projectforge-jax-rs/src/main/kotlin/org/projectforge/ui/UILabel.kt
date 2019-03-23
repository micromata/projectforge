package org.projectforge.ui

import com.google.gson.annotations.SerializedName

data class UILabel(
        /**
         * i18n key will be automatically translated. For protecting the auto-translation, please set [protectLabel] = true
         * or simply use trailing '^' char, e. g. "^My untranslated label".
         */
        override var label: String? = null,
                   @SerializedName("for")
                   var labelFor: String? = null,
                   @Transient
                   var reference: UIElement? = null,
                   @Transient
                   var protectLabel: Boolean = false,
                   @SerializedName("additional-label")
                   override var additionalLabel: String? = null)
    : UIElement(UIElementType.LABEL), UILabelledElement
