package org.projectforge.ui

import com.google.gson.annotations.SerializedName

enum class UIButtonStyle {
    @SerializedName("default")
    DEFAULT,
    @SerializedName("cancel")
    CANCEL,
    @SerializedName("danger")
    DANGER,
    @SerializedName("warn")
    WARN
}
