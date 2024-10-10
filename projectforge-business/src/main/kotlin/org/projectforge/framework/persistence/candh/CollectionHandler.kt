/////////////////////////////////////////////////////////////////////////////
//
// Project ProjectForge Community Edition
//         www.projectforge.org
//
// Copyright (C) 2001-2024 Micromata GmbH, Germany (www.micromata.com)
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
import org.hibernate.collection.spi.PersistentSet
import org.projectforge.common.AnnotationsUtils
import org.projectforge.framework.persistence.api.BaseDO
import org.projectforge.framework.persistence.api.EntityCopyStatus
import org.projectforge.framework.persistence.candh.CandHMaster.copyValues
import org.projectforge.framework.persistence.candh.CandHMaster.propertyWasModified
import org.projectforge.framework.persistence.history.*
import org.projectforge.framework.persistence.utils.CollectionUtils
import java.io.Serializable
import java.util.*
import kotlin.reflect.KMutableProperty1
import kotlin.reflect.full.createInstance
import kotlin.reflect.full.isSubclassOf
import kotlin.reflect.jvm.jvmErasure

private val log = KotlinLogging.logger {}

/**
 * Used for mutable collections. TreeSet, ArrayList, HashSet and PersistentSet are supported.
 * Supports deep copy of collection entries as well, as history handling (adding, removing and modifying entries).
 */
open class CollectionHandler : CandHIHandler {
    override fun accept(property: KMutableProperty1<*, *>): Boolean {
        return property.returnType.jvmErasure.isSubclassOf(Collection::class)
    }

    override fun process(
        propertyContext: PropertyContext,
        context: CandHContext,
    ): Boolean {
        log.debug { "process: Processing collection property '${propertyContext.propertyName}': $propertyContext" }
        val pc = propertyContext
        val property = pc.property
        val dest = pc.dest
        if (!collectionManagedBySrcClazz(pc.property)) {
            // Collection isn't managed by this class, therefore do nothing.
            return true
        }
        @Suppress("UNCHECKED_CAST")
        property as KMutableProperty1<BaseDO<*>, Any?>
        val srcCollection = pc.srcPropertyValue as? Collection<Any?>

        @Suppress("UNCHECKED_CAST")
        var destCollection = pc.destPropertyValue as? MutableCollection<Any?>
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
                propertyName = pc.propertyName,
                value = null,
                oldValue = destCollection
            )
            destCollection?.clear() // Clear the collection. Can't set it to null, because is should be a persisted collection.
            log.debug { "process: property '${propertyContext.propertyName}' was modified." }
            propertyWasModified(context, propertyContext, null)
            return true
        }
        // Calculates the differences between src and dest collection: added, removed and kept entries.
        val compareResults = CollectionUtils.compareCollections(srcCollection, destCollection, withKept = true)
        if (destCollection == null) {
            // destCollection is null, so we have to create a new collection.
            destCollection = createCollectionInstance(pc.srcPropertyValue)
            property.set(dest, destCollection)
        }
        compareResults.removed?.forEach { removeEntry ->
            log.debug { "process: Removing collection '${propertyContext.propertyName}' entry: $removeEntry" }
            destCollection.remove(removeEntry)
            log.debug { "process: Removing entry $removeEntry from destPropertyValue from collection '${propertyContext.propertyName}'." }
        }
        compareResults.added?.forEach { addEntry ->
            log.debug { "process: Adding new collection entry: $addEntry" }
            destCollection.add(addEntry)
            log.debug { "process: Adding entry $addEntry to destPropertyValue." }
        }
        val entry = compareResults.anyOrNull
        pc.entriesHistorizable = HistoryServiceUtils.isHistorizable(entry)
        val updated = mutableListOf<Pair<Any, Any>>() // first is oldValue, second is newValue.
        val behavior = AnnotationsUtils.getAnnotation(pc.property, PersistenceBehavior::class.java)
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
                            && pc.entriesHistorizable == false
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
        if (!compareResults.removed.isNullOrEmpty() || !compareResults.added.isNullOrEmpty()) {
            if (collectionManagedByOwnerEntity) {
                if (pc.entriesHistorizable == true) {
                    // If collection is managed by this class and the entries are to be historized themselves,
                    // we don't need to add a history entry of removed and added entries as lists.
                    compareResults.removed?.forEach { removed ->
                        context.addHistoryEntryWrapper(
                            entity = removed as BaseDO<*>,
                            entityOpType = EntityOpType.Delete,
                        )
                    }
                    if (!compareResults.added.isNullOrEmpty()) {
                        // If there are modified entries or added entries, we don't need to add a history entry of added and updated entries as list.
                        // toAdd: Entity id is null, so can't create history master entry now.
                        // We store the src collection entries in the history context and create the history master entries later.
                        context.historyContext?.addSrcCollectionWithNewEntries(pc, compareResults.kept)
                    }
                }
                if (updated.isNotEmpty()) {
                    val oldValues = updated.map { it.first }
                    val newValues = updated.map { it.second }
                    context.addHistoryEntryWrapper(
                        entity = dest,
                        entityOpType = EntityOpType.Update,
                    )?.addAttribute(
                        propertyTypeClass = CollectionUtils.getTypeClassOfEntries(newValues),
                        propertyName = pc.propertyName,
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
                    propertyName = pc.propertyName,
                    value = compareResults.added,
                    oldValue = compareResults.removed,
                )
            }
            // Writes no history entry (type = null), because it's already done:
            propertyWasModified(context, propertyContext, null)
        }
        return true
    }

    /**
     * If collection is declared as OneToMany and not marked as @NoHistory, the collection is managed by the source class.
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

    private fun createCollectionInstance(
        srcCollection: Any
    ): MutableCollection<Any?> {
        val collection: MutableCollection<Any?> = when (srcCollection) {
            is TreeSet<*> -> TreeSet()
            is HashSet<*> -> HashSet()
            is ArrayList<*> -> ArrayList()
            is PersistentSet<*> -> HashSet()
            else -> {
                log.error { "createCollectionInstance: Unsupported collection type: " + srcCollection.javaClass.name }
                ArrayList()
            }
        }
        log.debug { "createCollectionInstance: Creating instance of ${collection.javaClass} as destPropertyValue." }
        return collection
    }
}
