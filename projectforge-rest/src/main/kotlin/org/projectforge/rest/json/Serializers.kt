/////////////////////////////////////////////////////////////////////////////
//
// Project ProjectForge Community Edition
//         www.projectforge.org
//
// Copyright (C) 2001-2019 Micromata GmbH, Germany (www.micromata.com)
//
// ProjectForge is dual-licensed.
//
// This community edition is free software; you can redistribute it and/or
// modify it under the terms of the GNU General Public License as published
// by the Free Software Foundation; version 3 of the License.
//
// This community edition is distributed in the hope that it will be useful,
// but WITHOUT ANY WARRANTY; without even the implied warranty of
// MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU General
// Public License for more details.
//
// You should have received a copy of the GNU General Public License along
// with this program; if not, see http://www.gnu.org/licenses/.
//
/////////////////////////////////////////////////////////////////////////////

package org.projectforge.rest.json

import com.fasterxml.jackson.core.JsonGenerator
import com.fasterxml.jackson.core.JsonProcessingException
import com.fasterxml.jackson.databind.SerializerProvider
import com.fasterxml.jackson.databind.ser.std.StdSerializer
import org.projectforge.business.address.AddressbookDO
import org.projectforge.business.fibu.EmployeeDO
import org.projectforge.business.fibu.KundeDO
import org.projectforge.business.fibu.KundeDao
import org.projectforge.business.fibu.ProjektDao
import org.projectforge.business.fibu.kost.Kost1DO
import org.projectforge.business.fibu.kost.Kost2DO
import org.projectforge.business.task.TaskDO
import org.projectforge.framework.persistence.user.entities.GroupDO
import org.projectforge.framework.persistence.user.entities.PFUserDO
import org.projectforge.framework.persistence.user.entities.TenantDO
import org.projectforge.registry.Registry
import org.projectforge.rest.dto.Kost1
import org.projectforge.rest.task.TaskServicesRest
import java.io.IOException


/**
 * Serialization for PFUserDO etc.
 */
class PFUserDOSerializer : StdSerializer<PFUserDO>(PFUserDO::class.java) {
    private class User(val id: Int?, val username: String?, val fullname: String?)

    @Throws(IOException::class, JsonProcessingException::class)
    override fun serialize(value: PFUserDO?, jgen: JsonGenerator, provider: SerializerProvider) {
        if (value == null) {
            jgen.writeNull()
            return
        }
        val user = User(value.id, value.username, value.getFullname())
        jgen.writeObject(user)
    }
}

/**
 * Serialization for GroupDO.
 */
class GroupDOSerializer : StdSerializer<GroupDO>(GroupDO::class.java) {
    private class Group(val id: Int?, val name: String?)

    @Throws(IOException::class, JsonProcessingException::class)
    override fun serialize(value: GroupDO?, jgen: JsonGenerator, provider: SerializerProvider) {
        if (value == null) {
            jgen.writeNull()
            return
        }
        val group = Group(value.id, name = value.name)
        jgen.writeObject(group)
    }
}

/**
 * Serialization for TaskDO.
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
 * Serialization for Kost2DO.
 */
class Kost2DOSerializer : StdSerializer<Kost2DO>(Kost2DO::class.java) {
    private class Kunde(val id: Int?, val name: String?)
    private class Projekt(val id: Int?, val name: String?, var kunde: Kunde? = null)
    private class Kost2(val id: Int?, val description: String?, var projekt: Projekt? = null)

    @Throws(IOException::class, JsonProcessingException::class)
    override fun serialize(value: Kost2DO?, jgen: JsonGenerator, provider: SerializerProvider) {
        if (value == null) {
            jgen.writeNull()
            return
        }
        val kost2 = Kost2(value.id, value.description)
        if (value.projekt != null) {
            val projektDao = Registry.instance.getEntry(ProjektDao::class.java)?.dao as ProjektDao
            val projektDO = projektDao.internalGetById(value.projektId)
            if (projektDO != null) {
                val projekt = Projekt(projektDO.id, projektDO.name)
                if (projektDO.kunde != null) {
                    val kundeDao = Registry.instance.getEntry(KundeDao::class.java)?.dao as KundeDao
                    val kundeDO = kundeDao.internalGetById(projektDO.kundeId)
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

/**
 * Serialization for Kost1DO.
 */
class Kost1DOSerializer : StdSerializer<Kost1DO>(Kost1DO::class.java) {
    @Throws(IOException::class, JsonProcessingException::class)
    override fun serialize(value: Kost1DO?, jgen: JsonGenerator, provider: SerializerProvider) {
        if (value == null) {
            jgen.writeNull()
            return
        }
        val kost1 = Kost1(value.id, nummernkreis = value.nummernkreis, bereich = value.bereich, teilbereich = value.teilbereich, endziffer = value.endziffer,
                description = value.description, formattedNumber = value.formattedNumber)
        jgen.writeObject(kost1)
    }
}

/**
 * Serialization for KundeDO
 */
class KundeDOSerializer : StdSerializer<KundeDO>(KundeDO::class.java) {
    private class Kunde(val id: Int?, val name: String?)

    @Throws(IOException::class, JsonProcessingException::class)
    override fun serialize(value: KundeDO?, jgen: JsonGenerator, provider: SerializerProvider) {
        if (value == null) {
            jgen.writeNull()
            return
        }
        val kunde = Kunde(value.id, value.name)
        jgen.writeObject(kunde)
    }
}

class TenantDOSerializer : StdSerializer<TenantDO>(TenantDO::class.java) {
    private class Tenant(val id: Int?, val name: String?)

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
    private class Addressbook(val id: Int?, val title: String?)

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

/**
 * Serialization for EmployeeDO
 */
class EmployeeDOSerializer : StdSerializer<EmployeeDO>(EmployeeDO::class.java) {
    private class Employee(val id: Int?, val username: String?, val fullname: String?)

    @Throws(IOException::class, JsonProcessingException::class)
    override fun serialize(value: EmployeeDO?, jgen: JsonGenerator, provider: SerializerProvider) {
        if (value == null) {
            jgen.writeNull()
            return
        }
        val employee = Employee(value.id, value.user?.username, value.user?.getFullname())
        jgen.writeObject(employee)
    }
}
