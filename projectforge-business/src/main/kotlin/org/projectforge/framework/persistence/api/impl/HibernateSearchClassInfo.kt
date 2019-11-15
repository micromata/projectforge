/////////////////////////////////////////////////////////////////////////////
//
// Project ProjectForge Community Edition
//         www.projectforge.org
//
// Copyright (C) 2001-2019 Micromata GmbH, Germany (www.micromata.com)
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

import com.fasterxml.jackson.annotation.JsonIgnore
import com.fasterxml.jackson.core.JsonGenerator
import com.fasterxml.jackson.core.JsonProcessingException
import com.fasterxml.jackson.databind.SerializerProvider
import com.fasterxml.jackson.databind.annotation.JsonSerialize
import com.fasterxml.jackson.databind.ser.std.StdSerializer
import org.hibernate.search.annotations.ClassBridge
import org.hibernate.search.annotations.ClassBridges
import org.hibernate.search.annotations.DateBridge
import org.hibernate.search.annotations.DocumentId
import org.projectforge.common.BeanHelper
import org.projectforge.common.ClassUtils
import org.projectforge.common.props.PropUtils
import org.projectforge.framework.ToStringUtil
import org.projectforge.framework.persistence.api.BaseDao
import org.projectforge.framework.persistence.entities.DefaultBaseDO
import org.slf4j.LoggerFactory
import java.io.IOException
import java.lang.reflect.AccessibleObject
import java.lang.reflect.Method
import java.lang.reflect.Modifier
import javax.persistence.Id


class HibernateSearchClassInfo(baseDao: BaseDao<*>) {
    @JsonIgnore
    private val log = LoggerFactory.getLogger(HibernateSearchClassInfo::class.java)

    private val fieldInfos = mutableListOf<HibernateSearchFieldInfo>()

    @JsonSerialize(using = ClassBridgesSerializer::class)
    val classBridges: Array<ClassBridge>

    val allFieldNames
        get() = fieldInfos.map { it.luceneField }.toTypedArray()

    val stringFieldNames
        get() = fieldInfos.filter { it.isStringSearchSupported() }.map { it.luceneField }.toTypedArray()

    val numericFieldNames
        get() = fieldInfos.filter { it.isNumericSearchSupported() }.map { it.luceneField }.toTypedArray()

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
                val parentClass = ClassUtils.getClassOfField(clazz, fieldName)
                if (parentClass != null) {
                    val name = fieldName.substring(fieldName.lastIndexOf('.') + 1)
                    val classBridges = getClassBridges(parentClass)
                    val bridge = classBridges.find { it.name == name }
                    if (bridge != null) {
                        bridges.add(bridge)
                        fieldInfos.add(HibernateSearchFieldInfo(fieldName, ClassBridge::class.java)) // Search for class bridge name.
                        fieldFound = true
                    }
                }
            }
            if (!fieldFound) {
                log.warn("Search property '${baseDao.doClass}.$fieldName' not found, but declared as additional field (ignoring it).")
            }
        }

        // Check @ClassBridge annotation:
        getClassBridges(clazz).forEach {
            fieldInfos.add(HibernateSearchFieldInfo(it.name, ClassBridge::class.java)) // Search for class bridge name.
            bridges.add(it)
        }
        classBridges = bridges.toTypedArray()
        log.info("SearchInfo for class ${ClassUtils.getProxiedClass(baseDao::
        class.java).simpleName}: $this")
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

    internal fun get(field: String): HibernateSearchFieldInfo? {
        return fieldInfos.find { it.javaProp == field || it.luceneField == field }
    }

    private fun checkAndRegister(clazz: Class<*>, fieldName: String, fieldType: Class<*>, accessible: AccessibleObject): Boolean {
        var info = get(fieldName)
        val isNew = info == null
        if (info == null) {
            info = HibernateSearchFieldInfo(fieldName, fieldType)
        }
        var isSearchField = false
        if (accessible.isAnnotationPresent(org.hibernate.search.annotations.Field::class.java)) {
            // @Field(index = Index.YES /*TOKENIZED*/),
            info.add(accessible.getAnnotation(org.hibernate.search.annotations.Field::class.java))
            isSearchField = true
        } else if (accessible.isAnnotationPresent(org.hibernate.search.annotations.Fields::class.java)) {
            // @Fields( {
            // @Field(index = Index.YES /*TOKENIZED*/),
            // @Field(name = "name_forsort", index = Index.YES, analyze = Analyze.NO /*UN_TOKENIZED*/)
            // } )
            val annFields = accessible.getAnnotation(org.hibernate.search.annotations.Fields::class.java)
            annFields?.value?.forEach {
                info.add(it)
            }
            isSearchField = true
        } else if (accessible.isAnnotationPresent(Id::class.java)) {
            info.add(accessible.getAnnotation(Id::class.java))
            isSearchField = true
        } else if (accessible.isAnnotationPresent(DocumentId::class.java)) {
            info.add(accessible.getAnnotation(DocumentId::class.java))
            isSearchField = true
        } else if (fieldName.endsWith(".id")) {
            val parentClass = ClassUtils.getClassOfField(clazz, fieldName)
            if (parentClass != null && DefaultBaseDO::class.java.isAssignableFrom(parentClass)) {
                // Embedded id found.
                info.idProperty = true
                isSearchField = true
            }
        }

        if (!isNew || !isSearchField) {
            return false
        }
        if (accessible.isAnnotationPresent(DateBridge::class.java)) {
            info.dateBridgeAnn = accessible.getAnnotation(DateBridge::class.java)
        }
        fieldInfos.add(info)
        return true
    }

    class FieldInfo(val fieldName: String, val type: Class<*>)

    private fun isGetter(method: Method): FieldInfo? {
        if (Modifier.isPublic(method.modifiers) && method.parameterTypes.isEmpty()) {
            if (method.name.matches("^get[A-Z].*".toRegex()) && method.returnType != Void.TYPE)
                return FieldInfo(method.name.substring(3).decapitalize(), method.returnType)
            if (method.name.matches("^is[A-Z].*".toRegex()) && method.returnType == Boolean::class.javaPrimitiveType)
                return FieldInfo(method.name.substring(2).decapitalize(), method.returnType)
        }
        return null
    }

    private fun isSetter(method: Method): FieldInfo? {
        if (Modifier.isPublic(method.modifiers) &&
                method.returnType == Void.TYPE &&
                method.parameterTypes.size == 1 &&
                method.name.matches("^set[A-Z].*".toRegex())) {
            return FieldInfo(method.name.substring(3).decapitalize(), method.parameterTypes[0])
        }
        return null
    }

    private fun getClassBridges(clazz: Class<*>): List<ClassBridge> {
        val result = mutableSetOf<ClassBridge>()
        val bridge = ClassUtils.getClassAnnotation(clazz, ClassBridge::class.java)
        if (bridge != null) {
            result.add(bridge)
        }
        val bridges = ClassUtils.getClassAnnotation(clazz, ClassBridges::class.java)
        if (bridges != null) {
            result.addAll(bridges.value)
        }
        return result.toList()
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
            jgen.writeString(value.map { it.name }.joinToString(", "))
        }
    }
}
