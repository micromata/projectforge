package org.projectforge.ui

import com.google.gson.annotations.SerializedName

data class UITableColumn(val id: String,
                         var title: String? = null,
                         @Transient
                         var protectTitle: Boolean = false,
                         @SerializedName("data-type")
                         var dataType: UIDataType = UIDataType.STRING,
                         var sortable: Boolean = true,
                         var formatter: Formatter? = null)
    : UIElement(UIElementType.TABLE_COLUMN)

enum class Formatter {
    @SerializedName("timestamp-minutes")
    TIMESTAMP_MINUTES,
    @SerializedName("user")
    USER
}
