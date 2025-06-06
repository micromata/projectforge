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

package org.projectforge.framework.persistence.api

import org.apache.commons.collections4.CollectionUtils
import org.apache.commons.lang3.StringUtils
import org.projectforge.framework.ToStringUtil
import java.io.Serializable
import java.util.Date

/**
 * Base search filter supported by the DAO's for filtering the result lists. The search filter will be translated via
 * QueryFilter into hibernate query criteria.
 *
 * @author Kai Reinhard (k.reinhard@micromata.de)
 */
open class BaseSearchFilter : Serializable {
    var searchString: String? = null

    var deleted: Boolean = false // Initialization unnecessary but for documentation.

    /**
     * If true, deleted and undeleted objects will be shown.
     */
    var ignoreDeleted: Boolean = false // Initialization unnecessary but for documentation.

    var maxRows: Int = -1

    var pageSize: Int = -1

    /**
     * If true, the result set will be ordered limited by the database query.
     */
    var sortAndLimitMaxRowsWhileSelect: Boolean = false

    /**
     * If true then modifiedByUser and time of last modification is used for filtering.
     */
    var useModificationFilter: Boolean = false

    var modifiedByUserId: Long? = null

    @Deprecated("")
    var startTimeOfLastModification: Date? = null

    @Deprecated("")
    var stopTimeOfLastModification: Date? = null


    open var startTimeOfModification: Date? = null
    open var stopTimeOfModification: Date? = null

    /*
    * If given [BaseDao.select] will only search for entries which last date of modification
    * AbstractBaseDO.getLastUpdate() isn't before given date.
    */
    var modifiedSince: Date? = null

    /**
     * If true the history entries are included in the search.
     */
    var searchHistory = false

    /**
     * If an error occurred (e. g. lucene parse exception) this message will be returned.
     */
    var errorMessage: String? = null

    var sortProperties: MutableList<SortProperty?>? = null

    var searchFields: Array<String>? = null

    constructor()

    constructor(filter: BaseSearchFilter?) {
        if (filter == null) {
            return
        }
        copyBaseSearchFieldsFrom(filter)
    }

    fun copyBaseSearchFieldsFrom(filter: BaseSearchFilter) {
        this.searchFields = filter.searchFields
        this.searchString = filter.searchString
        this.deleted = filter.deleted
        this.ignoreDeleted = filter.ignoreDeleted
        this.maxRows = filter.maxRows
        this.sortProperties = filter.sortProperties
        this.sortAndLimitMaxRowsWhileSelect = filter.sortAndLimitMaxRowsWhileSelect
        this.useModificationFilter = filter.useModificationFilter
        this.modifiedByUserId = filter.modifiedByUserId
        this.startTimeOfModification = filter.startTimeOfModification
        this.stopTimeOfModification = filter.stopTimeOfModification
        this.searchHistory = filter.searchHistory
    }

    /**
     * @return this for chaining.
     */
    open fun reset(): BaseSearchFilter? {
        deleted = false
        ignoreDeleted = false
        searchString = ""
        searchHistory = false
        return this
    }

    fun isSearchNotEmpty(): Boolean {
        return StringUtils.isNotEmpty(searchString)
    }

    fun applyModificationFilter(): Boolean {
        return this.useModificationFilter && (this.startTimeOfModification != null || this.stopTimeOfModification != null || this.modifiedByUserId != null)
    }

    /**
     * @return the first sort order if available, otherwise null.
     */
    fun getSortOrder(): SortOrder {
        return if (CollectionUtils.isNotEmpty(sortProperties)) sortProperties!!.get(0)!!.sortOrder else SortOrder.ASCENDING
    }

    /**
     * @return the first sort order if available, otherwise null.
     */
    fun getSortProperty(): String? {
        return if (CollectionUtils.isNotEmpty(sortProperties)) sortProperties!!.get(0)!!.property else null
    }

    /**
     * @param property
     * @return this for chaining.
     */
    fun setSortProperty(property: String?): BaseSearchFilter {
        return setSortProperty(property, SortOrder.ASCENDING)
    }

    /**
     * @param property
     * @param sortOrder
     * @return this for chaining.
     */
    fun setSortProperty(property: String?, sortOrder: SortOrder): BaseSearchFilter {
        sortProperties = ArrayList<SortProperty?>()
        sortProperties!!.add(SortProperty(property, sortOrder))
        return this
    }

    /**
     * @param property
     * @param sortOrder
     * @return this for chaining.
     */
    /**
     * @param property
     * @return this for chaining.
     */
    @JvmOverloads
    fun appendSortProperty(property: String?, sortOrder: SortOrder = SortOrder.ASCENDING): BaseSearchFilter {
        if (sortProperties == null) sortProperties = ArrayList<SortProperty?>()
        sortProperties!!.add(SortProperty(property, sortOrder))
        return this
    }

    fun hasErrorMessage(): Boolean {
        return StringUtils.isNotEmpty(errorMessage)
    }

    /**
     * @return this for chaining.
     */
    fun clearErrorMessage(): BaseSearchFilter {
        this.errorMessage = null
        return this
    }

    override fun toString(): String {
        return ToStringUtil.Companion.toJsonString(this)
    }

    companion object {
        private const val serialVersionUID = 5970378227395811426L
    }
}
