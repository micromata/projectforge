package org.projectforge.rest.json

import com.google.gson.*
import org.projectforge.business.fibu.KundeDao
import org.projectforge.business.fibu.ProjektDao
import org.projectforge.business.fibu.kost.Kost2DO
import org.projectforge.business.task.TaskDO
import org.projectforge.business.tasktree.TaskTreeHelper
import org.projectforge.framework.persistence.user.entities.PFUserDO
import org.projectforge.framework.persistence.user.entities.TenantDO
import org.projectforge.registry.Registry
import org.projectforge.rest.TaskServicesRest
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

/**
 * Serialization for TaskDO etc.
 */
class TaskDOSerializer : JsonSerializer<TaskDO> {
    @Synchronized
    override fun serialize(obj: TaskDO?, type: Type, jsonSerializationContext: JsonSerializationContext): JsonElement? {
        if (obj == null) return null
        val task = TaskServicesRest.Task(obj.id, title=obj.title)
        val list = TaskTreeHelper.getTaskTree().getPathToRoot(obj.parentTaskId)
        val pathList = mutableListOf<TaskServicesRest.Task>()
        list?.forEach {
            val ancestor = TaskServicesRest.Task(it.task.id, title = it.task.title)
            pathList.add(ancestor)
        }
        task.path = pathList
        val result = jsonSerializationContext.serialize(task)
        return result
    }
}

/**
 * Serialization for Kost2DO etc.
 */
class Kost2DOSerializer : JsonSerializer<Kost2DO> {
    @Synchronized
    override fun serialize(obj: Kost2DO?, type: Type, jsonSerializationContext: JsonSerializationContext): JsonElement? {
        if (obj == null) return null
        val result = JsonObject()
        result.add("id", JsonPrimitive(obj.id))
        result.add("formattedNumber", JsonPrimitive(obj.formattedNumber))
        if (obj.description != null)
            result.add("description", JsonPrimitive(obj.description))
        if (obj.projekt != null) {
            val projektDao = Registry.instance.getEntry(ProjektDao::class.java)?.dao as ProjektDao
            val projectDO = projektDao?.getById(obj.projektId)
            if (projectDO != null) {
                val project = JsonObject()
                project.add("id", JsonPrimitive(projectDO.id))
                project.add("name", JsonPrimitive(projectDO.name))
                if (projectDO.kunde != null) {
                    val customerDao = Registry.instance.getEntry(KundeDao::class.java)?.dao as KundeDao
                    val customerDO = customerDao?.getById(projectDO.kundeId)
                    if (customerDO != null) {
                        val customer = JsonObject()
                        customer.add("id", JsonPrimitive(customerDO.id))
                        customer.add("name", JsonPrimitive(customerDO.name))
                        project.add("customer", customer)
                    }
                }
                result.add("project", project)
            }
        }
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
