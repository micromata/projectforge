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
import org.projectforge.framework.persistence.history.PersistenceBehavior
import org.projectforge.framework.persistence.candh.CandHMaster.copyValues
import org.projectforge.framework.persistence.candh.CandHMaster.propertyWasModified
import org.projectforge.framework.persistence.history.EntityOpType
import org.projectforge.framework.persistence.history.NoHistory
import org.projectforge.framework.persistence.utils.CollectionUtils
import java.io.Serializable
import java.util.*
import kotlin.reflect.KMutableProperty1
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
            // Both collections are null or empty, so nothing to do.
            return true
        }
        if (srcCollection.isNullOrEmpty()) {
            context.debugContext?.add(propertyContext)
            context.addHistoryEntry(
                propertyTypeClass = destCollection!!.first()!!::class.java,
                propertyName = pc.propertyName,
                value = null,
                oldValue = destCollection
            )
            destCollection.clear() // Clear the collection. Can't set it to null, because is should be a persisted collection.
            propertyWasModified(context, propertyContext, null)
            return true
        }
        // Calculates the differences between src and dest collection: added, removed and kept entries.
        val compareResults = CollectionUtils.compareCollections(srcCollection, destCollection, withKept = true)
        if (destCollection == null) {
            destCollection = createCollectionInstance(context, pc, pc.srcPropertyValue)
            property.set(dest, destCollection)
        }
        compareResults.removed?.forEach { removeEntry ->
            log.debug { "Removing collection entry: $removeEntry" }
            destCollection.remove(removeEntry)
            context.debugContext?.add(
                propertyContext, "Removing entry $removeEntry from destPropertyValue.",
            )
        }
        compareResults.added?.forEach { addEntry ->
            log.debug { "Adding new collection entry: $addEntry" }
            destCollection.add(addEntry)
            context.debugContext?.add(propertyContext, msg = "Adding entry $addEntry to destPropertyValue.")
        }
        var collectionManagedByThis = false
        run loop@{
            compareResults.kept?.forEach { keptEntry ->
                // Kept entries are part of src and dest collection, so we have to check modifications.
                if (keptEntry !is BaseDO<*>) {
                    // Kept entries are not of type BaseDO, so we can't check modifications.
                    return@loop // break foreach loop
                }
                val behavior = AnnotationsUtils.getAnnotation(pc.property, PersistenceBehavior::class.java)
                context.debugContext?.add(
                    propertyContext,
                    msg = "srcEntry of src-collection is BaseDO. autoUpdateCollectionEntres = ${behavior?.autoUpdateCollectionEntries == true}",
                )
                if (behavior != null && behavior.autoUpdateCollectionEntries) {
                    collectionManagedByThis = true
                    val destEntry =
                        destCollection.first { CollectionUtils.idObjectsEqual(it as BaseDO<*>, keptEntry) }
                    try {
                        context.historyContext?.pushHistoryEntryWrapper(keptEntry)
                        @Suppress("UNCHECKED_CAST")
                        copyValues(
                            keptEntry,
                            destEntry as BaseDO<Serializable>,
                            context,
                        )
                    } finally {
                        context.historyContext?.popHistoryEntryWrapper()
                    }
                }
            }
        }
        if (!compareResults.removed.isNullOrEmpty() || !compareResults.added.isNullOrEmpty()) {
            if (collectionManagedByThis) {
                // If collection is managed by this class, we don't need to add a history entry of removed and added entries as lists.
                compareResults.removed?.forEach { entry ->
                    context.addHistoryEntryWrapper(
                        entity = entry as BaseDO<*>,
                        entityOpType = EntityOpType.Delete,
                    )
                }
                if (!compareResults.added.isNullOrEmpty()) {
                    // toAdd: Entity id is null, so can't create history entry now.
                    // We store the src collection entries in the history context and create the history entries later.
                    context.historyContext?.addSrcCollectionWithNewEntries(pc, compareResults.kept)
                }
            } else {
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
            // No history for this collection, so nothing to do by this src class.
            return false
        }
        if (annotations.any { it.annotationClass == JoinColumn::class } ||
            annotations.any { it.annotationClass == JoinTable::class }
        ) {
            // There is a join table or column for this entity, so we're assuming to manage this collection.
            return true
        }
        annotations.firstOrNull { it.annotationClass == OneToMany::class }?.let { annotation ->
            annotation as OneToMany
            // There is a mappedBy column for this entity, so we're assuming to manage this collection.
            return annotation.mappedBy.isNotEmpty()
        }
        return false
    }

    private fun createCollectionInstance(
        context: CandHContext,
        propertyContext: PropertyContext,
        srcCollection: Any
    ): MutableCollection<Any?> {
        val collection: MutableCollection<Any?> = when (srcCollection) {
            is TreeSet<*> -> TreeSet()
            is HashSet<*> -> HashSet()
            is ArrayList<*> -> ArrayList()
            is PersistentSet<*> -> HashSet()
            else -> {
                log.error { "Unsupported collection type: " + srcCollection.javaClass.name }
                ArrayList()
            }
        }
        propertyContext.apply {
            context.debugContext?.add(propertyContext, msg = "Creating ${collection.javaClass} as destPropertyValue.")
        }
        return collection
    }
}
