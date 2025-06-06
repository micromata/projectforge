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

package org.projectforge.framework.persistence.search

import jakarta.annotation.PostConstruct
import jakarta.persistence.EntityManager
import jakarta.persistence.FlushModeType
import mu.KotlinLogging
import org.hibernate.search.mapper.orm.Search
import org.hibernate.search.mapper.pojo.mapping.definition.annotation.IndexedEmbedded
import org.projectforge.framework.persistence.api.BaseDO
import org.projectforge.framework.persistence.jpa.PfPersistenceService
import org.projectforge.registry.Registry
import org.projectforge.registry.RegistryEntry
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Component
import java.lang.reflect.ParameterizedType

private val log = KotlinLogging.logger {}

/**
 * Hotfix: Hibernate-search does not update index of dependent objects.
 *
 * @author Kai Reinhard (k.reinhard@micromata.de)
 */
@Component
class HibernateSearchDependentObjectsReindexer {
    @Autowired
    private lateinit var persistenceService: PfPersistenceService

    /**
     * Key is the embedded class (annotated with @IndexEmbedded), value the set of all dependent objects.
     */
    @JvmField
    val map = mutableMapOf<Class<out BaseDO<*>>, MutableList<Entry>>()

    @PostConstruct
    fun init() {
        for (registryEntry in Registry.getInstance().orderedList) {
            register(registryEntry)
        }
    }

    inner class Entry(// The dependent class which contains the annotated field.
        @JvmField var clazz: Class<out BaseDO<*>>?, @JvmField var fieldName: String?, var setOrCollection: Boolean
    ) {
        /**
         * @see java.lang.Object.toString
         */
        override fun toString(): String {
            return "Entry[clazz=" + clazz!!.name + ",fieldName=" + fieldName + "]"
        }

        /**
         * @see java.lang.Object.equals
         */
        override fun equals(other: Any?): Boolean {
            if (other !is Entry) {
                return false
            }
            val o = other
            return clazz == o.clazz && fieldName == o.fieldName
        }

        override fun hashCode(): Int {
            val prime = 31
            var result = 1
            result = prime * result + (if ((clazz == null)) 0 else clazz.hashCode())
            result = prime * result + (if ((fieldName == null)) 0 else fieldName.hashCode())
            result = prime * result + (if (setOrCollection) 1231 else 1237)
            return result
        }
    }

    fun reindexDependents(obj: BaseDO<*>) {
        object : Thread() {
            override fun run() {
                persistenceService.runInTransaction() { context ->
                    val em = context.em
                    em.flushMode = FlushModeType.AUTO
                    val alreadyReindexed = mutableSetOf<String>()
                    val entryList = map[obj.javaClass]
                    reindexDependents(em, obj, entryList, alreadyReindexed)
                    val size = alreadyReindexed.size
                    if (size >= 10) {
                        log.info(
                            ("Re-indexing of " + size + " objects done after updating " + obj.javaClass.name + ":"
                                    + obj.id)
                        )
                    }
                    null
                }
            }
        }.start()
    }

    private fun reindexDependents(
        em: EntityManager, obj: BaseDO<*>,
        entryList: List<Entry>?, alreadyReindexed: MutableSet<String>
    ) {
        if (entryList.isNullOrEmpty()) {
            // Nothing to do.
            return
        }
        for (entry in entryList) {
            val registryEntry = Registry.getInstance().getEntryByDO(entry.clazz)
                ?: // Nothing to do
                return
            val result = getDependents(em, registryEntry, entry, obj)
            for (dependentObject in result) {
                var useObj = dependentObject
                if (useObj is Array<*> && useObj.isArrayOf<Any>()) {
                    @Suppress("UNCHECKED_CAST")
                    useObj = (useObj as Array<Any>)[0]
                }
                if (useObj is BaseDO<*>) {
                    reindexDependents(em, useObj, alreadyReindexed)
                }
            }
        }
    }

    private fun reindexDependents(
        em: EntityManager, obj: BaseDO<*>,
        alreadyReindexed: MutableSet<String>
    ) {
        if (alreadyReindexed.contains(getReindexId(obj))) {
            if (log.isDebugEnabled) {
                log.debug("Object already re-indexed (skipping): " + getReindexId(obj))
            }
            return
        }
        em.flush() // Needed to flush the object changes!
        val searchSession = Search.session(em)

        //HibernateCompatUtils.setCacheMode(fullTextSession, CacheMode.IGNORE);
        try {
            var dbObj = em.find(obj.javaClass, obj.id)
            if (dbObj == null) {
                dbObj = em.find(obj.javaClass, obj.id)
            }
            searchSession.indexingPlan().addOrUpdate(dbObj)
            alreadyReindexed.add(getReindexId(dbObj))
            if (log.isDebugEnabled) {
                log.debug("Object added to index: " + getReindexId(dbObj))
            }
        } catch (ex: Exception) {
            // Don't fail if any exception while re-indexing occurs.
            log.info("Fail to re-index " + obj.javaClass + ": " + ex.message)
        }
        // em.flush(); // clear every batchSize since the queue is processed
        val entryList =
            map[obj.javaClass]!!
        reindexDependents(em, obj, entryList, alreadyReindexed)
    }

    private fun getDependents(
        em: EntityManager,
        registryEntry: RegistryEntry,
        entry: Entry,
        obj: BaseDO<*>
    ): List<*> {
        val queryString = if (entry.setOrCollection) {
            "from " + registryEntry.doClass.name + " o join o." + entry.fieldName + " r where r.id=:id"
        } else {
            "from " + registryEntry.doClass.name + " o where o." + entry.fieldName + ".id=:id"
        }
        if (log.isDebugEnabled) {
            log.debug(queryString + ", id=" + obj.id)
        }
        val result: List<*> = em.createQuery(queryString, registryEntry.doClass)
            .setParameter("id", obj.id)
            .resultList
        return result
    }

    private fun getReindexId(obj: BaseDO<*>): String {
        return obj.javaClass.toString() + ":" + obj.id
    }

    private fun register(registryEntry: RegistryEntry) {
        val clazz = registryEntry.doClass
        register(clazz)
    }

    fun register(clazz: Class<out BaseDO<*>?>) {
        val fields = clazz.declaredFields
        for (field in fields) {
            if (field.isAnnotationPresent(IndexedEmbedded::class.java)) {
                //    field.isAnnotationPresent(ContainedIn.class)) {
                var embeddedClass = field.type
                var setOrCollection = false
                if (MutableSet::class.java.isAssignableFrom(embeddedClass)
                    || MutableCollection::class.java.isAssignableFrom(embeddedClass)
                ) {
                    // Please use @ContainedIn.
                    val type = field.genericType
                    if (type is ParameterizedType) {
                        val actualTypeArgument = type.actualTypeArguments[0]
                        if (actualTypeArgument is Class<*>) {
                            embeddedClass = actualTypeArgument
                            setOrCollection = true
                        }
                    }
                }
                if (!BaseDO::class.java.isAssignableFrom(embeddedClass)) {
                    // Only BaseDO objects are supported.
                    continue
                }
                val name = field.name
                val entry = Entry(clazz, name, setOrCollection)
                var list = map[embeddedClass]
                if (list == null) {
                    list = ArrayList()
                    @Suppress("UNCHECKED_CAST")
                    val embeddedBaseDOClass = embeddedClass as Class<out BaseDO<*>>
                    map[embeddedBaseDOClass] = list
                } else {
                    for (e in list) {
                        if (entry == e) {
                            log.warn("Entry already registered: $entry")
                        }
                    }
                }
                list.add(entry)
            }
        }
    }
}
