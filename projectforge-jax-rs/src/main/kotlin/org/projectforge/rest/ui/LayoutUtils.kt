package org.projectforge.rest.ui

import org.projectforge.common.i18n.I18nEnum
import org.projectforge.common.props.PropUtils
import org.projectforge.framework.i18n.I18nHelper
import org.projectforge.framework.persistence.api.HibernateUtils
import org.projectforge.framework.persistence.entities.DefaultBaseDO
import org.projectforge.ui.*

fun translate(i18nKey: String?): String {
    if (i18nKey == null) return "???"
    return I18nHelper.getLocalizedMessage(i18nKey)
}

fun translate(i18nKey: String?, vararg params: Any): String {
    if (i18nKey == null) return "???"
    return I18nHelper.getLocalizedMessage(i18nKey, params)
}

class LayoutUtils {

    companion object {
        private val log = org.slf4j.LoggerFactory.getLogger(LayoutUtils::class.java)

        /**
         * Auto-detects max-length of input fields (by referring the @Column annotations of clazz) and
         * i18n-keys (by referring the [org.apache.wicket.extensions.markup.html.repeater.data.table.PropertyColumn] annotations of clazz).
         */
        fun process(layout: UILayout, clazz: Class<*>): UILayout {
            processAllElements(layout.getAllElements(), clazz)
            layout.title = translate(layout.title)
            return layout
        }

        /**
         * Auto-detects max-length of input fields (by referring the @Column annotations of clazz) and
         * i18n-keys (by referring the @PropertColumn annotations of clazz).<br>
         * Adds the action buttons (cancel, undelete, markAsDeleted, update and/or add dependent on the given data.<br>
         * Calls also fun [process].
         * @see LayoutUtils.process
         */
        fun processEditPage(layout: UILayout,
                //restService: AbstractDORest<ExtendedBaseDO<Int>,
                // BaseDao<ExtendedBaseDO<Int>>>,
                            clazz: Class<*>,
                            data: DefaultBaseDO?): UILayout {
            layout.addAction(UIButton("cancel", "@", UIButtonStyle.DANGER))
            if (data != null && data.id != null) {
                if (data.isDeleted) layout.add(UIButton("undelete", "@", UIButtonStyle.WARNING))
                else layout.addAction(UIButton("markAsDeleted", "@", UIButtonStyle.WARNING))
            }
            //if (restService.prepareClone(restService.newBaseDO())) {
            //    layout.addAction(UIButton("clone", "@", UIButtonStyle.PRIMARY))
            //}
            if (data != null && data.id != null) {
                layout.addAction(UIButton("update", "@", UIButtonStyle.PRIMARY))
            } else {
                layout.addAction(UIButton("create", "@", UIButtonStyle.PRIMARY))
            }
            process(layout, clazz)
            return layout;
        }

        /**
         * Sets all length of input fields and text areas with maxLength 0 to the Hibernate JPA definition (@Column).
         * @see HibernateUtils.getPropertyLength
         */
        private fun processAllElements(elements: List<Any>, clazz: Class<*>) {
            var counter = 0
            elements.forEach {
                if (it is UIElement) it.key = ++counter
                when (it) {
                    is UIInput -> {
                        val maxLength = getMaxLength(clazz, it.maxLength, it.id, it)
                        if (maxLength != null) it.maxLength = maxLength
                    }
                    is UITextarea -> {
                        val maxLength = getMaxLength(clazz, it.maxLength, it.id, it)
                        if (maxLength != null) it.maxLength = maxLength
                    }
                    is UILabel -> {
                        var translation = processLabelString(it.value, clazz, getProperty(it.reference), it)
                        if (translation != null) it.value = translation
                        translation = processLabelString(it.additionalValue, clazz, getProperty(it.reference), it, true)
                        if (translation != null) it.additionalValue = translation
                    }
                    is UISelect -> {
                        if (it.i18nEnum != null) {
                            getEnumValues(it.i18nEnum).forEach { value ->
                                if (value is I18nEnum) {
                                    val translation = translate(value.i18nKey)
                                    it.add(UISelectValue(value.name, translation))
                                } else {
                                    log.error("UISelect supports only enums of type I18nEnum, not '${value}': '${it}'")
                                }
                            }
                        }
                    }
                    is UITableColumn -> {
                        val translation = processLabelString(it.title, clazz, it.id, it)
                        if (translation != null) it.title = translation
                    }
                    is UIButton -> {
                        if (it.title == "@") {
                            val i18nKey = when (it.id) {
                                "cancel" -> "cancel"
                                "clone" -> "clone"
                                "create" -> "create"
                                "markAsDeleted" -> "markAsDeleted"
                                "reset" -> "reset"
                                "search" -> "search"
                                "undelete" -> "undelete"
                                "update" -> "save"
                                else -> null
                            }
                            if (i18nKey == null) {
                                log.error("i18nKey not found for action button '${it.id}'.")
                            } else {
                                it.title = translate(i18nKey)
                            }
                        }
                    }
                }
            }
        }

        /**
         * @ -> will be replaced by the translation of the property key.
         */
        private fun processLabelString(labelString: String?, clazz: Class<*>, property: String?, uiElement: UIElement,
                                       useAdditionalI18nKey: Boolean = false): String? {
            // additionalI18nKey may be null or '@'
            if ((useAdditionalI18nKey && labelString == null) || labelString == "@") {
                if (property == null) {
                    // This may occur for UILabels without reference to property.
                    if (labelString == "@")
                        log.error("Can't find null property, element ${uiElement}.")
                    return null
                }
                return getI18nKey(clazz, "@", property, uiElement, useAdditionalI18nKey)
            }
            return null
        }

        // fun getEnumValues(enumClass: KClass<out Enum<*>>): Array<out Enum<*>> = enumClass.java.enumConstants
        private fun getEnumValues(enumClass: Class<out Enum<*>>): Array<out Enum<*>> = enumClass.enumConstants

        private fun getMaxLength(clazz: Class<*>, current: Int?, property: String, element: UIElement): Int? {
            if (current != null) return null;
            val maxLength = HibernateUtils.getPropertyLength(clazz, property)
            if (maxLength == null) {
                log.error("Length not found in Entity '${clazz}' for UI element '${element}'.")
            }
            return maxLength
        }

        /**
         * @param clazz of the DO object.
         * @param current The current label.
         * @param property The name of the property (field) of the DO object.
         * @param elemnent The current proceeded element.
         */
        private fun getI18nKey(clazz: Class<*>, current: String?, property: String?, element: UIElement,
                               useAdditionalI18nKey: Boolean = false): String? {
            if (current != "@") return null;
            val propInfo = PropUtils.get(clazz, property)
            if (propInfo == null) {
                log.error("PropertyInfo not found in Entity '${clazz}' for UI element '${element}'.")
                return null
            }
            if (useAdditionalI18nKey) {
                if (propInfo.additionalI18nKey.isNotEmpty()) return translate(propInfo.additionalI18nKey)
                return null
            }
            return translate(propInfo.i18nKey)
        }

        private fun getProperty(element: UIElement?): String? {
            if (element == null) return null
            return when (element) {
                is UIInput -> element.id
                is UISelect -> element.id
                is UIMultiSelect -> element.id
                is UITextarea -> element.id
                is UITableColumn -> element.id
                else -> null
            }
        }

    }
}