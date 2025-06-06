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

package org.projectforge.framework.persistence.candh

import mu.KotlinLogging
import org.projectforge.framework.persistence.api.BaseDO
import org.projectforge.framework.persistence.api.IdObject
import org.projectforge.framework.persistence.history.EntityOpType
import org.projectforge.framework.persistence.history.HistoryEntryDO
import org.projectforge.framework.persistence.history.PropertyOpType
import kotlin.reflect.KMutableProperty1
import kotlin.reflect.jvm.jvmErasure

private val log = KotlinLogging.logger {}

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

    /**
     * Returns the prepared history entries. This method should be called after all history entries have been added.
     * The history entries are prepared and the new collection entries are added.
     * @param mergedObj The merged object (new collection entries with db ids).
     * @param destObj The destination object (object used in em.merge()). The ids of the new collection entries are not set.
     */
    fun getPreparedHistoryEntries(mergedObj: BaseDO<*>,srcObj: BaseDO<*>): List<HistoryEntryDO> {
        log.debug { "getPreparedHistoryEntries: ${entity::class.simpleName}" }
        val entryList = historyEntryWrappers.map { it.prepareAndGetHistoryEntry() }.toMutableList()
        CollectionHandler.writeInsertHistoryEntriesForNewCollectionEntries(mergedObj, srcObj, entryList)
        return entryList
    }

    /**
     * Adds history historyEntryWrapper. This will not be removed, even if it has no attributes.
     */
    fun addHistoryEntryWrapper(
        entity: BaseDO<*>,
        entityOpType: EntityOpType = EntityOpType.Update,
    ): CandHHistoryEntryWrapper {
        log.debug { "addHistoryWrapper: ${entity::class.simpleName}, entityOpType=$entityOpType" }
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
        log.debug { "pushHistoryEntryWrapper: ${entity::class.simpleName}, entityOpType=$entityOpType" }
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
        log.debug { "popHistoryEntryWrapper" }
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
                return historyEntryWrapper.attributeWrappers!!
            }
        }

    /**
     * Add a new history entry for the given property context. The current historyEntryWrapper will be used.
     */
    fun add(propertyContext: PropertyContext, opType: PropertyOpType) {
        propertyContext.apply {
            add(
                property = property,
                opType = opType,
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
        opType: PropertyOpType,
        oldValue: Any?,
        newValue: Any?,
        propertyName: String?,
    ) {
        log.debug { "add: Add history entry: ${property.returnType.jvmErasure}, $opType, oldValue=$oldValue, newValue=$newValue, propertyName=$propertyName" }
        currentHistoryEntryAttrs.add(
            CandHHistoryAttrWrapper.create(
                property = property,
                optype = opType,
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
        log.debug { "add: Add history entry: $propertyTypeClass, $optype, oldValue=$oldValue, newValue=$newValue, propertyName=$propertyName" }
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
