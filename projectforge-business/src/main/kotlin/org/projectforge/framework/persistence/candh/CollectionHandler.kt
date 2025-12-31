/////////////////////////////////////////////////////////////////////////////
//
// Project ProjectForge Community Edition
//         www.projectforge.org
//
// Copyright (C) 2001-2026 Micromata GmbH, Germany (www.micromata.com)
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

package org.projectforge.framework.persistence.candh

import jakarta.persistence.JoinColumn
import jakarta.persistence.JoinTable
import jakarta.persistence.OneToMany
import mu.KotlinLogging
import org.hibernate.collection.spi.PersistentList
import org.hibernate.collection.spi.PersistentSet
import org.projectforge.common.AnnotationsUtils
import org.projectforge.common.KClassUtils
import org.projectforge.framework.persistence.api.BaseDO
import org.projectforge.framework.persistence.api.EntityCopyStatus
import org.projectforge.framework.persistence.api.HibernateUtils
import org.projectforge.framework.persistence.candh.CandHMaster.copyValues
import org.projectforge.framework.persistence.candh.CandHMaster.propertyWasModified
import org.projectforge.framework.persistence.entities.AbstractHistorizableBaseDO
import org.projectforge.framework.persistence.history.*
import org.projectforge.framework.persistence.utils.CollectionUtils
import java.io.Serializable
import java.util.*
import kotlin.reflect.KMutableProperty1
import kotlin.reflect.full.createInstance

private val log = KotlinLogging.logger {}

/**
 * Used for mutable collections. TreeSet, ArrayList, HashSet and PersistentSet are supported.
 * Supports deep copy of collection entries as well, as history handling (adding, removing and modifying entries).
 */
open class CollectionHandler : CandHIHandler {
    override fun accept(property: KMutableProperty1<*, *>): Boolean {
        return CollectionUtils.isCollection(property)
    }

    override fun process(
        propertyContext: PropertyContext,
        context: CandHContext,
    ): Boolean {
        log.debug { "process: Processing collection property '${propertyContext.propertyName}': $propertyContext" }
        val property = propertyContext.property
        val dest = propertyContext.dest
        if (!collectionManagedBySrcClazz(propertyContext.property)) {
            // Collection isn't managed by this class, therefore do nothing.
            return true
        }
        @Suppress("UNCHECKED_CAST")
        property as KMutableProperty1<BaseDO<*>, Any?>
        val srcCollection = propertyContext.srcPropertyValue as? Collection<Any?>

        @Suppress("UNCHECKED_CAST")
        var destCollection = propertyContext.destPropertyValue as? MutableCollection<Any?>
        if (srcCollection.isNullOrEmpty() && destCollection.isNullOrEmpty()) {
            log.debug { "process: Both collections '${propertyContext.propertyName}' are null or empty, so nothing to do." }
            // Both collections are null or empty, so nothing to do.
            return true
        }
        if (srcCollection.isNullOrEmpty()) {
            // srcCollection is null or empty, so we have to remove all entries from destCollection.
            log.debug { "process: srcCollection '${propertyContext.propertyName}' is null or empty." }
            context.addHistoryEntry(
                propertyTypeClass = CollectionUtils.getTypeClassOfEntries(destCollection),
                propertyName = propertyContext.propertyName,
                value = null,
                oldValue = destCollection?.toList() // Copy the collection, because it will be cleared.
            )
            destCollection?.clear() // Clear the collection. Can't set it to null, because it should be a persisted collection.
            log.debug { "process: property '${propertyContext.propertyName}' was modified." }
            propertyWasModified(context, propertyContext, null)
            return true
        }
        // Calculates the differences between src and dest collection: added, removed and kept entries.
        val compareResults = CollectionUtils.compareCollections(srcCollection, destCollection, withKept = true)
        if (destCollection == null) {
            // destCollection is null, so we have to create a new collection.
            destCollection = createCollectionInstance(propertyContext.srcPropertyValue)
            property.set(dest, destCollection)
        }
        // Check if collection is explicitly marked for soft-delete
        val hasSoftDeleteAnnotation = AnnotationsUtils.getAnnotation(
            propertyContext.property,
            SoftDeleteCollection::class.java
        ) != null

        compareResults.removed?.forEach { removeEntry ->
            log.debug { "process: Removing collection '${propertyContext.propertyName}' entry: $removeEntry" }
            if (hasSoftDeleteAnnotation && removeEntry is AbstractHistorizableBaseDO<*>) {
                // Soft-delete: Mark as deleted instead of physical removal
                // This preserves history entries and avoids unique constraint violations during updates
                removeEntry.deleted = true
                log.debug { "process: Marked entry as deleted (soft-delete, @SoftDeleteCollection): $removeEntry" }
            } else {
                // Physical removal (default behavior)
                destCollection.remove(removeEntry)
                log.debug { "process: Physically removed entry from collection '${propertyContext.propertyName}': $removeEntry" }
            }
        }
        compareResults.added?.forEach { addEntry ->
            log.debug { "process: Adding new collection entry: $addEntry" }
            if (hasSoftDeleteAnnotation && addEntry is AbstractHistorizableBaseDO<*> && addEntry.id != null) {
                // Check if this is a reactivation of a deleted entry (only for @SoftDeleteCollection)
                val existingDeleted = destCollection.firstOrNull {
                    it is AbstractHistorizableBaseDO<*> && it.id == addEntry.id && it.deleted
                }
                if (existingDeleted is AbstractHistorizableBaseDO<*>) {
                    // Reactivate: remove deleted flag instead of adding again
                    existingDeleted.deleted = false
                    log.debug { "process: Reactivated deleted entry (@SoftDeleteCollection): $existingDeleted" }
                    return@forEach  // Don't add again
                }
            }
            destCollection.add(addEntry)
            log.debug { "process: Added entry to destPropertyValue: $addEntry" }
        }
        val entry = compareResults.anyOrNull // Get any entry from the collections for extracting the class type.
        propertyContext.entriesHistorizable = HistoryServiceUtils.isHistorizable(entry)
        val updated = mutableListOf<Pair<Any, Any>>() // first is oldValue, second is newValue.
        val behavior = AnnotationsUtils.getAnnotation(propertyContext.property, PersistenceBehavior::class.java)
        log.debug { "process: srcEntry of src-collection is BaseDO. autoUpdateCollectionEntres = ${behavior?.autoUpdateCollectionEntries == true}" }
        val collectionManagedByOwnerEntity = behavior != null && behavior.autoUpdateCollectionEntries
        log.debug { "process: collectionManagedByOwnerEntity=$collectionManagedByOwnerEntity" }
        run loop@{
            compareResults.kept?.forEach { keptEntry ->
                // Kept entries are part of src and dest collection, so we have to check modifications.
                if (keptEntry !is BaseDO<*>) {
                    // Kept entries are not of type BaseDO, so we can't check modifications.
                    return@loop // break foreach loop
                }
                if (collectionManagedByOwnerEntity) {
                    log.debug { "process: srcEntry collection managed by owner entity." }
                    val destEntry = destCollection.first { CollectionUtils.idObjectsEqual(it as BaseDO<*>, keptEntry) }
                    try {
                        context.historyContext?.pushHistoryEntryWrapper(keptEntry)
                        @Suppress("UNCHECKED_CAST")
                        val oldValue = keptEntry::class.createInstance() as BaseDO<Serializable>
                        // oldValue is a deep copy of destEntry (unmodified from database).
                        @Suppress("UNCHECKED_CAST")
                        copyValues(destEntry as BaseDO<Serializable>, oldValue, context.clone())
                        if (copyValues(keptEntry, destEntry, context) == EntityCopyStatus.MAJOR
                            && propertyContext.entriesHistorizable == false
                        ) {
                            // If the entry was modified and isn't historizable itself, we have to add a history entry.
                            updated.add(Pair(oldValue, destEntry))
                        }
                    } finally {
                        context.historyContext?.popHistoryEntryWrapper()
                    }
                }
            }
        }
        if (updated.isNotEmpty() || !compareResults.removed.isNullOrEmpty() || !compareResults.added.isNullOrEmpty()) {
            if (collectionManagedByOwnerEntity) {
                if (propertyContext.entriesHistorizable == true) {
                    // If collection is managed by this class and the entries are to be historized themselves,
                    // we don't need to add a history entry of removed and added entries as lists.
                    // We add history entries of the entries itself.
                    compareResults.removed?.forEach { removed ->
                        context.addHistoryEntryWrapper(
                            entity = removed as BaseDO<*>,
                            entityOpType = EntityOpType.Delete,
                        )
                    }
                    //
                    // Note for added entries: It's later done in [writeInsertHistoryEntriesForNewCollectionEntries].
                    // toAdd: Entity id is null, so can't create history master entry now.
                    //
                }
                if (updated.isNotEmpty()) {
                    val oldValues = updated.map { it.first }
                    val newValues = updated.map { it.second }
                    context.addHistoryEntryWrapper(
                        entity = dest,
                        entityOpType = EntityOpType.Update,
                    )?.addAttribute(
                        propertyTypeClass = CollectionUtils.getTypeClassOfEntries(newValues),
                        propertyName = propertyContext.propertyName,
                        optype = PropertyOpType.Update,
                        oldValue = oldValues,
                        newValue = newValues,
                    )
                }
            } else {
                // The collection isn't managed by this class, so we have to add a history entry of removed and added entries as lists.
                // There are entries to remove or add.
                context.addHistoryEntry(
                    propertyTypeClass = destCollection.first()!!::class.java,
                    propertyName = propertyContext.propertyName,
                    value = compareResults.added,
                    oldValue = compareResults.removed,
                )
            }
            // Writes no history entry (type = null), because it's already done:
            propertyWasModified(context, propertyContext, null)
        }
        return true
    }

    private fun createCollectionInstance(
        srcCollection: Any
    ): MutableCollection<Any?> {
        val collection: MutableCollection<Any?> = when (srcCollection) {
            is TreeSet<*> -> TreeSet()
            is HashSet<*>, is PersistentSet<*> -> HashSet()
            is ArrayList<*>, is PersistentList<*> -> ArrayList()
            else -> {
                log.error { "createCollectionInstance: Unsupported collection type: " + srcCollection.javaClass.name }
                ArrayList()
            }
        }
        log.debug { "createCollectionInstance: Creating instance of ${collection.javaClass} as destPropertyValue." }
        return collection
    }

    companion object {
        /**
         * If collection is declared as OneToMany and not marked as @NoHistory, the collection is managed by the source class.
         * This includes both 1:n (OneToMany) and n:m (ManyToMany) relationships for history tracking.
         */
        private fun collectionManagedBySrcClazz(property: KMutableProperty1<*, *>): Boolean {
            val annotations = AnnotationsUtils.getAnnotations(property)
            if (annotations.any { it.annotationClass == NoHistory::class }) {
                log.debug { "collectionManagedBySrcClazz: Collection is marked as NoHistory, so nothing to do." }
                // No history for this collection, so nothing to do by this src class.
                return false
            }
            if (annotations.any { it.annotationClass == JoinColumn::class } ||
                annotations.any { it.annotationClass == JoinTable::class }
            ) {
                log.debug { "collectionManagedBySrcClazz: Collection is managed by this class." }
                // There is a join table or column for this entity, so we're assuming to manage this collection.
                return true
            }
            annotations.firstOrNull { it.annotationClass == OneToMany::class }?.let { annotation ->
                annotation as OneToMany
                val mappedBy = annotation.mappedBy
                log.debug { "collectionManagedBySrcClazz: Collection mappedBy='$mappedBy' -> managed by this=${mappedBy.isNotEmpty()}" }
                // There is a mappedBy column for this entity, so we're assuming to manage this collection.
                return mappedBy.isNotEmpty()
            }
            return false
        }

        /*
         * This is necessary, because the id values of the new collection entries are null after persisting the merged object.
         * @param mergedObj The merged object.
         * @param destObj The destination object.
         * @param entryList The list of history entries. New entries will be added to this list.
         */
        internal fun writeInsertHistoryEntriesForNewCollectionEntries(
            mergedObj: BaseDO<*>?,
            srcObj: BaseDO<*>?,
            entryList: MutableList<HistoryEntryDO>,
        ) {
            if (mergedObj == null) {
                log.debug { "writeInsertHistoryEntriesForNewCollectionEntries: Check for history entries for new collection entries. mergedObj: null" }
                return
            }
            log.debug { "writeInsertHistoryEntriesForNewCollectionEntries: Check for history entries for new collection entries. mergedObj: ${mergedObj::class.simpleName}:${mergedObj.id}" }
            KClassUtils.filterPublicMutableProperties(mergedObj::class).forEach { property ->
                @Suppress("UNCHECKED_CAST")
                property as KMutableProperty1<BaseDO<*>, Any?>
                if (!CollectionUtils.isCollection(property) || !collectionManagedBySrcClazz(property)) {
                    // No collection or not managed by us, continue.
                    return@forEach
                }
                val mergedCol = property.get(mergedObj) as Collection<*>?
                val firstOrNull = mergedCol?.firstOrNull() ?: return@forEach // Collection is empty, continue.
                val srcCol = if (srcObj != null) {
                    property.get(srcObj) as Collection<*>? ?: return@forEach // Collection is empty, continue.
                } else {
                    null
                }
                if (firstOrNull !is BaseDO<*>) {
                    // Collection entry isn't a BaseDO, so we can't call setIdValuesOfCollectionEntries recursively.
                    return@forEach
                }
                @Suppress("UNCHECKED_CAST")
                mergedCol as Collection<BaseDO<Long>>
                val newCollectionEntriesWithIdNull = mutableListOf<BaseDO<Long>>()
                mergedCol.forEach { mergedEntry ->
                    @Suppress("UNCHECKED_CAST")
                    if (srcCol == null || (srcCol as Collection<BaseDO<Long>>).none { it.id == mergedEntry.id }) {
                        // Entry is new, so we have to get the id values of the new collection entries.
                        newCollectionEntriesWithIdNull.add(mergedEntry)
                    }
                }
                newCollectionEntriesWithIdNull.forEach { newEntry ->
                    log.debug { "writeInsertHistoryEntriesForNewCollectionEntries: Create history entry for new collection entry. newEntry=${newEntry::class.simpleName}:${newEntry.id}" }
                    entryList.add(HistoryEntryDO.create(newEntry, EntityOpType.Insert))
                }
                // If autoUpdateCollectionEntries is true, we have to historize the child collections of all existing entries:
                val behavior = AnnotationsUtils.getAnnotation(property, PersistenceBehavior::class.java)
                log.debug { "writeInsertHistoryEntriesForNewCollectionEntries: srcEntry of src-collection is BaseDO. autoUpdateCollectionEntres = ${behavior?.autoUpdateCollectionEntries == true}" }
                if (behavior?.autoUpdateCollectionEntries == true) {
                    // No, we have to handle the child collections of all existing collection entries:
                    // Example: RechnungDO -> list of RechnungPositionDO -> list of KostZuweisungDO.
                    mergedCol.forEach { mergedEntry ->
                        if (HibernateUtils.isFullyInitialized(mergedEntry) && mergedEntry.id != null) {
                            log.debug { "writeInsertHistoryEntriesForNewCollectionEntries: Check for history entries of child collections of (if any): ${mergedEntry::class.simpleName}:${mergedEntry.id}" }
                            // Historize only existing entries.
                            // If an entry is new, it's clear, that any existing child entry is also new.
                            @Suppress("UNCHECKED_CAST")
                            val srcEntry =
                                srcCol?.firstOrNull { (it as BaseDO<Long>).id == mergedEntry.id } as BaseDO<Long>?
                            writeInsertHistoryEntriesForNewCollectionEntries(mergedEntry, srcEntry, entryList)
                        }
                    }
                }
            }
        }
    }
}
