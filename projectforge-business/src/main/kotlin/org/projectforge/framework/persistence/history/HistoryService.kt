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

import jakarta.persistence.OneToMany
import mu.KotlinLogging
import org.projectforge.common.AnnotationsUtils
import org.projectforge.common.StringHelper2
import org.projectforge.framework.persistence.api.BaseDO
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
        val result = persistenceService.namedQuery(
            PfHistoryMasterDO.SELECT_HISTORY_FOR_BASEDO,
            PfHistoryMasterDO::class.java,
            Pair("entityId", baseDO.id),
            Pair("entityName", baseDO::class.java.name),
        ).toMutableList()
        HibernateMetaModel.getEntityInfo(baseDO)?.getPropertiesWithAnnotation(OneToMany::class)
            ?.filter { !it.hasAnnotation(NoHistory::class) }?.let { oneToManyProps ->
            // Check all history entries for embedded objects.
            // Key is the class type of the members, e.g. org.projectforge....OrderPositionDO of OrderDO. Values are all entity_ids found.
            val embeddedObjectsMap = mutableMapOf<String, MutableSet<Long>>()
            result.forEach { master ->
                master.attributes?.forEach { attr ->
                    attr.plainPropertyName?.let { propertyName ->
                        val propertyInfo = oneToManyProps.find { it.propertyName == propertyName } ?: return@forEach
                        attr.propertyTypeClass?.let { propertyTypeClass ->
                            // oneToMany.targetEntity not always given, using propertyName instead:
                            if (propertyInfo.propertyName == propertyName ||
                                !AnnotationsUtils.hasAnnotation(
                                    baseDO::class.java,
                                    propertyName,
                                    NoHistory::class.java,
                                )
                            ) {
                                log.info { "****** $propertyName" }
                                val entityIds = mutableSetOf<Long>()
                                val ids1 = StringHelper2.splitToListOfLongValues(attr.value)
                                val ids2 = StringHelper2.splitToListOfLongValues(attr.oldValue)
                                entityIds.addAll(ids1)
                                entityIds.addAll(ids2)
                                entityIds.forEach { entityId ->
                                    embeddedObjectsMap.computeIfAbsent(propertyTypeClass) { mutableSetOf() }
                                        .add(entityId)
                                }
                            }
                        }
                    }
                }
            }
            embeddedObjectsMap.forEach { (propertyTypeClass, entityIds) ->
                entityIds.forEach { entityId ->
                    val historyEntries = persistenceService.namedQuery(
                        PfHistoryMasterDO.SELECT_HISTORY_FOR_BASEDO,
                        PfHistoryMasterDO::class.java,
                        Pair("entityId", entityId),
                        Pair("entityName", propertyTypeClass),
                    )
                    result.addAll(historyEntries)
                }
            }
        }
        return result
    }

    /**
     * Save method will be called automatically by the Dao services.
     */
    fun save(master: PfHistoryMasterDO, attrs: Collection<PfHistoryAttrDO>? = null): Long? {
        persistenceService.runInTransaction { context ->
            val em = context.em
            master.modifiedBy = ThreadLocalUserContext.user?.id?.toString() ?: "anon"
            master.modifiedAt = Date()
            em.persist(master)
            log.info { "Saving history: $master" }
            attrs?.forEach { attr ->
                attr.master = master
                em.persist(attr)
            }
        }
        return master.id
    }
}
