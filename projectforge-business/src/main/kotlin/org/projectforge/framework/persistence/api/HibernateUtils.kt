/////////////////////////////////////////////////////////////////////////////
//
// Project ProjectForge Community Edition
//         www.projectforge.org
//
// Copyright (C) 2001-2026 Micromata GmbH, Germany (www.micromata.com)
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

package org.projectforge.framework.persistence.api

import jakarta.persistence.*
import mu.KotlinLogging
import org.hibernate.Hibernate
import org.hibernate.dialect.HSQLDialect
import org.hibernate.engine.spi.SessionFactoryImplementor
import org.hibernate.proxy.HibernateProxy
import org.projectforge.business.fibu.KundeDO
import org.projectforge.business.fibu.kost.Kost2ArtDO
import org.projectforge.common.BeanHelper
import org.projectforge.common.DatabaseDialect
import org.projectforge.framework.access.AccessEntryDO
import org.projectforge.framework.persistence.entities.DefaultBaseDO
import org.projectforge.framework.persistence.history.HistoryValueService
import org.projectforge.framework.persistence.metamodel.HibernateMetaModel
import org.projectforge.framework.persistence.metamodel.HibernateMetaModel.getEntityInfo
import org.projectforge.framework.persistence.user.entities.UserPrefEntryDO
import java.io.Serializable
import kotlin.reflect.KCallable
import kotlin.reflect.KClass
import kotlin.reflect.KMutableProperty1


private val log = KotlinLogging.logger {}

/**
 * Singleton holding the hibernate configuration. Should be configured by a servlet on initialization after hibernate
 * initialization.
 *
 * @author Kai Reinhard (k.reinhard@micromata.de)
 */
object HibernateUtils {
    // key is entity name, value is ent.
    //private val entityClassMapByName = mutableMapOf<String, EntityType<*>>()

    @JvmStatic
    lateinit var databaseDialect: DatabaseDialect
        private set

    fun getRealClass(obj: Any): Class<*> {
        if (obj is Class<*>) {
            throw UnsupportedOperationException("Shouldn't be used for classes.")
        }
        return if (obj is HibernateProxy) {
            // Proxy auflösen und die eigentliche Klasse bekommen
            obj.hibernateLazyInitializer.persistentClass
        } else {
            // Kein Proxy, daher einfach die Klasse des Objekts zurückgeben
            obj.javaClass
        }
    }

    /**
     * Removes any suffixes from the class name, such as $HibernateProxy.
     * @see HistoryValueService.getUnifiedTypeName
     */
    fun getUnifiedClassname(cls: Class<*>): String {
        return HistoryValueService.getUnifiedTypeName(cls.name)
    }

    /**
     * Removes any suffixes from the class name, such as $HibernateProxy.
     * @see HistoryValueService.getUnifiedTypeName
     */
    fun getUnifiedClassname(className: String?): String {
        return HistoryValueService.getUnifiedTypeName(className)
    }

    /**
     * Called by [PfPersistenceService] after construction.
     */
    fun internalInit(entityManagerFactory: EntityManagerFactory) {
        val sessionFactoryImplementor = entityManagerFactory.unwrap(
            SessionFactoryImplementor::class.java
        )
        val jdbcEnvironment = sessionFactoryImplementor.jdbcServices.jdbcEnvironment
        val dialect = jdbcEnvironment.dialect
        if (dialect is org.hibernate.dialect.PostgreSQLDialect) {
            databaseDialect = DatabaseDialect.PostgreSQL;
        } else if (dialect is HSQLDialect) {
            databaseDialect = DatabaseDialect.HSQL;
        } else {
            log.warn("Unknown or unsupported dialect: " + dialect.javaClass.name);
            databaseDialect = DatabaseDialect.PostgreSQL;
        }
        HibernateMetaModel.internalInit(sessionFactoryImplementor)
    }

    /**
     * Checks if the given object is loaded. If not, the object is not fully initialized and can't be used.
     * Uses Hibernate.isInitialized(obj) and Hibernate.isInitialized(obj.hibernateLazyInitializer.implementation) for lazy proxies.
     * @param obj
     */
    fun isFullyInitialized(obj: Any?): Boolean {
        obj ?: return false
        if (!Hibernate.isInitialized(obj)) {
            return false
        }
        // Lazy-Proxy?
        if (obj is HibernateProxy) {
            return Hibernate.isInitialized(obj.hibernateLazyInitializer.implementation)
        }
        return true
    }

    fun isEntity(entity: Class<*>): Boolean {
        return HibernateMetaModel.isEntity(entity)
    }

    fun isPersistedProperty(entityClass: Class<*>, property: KCallable<*>): Boolean {
        return getEntityInfo(entityClass)?.isPersistedProperty(property) == true
    }

    fun isPersistedProperty(entityClass: Class<*>, property: KMutableProperty1<*, *>): Boolean {
        return getEntityInfo(entityClass)?.isPersistedProperty(property) == true
    }

    fun isPersistedProperty(entityClass: KClass<*>, property: String): Boolean {
        return getEntityInfo(entityClass.java)?.isPersistedProperty(property) == true
    }

    fun isPersistedProperty(entityClass: Class<*>, property: String): Boolean {
        return getEntityInfo(entityClass)?.isPersistedProperty(property) == true
    }

    private var TEST_MODE = false

    /**
     * For internal test cases only! If true, log errors are suppressed. Please call [.exitTestMode] always
     * directly after your test call!
     */
    @JvmStatic
    fun enterTestMode() {
        TEST_MODE = true
        log.info("***** Entering TESTMODE.")
    }

    /**
     * For internal test cases only! If true, log errors are suppressed. Please set TEST_MODE always to false after your
     * test call!
     */
    @JvmStatic
    fun exitTestMode() {
        TEST_MODE = false
        log.info("***** Exit TESTMODE.")
    }

    /**
     * Workaround for: http://opensource.atlassian.com/projects/hibernate/browse/HHH-3502:
     *
     * @param obj
     * @return
     */
    @JvmStatic
    fun getIdentifier(obj: BaseDO<*>): Serializable? {
        return obj.id
        /*        if (Hibernate.isInitialized(obj)) {
                    return obj.id
                } else if (obj is DefaultBaseDO) {
                    return obj.id
                } else if (obj is AccessEntryDO) {
                    return obj.id
                } else if (obj is Kost2ArtDO) {
                    return obj.id
                } else if (obj is KundeDO) {
                    return obj.id
                } else if (obj is UserPrefEntryDO) {
                    return obj.id
                }

                log.error(
                    "Couldn't get the identifier of the given object (Jassist/Hibernate-Bug: HHH-3502) for class: "
                            + obj.javaClass.name
                )
                return null*/
    }

    /**
     * @param obj
     * @return
     */
    fun <T : Serializable> setIdentifier(obj: BaseDO<T>, value: T) {
        if (Hibernate.isInitialized(obj)) {
            obj.id = value
        } else if (obj is DefaultBaseDO) {
            (obj as DefaultBaseDO).id = value as Long
        } else if (obj is AccessEntryDO) {
            (obj as AccessEntryDO).id = value as Long
        } else if (obj is Kost2ArtDO) {
            (obj as Kost2ArtDO).id = value as Long
        } else if (obj is KundeDO) {
            (obj as KundeDO).nummer = value as Long
        } else if (obj is UserPrefEntryDO) {
            (obj as UserPrefEntryDO).id = value as Long
        } else {
            log.error("Couldn't set the identifier of the given object for class: " + obj.javaClass.name)
        }
    }

    fun getIdentifier(obj: Any): Serializable? {
        if (obj is BaseDO<*>) {
            return getIdentifier(obj)
        }
        for (field in obj.javaClass.declaredFields) {
            if (field.isAnnotationPresent(Id::class.java) && field.isAnnotationPresent(GeneratedValue::class.java)) {
                val isAccessible = field.canAccess(obj)
                try {
                    field.isAccessible = true
                    val idObject = field[obj]
                    field.isAccessible = isAccessible
                    if (idObject != null && Serializable::class.java.isAssignableFrom(idObject.javaClass)) {
                        return idObject as Serializable
                    }
                } catch (e: IllegalArgumentException) {
                    e.printStackTrace()
                } catch (e: IllegalAccessException) {
                    e.printStackTrace()
                }
            }
        }
        return null
    }

    fun <T : Serializable> setIdentifier(obj: Any, value: T) {
        if (obj is BaseDO<*>) {
            @Suppress("UNCHECKED_CAST")
            setIdentifier(obj as BaseDO<T>, value)
        }
        for (field in obj.javaClass.declaredFields) {
            if (field.isAnnotationPresent(Id::class.java) && field.isAnnotationPresent(GeneratedValue::class.java)) {
                val isAccessible = field.canAccess(obj)
                try {
                    field.isAccessible = true
                    field[obj] = value
                    field.isAccessible = isAccessible
                } catch (e: IllegalArgumentException) {
                    e.printStackTrace()
                } catch (e: IllegalAccessException) {
                    e.printStackTrace()
                }
            }
        }
    }

    @JvmStatic
    fun getDBTableName(entityClass: Class<*>): String? {
        return HibernateMetaModel.getEntityInfo(entityClass)?.tableName
    }

    /**
     * Gets the info element of the given property.
     *
     * @param entity       Class name of the entity
     * @param propertyName Java bean property name.
     * @return info if exists.
     */
    @JvmStatic
    fun getColumnAnnotation(entity: Class<*>, propertyName: String): Column? {
        return HibernateMetaModel.getColumnInfo(entity.name, propertyName)
    }


    /**
     * Gets the length of the given property, defined in annotation @Column(length=...).
     *
     * @param entity       Class name of the entity
     * @param propertyName Java bean property name.
     * @return length if exists, otherwise null.
     */
    @JvmStatic
    fun getPropertyLength(entity: Class<*>, propertyName: String): Int? {
        return HibernateMetaModel.getPropertyLength(entity.name, propertyName)
    }

    /**
     * Gets the length of the given property, defined in annotation @Column(length=...).
     *
     * @param entityName   Class name of the entity
     * @param propertyName Java bean property name.
     * @return length if exists, otherwise null.
     */
    @JvmStatic
    fun getPropertyLength(entityName: String, propertyName: String): Int? {
        return HibernateMetaModel.getPropertyLength(entityName, propertyName)
    }

    /**
     * Shorten the length of the property if to long for the data-base. No log message is produced. The field must be
     * declared with JPA annotations in the given class (clazz). For getting and setting the property the getter and
     * setter method is used.
     *
     * @param clazz         The class where the field is declared.
     * @param object
     * @param propertyNames
     * @return true If at least one property was shortened.
     */
    @JvmStatic
    fun shortenProperties(clazz: Class<*>, `object`: Any, vararg propertyNames: String): Boolean {
        var result = false
        for (propertyName in propertyNames) {
            if (shortenProperty(clazz, `object`, propertyName)) {
                result = true
            }
        }
        return result
    }

    /**
     * Shorten the length of the property if to long for the data-base. No log message is produced. The field must be
     * declared with JPA annotations in the given class (clazz). For getting and setting the property the getter and
     * setter method is used.
     *
     * @param clazz        The class where the field is declared.
     * @param object
     * @param propertyName
     * @return true If the property was shortened.
     */
    fun shortenProperty(clazz: Class<*>, `object`: Any, propertyName: String): Boolean {
        val length = getPropertyLength(
            clazz,
            propertyName
        )
            ?: return false
        val value = BeanHelper.getProperty(`object`, propertyName) as? String
        if (value != null && value.length > length) {
            BeanHelper.setProperty(`object`, propertyName, value.substring(0, length - 1))
            return true
        }
        return false
    }
}
