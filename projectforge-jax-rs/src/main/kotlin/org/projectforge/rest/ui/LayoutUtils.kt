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

/**
 * Utils for the Layout classes for handling auto max-length (get from JPA entities) and translations as well as
 * generic default layouts for list and edit pages.
 */
class LayoutUtils {

    companion object {
        private val log = org.slf4j.LoggerFactory.getLogger(LayoutUtils::class.java)

        /**
         * Auto-detects max-length of input fields (by referring the @Column annotations of clazz) and
         * i18n-keys (by referring the [org.apache.wicket.extensions.markup.html.repeater.data.table.PropertyColumn] annotations of clazz).
         * @return List of all elements used in the layout.
         */
        fun process(layout: UILayout, clazz: Class<*>): List<Any?> {
            val elements = processAllElements(layout.getAllElements(), clazz)
            var counter = 0
            layout.namedContainers.forEach {
                it.key = "nc-${++counter}"
            }
            layout.title = getLabelTransformation(layout.title)
            return elements
        }

        /**
         * Auto-detects max-length of input fields (by referring the @Column annotations of clazz) and
         * i18n-keys (by referring the [org.apache.wicket.extensions.markup.html.repeater.data.table.PropertyColumn] annotations of clazz).
         * <br>
         * If no named container called "filter-options" is found, it will be attached automatically by calling [addListFilterContainer]
         */
        fun processListPage(layout: UILayout, clazz: Class<*>): UILayout {
            var found = false
            layout.namedContainers.forEach {
                if (it.id == "filter-options") {
                    found = true // Container found. Don't attach it automatically.
                }
            }
            if (!found) {
                addListFilterContainer(layout)
            }
            layout
                    .addAction(UIButton("reset", style = UIButtonStyle.DANGER))
                    .addAction(UIButton("search", style = UIButtonStyle.PRIMARY))
            process(layout, clazz)
            return layout
        }

        fun addListFilterContainer(layout: UILayout, vararg elements: UIElement, autoAppendDefaultSettings: Boolean? = true) {
            val filterGroup = UIGroup()
            elements.forEach { filterGroup.add(it) }
            if (autoAppendDefaultSettings == true) addListDefaultOptions(filterGroup)
            layout.add(UINamedContainer("filter-options").add(filterGroup))
        }

        /**
         * Adds the both checkboxes "only deleted" and "search history" to the given group. This is automatically called
         * by processListPage and appended to the first group found in named container "filter-options".
         */
        fun addListDefaultOptions(group: UIGroup) {
            group
                    .add(UICheckbox("filter.onlyDeleted", label = "onlyDeleted", tooltip = "onlyDeleted.tooltip"))
                    .add(UICheckbox("filter.searchHistory", label = "search.searchHistory", tooltip = "search.searchHistory.additional.tooltip"))
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
            layout.addAction(UIButton("cancel", style = UIButtonStyle.DANGER))
            if (data != null && data.id != null) {
                if (data.isDeleted) layout.add(UIButton("undelete", style = UIButtonStyle.WARNING))
                else layout.addAction(UIButton("markAsDeleted", style = UIButtonStyle.WARNING))
            }
            //if (restService.prepareClone(restService.newBaseDO())) {
            //    layout.addAction(UIButton("clone", style = UIButtonStyle.PRIMARY))
            //}
            if (data != null && data.id != null) {
                layout.addAction(UIButton("update", style = UIButtonStyle.PRIMARY))
            } else {
                layout.addAction(UIButton("create", style = UIButtonStyle.PRIMARY))
            }
            process(layout, clazz)
            return layout
        }

        /**
         * Sets all length of input fields and text areas with maxLength 0 to the Hibernate JPA definition (@Column).
         * @param elements List of all elements used in the layout.
         * @param clazz The class of the property to search for annotations [@PropertyInfo]
         * @return The unmodified parameter elements.
         * @see HibernateUtils.getPropertyLength
         */
        private fun processAllElements(elements: List<Any>, clazz: Class<*>): List<Any?> {
            var counter = 0
            elements.forEach {
                if (it is UIElement) it.key = "el-${++counter}"
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
                    is UICheckbox -> {
                        it.label = getLabelTransformation(it.label)
                        it.tooltip = getLabelTransformationNullable(it.tooltip)
                    }
                    is UITableColumn -> {
                        val translation = processLabelString(it.title, clazz, it.id, it)
                        if (translation != null) it.title = translation
                    }
                    is UIButton -> {
                        if (it.title == null) {
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
                if (it is UILabelledElement && it is UIElement) {
                    var translation = processLabelString(it.label, clazz, getId(it), it)
                    if (translation != null)
                        it.label = translation
                    else {
                        if (it is UILabel)
                            it.label = getLabelTransformation(it.label)
                        else
                            it.label = getLabelTransformationNullable(it.label)
                    }
                    translation = processLabelString(it.additionalLabel, clazz, getId(it), it, true)
                    if (translation != null)
                        it.additionalLabel = translation
                    else
                        it.additionalLabel = getLabelTransformationNullable(it.additionalLabel)
                }
            }
            return elements
        }

        /**
         * @ -> will be replaced by the translation of the property key.
         */
        private fun processLabelString(labelString: String?, clazz: Class<*>, property: String?, uiElement: UIElement,
                                       useAdditionalI18nKey: Boolean = false): String? {
            if (labelString?.startsWith("'") == true) {
                return labelString.substring(1)
            }
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
         * If the given label starts with "'" the label itself as substring after "'" will be returned: "'This is an text." -> "This is an text"<br>
         * Otherwise method [translate] will be called and the result returned.
         * @param label to process
         * @return Modified label or unmodified label.
         */
        private fun getLabelTransformation(label: String? = null): String {
            if (label?.startsWith("'") == true) return label.substring(1)
            return translate(label)
        }

        /**
         * If the given label starts with "'" the label itself as substring after "'" will be returned: "'This is an text." -> "This is an text"<br>
         * Otherwise method [translate] will be called and the result returned.
         * @param label to process
         * @return Modified label or unmodified label.
         */
        private fun getLabelTransformationNullable(label: String? = null): String? {
            if (label?.startsWith("'") == true) return label.substring(1)
            if (label == null) return null
            return translate(label)
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

        /**
         * @return The id of the given element if supported.
         */
        private fun getId(element: UIElement?): String? {
            if (element == null) return null
            if (element is UILabel) {
                return getId(element.reference)
            }
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