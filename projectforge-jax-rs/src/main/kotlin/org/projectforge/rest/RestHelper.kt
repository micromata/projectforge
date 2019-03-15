package org.projectforge.rest

import org.apache.poi.ss.formula.functions.T
import org.projectforge.framework.persistence.api.BaseDao
import org.projectforge.framework.persistence.api.BaseSearchFilter
import org.projectforge.framework.persistence.api.ExtendedBaseDO
import java.util.*
import javax.ws.rs.core.Response

class RestHelper {
    companion object {
        fun getList(baseDao : BaseDao<out ExtendedBaseDO<Int>>?, filter: BaseSearchFilter) : Response {
            val list = baseDao!!.getList(filter)
            val json = JsonUtils.toJson(list)
            return Response.ok(json).build()
        }
    }
}