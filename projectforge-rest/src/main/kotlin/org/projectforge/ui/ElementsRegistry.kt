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

package org.projectforge.ui

import de.micromata.genome.jpa.metainf.ColumnMetadata
import de.micromata.genome.jpa.metainf.ColumnMetadataBean
import org.aspectj.weaver.tools.cache.SimpleCacheFactory.path
import org.projectforge.business.task.TaskDO
import org.projectforge.common.anots.PropertyInfo
import org.projectforge.common.i18n.I18nEnum
import org.projectforge.common.props.PropUtils
import org.projectforge.framework.persistence.jpa.PfEmgrFactory
import org.projectforge.framework.persistence.user.entities.PFUserDO
import org.springframework.beans.BeanUtils
import java.lang.reflect.Field
import java.math.BigDecimal
import java.util.*
import javax.persistence.Basic
import javax.persistence.Column
import javax.persistence.JoinColumn

/**
 * Registry holds properties of UIElements...
 * Builds elements automatically dependent on their property type. For Strings with maxLength > 255 a [UITextArea] will be created instead
 * of an [UIInput].
 */
object ElementsRegistry {
    private val log = org.slf4j.LoggerFactory.getLogger(ElementsRegistry::class.java)

    fun getProperties(clazz: Class<*>): Map<String, ElementInfo>? {
        return registryMap[clazz]
    }

    /**
     * Contains all found and created UIElements named by class:property.
     */
    private val registryMap = mutableMapOf<Class<*>, MutableMap<String, ElementInfo>>()
    /**
     * Contains all not found and unavailable UIElements named by class:property.
     */
    private val unavailableElementsSet = mutableSetOf<String>()

    internal fun buildElement(lc: LayoutContext, property: String): UIElement {
        val mapKey = getMapKey(lc.dataObjectClazz, property) ?: return UILabel(property)
        val elementInfo = getElementInfo(lc, property)
        if (elementInfo == null) {
            log.info("Can't build UIElement from $mapKey.")
            return UILabel("??? $mapKey ???")
        }
        var element: UIElement?
        if (elementInfo.readOnly) {
            element = UIReadOnlyField(property, dataType = getDataType(elementInfo) ?: UIDataType.STRING)
        } else {
            val dataType = getDataType(elementInfo)
            element =
                    when (elementInfo.propertyType) {
                        String::class.java -> {
                            val maxLength = elementInfo.maxLength
                            if (maxLength != null && maxLength > 255) {
                                UITextArea(property, maxLength = elementInfo.maxLength, layoutContext = lc)
                            } else {
                                UIInput(property, maxLength = elementInfo.maxLength, required = elementInfo.required, layoutContext = lc)
                            }
                        }
                        Boolean::class.java -> UICheckbox(property)
                        Date::class.java -> UIInput(property, required = elementInfo.required, layoutContext = lc, dataType = dataType!!)
                        java.sql.Date::class.java -> UIInput(property, required = elementInfo.required, layoutContext = lc, dataType = dataType!!)
                        java.sql.Timestamp::class.java -> UIInput(property, required = elementInfo.required, layoutContext = lc, dataType = dataType!!)
                        PFUserDO::class.java -> UIInput(property, required = elementInfo.required, layoutContext = lc, dataType = dataType!!)
                        Integer::class.java -> UIInput(property, required = elementInfo.required, layoutContext = lc, dataType = dataType!!)
                        BigDecimal::class.java -> UIInput(property, required = elementInfo.required, layoutContext = lc, dataType = dataType!!)
                        TaskDO::class.java -> UIInput(property, required = elementInfo.required, layoutContext = lc, dataType = dataType!!)
                        Locale::class.java -> UIInput(property, required = elementInfo.required, layoutContext = lc, dataType = dataType!!)
                        else -> null
                    }
        }
        if (element == null) {
            if (elementInfo.propertyType.isEnum) {
                if (I18nEnum::class.java.isAssignableFrom(elementInfo.propertyType)) {
                    @Suppress("UNCHECKED_CAST")
                    element = UISelect<String>(property, required = elementInfo.required, layoutContext = lc)
                            .buildValues(i18nEnum = elementInfo.propertyType as Class<out Enum<*>>)
                } else {
                    log.warn("Properties of enum not implementing I18nEnum not yet supported: $mapKey.")
                    unavailableElementsSet.add(mapKey)
                    return UILabel("??? $mapKey ???")
                }
            } else {
                log.warn("Unsupported property type '${elementInfo.propertyType}': $mapKey")
            }
        }
        if (element is UILabelledElement) {
            LayoutUtils.setLabels(elementInfo, element)
        }
        return element ?: UILabel(property)
    }

    private fun getDataType(elementInfo: ElementInfo): UIDataType? {
        return when (elementInfo.propertyType) {
            String::class.java -> UIDataType.STRING
            Boolean::class.java -> UIDataType.BOOLEAN
            Date::class.java -> UIDataType.TIMESTAMP
            java.sql.Date::class.java -> UIDataType.DATE
            java.sql.Timestamp::class.java -> UIDataType.TIMESTAMP
            PFUserDO::class.java -> UIDataType.USER
            Integer::class.java -> UIDataType.INT
            BigDecimal::class.java -> UIDataType.DECIMAL
            TaskDO::class.java -> UIDataType.TASK
            Locale::class.java -> UIDataType.LOCALE
            else -> null
        }
    }

    internal fun getElementInfo(lc: LayoutContext, property: String): ElementInfo? {
        val parts = property.split('.')
        if (!parts.isNullOrEmpty()) {
            val listElementInfo = lc.getListElementInfo(parts[0])
            if (listElementInfo != null) {
                // property starts with list element name, therefore try to find the property of this list element
                // instead of the data object class:
                if (listElementInfo.genericType != null) {
                    return getElementInfo(listElementInfo.genericType, property.substring(parts[0].length + 1))
                }
                log.warn("Can't detect generic type of list element '$path' for property '$property'")
            }
        }
        return getElementInfo(lc.dataObjectClazz, property)
    }

    /**
     * If possible, use [getElementInfo] with layoutContext instead for supporting list elements.
     * @param property name of property (nested properties are supported, like timesheet.task.id.
     */
    internal fun getElementInfo(clazz: Class<*>?, property: String): ElementInfo? {
        if (clazz == null)
            return null
        val mapKey = getMapKey(clazz, property)!!
        var elementInfo = ensureClassMap(clazz)[property]
        if (elementInfo != null) {
            return elementInfo // Element found
        }
        if (unavailableElementsSet.contains(mapKey)) {
            return null // Element can't be determined (a previous try failed)
        }
        val propertyField = getPropertyField(clazz, property)
        if (propertyField != null) {
            elementInfo = ElementInfo(property, propertyField)
        } else {
            elementInfo = getPropertyInfo(clazz, property)
            if (elementInfo == null) {
                log.info("Property $clazz.$property not found. Can't autodetect layout.")
                unavailableElementsSet.add(mapKey)
                return null
            }
        }
        if (property.contains('.')) { // Nested property, like timesheet.task.id?
            val parentProperty = property.substring(0, property.lastIndexOf('.'))
            val parentInfo = getElementInfo(clazz, parentProperty)
            elementInfo.parent = parentInfo
        }
        var propertyInfo: PropertyInfo? = null
        if (propertyField != null) {
            propertyInfo = PropUtils.get(clazz, property)
        }
        if (propertyInfo == null) {
            // Try to get the annotation of the getter method.
            val test = BeanUtils.getPropertyDescriptor(clazz, property)
            val readMethod = test?.readMethod
            propertyInfo = BeanUtils.getPropertyDescriptor(clazz, property)?.readMethod?.getAnnotation(PropertyInfo::class.java)
        }
        if (propertyInfo == null) {
            log.warn("@PropertyInfo '$clazz:$property' not found.")
            return elementInfo
        }
        val colinfo = getColumnMetadata(clazz, property)
        if (colinfo != null) {
            elementInfo.maxLength = colinfo.maxLength
            if (!(colinfo.isNullable) || propertyInfo.required)
                elementInfo.required = true
        }
        elementInfo.i18nKey = getNullIfEmpty(propertyInfo.i18nKey)
        elementInfo.additionalI18nKey = getNullIfEmpty(propertyInfo.additionalI18nKey)
        ensureClassMap(clazz)[property] = elementInfo
        return elementInfo
    }

    private fun getPropertyField(clazz: Class<*>?, property: String): Field? {
        if (clazz == null)
            return null
        return PropUtils.getField(clazz, property, true)
    }

    private fun getPropertyInfo(clazz: Class<*>?, property: String): ElementInfo? {
        if (clazz == null)
            return null
        val desc = BeanUtils.getPropertyDescriptor(clazz, property) ?: return null
        return ElementInfo(property, propertyType = desc.propertyType, readOnly = (desc.writeMethod == null))
    }

    private fun ensureClassMap(clazz: Class<*>): MutableMap<String, ElementInfo> {
        var result = registryMap[clazz]
        if (result == null) {
            result = mutableMapOf()
            registryMap[clazz] = result
        }
        return result
    }

    private fun getMapKey(clazz: Class<*>?, property: String?): String? {
        if (clazz == null || property == null) {
            return null
        }
        return "${clazz.name}.$property"
    }

    /**
     * @param entity
     * @param property
     * @return null if not found,
     */
    private fun getColumnMetadata(entity: Class<*>?, property: String): ColumnMetadata? {
        if (entity == null)
            return null
        val persistentClass = PfEmgrFactory.get().metadataRepository.findEntityMetadata(entity) ?: return null
        val columnMetaData = persistentClass.columns[property] ?: return null
        if (!columnMetaData.isNullable) {
            val joinColumnAnn = columnMetaData.findAnnoation(JoinColumn::class.java)
            if (joinColumnAnn != null) {
                // Fix for error in method findEntityMetadata: For @JoinColumn nullable is always returned as false:
                (columnMetaData as ColumnMetadataBean).isNullable = joinColumnAnn.nullable
            }
            val basicAnn = columnMetaData.findAnnoation(Basic::class.java)
            if (basicAnn != null) {
                val columnAnn = columnMetaData.findAnnoation(Column::class.java)
                if (columnAnn == null) {
                    // Fix for error in method findEntityMetadata: For @Basic without @Column nullable is always returned as false:
                    (columnMetaData as ColumnMetadataBean).isNullable = true // nullable is true, if @Column is not given (JPA).
                }
            }
        }
        return columnMetaData
    }

    private fun getNullIfEmpty(value: String?): String? {
        if (value.isNullOrEmpty()) {
            return null
        }
        return value
    }
}
