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
internal object UIElementsRegistry {
    private val log = org.slf4j.LoggerFactory.getLogger(LayoutUtils::class.java)

    internal class ElementInfo(val propertyType: Class<*>,
                               var maxLength: Int? = null,
                               var required: Boolean = false,
                               var i18nKey: String? = null,
                               var additionalI18nKey: String? = null)

    /**
     * Contains all found and created UIElements named by class:property.
     */
    private val registryMap = mutableMapOf<String, ElementInfo>()
    /**
     * Contains all not found and unavailable UIElements named by class:property.
     */
    private val unavailableElementsSet = mutableSetOf<String>()

    inline fun buildElement(layoutSettings: LayoutSettings, property: String): UIElement? {
        val mapKey = getMapKey(layoutSettings.dataObjectClazz, property)
        if (mapKey == null) {
            return null
        }
        val elementInfo = getElementInfo(layoutSettings.dataObjectClazz, property)
        if (elementInfo == null) {
            log.info("Can't build UIElement from ${mapKey}.")
            return null
        }

        var element: UIElement? =
                when (elementInfo.propertyType) {
                    String::class.java -> UIInput(property, maxLength = elementInfo.maxLength, required = elementInfo.required)
                    Boolean::class.java -> UICheckbox(property)
                    else -> null
                }
        if (element == null) {
            if (elementInfo.propertyType.isEnum) {
                if (I18nEnum::class.java.isAssignableFrom(elementInfo.propertyType)) {
                    element = UISelect(property, i18nEnum = elementInfo.propertyType as Class<out Enum<*>>)
                } else {
                    log.warn("Properties of enum not implementing I18nEnum not yet supported: ${mapKey}.")
                    unavailableElementsSet.add(mapKey)
                    return null
                }
            }
        }
        if ((layoutSettings.useInlineLabels || element is UILabel) && element is UILabelledElement) {
            if (!elementInfo.i18nKey.isNullOrEmpty())
                element.label = translate(elementInfo.i18nKey)
            if (!elementInfo.additionalI18nKey.isNullOrEmpty())
                element.additionalLabel = translate(elementInfo.additionalI18nKey)
        }
        return element
    }

    private fun getElementInfo(clazz: Class<*>?, property: String): ElementInfo? {
        if (clazz == null)
            return null
        val mapKey = getMapKey(clazz, property)
        if (mapKey == null) {
            return null
        }
        var elementInfo = registryMap.get(mapKey)
        if (elementInfo != null) {
            return elementInfo // Element found
        }
        if (unavailableElementsSet.contains(mapKey)) {
            return null // Element can't be determined (a previous try failed)
        }
        val propertyDescriptor = BeanUtils.getPropertyDescriptor(clazz, property)
        if (propertyDescriptor == null || propertyDescriptor.propertyType == null) {
            log.info("Property ${clazz}.${property} not found. Can't autodetect layout.")
            unavailableElementsSet.add(mapKey)
            return null
        }
        elementInfo = ElementInfo(propertyDescriptor.propertyType)
        val propertyInfo = PropUtils.get(clazz, property)
        val colinfo = getColumnMetadata(clazz, property)

        elementInfo.maxLength = colinfo?.getMaxLength()
        if (!(colinfo?.isNullable == true) || propertyInfo.required == true)
            elementInfo.required = true
        elementInfo.i18nKey = getNullIfEmpty(propertyInfo?.i18nKey)
        elementInfo.additionalI18nKey = getNullIfEmpty(propertyInfo?.additionalI18nKey)

        registryMap.put(mapKey, elementInfo)
        return elementInfo
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