package org.projectforge.framework.persistence.jpa.candh

import org.projectforge.framework.persistence.api.EntityCopyStatus
import org.projectforge.framework.persistence.history.PropertyOpType
import org.projectforge.framework.persistence.jpa.CopyAndHistoryDebugContext
import org.projectforge.framework.persistence.jpa.HistoryContext

class CandHContext(
    var currentCopyStatus: EntityCopyStatus = EntityCopyStatus.NONE,
    // On top-level the field name and for nested fields the path, such as "address.street".
    var currentObjectPath: String = "",
    createHistory: Boolean = true,
    debug: Boolean = false,
) {
    internal val debugContext = if (debug) CopyAndHistoryDebugContext() else null
    internal val historyContext = if (createHistory) HistoryContext() else null
    internal fun addHistoryEntry(fieldName: String, type: PropertyOpType, newValue: Any?, oldValue: Any?) {
        historyContext?.add(
            path = "${currentObjectPath}$fieldName",
            type = type,
            newValue = newValue,
            oldValue = oldValue
        )
    }

    fun combine(status: EntityCopyStatus): EntityCopyStatus {
        val newStatus = currentCopyStatus.combine(status)
        debugContext?.let {
            if (newStatus != currentCopyStatus) {
                it.add(msg = "Status changed from $currentCopyStatus to $newStatus")
            }
        }
        currentCopyStatus = newStatus
        return currentCopyStatus
    }
}
