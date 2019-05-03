package org.projectforge.rest.json

import com.fasterxml.jackson.core.JsonGenerator
import com.fasterxml.jackson.core.JsonProcessingException
import com.fasterxml.jackson.databind.SerializerProvider
import com.fasterxml.jackson.databind.ser.std.StdSerializer
import org.projectforge.business.address.AddressbookDO
import org.projectforge.business.fibu.KundeDao
import org.projectforge.business.fibu.ProjektDao
import org.projectforge.business.fibu.kost.Kost2DO
import org.projectforge.business.task.TaskDO
import org.projectforge.framework.persistence.user.entities.PFUserDO
import org.projectforge.framework.persistence.user.entities.TenantDO
import org.projectforge.registry.Registry
import org.projectforge.rest.task.TaskServicesRest
import java.io.IOException


/**
 * Serialization for PFUserDO etc.
 */
class PFUserDOSerializer : StdSerializer<PFUserDO>(PFUserDO::class.java) {
    private class User(val id: Int, val username: String?, val fullname: String?)

    @Throws(IOException::class, JsonProcessingException::class)
    override fun serialize(value: PFUserDO?, jgen: JsonGenerator, provider: SerializerProvider) {
        if (value == null) {
            jgen.writeNull()
            return
        }
        val user = User(value.id, value.username, value.fullname)
        jgen.writeObject(user)
    }
}

/**
 * Serialization for TaskDO etc.
 */
class TaskDOSerializer : StdSerializer<TaskDO>(TaskDO::class.java) {
    @Throws(IOException::class, JsonProcessingException::class)
    override fun serialize(value: TaskDO?, jgen: JsonGenerator, provider: SerializerProvider) {
        if (value == null) {
            jgen.writeNull()
            return
        }
        val task = TaskServicesRest.Task(value.id, title = value.title)
        jgen.writeObject(task)
    }
}

/**
 * Serialization for Kost2DO etc.
 */
class Kost2DOSerializer : StdSerializer<Kost2DO>(Kost2DO::class.java) {
    private class Kunde(val id: Int, val name: String?)
    private class Projekt(val id: Int, val name: String?, var kunde: Kunde? = null)
    private class Kost2(val id: Int, val description: String?, var projekt: Projekt? = null)

    @Throws(IOException::class, JsonProcessingException::class)
    override fun serialize(value: Kost2DO?, jgen: JsonGenerator, provider: SerializerProvider) {
        if (value == null){
            jgen.writeNull()
            return
        }
        val kost2 = Kost2(value.id, value.description)
        if (value.projekt != null) {
            val projektDao = Registry.instance.getEntry(ProjektDao::class.java)?.dao as ProjektDao
            val projektDO = projektDao.getById(value.projektId)
            if (projektDO != null) {
                val projekt = Projekt(projektDO.id, projektDO.name)
                if (projektDO.kunde != null) {
                    val kundeDao = Registry.instance.getEntry(KundeDao::class.java)?.dao as KundeDao
                    val kundeDO = kundeDao.getById(projektDO.kundeId)
                    if (kundeDO != null) {
                        val kunde = Kunde(kundeDO.id!!, kundeDO.name)
                        projekt.kunde = kunde
                    }
                }
                kost2.projekt = projekt
            }
        }
        jgen.writeObject(kost2)
    }
}

class TenantDOSerializer : StdSerializer<TenantDO>(TenantDO::class.java) {
    private class Tenant(val id: Int, val name: String?)

    @Throws(IOException::class, JsonProcessingException::class)
    override fun serialize(value: TenantDO?, jgen: JsonGenerator, provider: SerializerProvider) {
        if (value == null) {
            jgen.writeNull()
            return
        }
        val tenant = Tenant(value.id, value.name)
        jgen.writeObject(tenant)
    }
}

/**
 * Serialization for AddressbookDO
 */
class AddressbookDOSerializer : StdSerializer<AddressbookDO>(AddressbookDO::class.java) {
    private class Addressbook(val id: Int, val title: String?)

    @Throws(IOException::class, JsonProcessingException::class)
    override fun serialize(value: AddressbookDO?, jgen: JsonGenerator, provider: SerializerProvider) {
        if (value == null) {
            jgen.writeNull()
            return
        }
        val addressbook = Addressbook(value.id, value.title)
        jgen.writeObject(addressbook)
    }
}
