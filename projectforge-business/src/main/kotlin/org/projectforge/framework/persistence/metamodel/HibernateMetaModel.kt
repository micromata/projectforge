package org.projectforge.framework.persistence.metamodel

import mu.KotlinLogging
import org.hibernate.engine.spi.SessionFactoryImplementor
import org.hibernate.metamodel.spi.MetamodelImplementor
import org.hibernate.persister.entity.SingleTableEntityPersister

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
    fun getEntityInfo(entityClass: Class<*>): EntityInfo? {
        return entityInfoByEntityClass[entityClass]
    }

    @JvmStatic
    fun isEntity(entity: Class<*>): Boolean {
        return entityInfoByEntityClass.containsKey(entity)
    }

    @JvmStatic
    fun getPropertyLength(entityName: String, propertyName: String): Int? {
        return entityInfoByName[entityName]?.getColumnLength(propertyName)
    }

    @JvmStatic
    fun getPropertyLength(entityClass: Class<*>, propertyName: String): Int? {
        return entityInfoByEntityClass[entityClass]?.getColumnLength(propertyName)
    }

    /** Called by HibernateUtils. */
    fun internalInit(sessionFactory: SessionFactoryImplementor, metamodel: MetamodelImplementor) {
        this.sessionFactory
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
            entityInfoByName[entityType.name] = info
            entityInfoByEntityClass[entityType.javaType] = info
        }
    }

}
