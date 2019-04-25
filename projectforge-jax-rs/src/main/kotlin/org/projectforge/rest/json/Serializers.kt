package org.projectforge.rest.json

import com.google.gson.JsonElement
import com.google.gson.JsonSerializationContext
import com.google.gson.JsonSerializer
import org.projectforge.business.fibu.KundeDao
import org.projectforge.business.fibu.ProjektDao
import org.projectforge.business.fibu.kost.Kost2DO
import org.projectforge.business.task.TaskDO
import org.projectforge.framework.persistence.user.entities.PFUserDO
import org.projectforge.framework.persistence.user.entities.TenantDO
import org.projectforge.registry.Registry
import org.projectforge.rest.task.TaskServicesRest
import java.lang.reflect.Type


/**
 * Serialization for PFUserDO etc.
 */
class PFUserDOSerializer : JsonSerializer<PFUserDO> {
    private class User(val id: Int, val username: String?, val fullname: String?)

    @Synchronized
    override fun serialize(obj: PFUserDO?, type: Type, jsonSerializationContext: JsonSerializationContext): JsonElement? {
        if (obj == null) return null
        val user = User(obj.id, obj.username, obj.fullname)
        return jsonSerializationContext.serialize(user)
    }
}

/**
 * Serialization for TaskDO etc.
 */
class TaskDOSerializer : JsonSerializer<TaskDO> {
    @Synchronized
    override fun serialize(obj: TaskDO?, type: Type, jsonSerializationContext: JsonSerializationContext): JsonElement? {
        if (obj == null) return null
        val task = TaskServicesRest.Task(obj.id, title = obj.title)
        return jsonSerializationContext.serialize(task)
    }
}

/**
 * Serialization for Kost2DO etc.
 */
class Kost2DOSerializer : JsonSerializer<Kost2DO> {
    private class Kunde(val id: Int, val name: String?)
    private class Projekt(val id: Int, val name: String?, var kunde: Kunde? = null)
    private class Kost2(val id: Int, val description: String?, var projekt: Projekt? = null)

    @Synchronized
    override fun serialize(obj: Kost2DO?, type: Type, jsonSerializationContext: JsonSerializationContext): JsonElement? {
        if (obj == null) return null
        val kost2 = Kost2(obj.id, obj.description)
        if (obj.projekt != null) {
            val projektDao = Registry.instance.getEntry(ProjektDao::class.java)?.dao as ProjektDao
            val projektDO = projektDao.getById(obj.projektId)
            if (projektDO != null) {
                val projekt = Projekt(projektDO.id, projektDO.name)
                if (projektDO.kunde != null) {
                    val kundeDao = Registry.instance.getEntry(KundeDao::class.java)?.dao as KundeDao
                    val kundeDO = kundeDao.getById(projektDO.kundeId)
                    if (kundeDO != null) {
                        val kunde = Kunde(kundeDO.id, kundeDO.name)
                        projekt.kunde = kunde
                    }
                }
                kost2.projekt = projekt
            }
        }
        return jsonSerializationContext.serialize(kost2)
    }
}

class TenantDOSerializer : JsonSerializer<TenantDO> {
    private class Tenant(val id: Int, val name: String?)

    @Synchronized
    override fun serialize(obj: TenantDO?, type: Type, jsonSerializationContext: JsonSerializationContext): JsonElement? {
        if (obj == null) return null
        val tenant = Tenant(obj.id, obj.name)
        return jsonSerializationContext.serialize(tenant)
    }
}
