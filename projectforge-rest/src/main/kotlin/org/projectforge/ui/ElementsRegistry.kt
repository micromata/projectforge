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

package org.projectforge.ui

import mu.KotlinLogging
import org.projectforge.business.fibu.EmployeeDO
import org.projectforge.business.fibu.kost.Kost1DO
import org.projectforge.business.fibu.kost.Kost2DO
import org.projectforge.business.task.TaskDO
import org.projectforge.common.anots.PropertyInfo
import org.projectforge.common.i18n.I18nEnum
import org.projectforge.common.props.PropUtils
import org.projectforge.common.props.PropertyType
import org.projectforge.framework.persistence.jpa.ColumnMetaData
import org.projectforge.framework.persistence.jpa.EntityMetaDataRegistry
import org.projectforge.framework.persistence.user.entities.GroupDO
import org.projectforge.framework.persistence.user.entities.PFUserDO
import org.springframework.beans.BeanUtils
import java.lang.reflect.Field
import java.math.BigDecimal
import java.time.LocalDate
import java.util.*

private val log = KotlinLogging.logger {}

/**
 * Registry holds properties of UIElements...
 * Builds elements automatically dependent on their property type. For Strings with maxLength > 255 a [UITextArea] will be created instead
 * of an [UIInput].
 */
object ElementsRegistry {

    fun getProperties(clazz: Class<*>): Map<String, ElementInfo>? {
        synchronized(registryMap) {
            return registryMap[clazz]
        }
    }

    /**
     * Contains all found and created UIElements named by class:property.
     */
    private val registryMap = mutableMapOf<Class<*>, MutableMap<String, ElementInfo>>()

    /**
     * Contains all not found and unavailable UIElements named by class:property.
     */
    private val unavailableElementsSet = mutableSetOf<String>()

    /**
     * @param minLengthOfTextArea For text fields longer than minLengthOfTextArea, a UITextArea is used instead of UIInput.
     *                            Default length is [LayoutUtils.DEFAULT_MIN_LENGTH_OF_TEXT_AREA], meaning fields with max
     *                            length of more than [LayoutUtils.DEFAULT_MIN_LENGTH_OF_TEXT_AREA] will be displayed as TextArea.
     */
    internal fun buildElement(
        lc: LayoutContext,
        property: String,
        minLengthOfTextArea: Int = LayoutUtils.DEFAULT_MIN_LENGTH_OF_TEXT_AREA,
    ): UIElement {
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
                when (elementInfo.propertyClass) {
                    String::class.java -> {
                        val maxLength = elementInfo.maxLength
                        if (elementInfo.propertyType != PropertyType.INPUT && maxLength != null && maxLength >= minLengthOfTextArea) {
                            UITextArea(property, maxLength = elementInfo.maxLength, layoutContext = lc)
                        } else {
                            UIInput(
                                property,
                                maxLength = elementInfo.maxLength,
                                required = elementInfo.required,
                                layoutContext = lc
                            )
                        }
                    }

                    Boolean::class.java, java.lang.Boolean::class.java -> UICheckbox(property)

                    Date::class.java,
                    LocalDate::class.java,
                    java.sql.Date::class.java,
                    java.sql.Timestamp::class.java -> UIInput(
                        property,
                        required = elementInfo.required,
                        layoutContext = lc,
                        dataType = dataType!!
                    )

                    PFUserDO::class.java, GroupDO::class.java, Kost1DO::class.java, Kost2DO::class.java, EmployeeDO::class.java, TaskDO::class.java -> UIInput(
                        property,
                        required = elementInfo.required,
                        layoutContext = lc,
                        dataType = dataType!!
                    )

                    Integer::class.java, Long::class.java, BigDecimal::class.java -> UIInput(
                        property,
                        required = elementInfo.required,
                        layoutContext = lc,
                        dataType = dataType!!
                    )

                    Locale::class.java,
                    TimeZone::class.java -> {
                        UISelect<TimeZone>(
                            property, required = elementInfo.required, layoutContext = lc,
                            autoCompletion = AutoCompletion<String>(url = "timeZones/ac?search=:search")
                        )
                    }

                    else -> null
                }
        }
        if (element == null) {
            if (elementInfo.propertyClass.isEnum) {
                if (I18nEnum::class.java.isAssignableFrom(elementInfo.propertyClass)) {
                    @Suppress("UNCHECKED_CAST")
                    element = UISelect<String>(property, required = elementInfo.required, layoutContext = lc)
                        .buildValues(i18nEnum = elementInfo.propertyClass as Class<out Enum<*>>)
                } else {
                    log.warn("Properties of enum not implementing I18nEnum not yet supported: $mapKey.")
                    synchronized(unavailableElementsSet) {
                        unavailableElementsSet.add(mapKey)
                    }
                    return UILabel("??? $mapKey ???")
                }
            } else {
                log.warn("Unsupported property type '${elementInfo.propertyClass}': $mapKey")
            }
        }
        if (element is UILabelledElement) {
            LayoutUtils.setLabels(elementInfo, element)
        }
        return element ?: UILabel(property)
    }

    private fun getDataType(elementInfo: ElementInfo): UIDataType? {
        return UIDataTypeUtils.getDataType(elementInfo)
    }

    internal fun getElementInfo(lc: LayoutContext?, property: String): ElementInfo? {
        lc ?: return null
        val parts = property.split('.')
        if (parts.isNotEmpty()) {
            val listElementInfo = lc.getListElementInfo(parts[0])
            if (listElementInfo != null) {
                // property starts with list element name, therefore try to find the property of this list element
                // instead of the data object class:
                if (listElementInfo.genericType != null) {
                    return getElementInfo(listElementInfo.genericType, property.substring(parts[0].length + 1))
                }
                log.warn("Can't detect generic type of list element '$parts[0]' for property '$property'")
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
        synchronized(unavailableElementsSet) {
            if (unavailableElementsSet.contains(mapKey)) {
                return null // Element can't be determined (a previous try failed)
            }
        }
        val propertyField = getPropertyField(clazz, property)
        if (propertyField != null) {
            elementInfo = ElementInfo(property, propertyField)
        } else {
            elementInfo = getPropertyInfo(clazz, property)
            if (elementInfo == null) {
                log.info("Property $clazz.$property not found. Can't autodetect layout.")
                synchronized(unavailableElementsSet) {
                    unavailableElementsSet.add(mapKey)
                }
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
            propertyInfo =
                BeanUtils.getPropertyDescriptor(clazz, property)?.readMethod?.getAnnotation(PropertyInfo::class.java)
        }
        if (propertyInfo == null) {
            log.warn("@PropertyInfo '$clazz:$property' not found.")
            return elementInfo
        }
        val colinfo = getColumnMetadata(clazz, property)
        if (colinfo != null) {
            elementInfo.maxLength = colinfo.length
            if ((!(colinfo.nullable) || propertyInfo.required)) {
                if (elementInfo.propertyClass != Boolean::class.java && elementInfo.propertyClass != java.lang.Boolean::class.java) {
                    elementInfo.required = true
                }
            }
        }
        elementInfo.i18nKey = getNullIfEmpty(propertyInfo.i18nKey)
        elementInfo.propertyType = propertyInfo.type
        elementInfo.additionalI18nKey = getNullIfEmpty(propertyInfo.additionalI18nKey)
        elementInfo.tooltipI18nKey = getNullIfEmpty(propertyInfo.tooltip)
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
        return ElementInfo(property, propertyClass = desc.propertyType, readOnly = (desc.writeMethod == null))
    }

    private fun ensureClassMap(clazz: Class<*>): MutableMap<String, ElementInfo> {
        synchronized(registryMap) {
            var result = registryMap[clazz]
            if (result == null) {
                result = mutableMapOf()
                registryMap[clazz] = result
            }
            return result
        }
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
    private fun getColumnMetadata(entity: Class<*>?, property: String): ColumnMetaData? {
        if (entity == null)
            return null
        return EntityMetaDataRegistry.getColumnMetaData(entity, property)
    }

    private fun getNullIfEmpty(value: String?): String? {
        if (value.isNullOrEmpty()) {
            return null
        }
        return value
    }
}
