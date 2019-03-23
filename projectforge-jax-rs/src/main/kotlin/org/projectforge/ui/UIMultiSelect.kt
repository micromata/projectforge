package org.projectforge.ui

import com.google.gson.annotations.SerializedName

data class UIMultiSelect(val id: String,
                         @Transient
                         override val layoutSettings: LayoutSettings? = null,
                         val required: Boolean? = null,
                         override var label: String? = null,
                         @SerializedName("additional-label")
                         override var additionalLabel: String? = null,
                         override var tooltip: String? = null)
    : UIElement(UIElementType.MULTI_SELECT), UILabelledElement {
}
