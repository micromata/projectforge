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

package org.projectforge.framework.persistence.api.impl

import com.fasterxml.jackson.core.JsonGenerator
import com.fasterxml.jackson.core.JsonProcessingException
import com.fasterxml.jackson.databind.SerializerProvider
import com.fasterxml.jackson.databind.annotation.JsonSerialize
import com.fasterxml.jackson.databind.ser.std.StdSerializer
import mu.KotlinLogging
import org.hibernate.search.mapper.pojo.mapping.definition.annotation.DocumentId
import org.hibernate.search.mapper.pojo.mapping.definition.annotation.FullTextField
import org.hibernate.search.mapper.pojo.mapping.definition.annotation.GenericField
import org.hibernate.search.mapper.pojo.mapping.definition.annotation.KeywordField
import org.projectforge.common.BeanHelper
import org.projectforge.common.ClassUtils
import org.projectforge.common.extensions.capitalize
import org.projectforge.common.extensions.uncapitalize
import org.projectforge.common.props.PropUtils
import org.projectforge.framework.ToStringUtil
import org.projectforge.framework.persistence.api.BaseDao
import org.projectforge.framework.persistence.entities.DefaultBaseDO
import org.projectforge.framework.persistence.metamodel.HibernateMetaModel.getEntityInfo
import org.projectforge.framework.persistence.search.ClassBridge
import java.io.IOException
import java.lang.reflect.AccessibleObject
import java.lang.reflect.Method
import java.lang.reflect.Modifier
import kotlin.Throws

private val log = KotlinLogging.logger {}

class HibernateSearchClassInfo(baseDao: BaseDao<*>) {
    private val fieldInfos = mutableListOf<HibernateSearchFieldInfo>()

    @JsonSerialize(using = ClassBridgesSerializer::class)
    val classBridges: Array<ClassBridge>

    val allFieldNames
        get() = fieldInfos.map { it.luceneField }.toTypedArray()

    val stringFieldNames
        get() = fieldInfos.filter { it.isStringSearchSupported() }.map { it.luceneField }.toTypedArray()

    val numericFields
        get() = fieldInfos.filter { it.isNumericSearchSupported() }.map { Pair(it.luceneField, it.type) }.toTypedArray()

    private val clazz: Class<*>

    init {
        clazz = baseDao.doClass
        val fields = BeanHelper.getAllDeclaredFields(clazz)
        val bridges = mutableListOf<ClassBridge>()
        for (field in fields) {
            checkAndRegister(clazz, field.name, field.type, field)
        }
        val methods = clazz.methods
        for (method in methods) {
            var fieldInfo = isGetter(method)
            if (fieldInfo == null) {
                fieldInfo = isSetter(method)
            }
            if (fieldInfo != null) {
                checkAndRegister(clazz, fieldInfo.fieldName, fieldInfo.type, method)
            }
        }
        baseDao.additionalSearchFields?.forEach { fieldName ->
            val field = PropUtils.getField(clazz, fieldName, true)
            var fieldFound = false
            if (field != null && checkAndRegister(clazz, fieldName, field.type, field)) {
                fieldFound = true
            } else if (fieldName.contains('.')) {
                // Try to find ClassBridge of embedded object:
                val fieldInfo = ClassUtils.getFieldInfo(clazz, fieldName, true)
                if (fieldInfo != null) {
                    val type = if (fieldInfo.genericType != null) fieldInfo.genericType else fieldInfo.field.type
                    if (type != null) {
                        val name = fieldName.substring(fieldName.lastIndexOf('.') + 1)
                        val bridge = getClassBridges(type).find { it.name == name }
                        if (bridge != null) {
                            bridges.add(bridge)
                            fieldInfos.add(
                                HibernateSearchFieldInfo(
                                    fieldName,
                                    ClassBridge::class.java
                                )
                            ) // Search for class bridge name.
                            fieldFound = true
                        } else {
                            if (checkAndRegister(clazz, fieldName, type, fieldInfo.field)) {
                                fieldFound = true
                            }
                        }
                    }
                }
            }
            if (!fieldFound) {
                log.warn("Search property '${baseDao.doClass}.$fieldName' not found, but declared as additional field (ignoring it).")
            }
        }

        // Check @ClassBridge annotation:
        getClassBridges(clazz).forEach {
            fieldInfos.add(
                HibernateSearchFieldInfo(
                    it.name,
                    ClassBridge::class.java
                )
            ) // Search for class bridge name.
            bridges.add(it)
        }
        this.classBridges = bridges.toTypedArray()
        log.info(
            "SearchInfo for class ${
                ClassUtils.getProxiedClass(
                    baseDao::
                    class.java
                ).simpleName
            }: $this"
        )
    }

    fun isStringField(field: String): Boolean {
        return get(field)?.isStringSearchSupported() == true
    }

    fun containsField(field: String): Boolean {
        return get(field) != null
    }

    fun getClassBridge(name: String): ClassBridge? {
        return classBridges.find { it.name == name }
    }

    fun get(field: String): HibernateSearchFieldInfo? {
        return fieldInfos.find { it.javaProp == field || it.luceneField == field }
    }

    private fun checkAndRegister(
        clazz: Class<*>,
        fieldName: String,
        fieldType: Class<*>,
        accessible: AccessibleObject
    ): Boolean {
        var info = get(fieldName)
        val isNew = info == null
        if (info == null) {
            info = HibernateSearchFieldInfo(fieldName, fieldType)
        }
        var isSearchField = false
        if (getEntityInfo(clazz)?.getPropertyInfo(fieldName) != null) {
            info.persistentField = true
        }
        accessible.getAnnotationsByType(FullTextField::class.java)?.forEach { ann ->
            info.add(ann)
            isSearchField = true
        }
        accessible.getAnnotationsByType(GenericField::class.java)?.forEach { ann ->
            info.add(ann)
            isSearchField = true
        }
        accessible.getAnnotationsByType(KeywordField::class.java)?.forEach { ann ->
            info.add(ann)
            isSearchField = true
        }
        if (accessible.isAnnotationPresent(DocumentId::class.java)) {
            info.add(accessible.getAnnotation(DocumentId::class.java))
            isSearchField = true
        } else if (fieldName.endsWith(".id")) {
            // Check if embedded id is used.
            val fieldInfo = ClassUtils.getFieldInfo(clazz, fieldName)
            if (fieldInfo != null && DefaultBaseDO::class.java.isAssignableFrom(fieldInfo.clazz)) {
                // Embedded id found.
                info.idProperty = true
                isSearchField = true
            }
        }

        if (!isNew || !isSearchField) {
            return false
        }
        fieldInfos.add(info)
        return true
    }

    class FieldInfo(val fieldName: String, val type: Class<*>)

    private fun isGetter(method: Method): FieldInfo? {
        if (Modifier.isPublic(method.modifiers) && method.parameterTypes.isEmpty()) {
            if (method.name.matches("^get[A-Z].*".toRegex()) && method.returnType != Void.TYPE)
                return FieldInfo(method.name.substring(3).uncapitalize(), method.returnType)
            if (method.name.matches("^is[A-Z].*".toRegex()) && method.returnType == Boolean::class.javaPrimitiveType)
                return FieldInfo(method.name.substring(2).uncapitalize(), method.returnType)
        }
        return null
    }

    private fun isSetter(method: Method): FieldInfo? {
        if (Modifier.isPublic(method.modifiers) &&
            method.returnType == Void.TYPE &&
            method.parameterTypes.size == 1 &&
            method.name.matches("^set[A-Z].*".toRegex())
        ) {
            return FieldInfo(method.name.substring(3).capitalize(), method.parameterTypes[0])
        }
        return null
    }

    private fun getClassBridges(clazz: Class<*>): MutableList<ClassBridge> {
        val result = mutableSetOf<ClassBridge>()
        val classBridge = ClassUtils.getClassAnnotation(clazz, ClassBridge::class.java)
        if (classBridge != null) {
            result.add(classBridge)
        }
        return result.toMutableList()
    }

    override fun toString(): String {
        return ToStringUtil.toJsonString(this)
    }

    class ClassBridgesSerializer : StdSerializer<Any>(Any::class.java) {
        @Throws(IOException::class, JsonProcessingException::class)
        override fun serialize(value: Any?, jgen: JsonGenerator, provider: SerializerProvider) {
            if (value == null) {
                jgen.writeNull()
                return
            }
            value as Array<ClassBridge>
            jgen.writeString(value.joinToString(", ") { it.name })
        }
    }
}
