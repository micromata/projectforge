package org.projectforge.rest

import com.google.gson.annotations.SerializedName
import de.micromata.genome.db.jpa.history.api.HistoryEntry
import de.micromata.genome.db.jpa.history.entities.EntityOpType
import de.micromata.genome.db.jpa.history.entities.PropertyOpType
import org.projectforge.business.multitenancy.TenantRegistryMap
import org.projectforge.framework.persistence.user.entities.PFUserDO
import org.springframework.stereotype.Service
import java.util.*

/**
 * Convenient classes for working with DO inside Kotlin code...
 */
@Service
class HistoryService {
    data class DisplayHistoryEntry(
            @SerializedName("modified-at")
            var modifiedAt: Date? = null,
            @SerializedName("modified-by-user-id")
            var modfiedByUserId: String? = null,
            @SerializedName("modified-by-user")
            var modfiedByUser: String? = null,
            @SerializedName("entity-op-type")
            var entityOpType: EntityOpType? = null,
            @SerializedName("diff-entries")
            var diffEntries: MutableList<DisplayHistoryDiffEntry> = mutableListOf())

    data class DisplayHistoryDiffEntry(
            @SerializedName("property-op-type")
            var propertyOpType: PropertyOpType? = null,
            var property: String? = null,
            @SerializedName("old-value")
            var oldValue: String? = null,
            @SerializedName("new-value")
            var newValue: String? = null)

    /**
     * Creates a list of formatted history entries (get the user names etc.)
     */
    fun format(orig: Array<HistoryEntry<*>>): List<DisplayHistoryEntry> {
        val userGroupCache = TenantRegistryMap.getInstance().getTenantRegistry().getUserGroupCache()
        var entries = mutableListOf<DisplayHistoryEntry>()
        orig.forEach {
            var user: PFUserDO? = null;
            try {
                user = userGroupCache?.getUser(it.modifiedBy.toInt())
            } catch (e: NumberFormatException) {
                // Ignore error.
            }
            val entry = DisplayHistoryEntry(
                    modifiedAt = it.modifiedAt,
                    modfiedByUserId = it.modifiedBy,
                    modfiedByUser = user?.fullname,
                    entityOpType = it.entityOpType)
            it.diffEntries?.forEach { de ->
                val diffEntry = DisplayHistoryDiffEntry(
                        propertyOpType = de.propertyOpType,
                        property = de.propertyName,
                        oldValue = de.oldValue,
                        newValue = de.newValue)
                entry.diffEntries.add(diffEntry)
            }
            entries.add(entry)
        }
        return entries
    }
}