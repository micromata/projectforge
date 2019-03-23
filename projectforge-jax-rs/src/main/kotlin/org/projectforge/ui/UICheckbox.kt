package org.projectforge.ui

import com.google.gson.annotations.SerializedName

data class UICheckbox(val id: String,
                      override var tooltip: String? = null,
                      override var label: String? = null,
                      @SerializedName("additional-label")
                      override var additionalLabel: String? = null,
                      @Transient
                      override var protectLabels: Boolean = false)
    : UIElement(UIElementType.CHECKBOX), UILabelledElement