package org.projectforge.ui

import com.google.gson.annotations.SerializedName

data class UILabel(var value: String,
                   @SerializedName("for")
                   val labelFor:String? = null,
                   @Transient var reference : UIElement? = null) : UIElement(UIElementType.LABEL)
