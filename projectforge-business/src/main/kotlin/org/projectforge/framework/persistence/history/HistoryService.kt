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

package org.projectforge.framework.persistence.history

import jakarta.persistence.EntityManager
import jakarta.persistence.OneToMany
import mu.KotlinLogging
import org.projectforge.common.ClassUtils
import org.projectforge.common.StringHelper2
import org.projectforge.framework.access.AccessChecker
import org.projectforge.framework.persistence.api.BaseDO
import org.projectforge.framework.persistence.api.BaseDao
import org.projectforge.framework.persistence.api.ExtendedBaseDO
import org.projectforge.framework.persistence.api.IdObject
import org.projectforge.framework.persistence.jpa.PfPersistenceContext
import org.projectforge.framework.persistence.jpa.PfPersistenceService
import org.projectforge.framework.persistence.metamodel.HibernateMetaModel
import org.projectforge.framework.persistence.user.api.ThreadLocalUserContext.loggedInUser
import org.projectforge.framework.persistence.user.api.ThreadLocalUserContext.requiredLoggedInUser
import org.projectforge.framework.persistence.user.api.ThreadLocalUserContext.requiredLoggedInUserId
import org.projectforge.framework.persistence.user.entities.PFUserDO
import org.projectforge.framework.time.PFDateTime
import org.projectforge.registry.Registry
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Service
import java.io.Serializable
import java.util.*

private val log = KotlinLogging.logger {}

/**
 */
@Service
class HistoryService {
    class EntryInfo {
        var entry: HistoryEntryDO? = null
        var entityClass: Class<out BaseDO<*>>? = null
        var baseDao: BaseDao<*>? = null
        var entity: BaseDO<*>? = null
        var readAccess: Boolean = false
        var writeAccess: Boolean = false
    }

    @Autowired
    protected lateinit var accessChecker: AccessChecker

    @Autowired
    private lateinit var persistenceService: PfPersistenceService

    init {
        instance = this
    }

    /**
     * Merges the given entries into the list. Already existing entries with same id are not added twice.
     */
    fun mergeHistoryEntries(loadContext: HistoryLoadContext, entries: List<HistoryEntryDO>) {
        loadContext.merge(entries)
    }

    /**
     * Loads a history entry by id. If checkAccess is true, the access rights of the logged-in user are checked.
     * The access check is done by [BaseDao.checkLoggedInUserSelectAccess].
     * The entity is loaded by the [BaseDao.find] method (with checkAccess). If the entity is not found, an exception is thrown.
     * @param id The id of the history entry.
     * @param checkAccess If true, the access rights of the logged-in user are checked.
     * @return The entry and the entity.
     */
    fun findEntryAndEntityById(id: Serializable?, checkAccess: Boolean = true): EntryInfo? {
        id ?: return null
        val info = EntryInfo()
        persistenceService.runReadOnly { context ->
            val hist = context.find(HistoryEntryDO::class.java, id, attached = false).also {
                info.entry = it
            }
            if (hist == null) {
                log.error { "Can't load object of type HistoryEntryDO. Object with given id #$id not found." }
                throw IllegalArgumentException("Can't load object of type HistoryEntryDO.")
            }
            val entityClass = ClassUtils.forNameOrNull(hist.entityName)
                ?: throw IllegalArgumentException("Can't load object of type HistoryEntryDO.")
            try {
                @Suppress("UNCHECKED_CAST")
                entityClass as Class<out BaseDO<*>>
            } catch (ex: ClassCastException) {
                log.error(ex) { "Can't cast entityClass to BaseDO: ${entityClass.name}" }
                throw IllegalArgumentException("Can't load object of type HistoryEntryDO.")
            }
            info.entityClass = entityClass
            val baseDao = Registry.instance.getEntryByDO(entityClass)?.dao!!.also {
                info.baseDao = it
            }
            info.readAccess = baseDao.hasLoggedInUserHistoryAccess(checkAccess)
            if (checkAccess) {
                baseDao.checkLoggedInUserSelectAccess()
            }
            info.entity = baseDao.find(hist.entityId, checkAccess = checkAccess)?.also {
                try {
                    val method = baseDao::class.java.getMethod(
                        "hasUpdateAccess",
                        PFUserDO::class.java,
                        ExtendedBaseDO::class.java,
                        ExtendedBaseDO::class.java,
                        Boolean::class.java
                    )
                    info.writeAccess = method.invoke(baseDao, requiredLoggedInUser, it, null, false) as Boolean
                } catch (ex: Exception) {
                    log.error(ex) { "Can't check write access for entity: ${entityClass.name}: ${ex.message}" }
                }
            }
        }
        return info
    }

    /**
     * Appends the given user comment to the history entry with the given id.
     * If checkAccess is true, the access rights of the logged-in user are checked.
     * The access check is done by [BaseDao.checkLoggedInUserSelectAccess].
     * @param id The id of the history entry.
     * @param userComment The user comment to append.
     * @param checkAccess If true, the access rights of the logged-in user are checked.
     * @return The entry info.
     */
    fun appendUserComment(id: Serializable?, userComment: String?, checkAccess: Boolean = true): EntryInfo? {
        id ?: return null
        if (userComment.isNullOrBlank()) {
            return null
        }
        val info = findEntryAndEntityById(id, checkAccess) ?: return null
        log.info { "Appending user comment to history entry #$id of entity ${info.entity?.javaClass?.name}#${info.entity?.id}: $userComment" }
        persistenceService.runInTransaction { context ->
            val entry = info.entry!!
            val loggedInUserId = requiredLoggedInUserId
            val userString = if (entry.modifiedBy != loggedInUserId.toString()) {
                " (${requiredLoggedInUser.username})"
            } else {
                ""
            }
            entry.userComment = "${entry.userComment}\n${PFDateTime.now().isoString}Z$userString: $userComment"
            context.em.merge(entry)
        }
        return info
    }

    /**
     * Loads all history entries for the given baseDO by class and id.
     * Please note: Embedded objects are only loaded, if they're part of any history entry attribute of the given object.
     * Please use [BaseDao.loadHistory] for getting all embedded history entries.
     */
    fun loadHistory(baseDO: BaseDO<Long>, baseDao: BaseDao<*>? = null): HistoryLoadContext {
        val loadContext = HistoryLoadContext(baseDao)
        persistenceService.runReadOnly { context ->
            loadAndMergeHistory(baseDO::class.java, baseDO.id, loadContext, context)
        }
        return loadContext
    }

    /**
     * Loads all history entries for the given baseDO by class and id.
     * Please note: Embedded objects are only loaded, if they're part of any history entry attribute of the given object.
     * Please use [BaseDao.loadHistory] for getting all embedded history entries.
     */
    fun loadAndMergeHistory(
        baseDO: BaseDO<Long>, loadContext: HistoryLoadContext,
        customize: ((entry: HistoryEntryDO) -> Unit)? = null,
    ): HistoryLoadContext {
        persistenceService.runReadOnly { context ->
            loadAndMergeHistory(baseDO::class.java, baseDO.id, loadContext, context, customize)
        }
        return loadContext
    }


    /**
     * Loads all history entries for the given entity by class and id's.
     * Please note: Embedded objects are only loaded, if they're part of any history entry attribute of the given object.
     * Please use [BaseDao.loadHistory] for getting all embedded history entries.
     * @param loadContext contains already loaded history entries. The new one will be merged into this list.
     * @param entityClass The class of the entity.
     * @param entityIds The id's of the entities.
     */
    fun loadAndMergeHistory(
        entityClass: Class<out IdObject<Long>>,
        entityIds: Collection<Long>,
        loadContext: HistoryLoadContext,
    ) {
        persistenceService.runReadOnly { context ->
            loadAndMergeHistory(entityClass, entityIds, context, loadContext)
        }
    }

    /**
     * Convention: If you want to create a history entry of collections, the oldValue should contain all elements that are removed and the newValue should contain all elements that are added.
     * @param oldValue Supports all types supported by [HistoryValueHandlerRegistry]. Also, collections of objects are supported and will be serialized to a csv string.
     * @param newValue Supports all types supported by [HistoryValueHandlerRegistry]. Also, collections of objects are supported and will be serialized to a csv string.
     * @see HistoryBaseDaoAdapter.insertHistoryUpdateEntryWithSingleAttribute
     */
    fun insertUpdateHistoryEntry(
        entity: IdObject<Long>,
        property: String?,
        propertyTypeClass: Class<*>,
        oldValue: Any?,
        newValue: Any?,
    ) {
        accessChecker.checkRestrictedOrDemoUser()
        val contextUser = loggedInUser
        val userPk = contextUser?.id?.toString()
        if (userPk == null) {
            log.warn("No user found for creating history entry.")
        }
        persistenceService.runInTransaction { context ->
            HistoryBaseDaoAdapter.insertHistoryUpdateEntryWithSingleAttribute(
                entity = entity,
                propertyName = property,
                propertyTypeClass = propertyTypeClass,
                oldValue = oldValue,
                newValue = newValue,
                context,
            )
        }
    }

    /**
     * Forces to delete this history entry and all attributes. Used by force deletion of entities.
     */
    fun forceDeletion(historyEntry: HistoryEntryDO) {
        persistenceService.runInTransaction { context ->
            val attrs = context.executeNamedUpdate(
                HistoryEntryAttrDO.DELETE_HISTORY_ENTRY_ATTR_BY_PARENT_ID,
                "parentId" to historyEntry.id,
            )
            context.executeNamedUpdate(HistoryEntryDO.DELETE_HISTORY_ENTRY, "id" to historyEntry.id)
            log.info { "forceDeletion of history-entry for ${historyEntry.entityName}.${historyEntry.entityId}, number of deleted attrs=$attrs" }
        }

    }

    private fun loadAndMergeHistory(
        entityClass: Class<out IdObject<Long>>,
        entityIds: Collection<Long>,
        context: PfPersistenceContext,
        loadContext: HistoryLoadContext,
        customize: ((entry: HistoryEntryDO) -> Unit)? = null,
    ) {
        val newHistoryEntries = context.executeNamedQuery(
            namedQuery = HistoryEntryDO.SELECT_HISTORY_BY_ENTITY_IDS,
            resultClass = HistoryEntryDO::class.java,
            keyValues = arrayOf(
                Pair("entityIds", entityIds),
                Pair("entityName", entityClass.name),
            ),
        )
        processAndMergeHistory(entityClass, entityIds, newHistoryEntries, loadContext, context, customize)
    }

    private fun loadAndMergeHistory(
        entityClass: Class<out BaseDO<Long>>,
        entityId: Long?,
        loadContext: HistoryLoadContext,
        context: PfPersistenceContext,
        customize: ((entry: HistoryEntryDO) -> Unit)? = null,
    ) {
        entityId ?: return
        val newHistoryEntries = context.executeNamedQuery(
            namedQuery = HistoryEntryDO.SELECT_HISTORY_FOR_BASEDO,
            resultClass = HistoryEntryDO::class.java,
            keyValues = arrayOf(
                Pair("entityId", entityId),
                Pair("entityName", entityClass.name),
            ),
        )
        processAndMergeHistory(entityClass, listOf(entityId), newHistoryEntries, loadContext, context, customize)
    }

    private fun processAndMergeHistory(
        entityClass: Class<out IdObject<Long>>,
        entityIds: Collection<Long>,
        newHistoryEntries: List<HistoryEntryDO>,
        loadContext: HistoryLoadContext,
        context: PfPersistenceContext,
        customize: ((entry: HistoryEntryDO) -> Unit)? = null,
    ) {
        newHistoryEntries.forEach { entry ->
            customize?.invoke(entry)
            HistoryOldFormatConverter.transformOldAttributes(entry)
            loadContext.setCurrent(entry)
        }
        mergeHistoryEntries(loadContext, newHistoryEntries)
        // Check all history entries for embedded objects:
        HibernateMetaModel.getEntityInfo(entityClass)?.getPropertiesWithAnnotation(OneToMany::class)
            ?.let { oneToManyProps ->
                // Check all history entries for embedded objects.
                // Key is the class type of the members, e.g. org.projectforge....OrderPositionDO of OrderDO. Values are all entity_ids found.
                // This is important, because some embedded objects may be removed in the meantime, so we have to look especially in oldValue for removed
                // entities.
                val embeddedObjectsMap = mutableMapOf<String, MutableSet<Long>>()
                // Check all result history entries for embedded objects:
                newHistoryEntries.forEach { entry ->
                    entry.attributes?.forEach attributes@{ attr ->
                        attr.propertyName?.let { propertyName ->
                            oneToManyProps.find { it.propertyName == propertyName } ?: return@attributes
                            attr.propertyTypeClass?.let { propertyTypeClass ->
                                // oneToMany.targetEntity not always given, using propertyName instead:
                                val setOfIds = mutableSetOf<Long>()
                                // Ids are part of value if added to list, such as 1234,5678,9012
                                val ids1 = StringHelper2.splitToListOfLongValues(attr.value)
                                // Ids are part of oldValue if removed from list, such as 1234,5678,9012
                                val ids2 = StringHelper2.splitToListOfLongValues(attr.oldValue)
                                setOfIds.addAll(ids1)
                                setOfIds.addAll(ids2)
                                setOfIds.forEach { entityId ->
                                    embeddedObjectsMap.computeIfAbsent(propertyTypeClass) { mutableSetOf() }
                                        .add(entityId)
                                }
                                log.debug { "${entityClass}: entity ids added: '$propertyTypeClass': ${setOfIds.joinToString()}" }
                            }
                        }
                    }
                }
                context.executeQuery(
                    "from ${entityClass.simpleName} where ${HibernateMetaModel.getIdProperty(entityClass)} in :entityIds",
                    entityClass,
                    "entityIds" to entityIds
                ).forEach { baseDO ->
                    loadContext.addLoadedEntity(baseDO)
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
                    @Suppress("UNCHECKED_CAST")
                    val clazz = Class.forName(propertyTypeClass) as Class<out BaseDO<Long>>
                    try {
                        loadAndMergeHistory(clazz, entityIds, context, loadContext)
                    } catch (ex: Exception) {
                        log.error(ex) { "Can't get class of name '$propertyTypeClass' (skipping): ${ex.message}" }
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
    fun save(
        em: EntityManager,
        historyEntry: HistoryEntryDO,
        attrs: Collection<HistoryEntryAttrDO>? = null
    ): Long? {
        historyEntry.modifiedBy = loggedInUser?.id?.toString() ?: "anon"
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
