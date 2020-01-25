/////////////////////////////////////////////////////////////////////////////
//
// Project ProjectForge Community Edition
//         www.projectforge.org
//
// Copyright (C) 2001-2020 Micromata GmbH, Germany (www.micromata.com)
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

import org.projectforge.framework.access.OperationType
import org.projectforge.framework.i18n.UserException
import org.projectforge.framework.i18n.translateMsg
import org.projectforge.framework.persistence.api.BaseDao
import org.projectforge.framework.persistence.api.ExtendedBaseDO
import org.projectforge.framework.persistence.api.MagicFilter
import org.projectforge.framework.persistence.api.MagicFilterProcessor
import org.projectforge.rest.dto.PostData
import org.projectforge.ui.ResponseAction
import org.projectforge.ui.ValidationError
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import javax.servlet.http.HttpServletRequest


internal val log = org.slf4j.LoggerFactory.getLogger("org.projectforge.rest.core.AbstractBaseRestUtils")

fun <O : ExtendedBaseDO<Int>, DTO : Any, B : BaseDao<O>>
        getList(pagesRest: AbstractPagesRest<O, DTO, B>,
                baseDao: BaseDao<O>,
                magicFilter: MagicFilter)
        : ResultSet<O> {
    magicFilter.sortAndLimitMaxRowsWhileSelect = true
    val dbFilter = MagicFilterProcessor.doIt(baseDao.doClass, magicFilter)
    pagesRest.processMagicFilter(dbFilter, magicFilter)
    val list = baseDao.getList(dbFilter)
    val resultSet = ResultSet(pagesRest.filterList(list, magicFilter), list.size)
    return resultSet
}

fun <O : ExtendedBaseDO<Int>, DTO : Any, B : BaseDao<O>>
        saveOrUpdate(request: HttpServletRequest,
                     baseDao: BaseDao<O>,
                     obj: O,
                     postData: PostData<DTO>,
                     pagesRest: AbstractPagesRest<O, DTO, B>,
                     validationErrorsList: List<ValidationError>?)
        : ResponseEntity<ResponseAction> {

    if (!validationErrorsList.isNullOrEmpty()) {
        // Validation error occurred:
        return ResponseEntity(ResponseAction(validationErrors = validationErrorsList), HttpStatus.NOT_ACCEPTABLE)
    }
    val isNew = obj.id == null || obj.created == null // obj.created is needed for KundeDO (id isn't null for inserting new customers).
    pagesRest.beforeSaveOrUpdate(request, obj, postData)
    try {
        pagesRest.beforeDatabaseAction(request, obj, postData, if (obj.id != null) OperationType.UPDATE else OperationType.INSERT)
        baseDao.saveOrUpdate(obj) ?: obj.id
    } catch (ex: UserException) {
        log.error("Error while trying to save/update object '${obj::class.java}' with id #${obj.id}: message=${ex.i18nKey}, params='${ex.msgParams?.joinToString() { it.toString() }}'")
        val error = ValidationError(translateMsg(ex), messageId = ex.i18nKey)
        if (!ex.causedByField.isNullOrBlank()) error.fieldId = ex.causedByField
        val errors = listOf(error)
        return ResponseEntity(ResponseAction(validationErrors = errors), HttpStatus.NOT_ACCEPTABLE)
    }
    pagesRest.afterSaveOrUpdate(obj, postData)
    if (isNew) {
        return ResponseEntity(pagesRest.afterSave(obj, postData), HttpStatus.OK)
    } else {
        return ResponseEntity(pagesRest.afterUpdate(obj, postData), HttpStatus.OK)
    }
}

fun <O : ExtendedBaseDO<Int>, DTO : Any, B : BaseDao<O>>
        undelete(request: HttpServletRequest,
                 baseDao: BaseDao<O>,
                 obj: O,
                 postData: PostData<DTO>,
                 pagesRest: AbstractPagesRest<O, DTO, B>,
                 validationErrorsList: List<ValidationError>?)
        : ResponseEntity<ResponseAction> {
    if (validationErrorsList.isNullOrEmpty()) {
        pagesRest.beforeDatabaseAction(request, obj, postData, OperationType.UNDELETE)
        pagesRest.beforeUndelete(request, obj, postData)
        baseDao.undelete(obj)
        return ResponseEntity(pagesRest.afterUndelete(obj, postData), HttpStatus.OK)
    }
    // Validation error occurred:
    return ResponseEntity(ResponseAction(validationErrors = validationErrorsList), HttpStatus.NOT_ACCEPTABLE)
}

fun <O : ExtendedBaseDO<Int>, DTO : Any, B : BaseDao<O>>
        markAsDeleted(request: HttpServletRequest,
                      baseDao: BaseDao<O>,
                      obj: O,
                      postData: PostData<DTO>,
                      pagesRest: AbstractPagesRest<O, DTO, B>,
                      validationErrorsList: List<ValidationError>?)
        : ResponseEntity<ResponseAction> {
    if (validationErrorsList.isNullOrEmpty()) {
        pagesRest.beforeDatabaseAction(request, obj, postData, OperationType.DELETE)
        pagesRest.beforeMarkAsDeleted(request, obj, postData)
        baseDao.markAsDeleted(obj)
        return ResponseEntity(pagesRest.afterMarkAsDeleted(obj, postData), HttpStatus.OK)
    }
    // Validation error occurred:
    return ResponseEntity(ResponseAction(validationErrors = validationErrorsList), HttpStatus.NOT_ACCEPTABLE)
}

fun <O : ExtendedBaseDO<Int>, DTO : Any, B : BaseDao<O>>
        delete(request: HttpServletRequest,
               baseDao: BaseDao<O>,
               obj: O,
               postData: PostData<DTO>,
               pagesRest: AbstractPagesRest<O, DTO, B>,
               validationErrorsList: List<ValidationError>?)
        : ResponseEntity<ResponseAction> {
    if (validationErrorsList.isNullOrEmpty()) {
        pagesRest.beforeDatabaseAction(request, obj, postData, OperationType.DELETE)
        pagesRest.beforeDelete(request, obj, postData)
        baseDao.delete(obj)
        return ResponseEntity(pagesRest.afterDelete(obj, postData), HttpStatus.OK)
    }
    // Validation error occurred:
    return ResponseEntity(ResponseAction(validationErrors = validationErrorsList), HttpStatus.NOT_ACCEPTABLE)
}
