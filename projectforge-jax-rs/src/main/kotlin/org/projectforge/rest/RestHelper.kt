package org.projectforge.rest

import org.projectforge.framework.persistence.api.BaseDao
import org.projectforge.framework.persistence.api.BaseSearchFilter
import org.projectforge.framework.persistence.api.ExtendedBaseDO
import javax.ws.rs.core.Response

class RestHelper {
    companion object {
        fun <O : ExtendedBaseDO<Int>> getList(baseDao: BaseDao<O>?, filter: BaseSearchFilter): List<O> {
            val list = baseDao!!.getList(filter)
            list.forEach { it.tenant = null }
            return list
        }

        fun buildResponse(obj : Any): Response {
            val json = JsonUtils.toJson(obj)
            return Response.ok(json).build()
        }

        fun buildResponse(obj : ExtendedBaseDO<Int>): Response {
            obj.tenant = null
            val json = JsonUtils.toJson(obj)
            return Response.ok(json).build()
        }
    }
}