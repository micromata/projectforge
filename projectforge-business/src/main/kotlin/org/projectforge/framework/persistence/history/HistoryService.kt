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

package org.projectforge.framework.persistence.history

import jakarta.persistence.EntityManager
import jakarta.persistence.OneToMany
import mu.KotlinLogging
import org.projectforge.common.ClassUtils
import org.projectforge.common.StringHelper2
import org.projectforge.framework.persistence.api.BaseDO
import org.projectforge.framework.persistence.jpa.PfPersistenceContext
import org.projectforge.framework.persistence.jpa.PfPersistenceService
import org.projectforge.framework.persistence.metamodel.HibernateMetaModel
import org.projectforge.framework.persistence.user.api.ThreadLocalUserContext
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Service
import java.io.Serializable
import java.util.*

private val log = KotlinLogging.logger {}

/**
 */
@Service
class HistoryService {
    @Autowired
    private lateinit var persistenceService: PfPersistenceService

    init {
        instance = this
    }

    /**
     * Loads all history entries for the given baseDO by class and id.
     */
    fun loadHistory(baseDO: BaseDO<Long>): List<HistoryEntryDO> {
        return persistenceService.runReadOnly { context ->
            loadHistory(baseDO, context)
        }
    }

    /**
     * Loads all history entries for the given baseDO by class and id.
     */
    fun loadHistory(baseDO: BaseDO<Long>, context: PfPersistenceContext? = null): List<HistoryEntryDO> {
        val allHistoryEntries = mutableListOf<HistoryEntryDO>()
        if (context != null) {
            loadAndAddHistory(allHistoryEntries, baseDO::class.java, baseDO.id, context)
        } else {
            persistenceService.runReadOnly { ctx ->
                loadAndAddHistory(allHistoryEntries, baseDO::class.java, baseDO.id, ctx)
            }
        }
        allHistoryEntries.forEach { entry -> HistoryEntryDOUtils.transformOldAttributes(entry) }
        return allHistoryEntries
    }

    private fun loadAndAddHistory(
        allHistoryEntries: MutableList<HistoryEntryDO>,
        entityClass: Class<out BaseDO<*>>,
        entityId: Long?,
        context: PfPersistenceContext,
    ) {
        entityId ?: return
        val result = context.executeQuery(
            sql = HistoryEntryDO.SELECT_HISTORY_FOR_BASEDO,
            resultClass = HistoryEntryDO::class.java,
            namedQuery = true,
            keyValues = arrayOf(
                Pair("entityId", entityId),
                Pair("entityName", entityClass.name),
            ),
        )
        allHistoryEntries.addAll(result)
        // Check all history entries for embedded objects:
        HibernateMetaModel.getEntityInfo(entityClass)?.getPropertiesWithAnnotation(OneToMany::class)
            ?.let { oneToManyProps ->
                // Check all history entries for embedded objects.
                // Key is the class type of the members, e.g. org.projectforge....OrderPositionDO of OrderDO. Values are all entity_ids found.
                // This is important, because some embedded objects may be removed in the meantime, so we have to look especially in oldValue for removed
                // entities.
                val embeddedObjectsMap = mutableMapOf<String, MutableSet<Long>>()
                // Check all result history entries for embedded objects:
                result.forEach { entry ->
                    entry.attributes?.forEach attributes@{ attr ->
                        attr.plainPropertyName?.let { propertyName ->
                            oneToManyProps.find { it.propertyName == propertyName } ?: return@attributes
                            attr.propertyTypeClass?.let { propertyTypeClass ->
                                // oneToMany.targetEntity not always given, using propertyName instead:
                                val entityIds = mutableSetOf<Long>()
                                // Ids are part of value if added to list, such as 1234,5678,9012
                                val ids1 = StringHelper2.splitToListOfLongValues(attr.value)
                                // Ids are part of oldValue if removed from list, such as 1234,5678,9012
                                val ids2 = StringHelper2.splitToListOfLongValues(attr.oldValue)
                                entityIds.addAll(ids1)
                                entityIds.addAll(ids2)
                                entityIds.forEach { entityId ->
                                    embeddedObjectsMap.computeIfAbsent(propertyTypeClass) { mutableSetOf() }
                                        .add(entityId)
                                }
                                log.debug { "${entityClass}.${entityId}: entity ids added: '$propertyTypeClass': ${entityIds.joinToString()}" }
                            }
                        }
                    }
                }
                context.em.find(entityClass, entityId)?.let { baseDO ->
                    // Check now all actually embedded objects of the baseDO, load from the database:
                    oneToManyProps.forEach { propInfo ->
                        val propertyName = propInfo.propertyName
                        ClassUtils.getFieldInfo(entityClass, propertyName)?.field?.let { field ->
                            synchronized(field) {
                                val wasAccessible = field.canAccess(baseDO)
                                try {
                                    field.isAccessible = true
                                    synchronized(field) {
                                        field[baseDO]?.let { value ->
                                            if (value is Collection<*>) {
                                                value.forEach { embeddedObject ->
                                                    if (embeddedObject is BaseDO<*>) {
                                                        embeddedObject.id?.let { entityId ->
                                                            entityId as Long
                                                            embeddedObjectsMap.computeIfAbsent(embeddedObject::class.java.name) { mutableSetOf() }
                                                                .add(entityId)
                                                            log.debug { "${baseDO::class.java}.${baseDO.id}: entity ids added: '${embeddedObject::class.java.name}': $entityId" }
                                                        }
                                                    }
                                                }
                                            }
                                        }
                                    }
                                } finally {
                                    field.isAccessible = wasAccessible
                                }
                            }
                        }
                    }
                }
                embeddedObjectsMap.forEach { (propertyTypeClass, entityIds) ->
                    entityIds.forEach { entityId ->
                        try {
                            @Suppress("UNCHECKED_CAST")
                            val clazz = Class.forName(propertyTypeClass) as Class<out BaseDO<*>>
                            loadAndAddHistory(allHistoryEntries, clazz, entityId, context)
                        } catch (ex: Exception) {
                            log.error(ex) { "Can't get class of name '$propertyTypeClass' (skipping): ${ex.message}" }
                        }
                    }
                }
            }

    }

    /**
     * Save method will be called automatically by the Dao services.
     */
    fun save(historyEntry: HistoryEntryDO, attrs: Collection<HistoryEntryAttrDO>? = null): Long? {
        persistenceService.runInTransaction { context ->
            val em = context.em
            save(em, historyEntry, attrs)
        }
        return historyEntry.id
    }

    /**
     * Save method will be called automatically by the Dao services.
     */
    fun save(em: EntityManager, historyEntry: HistoryEntryDO, attrs: Collection<HistoryEntryAttrDO>? = null): Long? {
        historyEntry.modifiedBy = ThreadLocalUserContext.loggedInUser?.id?.toString() ?: "anon"
        historyEntry.modifiedAt = Date()
        em.persist(historyEntry)
        log.info { "Saving history: $historyEntry" }
        attrs?.forEach { attr ->
            attr.parent = historyEntry
            em.persist(attr)
        }
        return historyEntry.id
    }

    companion object {
        @JvmStatic
        lateinit var instance: HistoryService
            private set
    }
}
