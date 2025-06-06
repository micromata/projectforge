/////////////////////////////////////////////////////////////////////////////
//
// Project ProjectForge Community Edition
//         www.projectforge.org
//
// Copyright (C) 2001-2025 Micromata GmbH, Germany (www.micromata.com)
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
import org.projectforge.framework.i18n.translate
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
    pagesRest: AbstractPagesRest<O, DTO, B>,
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
    pagesRest: AbstractPagesRest<O, DTO, B>,
    baseDao: BaseDao<O>,
    magicFilter: MagicFilter
)
        : MutableList<O> {
    magicFilter.sortAndLimitMaxRowsWhileSelect = true
    val queryFilter = baseDao.createQueryFilter()
    val customResultFilters = pagesRest.preProcessMagicFilter(queryFilter, magicFilter)
    magicFilter.sortProperties = magicFilter.sortProperties.distinctBy { it.property }.toMutableList()
    MagicFilterProcessor.doIt(baseDao.doClass, magicFilter, queryFilter)
    pagesRest.postProcessMagicFilter(queryFilter, magicFilter)
    return baseDao.select(queryFilter, customResultFilters).toMutableList()
}

fun <O : ExtendedBaseDO<Long>, DTO : Any, B : BaseDao<O>>
        saveOrUpdate(
    request: HttpServletRequest,
    baseDao: BaseDao<O>,
    obj: O,
    postData: PostData<DTO>,
    pagesRest: AbstractPagesRest<O, DTO, B>,
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
            return ResponseEntity(pagesRest.onAfterSave(obj, postData), HttpStatus.OK)
        } else {
            return ResponseEntity(pagesRest.onAfterUpdate(obj, postData), HttpStatus.OK)
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
    pagesRest: AbstractPagesRest<O, DTO, B>,
    validationErrorsList: List<ValidationError>?
)
        : ResponseEntity<ResponseAction> {
    try {
        if (validationErrorsList.isNullOrEmpty()) {
            pagesRest.onBeforeDatabaseAction(request, obj, postData, OperationType.UNDELETE)
            pagesRest.onBeforeUndelete(request, obj, postData)
            baseDao.undelete(obj)
            return ResponseEntity(pagesRest.onAfterUndelete(obj, postData), HttpStatus.OK)
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
    pagesRest: AbstractPagesRest<O, DTO, B>,
    validationErrorsList: List<ValidationError>?
)
        : ResponseEntity<ResponseAction> {
    try {
        if (validationErrorsList.isNullOrEmpty()) {
            pagesRest.onBeforeDatabaseAction(request, obj, postData, OperationType.DELETE)
            pagesRest.onBeforeMarkAsDeleted(request, obj, postData)
            baseDao.markAsDeleted(obj)
            return ResponseEntity(pagesRest.onAfterMarkAsDeleted(obj, postData), HttpStatus.OK)
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
    pagesRest: AbstractPagesRest<O, DTO, B>
)
        : ResponseEntity<ResponseAction> {
    try {
        pagesRest.onBeforeDatabaseAction(request, obj, postData, OperationType.DELETE)
        pagesRest.onBeforeDelete(request, obj, postData)
        baseDao.forceDelete(obj)
        return ResponseEntity(pagesRest.onAfterDelete(obj, postData), HttpStatus.OK)
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
    pagesRest: AbstractPagesRest<O, DTO, B>,
    validationErrorsList: List<ValidationError>?
)
        : ResponseEntity<ResponseAction> {
    try {
        if (validationErrorsList.isNullOrEmpty()) {
            pagesRest.onBeforeDatabaseAction(request, obj, postData, OperationType.DELETE)
            pagesRest.onBeforeDelete(request, obj, postData)
            baseDao.delete(obj)
            return ResponseEntity(pagesRest.onAfterDelete(obj, postData), HttpStatus.OK)
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
        val msg = if (msgParams != null) {
            translateMsg(ex.i18nKey, *msgParams)
        } else {
            translate(ex.i18nKey)
        }
        val error = ValidationError(msg, messageId = ex.i18nKey)
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
