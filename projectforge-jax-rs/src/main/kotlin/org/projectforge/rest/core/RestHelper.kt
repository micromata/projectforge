package org.projectforge.rest.core

import org.projectforge.framework.persistence.api.BaseDao
import org.projectforge.framework.persistence.api.BaseSearchFilter
import org.projectforge.framework.persistence.api.ExtendedBaseDO
import org.projectforge.rest.JsonUtils
import org.projectforge.rest.json.JsonCreator
import org.projectforge.ui.ValidationError
import java.util.*
import javax.ws.rs.core.Response

class RestHelper(val timeZone: TimeZone = TimeZone.getTimeZone("UTC")) {

    private val jsonCreator = JsonCreator(timeZone)

    fun add(cls: Class<*>, typeAdapter: Any) {
        jsonCreator.add(cls, typeAdapter)
    }

    fun <O : ExtendedBaseDO<Int>> getList(baseDao: BaseDao<O>?, filter: BaseSearchFilter): List<O> {
        val list = baseDao!!.getList(filter)
        return list
    }

    fun buildResponse(obj: Any): Response {
        val json = jsonCreator.toJson(obj)
        return Response.ok(json).build()
    }

    fun buildResponse(obj: ExtendedBaseDO<Int>): Response {
        val json = jsonCreator.toJson(obj)
        return Response.ok(json).build()
    }

    fun <O : ExtendedBaseDO<Int>, B : BaseDao<O>, F : BaseSearchFilter> saveOrUpdate(baseDao: BaseDao<O>?, obj: O, dataObjectRest: AbstractDORest<O, B, F>, validationErrorsList: List<ValidationError>?): Response {
        if (validationErrorsList.isNullOrEmpty()) {
            val isNew = obj.id != null
            var id = baseDao!!.saveOrUpdate(obj) ?: obj.id
            dataObjectRest.afterSaveOrUpdate(obj)
            if (isNew)
                dataObjectRest.afterSave(obj)
            else
                dataObjectRest.afterUpdate(obj)
            val json = jsonCreator.toJson(id)
            return Response.ok(json).build()
        }
        // Validation error occurred:
        val json = jsonCreator.toJson(validationErrorsList)
        return Response.status(Response.Status.NOT_ACCEPTABLE).entity(json).build()
    }

    fun <O : ExtendedBaseDO<Int>> undelete(baseDao: BaseDao<O>?, obj: O, validationErrorsList: List<ValidationError>?): Response {
        if (validationErrorsList.isNullOrEmpty()) {
            var id = baseDao!!.undelete(obj)
            val json = jsonCreator.toJson(id)
            return Response.ok(json).build()
        }
        // Validation error occurred:
        val json = JsonUtils.toJson(validationErrorsList)
        return Response.status(Response.Status.NOT_ACCEPTABLE).entity(json).build()
    }

    fun <O : ExtendedBaseDO<Int>> markAsDeleted(baseDao: BaseDao<O>?, obj: O, validationErrorsList: List<ValidationError>?): Response {
        if (validationErrorsList.isNullOrEmpty()) {
            baseDao!!.markAsDeleted(obj)
            val json = jsonCreator.toJson(obj)
            return Response.ok(json).build()
        }
        // Validation error occurred:
        val json = jsonCreator.toJson(validationErrorsList)
        return Response.status(Response.Status.NOT_ACCEPTABLE).entity(json).build()
    }
}