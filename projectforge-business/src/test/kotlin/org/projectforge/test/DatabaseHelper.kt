package org.projectforge.test

import jakarta.persistence.EntityManager

/**
 * @author Kai Reinhard (k.reinhard@micromata.de)
 */
object DatabaseHelper {
    @JvmStatic
    fun clearDatabase(em: EntityManager) {
        em.createNativeQuery("SET REFERENTIAL_INTEGRITY FALSE").executeUpdate() // Ignore all foreign key constraints etc.
        em.createQuery("SELECT TABLE_NAME FROM INFORMATION_SCHEMA.TABLES WHERE TABLE_TYPE = 'TABLE' AND TABLE_SCHEMA = 'PUBLIC';")
            .resultList
            .forEach { tableName ->
                em.createNativeQuery("TRUNCATE TABLE $tableName").executeUpdate()
            }
        em.createNativeQuery("SET REFERENTIAL_INTEGRITY TRUE").executeUpdate()
    }
}
