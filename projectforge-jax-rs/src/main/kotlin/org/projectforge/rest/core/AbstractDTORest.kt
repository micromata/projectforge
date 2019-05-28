package org.projectforge.rest.core

import org.projectforge.framework.persistence.api.BaseDao
import org.projectforge.framework.persistence.api.BaseSearchFilter
import org.projectforge.framework.persistence.api.ExtendedBaseDO
import org.projectforge.ui.UILayout
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity

/**
 * This is the base class for all fronted functionality regarding query, editing etc. It also serves layout
 * data for the frontend.
 * <br>
 * For each entity type such as users, addresses, time sheets etc. an own class is inherited for doing customizations.
 * It's recommended for the frontend to develop generic list and edit pages by using the layout information served
 * by these rest services.
 */
abstract class AbstractDTORest<
        O : ExtendedBaseDO<Int>,
        DTO : Any,
        B : BaseDao<O>,
        F : BaseSearchFilter>(
        private val baseDaoClazz: Class<B>,
        private val filterClazz: Class<F>,
        private val i18nKeyPrefix: String)
    : AbstractBaseRest<O, DTO, B, F>(baseDaoClazz, filterClazz, i18nKeyPrefix) {

    override fun processResultSetBeforeExport(resultSet: ResultSet<Any>) {
        val orig = resultSet.resultSet
        resultSet.resultSet = orig.map {
            @Suppress("UNCHECKED_CAST")
            transformDO(it as O, false)
        }
    }

    /**
     * Must be overridden if flag [useDTO] is true. Throws [UnsupportedOperationException] at default.
     */
    abstract fun transformDO(obj: O, editMode : Boolean): DTO

    /**
     * Must be overridden if flag [useDTO] is true. Throws [UnsupportedOperationException] at default.
     */
    abstract fun transformDTO(dto: DTO): O

    override fun asDO(dto: DTO): O {
        return transformDTO(dto)
    }

    override fun createEditLayoutData(item: O, layout: UILayout): EditLayoutData {
        return EditLayoutData(transformDO(item, true), layout)
    }

    override fun returnItem(item: O): ResponseEntity<Any> {
        return ResponseEntity<Any>(transformDO(item, true), HttpStatus.OK)
    }
}
