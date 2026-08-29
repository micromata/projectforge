/////////////////////////////////////////////////////////////////////////////
//
// Project ProjectForge Community Edition
//         www.projectforge.org
//
// Copyright (C) 2001-2026 Micromata GmbH, Germany (www.micromata.com)
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

package org.projectforge.rest.core

import jakarta.servlet.http.HttpServletRequest
import mu.KotlinLogging
import org.projectforge.common.i18n.UserException
import org.projectforge.framework.access.OperationType
import org.projectforge.framework.i18n.translateMsg
import org.projectforge.framework.persistence.api.BaseDao
import org.projectforge.framework.persistence.api.ExtendedBaseDO
import org.projectforge.framework.persistence.api.MagicFilter
import org.projectforge.framework.persistence.api.MagicFilterProcessor
import org.projectforge.rest.dto.PostData
import org.projectforge.rest.multiselect.MultiSelectionSupport
import org.projectforge.ui.ResponseAction
import org.projectforge.ui.ValidationError
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity

private val log = KotlinLogging.logger {}

fun <O : ExtendedBaseDO<Long>, DTO : Any, B : BaseDao<O>>
        getList(
    request: HttpServletRequest,
    pagesRest: AbstractEntityRest<O, DTO, B>,
    baseDao: BaseDao<O>,
    magicFilter: MagicFilter
)
        : ResultSet<O> {
    if (pagesRest.isMultiSelectionMode(request, magicFilter)) {
        val entityIds = MultiSelectionSupport.getRegisteredEntityIds(request, pagesRest::class.java)
        val selectedEntityIds =
            MultiSelectionSupport.getRegisteredSelectedEntityIds(request, pagesRest::class.java) ?: listOf()
        val list = pagesRest.getListByIds(entityIds)
        return ResultSet(list, null, selectedEntityIds = selectedEntityIds, magicFilter = magicFilter)
    }
    val list = getObjectList(pagesRest, baseDao, magicFilter)
    val resultSet = ResultSet(pagesRest.filterList(list, magicFilter), null, list.size, magicFilter = magicFilter)
    return resultSet
}

fun <O : ExtendedBaseDO<Long>, DTO : Any, B : BaseDao<O>>
        getObjectList(
    pagesRest: AbstractEntityRest<O, DTO, B>,
    baseDao: BaseDao<O>,
    magicFilter: MagicFilter
)
        : MutableList<O> {
    val (queryFilter, customResultFilters) = buildQueryFilter(pagesRest, baseDao, magicFilter)
    return baseDao.select(queryFilter, customResultFilters).toMutableList()
}

/**
 * Builds the [QueryFilter] from a [MagicFilter] exactly as [getObjectList] does, returning it together with the
 * custom result filters. Shared by [getObjectList] (non-paged `POST list`) and [getListPage] (server-side
 * paging) so the two paths build the identical query — the invariant that lets a paged result be the same rows
 * in the same order as the whole `POST list` result.
 */
private fun <O : ExtendedBaseDO<Long>, DTO : Any, B : BaseDao<O>> buildQueryFilter(
    pagesRest: AbstractEntityRest<O, DTO, B>,
    baseDao: BaseDao<O>,
    magicFilter: MagicFilter,
): Pair<org.projectforge.framework.persistence.api.QueryFilter, List<org.projectforge.framework.persistence.api.impl.CustomResultFilter<O>>?> {
    magicFilter.sortAndLimitMaxRowsWhileSelect = true
    val queryFilter = baseDao.createQueryFilter()
    val customResultFilters = pagesRest.preProcessMagicFilter(queryFilter, magicFilter)
    // Offered for every entity with attachment support (see LayoutListFilterUtils), so it is applied
    // here rather than in each pages rest class.
    AttachmentsFilterSupport.preProcessMagicFilter(queryFilter, magicFilter)
    magicFilter.sortProperties = magicFilter.sortProperties.distinctBy { it.property }.toMutableList()
    MagicFilterProcessor.doIt(baseDao.doClass, magicFilter, queryFilter)
    pagesRest.postProcessMagicFilter(queryFilter, magicFilter)
    // A computed column is no database column; DBQueryBuilderByCriteria.addOrder would swallow it and ship a
    // query with no ORDER BY at all. Drop the computed sort properties from the QUERY here, once for every
    // entity — never from magicFilter.sortProperties, which filterList/sortIds still read to sort by them.
    queryFilter.sortProperties.removeIf { pagesRest.computedSortProperties.containsKey(it.property) }
    return queryFilter to customResultFilters
}

/**
 * Serves one page of a server-side paged list (Stage 2 of `MIGRATION-list-paging.md`).
 *
 * The whole ordered, access-checked id list is materialized once per (session, filter) and cached
 * ([ListPageCache]); each page is then a slice of it, loaded with [AbstractEntityRest.getListByIds]. So the
 * expensive per-row work of the pipeline — DTO mapping, currency formatting, Jackson — runs on 50 rows, not on
 * every matching row, while the paged rows stay identical to the ones `POST list` would return (same query via
 * [buildQueryFilter], same sort via [AbstractEntityRest.sortIds]).
 *
 * Correctness never rests on the cache: the served page always goes through `getListByIds` →
 * `BaseDao.select(ids)` → per-row access check, so a stale id list (a row deleted or hidden since it was built)
 * can at worst yield a short page, never a forbidden or wrong row.
 *
 * Caveat: [AbstractEntityRest.getListByIds] loads by `IN (…)`, which ignores `queryFilter.entityGraphName`, so a
 * page of 50 rows may N+1 — acceptable at a page's size. Do not enable paging for a page that overrides
 * `getListByIds` with id semantics of its own (e.g. `AddressCampaignValuePagesRest`'s synthetic negative ids).
 *
 * @param offset Index of the first row of the requested page within the whole result.
 * @param limit Page size.
 * @param refresh If true, the cached id list is ignored and rebuilt (used right after the client's own write).
 */
fun <O : ExtendedBaseDO<Long>, DTO : Any, B : BaseDao<O>>
        getListPage(
    request: HttpServletRequest,
    pagesRest: AbstractEntityRest<O, DTO, B>,
    baseDao: BaseDao<O>,
    listPageCache: ListPageCache,
    magicFilter: MagicFilter,
    offset: Int,
    limit: Int,
    refresh: Boolean,
)
        : ResultSet<O> {
    if (pagesRest.isMultiSelectionMode(request, magicFilter)) {
        // The registered ids already are the ordered id list; slice it directly, no query and no cache.
        val entityIds = MultiSelectionSupport.getRegisteredEntityIds(request, pagesRest::class.java)?.toList() ?: listOf()
        val selectedEntityIds =
            MultiSelectionSupport.getRegisteredSelectedEntityIds(request, pagesRest::class.java) ?: listOf()
        val pageIds = entityIds.drop(offset).take(limit)
        val page = restorePageOrder(pageIds, pagesRest.getListByIds(pageIds))
        return ResultSet(
            page, null, totalSize = entityIds.size, selectedEntityIds = selectedEntityIds, magicFilter = magicFilter,
            offset = offset, limit = limit, totalSizeExact = true,
        )
    }
    // Fingerprint the incoming filter (before buildQueryFilter mutates it): page 1 and page 2 of the same
    // filter share it, so they reuse one materialized id list.
    val fingerprint = magicFilter.resultFingerprint
    val cached = if (refresh) null else listPageCache.get(request, pagesRest.category, baseDao.doClass, fingerprint)
    val idList = cached ?: run {
        val (queryFilter, customResultFilters) = buildQueryFilter(pagesRest, baseDao, magicFilter)
        val idResult = baseDao.selectIds(queryFilter, customResultFilters)
        val sortedIds = pagesRest.sortIds(idResult.ids, magicFilter)
        listPageCache.put(request, pagesRest.category, baseDao.doClass, fingerprint, sortedIds, idResult.truncated)
    }
    val allIds = idList.ids
    val pageIds = if (offset >= allIds.size) {
        listOf()
    } else {
        allIds.copyOfRange(offset, minOf(offset + limit, allIds.size)).toList()
    }
    val page = restorePageOrder(pageIds, pagesRest.getListByIds(pageIds))
    val resultSet = ResultSet(
        page, null, totalSize = allIds.size, magicFilter = magicFilter,
        offset = offset, limit = limit, totalSizeExact = !idList.truncated,
    )
    // Whole-result statistics are a property of the id list, not of the page: compute them once and cache them
    // with the ids (invalidated together by the change counter), so paging through the same filter does not
    // recompute them on every flip. For a list whose aggregate reloads the whole result set from the database
    // (all invoices plus the previous-year set, all matching time sheets), that recompute was a full reload per
    // page. `aggregate` may legitimately return null (no statistics); recomputing null costs nothing.
    resultSet.statistics = idList.statistics ?: pagesRest.aggregate(allIds, magicFilter)?.also { idList.statistics = it }
    return resultSet
}

/**
 * Restores the order of [pageIds] on the objects [AbstractEntityRest.getListByIds] returned: `select(idList)`
 * loads by `IN (…)`, whose row order is arbitrary. A row whose id is gone (deleted since the list was built)
 * is dropped — a short page, not an error.
 */
private fun <O : ExtendedBaseDO<Long>> restorePageOrder(pageIds: List<*>, loaded: List<O>): List<O> {
    val byId = loaded.associateBy { it.id }
    return pageIds.mapNotNull { byId[it as? Long] }
}

fun <O : ExtendedBaseDO<Long>, DTO : Any, B : BaseDao<O>>
        saveOrUpdate(
    request: HttpServletRequest,
    baseDao: BaseDao<O>,
    obj: O,
    postData: PostData<DTO>,
    pagesRest: AbstractEntityRest<O, DTO, B>,
    validationErrorsList: List<ValidationError>?
)
        : ResponseEntity<ResponseAction> {

    try {
        if (!validationErrorsList.isNullOrEmpty()) {
            // Validation error occurred:
            return ResponseEntity(ResponseAction(validationErrors = validationErrorsList), HttpStatus.NOT_ACCEPTABLE)
        }
        val isNew =
            obj.id == null || obj.created == null // obj.created is needed for KundeDO (id isn't null for inserting new customers).
        pagesRest.onBeforeSaveOrUpdate(request, obj, postData)
        if (isNew) {
            pagesRest.onBeforeSave(request, obj, postData)
        } else {
            pagesRest.onBeforeUpdate(request, obj, postData)
        }
        pagesRest.onBeforeDatabaseAction(
            request,
            obj,
            postData,
            if (obj.id != null) OperationType.UPDATE else OperationType.INSERT
        )
        baseDao.insertOrUpdate(obj) ?: obj.id
        pagesRest.onAfterSaveOrUpdate(request, obj, postData)
        if (isNew) {
            return ResponseEntity(pagesRest.onAfterSave(request, obj, postData), HttpStatus.OK)
        } else {
            return ResponseEntity(pagesRest.onAfterUpdate(request, obj, postData), HttpStatus.OK)
        }
    } catch (ex: Exception) {
        return handleException("Error while trying to save/update object '${obj::class.java}' with id #${obj.id}", ex)
    }
}

fun <O : ExtendedBaseDO<Long>, DTO : Any, B : BaseDao<O>>
        undelete(
    request: HttpServletRequest,
    baseDao: BaseDao<O>,
    obj: O,
    postData: PostData<DTO>,
    pagesRest: AbstractEntityRest<O, DTO, B>,
    validationErrorsList: List<ValidationError>?
)
        : ResponseEntity<ResponseAction> {
    try {
        if (validationErrorsList.isNullOrEmpty()) {
            pagesRest.onBeforeDatabaseAction(request, obj, postData, OperationType.UNDELETE)
            pagesRest.onBeforeUndelete(request, obj, postData)
            baseDao.undelete(obj)
            return ResponseEntity(pagesRest.onAfterUndelete(request, obj, postData), HttpStatus.OK)
        }
        // Validation error occurred:
        return ResponseEntity(ResponseAction(validationErrors = validationErrorsList), HttpStatus.NOT_ACCEPTABLE)
    } catch (ex: Exception) {
        return handleException("Error while trying to undelete object '${obj::class.java}' with id #${obj.id}", ex)
    }
}

fun <O : ExtendedBaseDO<Long>, DTO : Any, B : BaseDao<O>>
        markAsDeleted(
    request: HttpServletRequest,
    baseDao: BaseDao<O>,
    obj: O,
    postData: PostData<DTO>,
    pagesRest: AbstractEntityRest<O, DTO, B>,
    validationErrorsList: List<ValidationError>?
)
        : ResponseEntity<ResponseAction> {
    try {
        if (validationErrorsList.isNullOrEmpty()) {
            pagesRest.onBeforeDatabaseAction(request, obj, postData, OperationType.DELETE)
            pagesRest.onBeforeMarkAsDeleted(request, obj, postData)
            baseDao.markAsDeleted(obj)
            return ResponseEntity(pagesRest.onAfterMarkAsDeleted(request, obj, postData), HttpStatus.OK)
        }
        // Validation error occurred:
        return ResponseEntity(ResponseAction(validationErrors = validationErrorsList), HttpStatus.NOT_ACCEPTABLE)
    } catch (ex: Exception) {
        return handleException(
            "Error while trying to mark object '${obj::class.java}' as deleted with id #${obj.id}",
            ex
        )
    }
}

fun <O : ExtendedBaseDO<Long>, DTO : Any, B : BaseDao<O>>
        forceDelete(
    request: HttpServletRequest,
    baseDao: BaseDao<O>,
    obj: O,
    postData: PostData<DTO>,
    pagesRest: AbstractEntityRest<O, DTO, B>
)
        : ResponseEntity<ResponseAction> {
    try {
        pagesRest.onBeforeDatabaseAction(request, obj, postData, OperationType.DELETE)
        pagesRest.onBeforeDelete(request, obj, postData)
        baseDao.forceDelete(obj)
        return ResponseEntity(pagesRest.onAfterDelete(request, obj, postData), HttpStatus.OK)
    } catch (ex: Exception) {
        return handleException(
            "Error while trying to forced deleting object '${obj::class.java}' with id #${obj.id}",
            ex
        )
    }
}

fun <O : ExtendedBaseDO<Long>, DTO : Any, B : BaseDao<O>>
        delete(
    request: HttpServletRequest,
    baseDao: BaseDao<O>,
    obj: O,
    postData: PostData<DTO>,
    pagesRest: AbstractEntityRest<O, DTO, B>,
    validationErrorsList: List<ValidationError>?
)
        : ResponseEntity<ResponseAction> {
    try {
        if (validationErrorsList.isNullOrEmpty()) {
            pagesRest.onBeforeDatabaseAction(request, obj, postData, OperationType.DELETE)
            pagesRest.onBeforeDelete(request, obj, postData)
            baseDao.delete(obj)
            return ResponseEntity(pagesRest.onAfterDelete(request, obj, postData), HttpStatus.OK)
        }
        // Validation error occurred:
        return ResponseEntity(ResponseAction(validationErrors = validationErrorsList), HttpStatus.NOT_ACCEPTABLE)
    } catch (ex: Exception) {
        return handleException("Error while trying to delete object '${obj::class.java}' with id #${obj.id}", ex)
    }
}

private fun handleException(msg: String, ex: Exception): ResponseEntity<ResponseAction> {
    if (ex is UserException) {
        val msgParams = ex.msgParams ?: ex.params
        log.error("$msg: message='${ex.i18nKey}', params='${msgParams?.joinToString() { it.toString() }}'")
        // Through translateMsg(ex), not with the params as they are: a MessageParam may be an i18n key itself
        // and has to be translated before it goes into the message, or the user reads the key.
        val error = ValidationError(translateMsg(ex), messageId = ex.i18nKey)
        if (!ex.causedByField.isNullOrBlank()) error.fieldId = ex.causedByField
        val errors = listOf(error)
        return ResponseEntity(ResponseAction(validationErrors = errors), HttpStatus.NOT_ACCEPTABLE)
    } else {
        log.error("$msg: message='${ex.message}'", ex)
        val error = ValidationError(ex.message)
        val errors = listOf(error)
        return ResponseEntity(ResponseAction(validationErrors = errors), HttpStatus.NOT_ACCEPTABLE)
    }
}
