package org.projectforge.ui

import com.google.gson.annotations.SerializedName

enum class UIDataType {
    @SerializedName("string")
    STRING,
    @SerializedName("date")
    DATE,
    @SerializedName("locale")
    LOCALE,
    @SerializedName("picture")
    PICTURE,
    @SerializedName("customized")
    CUSTOMIZED
}
