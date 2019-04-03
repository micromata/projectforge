package org.projectforge.rest.core

import org.projectforge.framework.persistence.api.BaseDao
import org.projectforge.framework.persistence.api.BaseSearchFilter
import org.projectforge.framework.persistence.api.ExtendedBaseDO
import org.projectforge.framework.persistence.user.api.ThreadLocalUserContext
import org.projectforge.rest.JsonUtils
import org.projectforge.rest.json.JsonCreator
import org.projectforge.ui.ValidationError
import java.util.*
import javax.ws.rs.core.Response

class RestHelper(
        /**
         * If not set, the user's time zone will be used (from [ThreadLocalUserContext]).
         */
        var timeZone: TimeZone? = null) {
    private var _jsonCreator: JsonCreator? = null

    private val adapterMap = mutableMapOf<Class<*>, Any>()

    /**
     * Late initialization needed, because especially in test cases in [Configuration] the [TenantService]
     * isn't available on start-up.
     */
    private fun getJsonCreator(): JsonCreator {
        if (_jsonCreator == null) {
            if (timeZone == null)
                timeZone = ThreadLocalUserContext.getTimeZone()
            _jsonCreator = JsonCreator(timeZone!!)
            adapterMap.forEach {
                _jsonCreator!!.add(it.key, it.value)
            }
        }
        return _jsonCreator!!
    }

    fun add(cls: Class<*>, typeAdapter: Any) {
        adapterMap.put(cls, typeAdapter)
    }

    fun <O : ExtendedBaseDO<Int>, B : BaseDao<O>, F : BaseSearchFilter>
            getList(dataObjectRest: AbstractDORest<O, B, F>, baseDao: BaseDao<O>?, filter: F)
            : ResultSet<Any> {
        val list = baseDao!!.getList(filter)
        val resultSet = ResultSet<Any>(dataObjectRest.filterList(list, filter), list.size)
        return resultSet
    }

    fun buildResponse(obj: Any): Response {
        val json = getJsonCreator().toJson(obj)
        return Response.ok(json).build()
    }

    fun buildResponse(obj: ExtendedBaseDO<Int>): Response {
        val json = getJsonCreator().toJson(obj)
        return Response.ok(json).build()
    }

    fun <O : ExtendedBaseDO<Int>, B : BaseDao<O>, F : BaseSearchFilter>
            saveOrUpdate(baseDao: BaseDao<O>?, obj: O, dataObjectRest: AbstractDORest<O, B, F>, validationErrorsList: List<ValidationError>?)
            : Response {
        if (validationErrorsList.isNullOrEmpty()) {
            val isNew = obj.id != null
            var id = baseDao!!.saveOrUpdate(obj) ?: obj.id
            dataObjectRest.afterSaveOrUpdate(obj)
            if (isNew)
                dataObjectRest.afterSave(obj)
            else
                dataObjectRest.afterUpdate(obj)
            val json = getJsonCreator().toJson(id)
            return Response.ok(json).build()
        }
        // Validation error occurred:
        val json = getJsonCreator().toJson(validationErrorsList)
        return Response.status(Response.Status.NOT_ACCEPTABLE).entity(json).build()
    }

    fun <O : ExtendedBaseDO<Int>>
            undelete(baseDao: BaseDao<O>?, obj: O, validationErrorsList: List<ValidationError>?)
            : Response {
        if (validationErrorsList.isNullOrEmpty()) {
            var id = baseDao!!.undelete(obj)
            val json = getJsonCreator().toJson(id)
            return Response.ok(json).build()
        }
        // Validation error occurred:
        val json = JsonUtils.toJson(validationErrorsList)
        return Response.status(Response.Status.NOT_ACCEPTABLE).entity(json).build()
    }

    fun <O : ExtendedBaseDO<Int>> markAsDeleted(baseDao: BaseDao<O>?, obj: O, validationErrorsList: List<ValidationError>?): Response {
        if (validationErrorsList.isNullOrEmpty()) {
            baseDao!!.markAsDeleted(obj)
            val json = getJsonCreator().toJson(obj)
            return Response.ok(json).build()
        }
        // Validation error occurred:
        val json = getJsonCreator().toJson(validationErrorsList)
        return Response.status(Response.Status.NOT_ACCEPTABLE).entity(json).build()
    }
}