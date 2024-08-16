package org.projectforge.framework.persistence.history

/**
 * The Class DiffEntry.
 *
 * @author Roger Rene Kommer (r.kommer.extern@micromata.de)
 */
class DiffEntry {
    /**
     * The property op type.
     */
    var propertyOpType: PropertyOpType? = null

    /**
     * The property name.
     */
    var propertyName: String? = null
    var oldProp: HistProp? = null

    var newProp: HistProp? = null

    val oldValue: String?
        get() = if (oldProp == null) null else oldProp!!.value

    val newValue: String?
        get() = if (newProp == null) null else newProp!!.value
}
