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

package org.projectforge.framework.persistence.database

import jakarta.persistence.JoinColumn
import jakarta.persistence.OneToMany
import org.junit.jupiter.api.Assertions
import org.junit.jupiter.api.Test
import org.projectforge.business.task.TaskDO
import org.projectforge.common.AnnotationsUtils
import org.projectforge.common.BeanHelper
import org.projectforge.common.ClassUtils
import org.projectforge.framework.json.JsonUtils
import org.projectforge.framework.persistence.api.HibernateUtils
import org.projectforge.framework.persistence.api.HibernateUtils.enterTestMode
import org.projectforge.framework.persistence.api.HibernateUtils.exitTestMode
import org.projectforge.framework.persistence.api.HibernateUtils.getPropertyLength
import org.projectforge.framework.persistence.metamodel.EntityInfo
import org.projectforge.framework.persistence.metamodel.HibernateMetaModel
import org.projectforge.test.AbstractTestBase

class HibernateUtilsTest : AbstractTestBase() {
    // Used for migrating database. In this case, the column type of the primary key is changed from Integer to Long.
    fun entityMetaDataTest() {
        class Entry(
            tableName: String?,
            val idColumnName: String? = "pk",
            val idColumnType: Class<*>? = java.lang.Integer::class.java,
            val entityInfo: EntityInfo? = null,
        ) {
            val tableName = tableName?.lowercase()
            override fun toString(): String {
                return JsonUtils.toJson(this)
            }
        }

        val entries = mutableListOf<Entry>()
        HibernateMetaModel.allEntityInfos().forEach { entry ->
            HibernateMetaModel.getEntityInfo(entry.entityClass)?.let { info ->
                val clazz = info.entityClass
                val tableName = info.tableName
                val idField = ClassUtils.getFieldInfo(clazz, "id")
                val idColumnType = idField?.field?.type
                val idColumnName = HibernateUtils.getColumnAnnotation(clazz, "id")?.name
                entries.add(Entry(tableName, idColumnName, idColumnType, info))
            }
        }
        // Add plugin entities (not existing in this test)
        entries.add(Entry("t_plugin_merlin_template"))
        entries.add(Entry("t_plugin_datatransfer_area"))
        entries.add(Entry("t_plugin_datatransfer_audit"))
        entries.add(Entry("T_PLUGIN_TODO"))
        entries.add(Entry("T_PLUGIN_MEMO"))
        entries.add(Entry("T_PLUGIN_BANKING_ACCOUNT"))
        entries.add(Entry("T_PLUGIN_BANKING_ACCOUNT_BALANCE"))
        entries.add(Entry("T_PLUGIN_BANKING_ACCOUNT_RECORD"))
        entries.add(Entry("T_PLUGIN_MARKETING_ADDRESS_CAMPAIGN"))
        entries.add(Entry("T_PLUGIN_MARKETING_ADDRESS_CAMPAIGN_VALUE"))
        entries.add(Entry("T_PLUGIN_LM_LICENSE"))
        entries.add(Entry("T_PLUGIN_SKILLMATRIX_ENTRY"))
        entries.add(Entry("T_PLUGIN_LIQUI_ENTRY"))
        for (i in 0..1) {
            val type = if (i == 0) " TYPE" else ""
            if (i == 0) {
                println("PostgreSQL:")
            } else {
                println("HsqlDB:")
            }
            entries.distinctBy { it.tableName }.sortedBy { it.tableName }.forEach { entry ->
                if (entry.idColumnType == java.lang.Integer::class.java) {
                    println("ALTER TABLE ${entry.tableName} ALTER COLUMN ${entry.idColumnName}$type bigint;")
                }
                val entityInfo = entry.entityInfo
                entityInfo?.entityClass?.let { clazz ->
                    BeanHelper.getAllDeclaredFields(clazz).forEach { field ->
                        val annotations = AnnotationsUtils.getAnnotations(clazz, field.name)
                        annotations.find { it.annotationClass == JoinColumn::class }?.let { joinColumn ->
                            if (annotations.none { it.annotationClass == OneToMany::class }) {
                                // OneToMany not handled here.
                                joinColumn as JoinColumn
                                println("ALTER TABLE ${entry.tableName} ALTER COLUMN ${joinColumn.name}$type bigint;")
                            }
                        }
                    }
                }
            }
        }
    }

    @Test
    fun propertyLengthTest() {
        var length = getPropertyLength(TaskDO::class.java.name, "title")
        Assertions.assertEquals(40, length)
        length = getPropertyLength(TaskDO::class.java.name, "shortDescription")
        Assertions.assertEquals(255, length)
        length = getPropertyLength(TaskDO::class.java, "shortDescription")
        Assertions.assertEquals(255, length)
        enterTestMode()
        length = getPropertyLength(TaskDO::class.java.name, "unknown")
        exitTestMode()
        Assertions.assertNull(length)
        length = getPropertyLength("unknown", "name")
        Assertions.assertNull(length)
    }
}
