package org.projectforge.ui

import com.google.gson.annotations.SerializedName

data class UILabel(val value: String,
                   @SerializedName("for")
                   val labelFor:String? = null) : UIElement(UIElementType.LABEL)
