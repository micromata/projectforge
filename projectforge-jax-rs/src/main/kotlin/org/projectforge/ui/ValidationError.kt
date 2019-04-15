package org.projectforge.ui

import com.google.gson.annotations.SerializedName

data class ValidationError(var message: String? = null,
                           @SerializedName("field-id")
                           var fieldId: String? = null,
                           @SerializedName("message-id")
                           var messageId: String? = null)