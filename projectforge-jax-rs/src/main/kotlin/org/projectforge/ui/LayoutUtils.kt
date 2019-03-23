package org.projectforge.ui

import org.projectforge.framework.i18n.I18nHelper
import org.projectforge.framework.persistence.api.HibernateUtils
import org.projectforge.framework.persistence.entities.DefaultBaseDO

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

        fun addListFilterContainer(layout: UILayout, vararg elements: UIElement, filterClass: Class<*>? = null, autoAppendDefaultSettings: Boolean? = true) {
            val filterGroup = UIGroup()
            if (autoAppendDefaultSettings == true) addListDefaultOptions(filterGroup)
            layout.add(UINamedContainer("filter-options").add(filterGroup))
        }

        /**
         * Adds the both checkboxes "only deleted" and "search history" to the given group. This is automatically called
         * by processListPage and appended to the first group found in named container "filter-options".
         */
        fun addListDefaultOptions(group: UIGroup) {
            group
                    .add(UICheckbox("onlyDeleted", label = "onlyDeleted", tooltip = "onlyDeleted.tooltip"))
                    .add(UICheckbox("searchHistory", label = "search.searchHistory", tooltip = "search.searchHistory.additional.tooltip"))
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
         * Does translation of buttons and UILabels
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
                    is UILabel -> {
                        if (!it.protectLabel) {
                            it.label = getLabelTransformation(it.label)
                            it.additionalLabel = getLabelTransformation(it.label)
                        }
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
            }
            return elements
        }

        /**
         * @return The id of the given element if supported.
         */
        internal fun getId(element: UIElement?, followLabelReference: Boolean = true): String? {
            if (element == null) return null
            if (followLabelReference == true && element is UILabel) {
                return getId(element.reference)
            }
            return when (element) {
                is UIInput -> element.id
                is UICheckbox -> element.id
                is UISelect -> element.id
                is UIMultiSelect -> element.id
                is UITextarea -> element.id
                is UITableColumn -> element.id
                else -> null
            }
        }

        /**
         * If the given label starts with "'" the label itself as substring after "'" will be returned: "'This is an text." -> "This is an text"<br>
         * Otherwise method [translate] will be called and the result returned.
         * @param label to process
         * @return Modified label or unmodified label.
         */
        internal fun getLabelTransformation(label: String?): String? {
            if (label == null)
                return null
            if (label.startsWith("'") == true)
                return label.substring(1)
            return translate(label)
        }

    }
}