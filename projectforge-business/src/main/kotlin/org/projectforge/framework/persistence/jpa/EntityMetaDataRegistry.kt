package org.projectforge.framework.persistence.jpa

object EntityMetaDataRegistry {
    private val columnMetaDataMap = mutableMapOf<Class<*>, EntityMetaData>()

    private val notFoundEntities = mutableSetOf<Class<*>>()

    fun getEntityMetaData(entityClass: Class<*>): EntityMetaData {
        if (notFoundEntities.contains(entityClass)) {
            throw IllegalArgumentException(notFoundExceptionMessage(entityClass))
        }
        columnMetaDataMap[entityClass]?.let { return it }
        try {
            val entityMetaData = EntityMetaData(entityClass)
            columnMetaDataMap[entityClass] = entityMetaData
            return entityMetaData
        } catch (e: Exception) {
            notFoundEntities.add(entityClass)
            throw IllegalArgumentException(notFoundExceptionMessage(entityClass))
        }
    }

    fun getColumnMetaData(entityClass: Class<*>, columnName: String): ColumnMetaData? {
        return getEntityMetaData(entityClass).getColumnMetaData(columnName)
    }

    private fun notFoundExceptionMessage(entityClass: Class<*>): String {
        return "EntityMetaData for class $entityClass not found."
    }
}
