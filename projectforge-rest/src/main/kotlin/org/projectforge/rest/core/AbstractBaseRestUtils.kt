/////////////////////////////////////////////////////////////////////////////
//
// Project ProjectForge Community Edition
//         www.projectforge.org
//
// Copyright (C) 2001-2019 Micromata GmbH, Germany (www.micromata.com)
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

import org.projectforge.framework.persistence.api.MagicFilter
import org.projectforge.framework.i18n.UserException
import org.projectforge.framework.i18n.translateMsg
import org.projectforge.framework.persistence.api.BaseDao
import org.projectforge.framework.persistence.api.BaseSearchFilter
import org.projectforge.framework.persistence.api.ExtendedBaseDO
import org.projectforge.ui.ResponseAction
import org.projectforge.ui.ValidationError
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import javax.servlet.http.HttpServletRequest


internal val log = org.slf4j.LoggerFactory.getLogger("org.projectforge.rest.core.AbstractBaseRestUtils")

fun <O : ExtendedBaseDO<Int>, DTO : Any, B : BaseDao<O>>
        getList(dataObjectRest: AbstractBaseRest<O, DTO, B>,
                baseDao: BaseDao<O>,
                magicFilter: MagicFilter)
        : ResultSet<O> {
    val filter = magicFilter.prepareQueryFilter()
    //filter.isSortAndLimitMaxRowsWhileSelect = true
    log.error("******* TODO *****")
    val legacyFilter = BaseSearchFilter()
    legacyFilter.isSortAndLimitMaxRowsWhileSelect = true
    legacyFilter.maxRows = 50
    val list = baseDao.getList(legacyFilter)
    val resultSet = ResultSet<O>(dataObjectRest.filterList(list, magicFilter), list.size)
    return resultSet
}

fun <O : ExtendedBaseDO<Int>, DTO : Any, B : BaseDao<O>>
        saveOrUpdate(request: HttpServletRequest,
                     baseDao: BaseDao<O>,
                     obj: O,
                     dto: DTO,
                     dataObjectRest: AbstractBaseRest<O, DTO, B>,
                     validationErrorsList: List<ValidationError>?)
        : ResponseEntity<ResponseAction> {

    if (!validationErrorsList.isNullOrEmpty()) {
        // Validation error occurred:
        return ResponseEntity(ResponseAction(validationErrors = validationErrorsList), HttpStatus.NOT_ACCEPTABLE)
    }
    val isNew = obj.id == null || obj.created == null // obj.created is needed for KundeDO (id isn't null for inserting new customers).
    dataObjectRest.beforeSaveOrUpdate(request, obj, dto)
    try {
        baseDao.saveOrUpdate(obj) ?: obj.id
    } catch (ex: UserException) {
        log.error("Error while trying to save/update object '${obj::class.java}' with id #${obj.id}: message=${ex.i18nKey}, params='${ex.msgParams?.joinToString() { it.toString() }}'")
        val error = ValidationError(translateMsg(ex), messageId = ex.i18nKey)
        if (!ex.causedByField.isNullOrBlank()) error.fieldId = ex.causedByField
        val errors = listOf(error)
        return ResponseEntity(ResponseAction(validationErrors = errors), HttpStatus.NOT_ACCEPTABLE)
    }
    dataObjectRest.afterSaveOrUpdate(obj, dto)
    if (isNew) {
        return ResponseEntity(dataObjectRest.afterSave(obj, dto), HttpStatus.OK)
    } else {
        return ResponseEntity(dataObjectRest.afterUpdate(obj, dto), HttpStatus.OK)
    }
}

fun <O : ExtendedBaseDO<Int>, DTO : Any, B : BaseDao<O>>
        undelete(baseDao: BaseDao<O>,
                 obj: O,
                 dto: DTO,
                 dataObjectRest: AbstractBaseRest<O, DTO, B>,
                 validationErrorsList: List<ValidationError>?)
        : ResponseEntity<ResponseAction> {
    if (validationErrorsList.isNullOrEmpty()) {
        baseDao.undelete(obj)
        return ResponseEntity(dataObjectRest.afterUndelete(obj, dto), HttpStatus.OK)
    }
    // Validation error occurred:
    return ResponseEntity(ResponseAction(validationErrors = validationErrorsList), HttpStatus.NOT_ACCEPTABLE)
}

fun <O : ExtendedBaseDO<Int>, DTO : Any, B : BaseDao<O>>
        markAsDeleted(baseDao: BaseDao<O>,
                      obj: O,
                      dto: DTO,
                      dataObjectRest: AbstractBaseRest<O, DTO, B>,
                      validationErrorsList: List<ValidationError>?)
        : ResponseEntity<ResponseAction> {
    if (validationErrorsList.isNullOrEmpty()) {
        baseDao.markAsDeleted(obj)
        return ResponseEntity(dataObjectRest.afterMarkAsDeleted(obj, dto), HttpStatus.OK)
    }
    // Validation error occurred:
    return ResponseEntity(ResponseAction(validationErrors = validationErrorsList), HttpStatus.NOT_ACCEPTABLE)
}

fun <O : ExtendedBaseDO<Int>, DTO : Any, B : BaseDao<O>>
        delete(baseDao: BaseDao<O>,
               obj: O,
               dto: DTO,
               dataObjectRest: AbstractBaseRest<O, DTO, B>,
               validationErrorsList: List<ValidationError>?)
        : ResponseEntity<ResponseAction> {
    if (validationErrorsList.isNullOrEmpty()) {
        baseDao.delete(obj)
        return ResponseEntity(dataObjectRest.afterDelete(obj, dto), HttpStatus.OK)
    }
    // Validation error occurred:
    return ResponseEntity(ResponseAction(validationErrors = validationErrorsList), HttpStatus.NOT_ACCEPTABLE)
}
