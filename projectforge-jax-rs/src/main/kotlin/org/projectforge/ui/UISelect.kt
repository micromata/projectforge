package org.projectforge.ui

import com.google.gson.annotations.SerializedName

data class UISelect(val id: String,
                    val values: MutableList<UISelectValue> = mutableListOf(),
                    val required: Boolean? = null,
                    @Transient
                    val i18nEnum: Class<out Enum<*>>? = null,
                    override var label: String? = null,
                    @SerializedName("additional-label")
                    override var additionalLabel: String? = null)
    : UIElement(UIElementType.SELECT), UILabelledElement {

    fun add(selectValue: UISelectValue): UISelect {
        values.add(selectValue)
        return this
    }
}
