package org.projectforge.ui

import com.google.gson.annotations.SerializedName

data class UIInput(val id: String,
                   @SerializedName("max-length")
                   var maxLength: Int? = null,
                   val required: Boolean? = null,
                   val focus: Boolean? = null,
                   override var label: String? = null,
                   @SerializedName("additional-label")
                   override var additionalLabel: String? = null)
    : UIElement(UIElementType.INPUT), UILabelledElement
