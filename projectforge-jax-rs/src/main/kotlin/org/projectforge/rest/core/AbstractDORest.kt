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
abstract class AbstractDORest<
        O : ExtendedBaseDO<Int>,
        B : BaseDao<O>,
        F : BaseSearchFilter>(
        private val baseDaoClazz: Class<B>,
        private val filterClazz: Class<F>,
        private val i18nKeyPrefix: String)
    : AbstractBaseRest<O, O, B, F>(baseDaoClazz, filterClazz, i18nKeyPrefix) {

    override fun processResultSetBeforeExport(resultSet: ResultSet<Any>) {
        resultSet.resultSet.forEach { processItemBeforeExport(it) }
    }

    override fun asDO(o: O): O {
        return o
    }

    override fun createEditLayoutData(item: O, layout: UILayout): EditLayoutData {
        return EditLayoutData(item, layout)
    }

    override fun returnItem(item: O): ResponseEntity<Any> {
        return ResponseEntity<Any>(item, HttpStatus.OK)
    }
}
