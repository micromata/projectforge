package org.projectforge.rest.core

/**
 * Contains the data including the result list (matching the filter) served by getList methods ([getInitialList] and [getList]).
 */
class ResultSet<O : Any>(var resultSet: List<O>,
                         var totalSize: Int? = null) {
    val size = resultSet.size
}
