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
import org.projectforge.framework.persistence.api.impl.EntityManagerUtil
import org.projectforge.framework.persistence.jpa.PfPersistenceService
import org.projectforge.framework.persistence.metamodel.HibernateMetaModel
import org.projectforge.framework.persistence.user.api.ThreadLocalUserContext
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Service
import java.util.*

private val log = KotlinLogging.logger {}

/**
 */
@Service
class HistoryService {
    @Autowired
    private lateinit var persistenceService: PfPersistenceService

    /**
     * Loads all history entries for the given baseDO by class and id.
     */
    fun loadHistory(baseDO: BaseDO<Long>): List<PfHistoryMasterDO> {
        val allHistoryEntries = mutableListOf<PfHistoryMasterDO>()
        persistenceService.runReadOnly { context ->
            val em = context.em
            loadAndAddHistory(em, allHistoryEntries, baseDO::class.java, baseDO.id)
        }
        return allHistoryEntries
    }

    private fun loadAndAddHistory(
        em: EntityManager,
        allHistoryEntries: MutableList<PfHistoryMasterDO>,
        entityClass: Class<out BaseDO<Long>>,
        entityId: Long?
    ) {
        entityId ?: return
        val result = EntityManagerUtil.query(
            em = em,
            sql = PfHistoryMasterDO.SELECT_HISTORY_FOR_BASEDO,
            resultClass = PfHistoryMasterDO::class.java,
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
                val embeddedObjectsMap = mutableMapOf<String, MutableSet<Long>>()
                // Check all result history entries for embedded objects:
                result.forEach { master ->
                    master.attributes?.forEach { attr ->
                        attr.plainPropertyName?.let { propertyName ->
                            oneToManyProps.find { it.propertyName == propertyName } ?: return@forEach
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
                em.find(entityClass, entityId)?.let { baseDO ->
                    // Check now all embedded objects of the baseDO:
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
                                                        embeddedObject as BaseDO<Long>
                                                        embeddedObject.id?.let { entityId ->
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
                            val clazz = Class.forName(propertyTypeClass) as Class<out BaseDO<Long>>
                            loadAndAddHistory(em, allHistoryEntries, clazz, entityId)
                        } catch (ex: Exception) {
                            log.error(ex) { "Can't get class of name '$propertyTypeClass' (skipping): ${ex.message}" }
                        }
                    }
                }
                println("******* Load also embedded objects, embedded by embedded objects (recursive)")
            }

    }

    /**
     * Save method will be called automatically by the Dao services.
     */
    fun save(master: PfHistoryMasterDO, attrs: Collection<PfHistoryAttrDO>? = null): Long? {
        persistenceService.runInTransaction { context ->
            val em = context.em
            save(em, master, attrs)
        }
        return master.id
    }

    /**
     * Save method will be called automatically by the Dao services.
     */
    fun save(em: EntityManager, master: PfHistoryMasterDO, attrs: Collection<PfHistoryAttrDO>? = null): Long? {
        master.modifiedBy = ThreadLocalUserContext.user?.id?.toString() ?: "anon"
        master.modifiedAt = Date()
        em.persist(master)
        log.info { "Saving history: $master" }
        attrs?.forEach { attr ->
            attr.master = master
            em.persist(attr)
        }
        return master.id
    }
}
