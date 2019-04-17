package org.projectforge.rest.json

import com.google.gson.Gson
import com.google.gson.GsonBuilder
import com.google.gson.JsonObject
import com.google.gson.JsonPrimitive
import org.projectforge.business.fibu.kost.Kost2DO
import org.projectforge.business.task.TaskDO
import org.projectforge.business.teamcal.admin.model.TeamCalDO
import org.projectforge.framework.persistence.user.entities.PFUserDO
import org.projectforge.framework.persistence.user.entities.TenantDO
import org.projectforge.rest.calendar.TeamCalDOSerializer
import org.projectforge.rest.converter.DateTimeFormat
import org.projectforge.rest.converter.DateTimeTypeAdapter
import org.projectforge.rest.converter.DateTypeAdapter
import org.projectforge.ui.UIMultiSelect
import java.util.*

class JsonCreator {
    private val typeAdapterMap = HashMap<Class<*>, Any>()

    companion object {
        fun addJsonPrimitive(parent: JsonObject, property: String, value: Any?) {
            if (value == null)
                return
            when (value) {
                is String -> parent.add(property, JsonPrimitive(value))
                is Number -> parent.add(property, JsonPrimitive(value))
                is Boolean -> parent.add(property, JsonPrimitive(value))
                else -> parent.add(property, JsonPrimitive(value.toString()))
            }
        }
    }

    constructor() {
        add(java.sql.Date::class.java, DateTypeAdapter())
        add(java.util.Date::class.java, DateTimeTypeAdapter(DateTimeFormat.JS_DATE_TIME_MILLIS))
        add(java.time.LocalDate::class.java, LocalDateTypeAdapter())
        add(Kost2DO::class.java, Kost2DOSerializer())
        add(PFUserDO::class.java, PFUserDOSerializer())
        add(TaskDO::class.java, TaskDOSerializer())
        add(TenantDO::class.java, TenantDOSerializer())

        // Calendar serializers
        add(TeamCalDO::class.java, TeamCalDOSerializer())

        // UI
        add(UIMultiSelect::class.java, UIMultiSelectTypeSerializer())
    }

    fun add(cls: Class<*>, typeAdapter: Any) {
        typeAdapterMap[cls] = typeAdapter
    }

    fun toJson(obj: Any): String {
        return createGson().toJson(obj)
    }

    private fun createGson(): Gson {
        val builder = GsonBuilder()
        for ((key, value) in typeAdapterMap) {
            builder.registerTypeHierarchyAdapter(key, value)
        }
        return builder.create()
    }
}
