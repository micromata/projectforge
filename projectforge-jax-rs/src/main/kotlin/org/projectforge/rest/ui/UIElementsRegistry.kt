package org.projectforge.rest.ui

import de.micromata.genome.jpa.metainf.ColumnMetadata
import org.projectforge.common.i18n.I18nEnum
import org.projectforge.common.props.PropUtils
import org.projectforge.framework.persistence.jpa.PfEmgrFactory
import org.projectforge.ui.*
import org.springframework.beans.BeanUtils

/**
 * Registry holds properties of UIElements...
 */
object UIElementsRegistry {
    private val log = org.slf4j.LoggerFactory.getLogger(LayoutUtils::class.java)
    /**
     * Contains all found and created UIElements named by class:property.
     */
    private val registryMap = mutableMapOf<String, UIElement>()
    /**
     * Contains all not found and unavailable UIElements named by class:property.
     */
    private val unavailableElementsSet = mutableSetOf<String>()

    fun getElement(clazz: Class<*>?, property: String): UIElement? {
        if (clazz == null)
            return null
        val mapKey = getMapKey(clazz, property)
        if (mapKey == null) {
            return null
        }
        var element = registryMap.get(mapKey)
        if (element != null) {
            return element // Element found
        }
        if (unavailableElementsSet.contains(mapKey)) {
            return null // Element can't be determined (a previous try failed)
        }
        val propertyDescriptor = BeanUtils.getPropertyDescriptor(clazz, property)
        if (propertyDescriptor == null) {
            log.info("Property ${clazz}.${property} not found. Can't autodetect layout.")
            unavailableElementsSet.add(mapKey)
            return null
        }

        val propertyInfo = PropUtils.get(clazz, property)
        val colinfo = getColumnMetadata(clazz, property)

        val propertyType = propertyDescriptor.propertyType
        val maxLength = colinfo?.getMaxLength()
        val required = !(colinfo?.isNullable == true) || (propertyInfo.required == true)
        val i18nKey = getNullIfEmpty(propertyInfo?.i18nKey)
        val addtionalI18nKey = getNullIfEmpty(propertyInfo?.additionalI18nKey)
        element = when (propertyType) {
            String::class.java -> UIInput(property, maxLength = maxLength, required = required)
            Boolean::class.java -> UICheckbox(property)
            else -> null
        }
        if (element == null) {
            if (propertyType.isEnum) {
                if (I18nEnum::class.java.isAssignableFrom(propertyType)) {
                    element = UISelect(property, i18nEnum = propertyType as Class<out Enum<*>>)
                } else {
                    log.warn("Properties of enum not implementing I18nEnum not yet supported: ${mapKey}.")
                    unavailableElementsSet.add(mapKey)
                    return null
                }
            }
        }
        if (element is UILabelledElement) {
            if (i18nKey != null)
                element.label = i18nKey
            if (addtionalI18nKey != null)
                element.additionalLabel = addtionalI18nKey
        }

        return element
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
        return persistentClass.columns[property]
    }

    private fun getNullIfEmpty(value: String?): String? {
        if (value.isNullOrEmpty()) {
            return null
        }
        return value
    }
}