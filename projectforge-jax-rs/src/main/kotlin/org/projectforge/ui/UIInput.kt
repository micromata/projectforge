package org.projectforge.ui

import com.google.gson.annotations.SerializedName

data class UIInput(val id: String,
                   @Transient
                   override val layoutSettings: LayoutSettings? = null,
                   @SerializedName("max-length")
                   var maxLength: Int? = null,
                   var required: Boolean? = null,
                   var focus: Boolean? = null,
                   var dataType : UIDataType = UIDataType.STRING,
                   override var label: String? = null,
                   @SerializedName("additional-label")
                   override var additionalLabel: String? = null,
                   override var tooltip: String? = null)
    : UIElement(UIElementType.INPUT), UILabelledElement
