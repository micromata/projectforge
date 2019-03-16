package org.projectforge.ui

import com.google.gson.annotations.SerializedName

data class UITextarea(val id: String,
                      @SerializedName("max-length")
                      var maxLength: Int? = null) : UIElement(UIElementType.TEXTAREA)
