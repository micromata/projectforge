package org.projectforge.ui

import com.google.gson.annotations.SerializedName

data class UITableColumn(val id: String,
                         var title: String? = null,
                         @SerializedName("data-type")
                         val dataType: UIDataType = UIDataType.STRING,
                         val sortable: Boolean = true)
    : UIElement(UIElementType.TABLE_COLUMN)