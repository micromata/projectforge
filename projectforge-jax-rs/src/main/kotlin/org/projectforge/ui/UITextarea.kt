package org.projectforge.ui

import com.google.gson.annotations.SerializedName

data class UITextarea(val id: String,
                      @Transient
                      override val layoutSettings: LayoutContext? = null,
                      @SerializedName("max-length")
                      var maxLength: Int? = null,
                      override var label: String? = null,
                      @SerializedName("additional-label")
                      override var additionalLabel: String? = null,
                      override var tooltip: String? = null)
    : UIElement(UIElementType.TEXTAREA), UILabelledElement
