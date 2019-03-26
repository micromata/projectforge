package org.projectforge.rest.core

import org.projectforge.framework.persistence.api.BaseDao
import org.projectforge.framework.persistence.api.BaseSearchFilter
import org.projectforge.framework.persistence.api.ExtendedBaseDO
import org.projectforge.rest.JsonUtils
import org.projectforge.rest.json.JsonCreator
import org.projectforge.ui.ValidationError
import javax.ws.rs.core.Response

class RestHelper {
    companion object {
        fun <O : ExtendedBaseDO<Int>> getList(baseDao: BaseDao<O>?, filter: BaseSearchFilter): List<O> {
            val list = baseDao!!.getList(filter)
            return list
        }

        fun buildResponse(obj: Any): Response {
            val json = JsonCreator.toJson(obj)
            return Response.ok(json).build()
        }

        fun buildResponse(obj: ExtendedBaseDO<Int>): Response {
            val json = JsonCreator.toJson(obj)
            return Response.ok(json).build()
        }

        fun <O : ExtendedBaseDO<Int>> saveOrUpdate(baseDao: BaseDao<O>?, obj: O, validationErrorsList: List<ValidationError>?): Response {
            if (validationErrorsList.isNullOrEmpty()) {
                var id = baseDao!!.saveOrUpdate(obj) ?: obj.id
                val json = JsonCreator.toJson(id)
                return Response.ok(json).build()
            }
            // Validation error occurred:
            val json = JsonCreator.toJson(validationErrorsList)
            return Response.status(Response.Status.NOT_ACCEPTABLE).entity(json).build()
        }

        fun <O : ExtendedBaseDO<Int>> undelete(baseDao: BaseDao<O>?, obj: O, validationErrorsList: List<ValidationError>?): Response {
            if (validationErrorsList.isNullOrEmpty()) {
                var id = baseDao!!.undelete(obj)
                val json = JsonCreator.toJson(id)
                return Response.ok(json).build()
            }
            // Validation error occurred:
            val json = JsonUtils.toJson(validationErrorsList)
            return Response.status(Response.Status.NOT_ACCEPTABLE).entity(json).build()
        }

        fun <O : ExtendedBaseDO<Int>> markAsDeleted(baseDao: BaseDao<O>?, obj: O, validationErrorsList: List<ValidationError>?): Response {
            if (validationErrorsList.isNullOrEmpty()) {
                baseDao!!.markAsDeleted(obj)
                val json = JsonCreator.toJson(obj)
                return Response.ok(json).build()
            }
            // Validation error occurred:
            val json = JsonCreator.toJson(validationErrorsList)
            return Response.status(Response.Status.NOT_ACCEPTABLE).entity(json).build()
        }
    }
}