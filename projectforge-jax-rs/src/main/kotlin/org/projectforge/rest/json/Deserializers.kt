package org.projectforge.rest.json

import com.fasterxml.jackson.core.JsonParser
import com.fasterxml.jackson.databind.DeserializationContext
import com.fasterxml.jackson.databind.JsonNode
import com.fasterxml.jackson.databind.deser.std.StdDeserializer
import com.fasterxml.jackson.databind.node.IntNode
import org.projectforge.framework.persistence.user.entities.PFUserDO


/**
 * Deserialization for PFUserDO etc.
 */
class PFUserDODeserializer : StdDeserializer<PFUserDO>(PFUserDO::class.java) {
    private class User(val id: Int?, val username: String?, val fullname: String?)

    override fun deserialize(p: JsonParser, ctxt: DeserializationContext?): PFUserDO? {
        val node: JsonNode = p.getCodec().readTree(p)
        val id = (node.get("id") as IntNode).numberValue() as Int
        val user = PFUserDO()
        user.id = id
        return user
    }
}
