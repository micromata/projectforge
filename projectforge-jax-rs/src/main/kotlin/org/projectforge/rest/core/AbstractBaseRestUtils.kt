package org.projectforge.rest.core

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

fun <O : ExtendedBaseDO<Int>, DTO : Any, B : BaseDao<O>, F : BaseSearchFilter>
        getList(dataObjectRest: AbstractBaseRest<O, DTO, B, F>, baseDao: BaseDao<O>, filter: F)
        : ResultSet<Any> {
    filter.isSortAndLimitMaxRowsWhileSelect = true
    val list = baseDao.getList(filter)
    val resultSet = ResultSet<Any>(dataObjectRest.filterList(list, filter), list.size)
    return resultSet
}

fun <O : ExtendedBaseDO<Int>, DTO : Any, B : BaseDao<O>, F : BaseSearchFilter>
        saveOrUpdate(request: HttpServletRequest,
                     baseDao: BaseDao<O>, obj: O,
                     dataObjectRest: AbstractBaseRest<O, DTO, B, F>,
                     validationErrorsList: List<ValidationError>?)
        : ResponseEntity<ResponseAction> {

    if (!validationErrorsList.isNullOrEmpty()) {
        // Validation error occurred:
        return ResponseEntity(ResponseAction(validationErrors = validationErrorsList), HttpStatus.NOT_ACCEPTABLE)
    }
    val isNew = obj.id == null
    dataObjectRest.beforeSaveOrUpdate(request, obj)
    try {
       baseDao.saveOrUpdate(obj) ?: obj.id
    } catch (ex: UserException) {
        log.error("Error while trying to save/update object '${obj::class.java}' with id #${obj.id}: message=${ex.i18nKey}, params='${ex.msgParams?.joinToString() { it.toString() }}'")
        val error = ValidationError(translateMsg(ex), messageId = ex.i18nKey)
        if (!ex.causedByField.isNullOrBlank()) error.fieldId = ex.causedByField
        val errors = listOf(error)
        return ResponseEntity(ResponseAction(validationErrors = errors), HttpStatus.NOT_ACCEPTABLE)
    }
    dataObjectRest.afterSaveOrUpdate(obj)
    if (isNew) {
        return ResponseEntity(dataObjectRest.afterSave(obj), HttpStatus.OK)
    } else {
        return ResponseEntity(dataObjectRest.afterUpdate(obj), HttpStatus.OK)
    }
}

fun <O : ExtendedBaseDO<Int>, DTO : Any, B : BaseDao<O>, F : BaseSearchFilter>
        undelete(baseDao: BaseDao<O>, obj: O,
                 dataObjectRest: AbstractBaseRest<O, DTO, B, F>,
                 validationErrorsList: List<ValidationError>?)
        : ResponseEntity<ResponseAction> {
    if (validationErrorsList.isNullOrEmpty()) {
        baseDao.undelete(obj)
        return ResponseEntity(dataObjectRest.afterUndelete(obj), HttpStatus.OK)
    }
    // Validation error occurred:
    return ResponseEntity(ResponseAction(validationErrors = validationErrorsList), HttpStatus.NOT_ACCEPTABLE)
}

fun <O : ExtendedBaseDO<Int>, DTO : Any, B : BaseDao<O>, F : BaseSearchFilter>
        markAsDeleted(baseDao: BaseDao<O>, obj: O,
                      dataObjectRest: AbstractBaseRest<O, DTO, B, F>,
                      validationErrorsList: List<ValidationError>?)
        : ResponseEntity<ResponseAction> {
    if (validationErrorsList.isNullOrEmpty()) {
        baseDao.markAsDeleted(obj)
        return ResponseEntity(dataObjectRest.afterMarkAsDeleted(obj), HttpStatus.OK)
    }
    // Validation error occurred:
    return ResponseEntity(ResponseAction(validationErrors = validationErrorsList), HttpStatus.NOT_ACCEPTABLE)
}

fun <O : ExtendedBaseDO<Int>, DTO : Any, B : BaseDao<O>, F : BaseSearchFilter>
        delete(baseDao: BaseDao<O>, obj: O,
               dataObjectRest: AbstractBaseRest<O, DTO, B, F>,
               validationErrorsList: List<ValidationError>?)
        : ResponseEntity<ResponseAction> {
    if (validationErrorsList.isNullOrEmpty()) {
        baseDao.delete(obj)
        return ResponseEntity(dataObjectRest.afterDelete(obj), HttpStatus.OK)
    }
    // Validation error occurred:
    return ResponseEntity(ResponseAction(validationErrors = validationErrorsList), HttpStatus.NOT_ACCEPTABLE)
}
