package org.projectforge.ui

import com.google.gson.annotations.SerializedName

interface UILabelledElement {
    var label: String?
    var additionalLabel: String?
    var tooltip : String?
    var protectLabels : Boolean
}