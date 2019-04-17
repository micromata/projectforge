package org.projectforge.rest.calendar

import com.google.gson.*
import org.projectforge.business.teamcal.admin.model.TeamCalDO
import java.lang.reflect.Type


/**
 * Serialization for TeamCalDO etc.
 */
class TeamCalDOSerializer : JsonSerializer<TeamCalDO> {
    @Synchronized
    override fun serialize(obj: TeamCalDO?, type: Type, jsonSerializationContext: JsonSerializationContext): JsonElement? {
        if (obj == null) return null
        val result = JsonObject()
        result.add("id", JsonPrimitive(obj.id))
        result.add("title", JsonPrimitive(obj.title))
        return result
    }
}
