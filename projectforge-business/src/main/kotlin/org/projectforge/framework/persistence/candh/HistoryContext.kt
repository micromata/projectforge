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

import org.projectforge.framework.persistence.api.BaseDO
import org.projectforge.framework.persistence.api.IdObject
import org.projectforge.framework.persistence.history.EntityOpType
import org.projectforge.framework.persistence.history.PfHistoryMasterDO
import org.projectforge.framework.persistence.history.PropertyOpType
import kotlin.reflect.KClass
import kotlin.reflect.KMutableProperty1

/**
 * Context for handling history entries.
 */
internal class HistoryContext(
    /**
     * Needed for initializing the history context with the first masterWrapper entry, if required.
     */
    val entity: BaseDO<*>,
    val entityOpType: EntityOpType = EntityOpType.Update
) {
    // All created masterWrappers for later processing.
    private val masterWrappers = mutableListOf<CandHHistoryMasterWrapper>()

    // Stack for the current masterWrapper, proceeded by CandHMaster.
    private val masterWrapperStack = mutableListOf<CandHHistoryMasterWrapper>()

    internal val currentMasterWrapper: CandHHistoryMasterWrapper?
        get() = masterWrapperStack.lastOrNull()

    private class SrcCollectionWithNewEntries(
        val propertyContext: PropertyContext,
        val existingEntries: Collection<Any>
    )

    /**
     * This map is used to store the original collection of the entity for generating history entries later of
     * new persisted collection entries. The id of the new entries is not known at this point.
     */
    private var srcCollectionsWithNewEntries: MutableCollection<SrcCollectionWithNewEntries>? = null

    fun addSrcCollectionWithNewEntries(propertyContext: PropertyContext, existingEntries: Collection<Any>) {
        srcCollectionsWithNewEntries = srcCollectionsWithNewEntries ?: mutableListOf()
        srcCollectionsWithNewEntries!!.add(SrcCollectionWithNewEntries(propertyContext, existingEntries))
    }

    fun getPreparedMasterEntries(): List<PfHistoryMasterDO> {
        masterWrappers.forEach { it.internalPrepareForPersist() } // Copy all attrs to master and internalSerializeValueObjects.
        val masterList = masterWrappers.map { it.master }.toMutableList()
        srcCollectionsWithNewEntries?.forEach { entry ->
            val pc = entry.propertyContext
            val dest = pc.dest

            @Suppress("UNCHECKED_CAST")
            val property = pc.property as KMutableProperty1<BaseDO<*>, Any?>

            @Suppress("UNCHECKED_CAST")
            val destCol = property.get(dest) as? Collection<Any>
            val existingEntries = entry.existingEntries
            destCol?.forEach { destEntry ->
                @Suppress("UNCHECKED_CAST")
                destEntry as IdObject<Long>
                if (!existingEntries.contains(destEntry)) {
                    // This is a new entry, not existing in the dest collection before.
                    masterList.add(PfHistoryMasterDO.create(destEntry, EntityOpType.Insert))
                }
            }
        }
        return masterList
    }

    /**
     * Adds history masterWrapper. This will not be removed, even if it has no attributes.
     */
    fun addHistoryMasterWrapper(
        entity: BaseDO<*>,
        entityOpType: EntityOpType = EntityOpType.Update,
    ): CandHHistoryMasterWrapper {
        @Suppress("UNCHECKED_CAST")
        entity as IdObject<Long>
        CandHHistoryMasterWrapper.create(entity = entity, entityOpType = entityOpType).let {
            masterWrappers.add(it)
            return it
        }
    }

    /**
     * Push a new masterWrapper of type [CandHHistoryMasterWrapper] to the stack. Don't forget to call [popHistoryMasterWrapper] when you're done.
     * This masterWrapper will be removed if [popHistoryMasterWrapper] is called, and it has no attributes.
     */
    fun pushHistoryMasterWrapper(
        entity: BaseDO<*>,
        entityOpType: EntityOpType = EntityOpType.Update,
    ): CandHHistoryMasterWrapper {
        @Suppress("UNCHECKED_CAST")
        entity as IdObject<Long>
        CandHHistoryMasterWrapper.create(entity = entity, entityOpType = entityOpType).let {
            masterWrapperStack.add(it)
            masterWrappers.add(it)
            return it
        }
    }

    /**
     * Pop the last masterWrapper from the stack. Throws an exception if the stack is empty.
     * If the masterWrapper has no attributes, it will be removed from the masterWrappers list.
     */
    fun popHistoryMasterWrapper(): CandHHistoryMasterWrapper {
        if (currentMasterWrapper?.attributeWrappers.isNullOrEmpty()) {
            // If the masterWrapper has no attributes, we don't need to keep it.
            masterWrappers.remove(currentMasterWrapper)
        }
        return masterWrapperStack.removeAt(masterWrapperStack.size - 1)
    }

    private val currentMasterAttributes: MutableSet<CandHHistoryAttrWrapper>
        get() {
            if (currentMasterWrapper == null) {
                // Now we need to create the first masterWrapper entry.
                pushHistoryMasterWrapper(entity, entityOpType)
            }
            currentMasterWrapper!!.let { masterWrapper ->
                masterWrapper.attributeWrappers = masterWrapper.attributeWrappers ?: mutableSetOf()
                @Suppress("UNCHECKED_CAST")
                return masterWrapper.attributeWrappers!!
            }
        }

    /**
     * Add a new history entry for the given property context. The current masterWrapper will be used.
     */
    fun add(propertyContext: PropertyContext, optype: PropertyOpType) {
        propertyContext.apply {
            val propertyTypeClass = (property.returnType.classifier as KClass<*>).java
            add(
                propertyTypeClass = propertyTypeClass,
                optype = optype,
                oldValue = destPropertyValue,
                newValue = srcPropertyValue,
                propertyName = propertyName,
            )
        }
    }

    /**
     * Add a new history entry for the given property context. The current masterWrapper will be used.
     */
    fun add(
        propertyTypeClass: Class<*>,
        optype: PropertyOpType,
        oldValue: Any?,
        newValue: Any?,
        propertyName: String?,
    ) {
        currentMasterAttributes.add(
            CandHHistoryAttrWrapper.create(
                propertyTypeClass = propertyTypeClass,
                optype = optype,
                oldValue = oldValue,
                newValue = newValue,
                propertyName = propertyName,
                masterWrapper = currentMasterWrapper
            )
        )
    }
}
