package org.projectforge.ui

import com.google.gson.annotations.SerializedName

data class UILabel(var value: String,
                   @SerializedName("for")
                   var labelFor:String? = null,
                   @Transient var reference : UIElement? = null) : UIElement(UIElementType.LABEL)
