package org.projectforge.ui

import de.micromata.genome.jpa.metainf.ColumnMetadata
import de.micromata.genome.jpa.metainf.ColumnMetadataBean
import org.projectforge.common.i18n.I18nEnum
import org.projectforge.common.props.PropUtils
import org.projectforge.framework.persistence.jpa.PfEmgrFactory
import org.springframework.beans.BeanUtils
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

    class ElementInfo(val propertyType: Class<*>,
                      var maxLength: Int? = null,
                      var required: Boolean? = null,
                      var i18nKey: String? = null,
                      var additionalI18nKey: String? = null)

    fun getProperties(clazz: Class<*>): Map<String, ElementInfo>? {
        return registryMap.get(clazz)
    }

    /**
     * Contains all found and created UIElements named by class:property.
     */
    private val registryMap = mutableMapOf<Class<*>, MutableMap<String, ElementInfo>>()
    /**
     * Contains all not found and unavailable UIElements named by class:property.
     */
    private val unavailableElementsSet = mutableSetOf<String>()

    internal fun buildElement(layoutSettings: LayoutContext, property: String): UIElement {
        val mapKey = getMapKey(layoutSettings.dataObjectClazz, property)
        if (mapKey == null) {
            return UILabel(property)
        }
        val elementInfo = getElementInfo(layoutSettings.dataObjectClazz, property)
        if (elementInfo == null) {
            log.info("Can't build UIElement from ${mapKey}.")
            return UILabel("??? ${mapKey} ???")
        }

        var element: UIElement? =
                when (elementInfo.propertyType) {
                    String::class.java -> {
                        val maxLength = elementInfo.maxLength
                        if (maxLength != null && maxLength > 255) {
                            UITextArea(property, maxLength = elementInfo.maxLength, layoutSettings = layoutSettings)
                        } else {
                            UIInput(property, maxLength = elementInfo.maxLength, required = elementInfo.required, layoutSettings = layoutSettings)
                        }
                    }
                    Boolean::class.java -> UICheckbox(property)
                    Date::class.java -> UIInput(property, required = elementInfo.required, layoutSettings = layoutSettings, dataType = UIDataType.DATE)
                    java.sql.Date::class.java -> UIInput(property, required = elementInfo.required, layoutSettings = layoutSettings, dataType = UIDataType.DATE)
                    Locale::class.java -> UIInput(property, required = elementInfo.required, layoutSettings = layoutSettings, dataType = UIDataType.LOCALE)
                    else -> null
                }
        if (element == null) {
            if (elementInfo.propertyType.isEnum) {
                if (I18nEnum::class.java.isAssignableFrom(elementInfo.propertyType)) {
                    @Suppress("UNCHECKED_CAST")
                    element = UISelect(property, layoutSettings = layoutSettings)
                            .buildValues(i18nEnum = elementInfo.propertyType as Class<out Enum<*>>)
                } else {
                    log.warn("Properties of enum not implementing I18nEnum not yet supported: ${mapKey}.")
                    unavailableElementsSet.add(mapKey)
                    return UILabel("??? ${mapKey} ???")
                }
            }
        }
        if (element is UILabelledElement) {
            LayoutUtils.setLabels(elementInfo, element)
        }
        return element ?: UILabel(property)
    }

    internal fun getElementInfo(layoutSettings: LayoutContext, property: String): ElementInfo? {
        return ElementsRegistry.getElementInfo(layoutSettings.dataObjectClazz, property)
    }

    internal fun getElementInfo(clazz: Class<*>?, property: String): ElementInfo? {
        if (clazz == null)
            return null
        val mapKey = getMapKey(clazz, property)!!
        var elementInfo = ensureClassMap(clazz).get(property)
        if (elementInfo != null) {
            return elementInfo // Element found
        }
        if (unavailableElementsSet.contains(mapKey)) {
            return null // Element can't be determined (a previous try failed)
        }
        val propertyType = getPropertyType(clazz, property)
        if (propertyType == null) {
            log.info("Property ${clazz}.${property} not found. Can't autodetect layout.")
            unavailableElementsSet.add(mapKey)
            return null
        }
        elementInfo = ElementInfo(propertyType)
        val propertyInfo = PropUtils.get(clazz, property)
        if (propertyInfo == null) {
            log.warn("Property '${clazz}:${property}' not found.")
            return elementInfo
        }
        val colinfo = getColumnMetadata(clazz, property)
        if (colinfo != null) {
            elementInfo.maxLength = colinfo.getMaxLength()
            if (!(colinfo.isNullable) || propertyInfo.required)
                elementInfo.required = true
        }
        elementInfo.i18nKey = getNullIfEmpty(propertyInfo?.i18nKey)
        elementInfo.additionalI18nKey = getNullIfEmpty(propertyInfo?.additionalI18nKey)

        ensureClassMap(clazz).put(property, elementInfo)
        return elementInfo
    }

    private fun getPropertyType(clazz: Class<*>?, property: String): Class<*>? {
        if (clazz == null)
            return null
        val desc = BeanUtils.getPropertyDescriptor(clazz, property)
        if (desc != null)
            return desc.propertyType
        if (clazz.superclass != null && clazz.superclass != Object::class.java)
            return getPropertyType(clazz.superclass, property)
        return null
    }

    private fun ensureClassMap(clazz: Class<*>): MutableMap<String, ElementInfo> {
        var result = registryMap.get(clazz)
        if (result == null) {
            result = mutableMapOf()
            registryMap.put(clazz, result)
        }
        return result
    }

    private fun getMapKey(clazz: Class<*>?, property: String?): String? {
        if (clazz == null || property == null) {
            return null
        }
        return "${clazz.name}.${property}"
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
        val columnMetaData = persistentClass.columns[property]
        if (columnMetaData == null)
            return null
        if (!columnMetaData.isNullable) {
            var joinColumnAnn = columnMetaData.findAnnoation(JoinColumn::class.java)
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