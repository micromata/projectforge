package org.projectforge.rest.core

import org.projectforge.framework.persistence.api.BaseDao
import org.projectforge.framework.persistence.api.BaseSearchFilter
import org.projectforge.framework.persistence.api.ExtendedBaseDO

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
        DTO: Any,
        B : BaseDao<O>,
        F : BaseSearchFilter>(
        private val baseDaoClazz: Class<B>,
        private val filterClazz: Class<F>,
        private val i18nKeyPrefix: String)
    : AbstractBaseObjectRest<O, DTO, B, F>(baseDaoClazz, filterClazz, i18nKeyPrefix) {
    init {
        useDTO = true
    }
}
