package org.projectforge.rest.json

import com.google.gson.*
import org.projectforge.ui.AutoCompletion
import org.projectforge.ui.UIMultiSelect
import java.lang.reflect.Type

/**
 * Serialization.
 * @author Kai Reinhard (k.reinhard@micromata.de)
 */
class UIMultiSelectTypeSerializer : JsonSerializer<UIMultiSelect> {

    @Synchronized
    public override fun serialize(obj: UIMultiSelect, type: Type, jsonSerializationContext: JsonSerializationContext): JsonElement? {
        if (obj == null) return null
        val result = JsonObject()

        val valueProperty = obj.valueProperty ?: "value"
        val labelProperty = obj.labelProperty ?: "label"

        JsonCreator.addJsonPrimitive(result, "id", obj.id)
        JsonCreator.addJsonPrimitive(result, "type", obj.type.name)
        JsonCreator.addJsonPrimitive(result, "key", obj.key)
        JsonCreator.addJsonPrimitive(result, "required", obj.required)
        JsonCreator.addJsonPrimitive(result, "label", obj.label)
        JsonCreator.addJsonPrimitive(result, "additionalLabel", obj.additionalLabel)
        JsonCreator.addJsonPrimitive(result, "tooltip", obj.tooltip)
        val values = JsonArray()
        JsonCreator.addJsonPrimitive(result, "labelProperty", obj.labelProperty)
        JsonCreator.addJsonPrimitive(result, "valueProperty", obj.valueProperty)
        result.add("values", values)
        obj.values?.forEach {
            if (it.value != null) {
                val value = JsonObject()
                JsonCreator.addJsonPrimitive(value, valueProperty, it.value)
                JsonCreator.addJsonPrimitive(value, labelProperty, it.label)
                values.add(value)
            }
        }
        if (obj.autoCompletion != null) {
            val autoCompletion = JsonObject()
            result.add("autoCompletion", autoCompletion)
            val ac = obj.autoCompletion
            JsonCreator.addJsonPrimitive(autoCompletion, "minChars", ac?.minChars)
            JsonCreator.addJsonPrimitive(autoCompletion, "url", ac?.url)
            writeEntries(autoCompletion, ac?.values, "values", valueProperty, labelProperty)
            writeEntries(autoCompletion, ac?.recent, "recent", valueProperty, labelProperty)
        }
        return result
    }

    private fun writeEntries(parent : JsonObject, entries : List<AutoCompletion.Entry<out Any?>>?, property : String, valueProperty : String, labelProperty : String) {
        if (entries != null) {
            val values = JsonArray()
            parent.add(property, values)
            entries.forEach {
                val entry = JsonObject()
                JsonCreator.addJsonPrimitive(entry, valueProperty, it.value)
                JsonCreator.addJsonPrimitive(entry, labelProperty, it.label)
                JsonCreator.addJsonPrimitive(entry, "allSearchableFields", it.allSearchableFields)
                values.add(entry)
            }
        }
    }
}
