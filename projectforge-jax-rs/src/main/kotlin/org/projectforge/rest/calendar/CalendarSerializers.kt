package org.projectforge.rest.calendar

import com.fasterxml.jackson.core.JsonGenerator
import com.fasterxml.jackson.core.JsonProcessingException
import com.fasterxml.jackson.databind.SerializerProvider
import com.fasterxml.jackson.databind.ser.std.StdSerializer
import org.projectforge.business.teamcal.admin.model.TeamCalDO
import java.io.IOException


/**
 * Serialization for TeamCalDO etc.
 */
class TeamCalDOSerializer : StdSerializer<TeamCalDO>(TeamCalDO::class.java) {
    @Throws(IOException::class, JsonProcessingException::class)
    override fun serialize(value: TeamCalDO?, jgen: JsonGenerator, provider: SerializerProvider) {
        if (value == null) return
        jgen.writeStartObject()
        jgen.writeNumberField("id", value.id)
        jgen.writeStringField("title", value.title)
        jgen.writeEndObject()
    }
}
