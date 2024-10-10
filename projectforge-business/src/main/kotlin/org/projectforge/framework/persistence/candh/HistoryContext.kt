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

import jline.console.internal.ConsoleRunner.property
import org.projectforge.framework.persistence.api.BaseDO
import org.projectforge.framework.persistence.api.IdObject
import org.projectforge.framework.persistence.history.EntityOpType
import org.projectforge.framework.persistence.history.HistoryEntryAttrDO
import org.projectforge.framework.persistence.history.HistoryEntryDO
import org.projectforge.framework.persistence.history.PropertyOpType
import org.projectforge.framework.persistence.utils.CollectionUtils
import kotlin.reflect.KClass
import kotlin.reflect.KMutableProperty1

/**
 * Context for handling history entries.
 */
internal class HistoryContext(
    /**
     * Needed for initializing the history context with the first historyEntryWrapper entry, if required.
     */
    val entity: BaseDO<*>,
    val entityOpType: EntityOpType = EntityOpType.Update
) {
    // All created historyEntryWrappers for later processing.
    private val historyEntryWrappers = mutableListOf<CandHHistoryEntryWrapper>()

    // Stack for the current historyEntryWrapper, proceeded by CandHMaster.
    private val historyEntryWrapperStack = mutableListOf<CandHHistoryEntryWrapper>()

    internal val currentHistoryEntryWrapper: CandHHistoryEntryWrapper?
        get() = historyEntryWrapperStack.lastOrNull()

    private class SrcCollectionWithNewEntries(
        val propertyContext: PropertyContext,
        /**
         * Already existing entries in the destination collection. The new src entries are not part of this collection.
         */
        val keptEntries: Collection<Any>?,
    )

    /**
     * This map is used to store the original collection of the entity for generating history entries later of
     * new persisted collection entries. The id of the new entries is not known at this point.
     */
    private var collectionsWithNewAndUpdatedEntries: MutableCollection<SrcCollectionWithNewEntries>? = null

    /**
     * Adds a new collection with new entries. The kept entries are already part of the destination collection.
     */
    fun addCollectionsWithNewAndUpdatedEntries(propertyContext: PropertyContext, keptEntries: Collection<Any>?) {
        collectionsWithNewAndUpdatedEntries = collectionsWithNewAndUpdatedEntries ?: mutableListOf()
        collectionsWithNewAndUpdatedEntries!!.add(SrcCollectionWithNewEntries(propertyContext, keptEntries))
    }

    fun getPreparedHistoryEntries(): List<HistoryEntryDO> {
        val entryList = historyEntryWrappers.map { it.prepareAndGetHistoryEntry() }.toMutableList()
        collectionsWithNewAndUpdatedEntries?.forEach { entry ->
            val pc = entry.propertyContext
            val dest = pc.dest

            @Suppress("UNCHECKED_CAST")
            val property = pc.property as KMutableProperty1<BaseDO<*>, Any?>

            @Suppress("UNCHECKED_CAST")
            val destCol = property.get(dest) as? Collection<Any>
            val keptEntries = entry.keptEntries
            if (pc.entriesHistorizable == true) {
                destCol?.forEach { destEntry ->
                    @Suppress("UNCHECKED_CAST")
                    destEntry as IdObject<Long>
                    if (keptEntries?.contains(destEntry) != true) {
                        // This is a new entry, not existing in the dest collection before.
                        entryList.add(HistoryEntryDO.create(destEntry, EntityOpType.Insert))
                    }
                }
            } else {
                @Suppress("UNCHECKED_CAST")
                val srcCol = property.get(pc.src) as? Collection<Any>
                // The collection is not historizable. We need to check if the collection has been changed.
                @Suppress("UNCHECKED_CAST")
                val histEntry = HistoryEntryDO.create(pc.src as IdObject<Long>, EntityOpType.Update)
                val attr = HistoryEntryAttrDO.create(
                    propertyTypeClass =CollectionUtils.getTypeClassOfEntries(pc.updated ?: destCol),
                    opType = PropertyOpType.Update,
                    propertyName = pc.propertyName,
                )
                attr.serializeAndSet(oldValue = pc.updated, newValue = destCol)
                histEntry.add(attr)
                entryList.add(histEntry)
            }
        }
        return entryList
    }

    /**
     * Adds history historyEntryWrapper. This will not be removed, even if it has no attributes.
     */
    fun addHistoryEntryWrapper(
        entity: BaseDO<*>,
        entityOpType: EntityOpType = EntityOpType.Update,
    ): CandHHistoryEntryWrapper {
        @Suppress("UNCHECKED_CAST")
        entity as IdObject<Long>
        CandHHistoryEntryWrapper.create(entity = entity, entityOpType = entityOpType).let {
            historyEntryWrappers.add(it)
            return it
        }
    }

    /**
     * Push a new historyEntryWrapper of type [CandHHistoryEntryWrapper] to the stack. Don't forget to call [popHistoryEntryWrapper] when you're done.
     * This historyEntryWrapper will be removed if [popHistoryEntryWrapper] is called, and it has no attributes.
     */
    fun pushHistoryEntryWrapper(
        entity: BaseDO<*>,
        entityOpType: EntityOpType = EntityOpType.Update,
    ): CandHHistoryEntryWrapper {
        @Suppress("UNCHECKED_CAST")
        entity as IdObject<Long>
        CandHHistoryEntryWrapper.create(entity = entity, entityOpType = entityOpType).let {
            historyEntryWrapperStack.add(it)
            historyEntryWrappers.add(it)
            return it
        }
    }

    /**
     * Pop the last historyEntryWrapper from the stack. Throws an exception if the stack is empty.
     * If the historyEntryWrapper has no attributes, it will be removed from the historyEntryWrappers list.
     */
    fun popHistoryEntryWrapper(): CandHHistoryEntryWrapper {
        if (currentHistoryEntryWrapper?.attributeWrappers.isNullOrEmpty()) {
            // If the historyEntryWrapper has no attributes, we don't need to keep it.
            historyEntryWrappers.remove(currentHistoryEntryWrapper)
        }
        return historyEntryWrapperStack.removeAt(historyEntryWrapperStack.size - 1)
    }

    private val currentHistoryEntryAttrs: MutableSet<CandHHistoryAttrWrapper>
        get() {
            if (currentHistoryEntryWrapper == null) {
                // Now we need to create the first historyEntryWrapper entry.
                pushHistoryEntryWrapper(entity, entityOpType)
            }
            currentHistoryEntryWrapper!!.let { historyEntryWrapper ->
                historyEntryWrapper.attributeWrappers = historyEntryWrapper.attributeWrappers ?: mutableSetOf()
                @Suppress("UNCHECKED_CAST")
                return historyEntryWrapper.attributeWrappers!!
            }
        }

    /**
     * Add a new history entry for the given property context. The current historyEntryWrapper will be used.
     */
    fun add(propertyContext: PropertyContext, optype: PropertyOpType) {
        propertyContext.apply {
            add(
                property = property,
                optype = optype,
                oldValue = destPropertyValue,
                newValue = srcPropertyValue,
                propertyName = propertyName,
            )
        }
    }

    /**
     * Add a new history entry for the given property context. The currenthistoryEntryWrapper must be given and will be used.
     */
    fun add(
        property: KMutableProperty1<*, *>,
        optype: PropertyOpType,
        oldValue: Any?,
        newValue: Any?,
        propertyName: String?,
    ) {
        currentHistoryEntryAttrs.add(
            CandHHistoryAttrWrapper.create(
                property = property,
                optype = optype,
                oldValue = oldValue,
                newValue = newValue,
                propertyName = propertyName,
            )
        )
    }

    /**
     * Add a new history entry for the given property context. The currenthistoryEntryWrapper must be given and will be used.
     */
    fun add(
        propertyTypeClass: Class<*>,
        optype: PropertyOpType,
        oldValue: Any?,
        newValue: Any?,
        propertyName: String?,
    ) {
        currentHistoryEntryAttrs.add(
            CandHHistoryAttrWrapper.create(
                propertyTypeClass = propertyTypeClass,
                optype = optype,
                oldValue = oldValue,
                newValue = newValue,
                propertyName = propertyName,
            )
        )
    }
}
