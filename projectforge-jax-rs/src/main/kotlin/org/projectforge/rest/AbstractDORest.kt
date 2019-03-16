package org.projectforge.rest

import org.bouncycastle.asn1.x509.X509ObjectIdentifiers
import org.bouncycastle.asn1.x509.X509ObjectIdentifiers.id
import org.projectforge.framework.access.AccessChecker
import org.projectforge.framework.persistence.api.BaseDao
import org.projectforge.framework.persistence.api.BaseSearchFilter
import org.projectforge.framework.persistence.api.ExtendedBaseDO
import org.projectforge.framework.persistence.user.entities.TenantDO
import org.projectforge.model.rest.RestPaths
import org.projectforge.rest.ui.Layout
import org.projectforge.ui.UILayout
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Controller
import javax.ws.rs.*
import javax.ws.rs.core.MediaType
import javax.ws.rs.core.Response

@Controller
abstract class AbstractDORest<O : ExtendedBaseDO<Int>, B : BaseDao<O>> {
    private val log = org.slf4j.LoggerFactory.getLogger(AbstractDORest::class.java)

    private data class EditLayoutData(val data: Any?, val ui: UILayout?)

    @Autowired
    open var accessChecker: AccessChecker? = null

    abstract fun getBaseDao(): BaseDao<O>

    abstract fun newBaseDO(): O

    @POST
    @Path(RestPaths.LIST)
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    fun getList(filter: BaseSearchFilter): Response {
        var list = RestHelper.getList(getBaseDao(), filter)
        list.forEach { processItemBeforeExport(it) }
        return RestHelper.buildResponse(list)
    }

    @GET
    @Path("list-test")
    @Produces(MediaType.APPLICATION_JSON)
    fun getListTest(@QueryParam("search") search: String?): Response {
        val filter: BaseSearchFilter = BaseSearchFilter()
        filter.searchString = search
        var list = RestHelper.getList(getBaseDao(), filter)
        list.forEach { processItemBeforeExport(it) }
        return RestHelper.buildResponse(list)
    }

    /**
     * Gets the item including the layout data at default.
     * @param id Id of the item to get or null, for new items (null  will be returned)
     * @param layout If given, layout definitions will be returned, otherwise only the item will be returned. The
     * layout will be also included if the id is not given.
     */
    @GET
    @Path("{id}")
    @Produces(MediaType.APPLICATION_JSON)
    fun getItem(@PathParam("id") id: Int): Response {
        val item = getBaseDao().getById(id)
        processItemBeforeExport(item)
        return RestHelper.buildResponse(item)
    }

    /**
     * Gets the item including the layout data at default.
     * @param id Id of the item to get or null, for new items (null  will be returned)
     * @param layout If given, layout definitions will be returned, otherwise only the item will be returned. The
     * layout will be also included if the id is not given.
     */
    @GET
    @Path("edit")
    @Produces(MediaType.APPLICATION_JSON)
    fun getItemAndLayout(@QueryParam("id") id: Int?): Response {
        val item : O
        if (id != null) {
            item = getBaseDao().getById(id)
            processItemBeforeExport(item)
        } else item = newBaseDO()
        val result = EditLayoutData(item, Layout.getEditLayout(item))
        return RestHelper.buildResponse(result)
    }

    open protected fun processItemBeforeExport(item: O) {
        val tenantId = item.tenantId
        item.tenant = TenantDO()
        item.tenant.pk = tenantId
    }

    @PUT
    @Path(RestPaths.SAVE_OR_UDATE)
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    fun saveOrUpdate(obj: O): Response {
        return RestHelper.saveOrUpdate(getBaseDao(), obj)
    }

    @PUT
    @Path(RestPaths.UNDELETE)
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    fun undelete(obj: O): Response {
        return RestHelper.undelete(getBaseDao(), obj)
    }

    @DELETE
    @Path(RestPaths.MARK_AS_DELETED)
    @Consumes(MediaType.APPLICATION_JSON)
    fun markAsDeleted(obj: O): Response {
        return RestHelper.markAsDeleted(getBaseDao(), obj)
    }
}