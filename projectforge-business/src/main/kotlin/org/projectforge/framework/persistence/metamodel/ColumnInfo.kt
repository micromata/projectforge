package org.projectforge.framework.persistence.metamodel


/**
 * @author Kai Reinhard (k.reinhard@micromata.de)
 */
class ColumnInfo(
    val propertyName: String,
    val columnName: String,
    val length: Int? = null,
    val nullable: Boolean? = null,
    val scale: Int? = null,
    val precision: Int? = null,
)
