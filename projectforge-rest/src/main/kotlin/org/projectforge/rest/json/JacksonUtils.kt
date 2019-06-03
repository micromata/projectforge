package org.projectforge.rest.json

import com.fasterxml.jackson.core.JsonGenerator
import java.math.BigDecimal

class JacksonUtils {
    companion object {
        fun writeField(jgen: JsonGenerator, field: String, value: Any?) {
            if (value == null) return
            when (value) {
                is String -> jgen.writeStringField(field, value)
                is Boolean -> jgen.writeBooleanField(field, value)
                is Int -> jgen.writeNumberField(field, value)
                is Double -> jgen.writeNumberField(field, value)
                is Float -> jgen.writeNumberField(field, value)
                is Long -> jgen.writeNumberField(field, value)
                is BigDecimal -> jgen.writeNumberField(field, value)
                else -> jgen.writeStringField(field, value.toString())
            }
        }

        fun writeField(jgen:JsonGenerator, field: String, value: Boolean?) {
            if (value == null) return
            jgen.writeBooleanField(field, value)
        }

        fun writeField(jgen:JsonGenerator, field: String, value: String?) {
            if (value == null) return
            jgen.writeStringField(field, value)
        }

        fun writeField(jgen:JsonGenerator, field: String, value: Int?) {
            if (value == null) return
            jgen.writeNumberField(field, value)
        }

        fun writeField(jgen:JsonGenerator, field: String, value: Double?) {
            if (value == null) return
            jgen.writeNumberField(field, value)
        }

        fun writeField(jgen:JsonGenerator, field: String, value: Float?) {
            if (value == null) return
            jgen.writeNumberField(field, value)
        }

        fun writeField(jgen:JsonGenerator, field: String, value: Long?) {
            if (value == null) return
            jgen.writeNumberField(field, value)
        }

        fun writeField(jgen:JsonGenerator, field: String, value: BigDecimal?) {
            if (value == null) return
            jgen.writeNumberField(field, value)
        }
    }
}
