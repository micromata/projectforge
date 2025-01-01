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

package org.projectforge.framework.persistence.metamodel

import jakarta.persistence.Column
import mu.KotlinLogging
import org.hibernate.engine.spi.SessionFactoryImplementor
import org.hibernate.metamodel.spi.MetamodelImplementor
import org.hibernate.persister.entity.SingleTableEntityPersister
import org.projectforge.framework.persistence.api.BaseDO
import kotlin.reflect.KCallable
import kotlin.reflect.KMutableProperty1

private val log = KotlinLogging.logger {}

/**
 * @author Kai Reinhard (k.reinhard@micromata.de)
 */
object HibernateMetaModel {
    private lateinit var sessionFactory: SessionFactoryImplementor
    private lateinit var metamodel: MetamodelImplementor
    private val entityInfoByName = mutableMapOf<String, EntityInfo>()
    private val entityInfoByEntityClass = mutableMapOf<Class<*>, EntityInfo>()

    @JvmStatic
    fun getEntityInfo(entityName: String): EntityInfo? {
        return entityInfoByName[entityName]
    }

    @JvmStatic
    fun getEntityInfo(baseDO: BaseDO<*>): EntityInfo? {
        return getEntityInfo(baseDO::class.java)
    }

    @JvmStatic
    fun getEntityInfo(entityClass: Class<*>): EntityInfo? {
        return entityInfoByEntityClass[entityClass]
    }

    @JvmStatic
    fun isEntity(entity: Class<*>): Boolean {
        return entityInfoByEntityClass.containsKey(entity)
    }

    fun allEntityInfos(): List<EntityInfo> {
        return entityInfoByName.values.toList()
    }

    fun getIdProperty(entityClass: Class<*>): String? {
        return getEntityInfo(entityClass)?.getIdProperty()
    }

    fun isPersistedProperty(entityClass: Class<*>, property: KCallable<*>): Boolean {
        return getEntityInfo(entityClass)?.isPersistedProperty(property) == true
    }

    fun isPersistedProperty(entityClass: Class<*>, property: KMutableProperty1<*, *>): Boolean {
        return getEntityInfo(entityClass)?.isPersistedProperty(property) == true
    }

    @JvmStatic
    fun getPropertyLength(entityName: String, propertyName: String): Int? {
        return entityInfoByName[entityName]?.getColumnLength(propertyName)
    }

    @JvmStatic
    fun getPropertyLength(entityClass: Class<*>, propertyName: String): Int? {
        return entityInfoByEntityClass[entityClass]?.getColumnLength(propertyName)
    }

    @JvmStatic
    fun getColumnInfo(entityName: String, propertyName: String): Column? {
        return entityInfoByName[entityName]?.getColumnAnnotation(propertyName)
    }


    /** Called by HibernateUtils. */
    fun internalInit(sessionFactory: SessionFactoryImplementor) {
        this.sessionFactory = sessionFactory
        val metamodel = sessionFactory.metamodel as MetamodelImplementor
        this.metamodel = metamodel
        // Register all entities:
        metamodel.entities.forEach { entityType ->
            val entityClass = entityType.javaType
            val entityPersister = metamodel.entityPersister(entityClass)
            val tableName = if (entityPersister is SingleTableEntityPersister) {
                entityPersister.tableName
            } else {
                null
            }
            val info = EntityInfo(entityClass, name = entityType.name, entityType = entityType, tableName = tableName)
            log.debug {
                "Register entity: ${entityType.name} (${entityClass.simpleName}) with table name: $tableName: info=$info"
            }
            entityInfoByName[entityType.name] = info
            entityInfoByName[entityClass.name] = info
            entityInfoByEntityClass[entityType.javaType] = info
        }
    }
}
