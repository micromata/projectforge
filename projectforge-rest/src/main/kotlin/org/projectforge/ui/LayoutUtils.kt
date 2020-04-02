/////////////////////////////////////////////////////////////////////////////
//
// Project ProjectForge Community Edition
//         www.projectforge.org
//
// Copyright (C) 2001-2020 Micromata GmbH, Germany (www.micromata.com)
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

import org.projectforge.favorites.Favorites
import org.projectforge.framework.i18n.addTranslations
import org.projectforge.framework.i18n.translate
import org.projectforge.framework.persistence.api.BaseDao
import org.projectforge.framework.persistence.api.ExtendedBaseDO
import org.projectforge.framework.persistence.api.HibernateUtils
import org.projectforge.model.rest.RestPaths
import org.projectforge.rest.core.AbstractPagesRest

/**
 * Utils for the Layout classes for handling auto max-length (get from JPA entities) and translations as well as
 * generic default layouts for list and edit pages.
 */
class LayoutUtils {

    companion object {
        private val log = org.slf4j.LoggerFactory.getLogger(LayoutUtils::class.java)

        @JvmStatic
        fun addCommonTranslations(translations: MutableMap<String, String>) {
            addTranslations("select.placeholder", "calendar.today", "task.title.list.select", translations = translations)
        }

        /**
         * Auto-detects max-length of input fields (by referring the @Column annotations of clazz) and
         * i18n-keys (by referring the [org.projectforge.common.anots.PropertyInfo] annotations of clazz).
         * @return List of all elements used in the layout.
         */
        @JvmStatic
        fun process(layout: UILayout): List<Any?> {
            val elements = processAllElements(layout.getAllElements())
            var counter = 0
            layout.namedContainers.forEach {
                it.key = "nc-${++counter}"
            }
            return elements
        }

        /**
         * Auto-detects max-length of input fields (by referring the @Column annotations of clazz) and
         * i18n-keys (by referring the [org.projectforge.common.anots.PropertyInfo] annotations of clazz).
         */
        @JvmOverloads
        @JvmStatic
        fun processListPage(layout: UILayout, pagesRest: AbstractPagesRest<out ExtendedBaseDO<Int>, *, out BaseDao<*>>): UILayout {
            layout
                    .addAction(UIButton("reset",
                            color = UIColor.SECONDARY,
                            outline = true,
                            responseAction = ResponseAction(pagesRest.getRestPath(RestPaths.FILTER_RESET), targetType = TargetType.GET)))
                    .addAction(UIButton("search",
                            color = UIColor.PRIMARY,
                            default = true,
                            responseAction = ResponseAction(pagesRest.getRestPath(RestPaths.LIST), targetType = TargetType.POST)))
            process(layout)
            layout.addTranslations("search")
            addCommonTranslations(layout)
            Favorites.addTranslations(layout.translations)
            return layout
        }

        /**
         * Auto-detects max-length of input fields (by referring the @Column annotations of clazz) and
         * i18n-keys (by referring the @PropertColumn annotations of clazz).<br>
         * Adds the action buttons (cancel, undelete, markAsDeleted, update and/or add dependent on the given data.<br>
         * Calls also fun [process].
         * @see LayoutUtils.process
         */
        @JvmOverloads
        @JvmStatic
        fun <O : ExtendedBaseDO<Int>> processEditPage(layout: UILayout, dto: Any, pagesRest: AbstractPagesRest<O, *, out BaseDao<O>>)
                : UILayout {
            layout.addAction(UIButton("cancel",
                    color = UIColor.SECONDARY,
                    outline = true,
                    responseAction = ResponseAction(pagesRest.getRestPath(RestPaths.CANCEL), targetType = TargetType.POST)))
            val userAccess = layout.userAccess
            if (pagesRest.isHistorizable()) {
                // 99% of the objects are historizable (undeletable):
                if (pagesRest.getId(dto) != null) {
                    if (userAccess.history == true) {
                        layout.showHistory = true
                    }
                    if (pagesRest.isDeleted(dto)) {
                        if (userAccess.insert == true) {
                            layout.addAction(UIButton("undelete",
                                    color = UIColor.PRIMARY,
                                    responseAction = ResponseAction(pagesRest.getRestPath(RestPaths.UNDELETE), targetType = TargetType.PUT)))
                        }
                    } else if (userAccess.delete == true) {
                        layout.addAction(UIButton("markAsDeleted",
                                color = UIColor.DANGER,
                                outline = true,
                                responseAction = ResponseAction(pagesRest.getRestPath(RestPaths.MARK_AS_DELETED), targetType = TargetType.DELETE),
                                confirmMessage = translate("question.markAsDeletedQuestion")))

                        layout.addTranslations("yes", "cancel")
                    }
                }
            } else if (userAccess.delete == true) {
                // MemoDO for example isn't historizable:
                layout.addAction(UIButton("deleteIt",
                        color = UIColor.DANGER,
                        outline = true,
                        responseAction = ResponseAction(pagesRest.getRestPath(RestPaths.DELETE), targetType = TargetType.DELETE),
                        confirmMessage = translate("question.deleteQuestion")))

                layout.addTranslations("yes", "cancel")
            }
            if (pagesRest.getId(dto) != null) {
                if (pagesRest.cloneSupport != AbstractPagesRest.CloneSupport.NONE) {
                    layout.addAction(UIButton("clone",
                            color = UIColor.SECONDARY,
                            outline = true,
                            responseAction = ResponseAction(pagesRest.getRestPath(RestPaths.CLONE), targetType = TargetType.POST)))
                }
                if (!pagesRest.isDeleted(dto)) {
                    if (userAccess.update == true) {
                        layout.addAction(UIButton("update",
                                color = UIColor.PRIMARY,
                                default = true,
                                responseAction = ResponseAction(pagesRest.getRestPath(RestPaths.SAVE_OR_UDATE), targetType = TargetType.PUT)))
                    }
                }
            } else if (userAccess.insert == true) {
                layout.addAction(UIButton("create",
                        color = UIColor.SUCCESS,
                        default = true,
                        responseAction = ResponseAction(pagesRest.getRestPath(RestPaths.SAVE_OR_UDATE), targetType = TargetType.PUT)))
            }
            process(layout)
            layout.addTranslations("label.historyOfChanges")
            addCommonTranslations(layout)
            return layout
        }

        private fun addCommonTranslations(layout: UILayout) {
            addCommonTranslations(layout.translations)
        }

        /**
         * @param layoutSettings One element is returned including the label (e. g. UIInput).
         */
        internal fun buildLabelInputElement(layoutSettings: LayoutContext, id: String): UIElement? {
            return ElementsRegistry.buildElement(layoutSettings, id)
        }

        /**
         * @param createRowCol If true, a new [UIRow] containing a new [UICol] with the given element is returned,
         * otherwise the element itself without any other operation.
         * @return The element itself or the surrounding [UIRow].
         */
        internal fun prepareElementToAdd(element: UIElement, createRowCol: Boolean): UIElement {
            return if (createRowCol) {
                val row = UIRow()
                val col = UICol()
                row.add(col)
                col.add(element)
                row
            } else {
                element
            }
        }

        internal fun setLabels(elementInfo: ElementInfo?, element: UILabelledElement) {
            if (elementInfo == null)
                return
            if (!elementInfo.i18nKey.isNullOrEmpty())
                element.label = elementInfo.i18nKey
            if (!elementInfo.additionalI18nKey.isNullOrEmpty() && !element.ignoreAdditionalLabel)
                element.additionalLabel = elementInfo.additionalI18nKey
            if (!elementInfo.tooltipI18nKey.isNullOrEmpty() && !element.ignoreTooltip)
                element.tooltip = elementInfo.tooltipI18nKey
        }

        /**
         * Does translation of buttons and UILabels
         * @param elements List of all elements used in the layout.
         * @return The unmodified parameter elements.
         * @see HibernateUtils.getPropertyLength
         */
        private fun processAllElements(elements: List<Any>): List<Any?> {
            var counter = 0
            elements.forEach {
                if (it is UIElement) it.key = "el-${++counter}"
                when (it) {
                    is UILabelledElement -> {
                        it.label = getLabelTransformation(it.label, it as UIElement)
                        it.additionalLabel = getLabelTransformation(it.additionalLabel, it, LabelType.ADDITIONAL_LABEL)
                        it.tooltip = getLabelTransformation(it.tooltip, it, LabelType.TOOLTIP)
                    }
                    is UIFieldset -> {
                        it.title = getLabelTransformation(it.title, it as UIElement)
                    }
                    is UITableColumn -> {
                        val translation = getLabelTransformation(it.title)
                        if (translation != null) it.title = translation
                    }
                    is UIAlert -> {
                        val title = getLabelTransformation(it.title)
                        if (title != null) it.title = title
                        val message = getLabelTransformation(it.message)
                        if (message != null) it.message = message
                    }
                    is UIButton -> {
                        if (it.title == null) {
                            val i18nKey = when (it.id) {
                                "cancel" -> "cancel"
                                "clone" -> "clone"
                                "create" -> "create"
                                "deleteIt" -> "delete"
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
                        val tooltip = getLabelTransformation(it.tooltip)
                        if (tooltip != null) it.tooltip = tooltip

                    }
                    is UIList -> {
                        // Translate position label
                        it.positionLabel = translate(it.positionLabel)
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
            if (followLabelReference && element is UILabel) {
                return getId(element.reference)
            }
            return when (element) {
                is UIInput -> element.id
                is UICheckbox -> element.id
                is UIRadioButton -> element.id
                is UIReadOnlyField -> element.id
                is UISelect<*> -> element.id
                is UITextArea -> element.id
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
        internal fun getLabelTransformation(label: String?, labelledElement: UIElement? = null, labelType: LabelType? = null): String? {
            if (label == null) {
                if (labelledElement is UILabelledElement) {
                    val layoutSettings = labelledElement.layoutContext
                    if (layoutSettings != null) {
                        val id = getId(labelledElement)
                        if (id != null) {
                            val elementInfo = ElementsRegistry.getElementInfo(layoutSettings, id)
                            when (labelType) {
                                LabelType.ADDITIONAL_LABEL -> {
                                    if (labelledElement.ignoreAdditionalLabel) {
                                        return null
                                    } else if (elementInfo?.additionalI18nKey != null) {
                                        return translate(elementInfo.additionalI18nKey)
                                    }
                                }
                                LabelType.TOOLTIP -> {
                                    if (labelledElement.ignoreTooltip) {
                                        return null
                                    } else if (elementInfo?.tooltipI18nKey != null) {
                                        return translate(elementInfo.tooltipI18nKey)
                                    }
                                }
                                else -> {
                                    if (elementInfo?.i18nKey != null) {
                                        return translate(elementInfo.i18nKey)
                                    }
                                }
                            }
                        }
                    }
                }
                return null
            }
            if (label.startsWith("'"))
                return label.substring(1)
            return translate(label)
        }
    }

    internal enum class LabelType { ADDITIONAL_LABEL, TOOLTIP }
}
