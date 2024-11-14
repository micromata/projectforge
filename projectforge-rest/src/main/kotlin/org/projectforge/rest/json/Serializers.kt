/////////////////////////////////////////////////////////////////////////////
//
// Project ProjectForge Community Edition
//         www.projectforge.org
//
// Copyright (C) 2001-2024 Micromata GmbH, Germany (www.micromata.com)
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
import org.projectforge.business.PfCaches
import org.projectforge.business.address.AddressbookDO
import org.projectforge.business.fibu.EmployeeDO
import org.projectforge.business.fibu.KundeDO
import org.projectforge.business.fibu.kost.Kost1DO
import org.projectforge.business.fibu.kost.Kost2DO
import org.projectforge.business.task.TaskDO
import org.projectforge.framework.persistence.user.entities.GroupDO
import org.projectforge.framework.persistence.user.entities.PFUserDO
import org.projectforge.rest.dto.*
import java.io.IOException


/**
 * Serialization for PFUserDO etc.
 */
class PFUserDOSerializer : StdSerializer<PFUserDO>(PFUserDO::class.java) {

    @Throws(IOException::class, JsonProcessingException::class)
    override fun serialize(value: PFUserDO?, jgen: JsonGenerator, provider: SerializerProvider) {
        val userDO = PfCaches.instance.getUserIfNotInitialized(value)
        if (userDO == null) {
            jgen.writeNull()
            return
        }
        val user = User(userDO.id, displayName = userDO.displayName, username = userDO.username)
        jgen.writeObject(user)
    }
}

/**
 * Serialization for GroupDO.
 */
class GroupDOSerializer : StdSerializer<GroupDO>(GroupDO::class.java) {
    @Throws(IOException::class, JsonProcessingException::class)
    override fun serialize(value: GroupDO?, jgen: JsonGenerator, provider: SerializerProvider) {
        val groupDO = PfCaches.instance.getGroupIfNotInitialized(value)
        if (groupDO == null) {
            jgen.writeNull()
            return
        }
        val group = Group(groupDO.id, displayName = groupDO.displayName, name = groupDO.name)
        jgen.writeObject(group)
    }
}

/**
 * Serialization for TaskDO.
 */
class TaskDOSerializer : StdSerializer<TaskDO>(TaskDO::class.java) {
    @Throws(IOException::class, JsonProcessingException::class)
    override fun serialize(value: TaskDO?, jgen: JsonGenerator, provider: SerializerProvider) {
        val taskDO = PfCaches.instance.getTaskIfNotInitialized(value)
        if (taskDO == null) {
            jgen.writeNull()
            return
        }
        val task = Task(taskDO.id, displayName = taskDO.displayName, title = taskDO.title)
        jgen.writeObject(task)
    }
}

/**
 * Serialization for Kost2DO.
 */
class Kost2DOSerializer : StdSerializer<Kost2DO>(Kost2DO::class.java) {
    //private class Kunde(val id: Int?, val name: String?)
    //private class Projekt(val id: Int?, val name: String?, var kunde: Kunde? = null)
    //private class Kost2(val id: Int?, val description: String?, var projekt: Projekt? = null)

    @Throws(IOException::class, JsonProcessingException::class)
    override fun serialize(value: Kost2DO?, jgen: JsonGenerator, provider: SerializerProvider) {
        val kost2DO = PfCaches.instance.getKost2IfNotInitialized(value)
        if (kost2DO == null) {
            jgen.writeNull()
            return
        }
        val kost2 = Kost2(kost2DO.id, displayName = kost2DO.displayName, description = kost2DO.description)
        val projektDO = PfCaches.instance.getProjektIfNotInitialized(kost2DO.projekt)
        if (projektDO != null) {
            val projekt = Project(projektDO.id, displayName = projektDO.displayName)
            val kundeDO = PfCaches.instance.getKundeIfNotInitialized(projektDO.kunde)
            if (kundeDO != null) {
                val kunde = Customer(kundeDO.nummer!!, displayName = kundeDO.displayName, name = kundeDO.name)
                projekt.customer = kunde
            }
            kost2.project = projekt
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
        val kost1DO = PfCaches.instance.getKost1IfNotInitialized(value)
        if (kost1DO == null) {
            jgen.writeNull()
            return
        }
        val kost1 = Kost1(
            kost1DO.id,
            nummernkreis = kost1DO.nummernkreis,
            bereich = kost1DO.bereich,
            teilbereich = kost1DO.teilbereich,
            endziffer = kost1DO.endziffer,
            description = kost1DO.description
        )
        jgen.writeObject(kost1)
    }
}

/**
 * Serialization for KundeDO
 */
class KundeDOSerializer : StdSerializer<KundeDO>(KundeDO::class.java) {

    @Throws(IOException::class, JsonProcessingException::class)
    override fun serialize(value: KundeDO?, jgen: JsonGenerator, provider: SerializerProvider) {
        val kundeDO = PfCaches.instance.getKundeIfNotInitialized(value)
        if (kundeDO == null) {
            jgen.writeNull()
            return
        }
        val kunde = Customer(kundeDO.nummer, displayName = kundeDO.displayName)
        jgen.writeObject(kunde)
    }
}

/**
 * Serialization for AddressbookDO
 */
class AddressbookDOSerializer : StdSerializer<AddressbookDO>(AddressbookDO::class.java) {

    @Throws(IOException::class, JsonProcessingException::class)
    override fun serialize(value: AddressbookDO?, jgen: JsonGenerator, provider: SerializerProvider) {
        val ab = PfCaches.instance.getAddressbookIfNotInitialized(value)
        if (ab == null) {
            jgen.writeNull()
            return
        }
        val addressbook = Addressbook(ab.id, displayName = ab.displayName)
        jgen.writeObject(addressbook)
    }
}

/**
 * Serialization for EmployeeDO
 */
class EmployeeDOSerializer : StdSerializer<EmployeeDO>(EmployeeDO::class.java) {

    @Throws(IOException::class, JsonProcessingException::class)
    override fun serialize(value: EmployeeDO?, jgen: JsonGenerator, provider: SerializerProvider) {
        val employeeDO = PfCaches.instance.getEmployeeIfNotInitialized(value)
        if (employeeDO == null) {
            jgen.writeNull()
            return
        }
        val employee = Employee(employeeDO.id, displayName = employeeDO.displayName)
        jgen.writeObject(employee)
    }
}
