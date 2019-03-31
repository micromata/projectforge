package org.projectforge.ui

import com.google.gson.annotations.SerializedName

enum class UIStyle {
    @SerializedName("danger")
    DANGER,
    @SerializedName("info")
    INFO,
    @SerializedName("link")
    LINK,
    @SerializedName("primary")
    PRIMARY,
    @SerializedName("secondary")
    SECONDARY,
    @SerializedName("success")
    SUCCESS,
    @SerializedName("warning")
    WARNING
}
