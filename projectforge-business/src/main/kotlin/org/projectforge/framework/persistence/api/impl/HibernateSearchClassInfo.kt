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
import org.hibernate.search.annotations.ClassBridge
import org.hibernate.search.annotations.ClassBridges
import org.hibernate.search.annotations.DocumentId
import org.projectforge.common.BeanHelper
import org.projectforge.common.ClassUtils
import org.projectforge.common.props.PropUtils
import org.projectforge.framework.ToStringUtil
import org.projectforge.framework.persistence.api.BaseDao
import org.slf4j.LoggerFactory
import java.lang.reflect.AccessibleObject
import java.lang.reflect.Method
import java.lang.reflect.Modifier
import javax.persistence.Id


class HibernateSearchClassInfo(baseDao: BaseDao<*>) {
    @JsonIgnore
    private val log = LoggerFactory.getLogger(HibernateSearchClassInfo::class.java)

    @JsonIgnore
    private val fieldInfos = mutableMapOf<String, HibernateSearchFieldInfo>()

    val stringFieldNames: Array<String>
    val numericFieldNames: Array<String>
    val allFieldNames: Array<String>
    val classBridges: Array<ClassBridge>

    private val clazz: Class<*>

    init {
        clazz = baseDao.doClass
        val fields = BeanHelper.getAllDeclaredFields(clazz)
        for (field in fields) {
            checkAndRegister(field.name, field.type, field)
        }
        val methods = clazz.methods
        for (method in methods) {
            var fieldInfo = isGetter(method)
            if (fieldInfo == null) {
                fieldInfo = isSetter(method)
            }
            if (fieldInfo != null) {
                checkAndRegister(fieldInfo.fieldName, fieldInfo.type, method)
            }
        }
        baseDao.additionalSearchFields?.forEach {
            val field = PropUtils.getField(clazz, it)
            if (field != null) {
                checkAndRegister(it, field.type, field)
            } else {
                log.warn("Search property '${baseDao.doClass}.$it' not found, but declared as additional field (ignoring it).")
            }
        }
        val stringSearchFields = mutableListOf<String>()
        val numericSearchFields = mutableListOf<String>()
        val allFields = mutableListOf<String>()
        val bridges = mutableListOf<ClassBridge>()
        // Check @ClassBridge annotation:
        val classBridgeAnn = ClassUtils.getClassAnnotation(clazz, ClassBridge::class.java)
        if (classBridgeAnn != null) {
            stringSearchFields.add(classBridgeAnn.name) // Search for class bridge name.
            allFields.add(classBridgeAnn.name)
            bridges.add(classBridgeAnn)
        }
        val classBridgesAnn = ClassUtils.getClassAnnotation(clazz, ClassBridges::class.java)
        if (classBridgesAnn != null) {
            classBridgesAnn.value.forEach {
                stringSearchFields.add(it.name) // Search for class bridge name.
                allFields.add(it.name)
                bridges.add(it)
            }
        }

        fieldInfos.values.forEach {
            if (it.isStringSearchSupported()) {
                stringSearchFields.add(it.field)
            } else if (it.isNumericSearchSupported()) {
                numericSearchFields.add(it.field)
            }
            allFields.add(it.field)
        }
        allFieldNames = allFields.toTypedArray()
        stringFieldNames = stringSearchFields.toTypedArray()
        numericFieldNames = numericSearchFields.toTypedArray()
        classBridges = bridges.toTypedArray()
        log.info("SearchInfo for class ${ClassUtils.getProxiedClass(baseDao::class.java).simpleName}: $this")
    }

    fun isStringField(field: String): Boolean {
        return stringFieldNames.contains(field)
    }

    fun containsField(field: String): Boolean {
        return fieldInfos.contains(field)
    }

    fun getClassBridge(name: String): ClassBridge? {
        return classBridges.find { it.name == name }
    }

    internal fun get(field: String): HibernateSearchFieldInfo? {
        return fieldInfos[field]
    }

    private fun checkAndRegister(fieldName: String, fieldType: Class<*>, accessible: AccessibleObject) {
        var info = fieldInfos[fieldName]
        var isNew = info == null
        if (info == null) {
            info = HibernateSearchFieldInfo(fieldName, fieldType)
        }
        if (accessible.isAnnotationPresent(org.hibernate.search.annotations.Field::class.java)) {
            // @Field(index = Index.YES /*TOKENIZED*/),
            info.add(accessible.getAnnotation(org.hibernate.search.annotations.Field::class.java))
        } else if (accessible.isAnnotationPresent(org.hibernate.search.annotations.Fields::class.java)) {
            // @Fields( {
            // @Field(index = Index.YES /*TOKENIZED*/),
            // @Field(name = "name_forsort", index = Index.YES, analyze = Analyze.NO /*UN_TOKENIZED*/)
            // } )
            val annFields = accessible.getAnnotation(org.hibernate.search.annotations.Fields::class.java)
            annFields?.value?.forEach {
                info.add(it)
            }
        } else if (accessible.isAnnotationPresent(Id::class.java)) {
            info.add(accessible.getAnnotation(Id::class.java))
        } else if (accessible.isAnnotationPresent(DocumentId::class.java)) {
            info.add(accessible.getAnnotation(DocumentId::class.java))
        } else {
            return // No annotation found for field
        }

        if (isNew && info.hasAnnotations()) {
            fieldInfos[fieldName] = info
        }
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

    override fun toString(): String {
        return ToStringUtil.toJsonString(this)
    }
}
