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
    private class TeamCal(val id: Int?, val title: String?)

    @Throws(IOException::class, JsonProcessingException::class)
    override fun serialize(value: TeamCalDO?, jgen: JsonGenerator, provider: SerializerProvider) {
        if (value == null) {
            jgen.writeNull()
            return
        }
        val teamCal = TeamCal(value.id, value.title)
        jgen.writeObject(teamCal)
    }
}