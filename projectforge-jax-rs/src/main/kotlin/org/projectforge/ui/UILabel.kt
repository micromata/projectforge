package org.projectforge.ui

import com.google.gson.annotations.SerializedName

data class UILabel(var value: String,
                   /**
                    * May-be used for additional sub labels.
                    */
                   @SerializedName("additional-value")
                   var additionalValue : String? = null,
                   @SerializedName("for")
                   var labelFor:String? = null,
                   @Transient var reference : UIElement? = null) : UIElement(UIElementType.LABEL)
