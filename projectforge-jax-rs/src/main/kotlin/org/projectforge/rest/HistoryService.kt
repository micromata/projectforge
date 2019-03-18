package org.projectforge.rest

import com.google.gson.annotations.SerializedName
import de.micromata.genome.db.jpa.history.api.DiffEntry
import de.micromata.genome.db.jpa.history.api.HistoryEntry
import de.micromata.genome.db.jpa.history.entities.EntityOpType
import de.micromata.genome.db.jpa.history.entities.PropertyOpType
import org.projectforge.business.multitenancy.TenantRegistryMap
import org.projectforge.common.i18n.I18nEnum
import org.projectforge.common.props.PropUtils
import org.projectforge.framework.persistence.user.entities.PFUserDO
import org.projectforge.rest.ui.translate
import org.springframework.stereotype.Service
import java.lang.reflect.Field
import java.util.*

/**
 * History entries will be transformed into human readable formats.
 */
@Service
class HistoryService {
    private val log = org.slf4j.LoggerFactory.getLogger(HistoryService::class.java)

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
            var clazz: Class<*>? = null
            try {
                clazz = Class.forName(it.entityName)
            } catch (ex: ClassNotFoundException) {
                log.warn("Class '${it.entityName}' not found.")
            }
            it.diffEntries?.forEach { de ->
                val diffEntry = DisplayHistoryDiffEntry(
                        propertyOpType = de.propertyOpType,
                        property = de.propertyName,
                        oldValue = de.oldValue,
                        newValue = de.newValue)
                if (clazz != null) {
                    try {
                        var field = clazz.getDeclaredField(de.propertyName)
                        field.isAccessible = true
                        if (field != null && field.type.isEnum()) {
                            diffEntry.oldValue = getI18nEnumTranslation(field, diffEntry.oldValue)
                            diffEntry.newValue = getI18nEnumTranslation(field, diffEntry.newValue)
                        }
                    } catch (ex: NoSuchFieldException) {
                        log.warn("No such field '${it.entityName}.${de.propertyName}': ${ex.message}.")
                    }
                    diffEntry.property = translateProperty(de, clazz)
                }
                entry.diffEntries.add(diffEntry)
            }
            entries.add(entry)
        }
        return entries
    }

    /**
     * Tries to get the translation via the i18n key defined in the PropertyInfo annotation fo the given field and value.
     */
    private fun getI18nEnumTranslation(field: Field, value: String?): String? {
        if (value == null) {
            return "";
        }
        val i18nEnum = I18nEnum.create(field.type, value) as I18nEnum
        return translate(i18nEnum.i18nKey)
    }

    /**
     * Tries to find a PropertyInfo annotation for the property field referred in the given diffEntry.
     * If found, the property name will be returned translated, if not, the property will be returned unmodified.
     */
    private fun translateProperty(diffEntry: DiffEntry, clazz: Class<*>): String? {
        // Try to get the PropertyInfo containing the i18n key of the property for translation.
        var propertyName = PropUtils.get(clazz, diffEntry.propertyName)?.i18nKey
        if (propertyName != null) {
            // translate the i18n key:
            propertyName = translate(propertyName)
        } else {
            propertyName = diffEntry.propertyName
        }
        return propertyName
    }
}