package org.projectforge.rest.json

import com.google.gson.*
import org.projectforge.framework.persistence.user.entities.PFUserDO
import org.projectforge.framework.persistence.user.entities.TenantDO
import java.lang.reflect.Type


/**
 * Serialization for PFUserDO etc.
 */
class PFUserDOSerializer : JsonSerializer<PFUserDO> {
    @Synchronized
    override fun serialize(obj: PFUserDO?, type: Type, jsonSerializationContext: JsonSerializationContext): JsonElement? {
        if (obj == null) return null
        val result = JsonObject()
        result.add("id", JsonPrimitive(obj.id))
        result.add("username", JsonPrimitive(obj.username))
        result.add("fullname", JsonPrimitive(obj.fullname))
        result.add("email", JsonPrimitive(obj.email))
        return result
    }
}

class TenantDOSerializer : JsonSerializer<TenantDO> {
    @Synchronized
    override fun serialize(obj: TenantDO?, type: Type, jsonSerializationContext: JsonSerializationContext): JsonElement? {
        if (obj == null) return null
        val result = JsonObject()
        result.add("id", JsonPrimitive(obj.id))
        result.add("name", JsonPrimitive(obj.name))
        return result
    }
}

private fun addProperty(result: JsonObject, property: String, value: Any?) {
    if (value == null) {
        return
    }
    when (value) {
        is String -> result.add(property, JsonPrimitive(value))
        is Int -> result.add(property, JsonPrimitive(value))
        else -> throw IllegalArgumentException("Can't create gson primitive: '${value}'.")
    }
}
