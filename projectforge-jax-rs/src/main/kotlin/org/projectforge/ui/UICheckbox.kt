package org.projectforge.ui

import com.google.gson.annotations.SerializedName

data class UICheckbox(val id: String,
                      var tooltip: String? = null,
                      override var label: String? = null,
                      @SerializedName("additional-label")
                      override var additionalLabel: String? = null)
    : UIElement(UIElementType.CHECKBOX), UILabelledElement {
    /**
     * The label of the checkbox will be auto-translated in [LayoutUtils.processAllElements]. You may prevent this
     * by setting [protectLabel] to true.
     */
    @Transient
    var protectLabel : Boolean? = false
}
