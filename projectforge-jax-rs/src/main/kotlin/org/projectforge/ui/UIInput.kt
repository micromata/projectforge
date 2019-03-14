package org.projectforge.ui

import com.google.gson.annotations.SerializedName

data class UIInput(val id: String,
                   @SerializedName("max-length")
                   val maxLength: Int,
                   val required: Boolean? = null,
                   val focus: Boolean? = null) : UIElement(UIElementType.INPUT)
