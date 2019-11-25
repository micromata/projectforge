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

import org.projectforge.framework.i18n.addTranslations
import org.projectforge.framework.i18n.translate
import org.projectforge.menu.MenuItem

class UILayout {
    class UserAccess(
            /**
             * The user has access to the object's history, if given.
             */
            var history: Boolean? = null,
            /**
             * The user has access to insert new objects.
             */
            var insert: Boolean? = null,
            var update: Boolean? = null,
            var delete: Boolean? = null
    ) {
        fun copyFrom(userAccess: UserAccess?) {
            this.history = userAccess?.history
            this.insert = userAccess?.insert
            this.update = userAccess?.update
            this.delete = userAccess?.delete
        }
        fun onlySelectAccess(): Boolean {
            return (insert != true && update != true && delete != true)
        }
    }

    constructor(title: String) {
        this.title = LayoutUtils.getLabelTransformation(title)
    }

    var title: String?
    /**
     * UserAccess only for displaying purposes. The real user access will be definitely checked before persisting any
     * data.
     */
    val userAccess = UserAccess()
    /**
     * Should only be true for edit pages, if history entries are supported or given (normally not, if editing new entries).
     * Show history is only true, if userAccess.history is also true.
     */
    var showHistory: Boolean? = null
    val layout: MutableList<UIElement> = mutableListOf()
    val namedContainers: MutableList<UINamedContainer> = mutableListOf()
    /**
     * The action buttons.
     */
    val actions = mutableListOf<UIElement>()

    val pageMenu = mutableListOf<MenuItem>()

    /**
     * All required translations for the frontend dependent on the logged-in-user's language.
     */
    val translations = mutableMapOf<String, String>()

    /**
     * @param i18nKey The translation i18n key. The translation for the logged-in-user will be added.
     * @return this for chaining.
     */
    fun addTranslations(translations: Map<String, String>): UILayout {
        this.translations.putAll(translations)
        return this
    }

    /**
     * @param i18nKey The translation i18n key. The translation for the logged-in-user will be added.
     * @return this for chaining.
     */
    fun addTranslations(vararg i18nKeys: String): UILayout {
        addTranslations(*i18nKeys, translations = translations)
        return this
    }

    /**
     * @param i18nKey The translation i18n key. The translation for the logged-in-user will be added.
     * @return this for chaining.
     */
    fun addTranslation(i18nKey: String, translation: String): UILayout {
        translations.put(i18nKey, translate(translation))
        return this
    }

    fun add(element: UIElement): UILayout {
        layout.add(element)
        return this
    }

    fun addAction(element: UIElement): UILayout {
        actions.add(element)
        return this
    }

    fun add(namedContainer: UINamedContainer): UILayout {
        namedContainers.add(namedContainer)
        return this
    }

    fun add(menu: MenuItem, index: Int? = null): UILayout {
        if (index != null)
            pageMenu.add(index, menu)
        else
            pageMenu.add(menu)
        return this
    }

    fun postProcessPageMenu() {
        pageMenu.forEach {
            it.postProcess()
        }
    }

    /**
     * Convenient method for adding a bunch of UIInput fields with the given ids.
     * @param createRowCol If true (default), the elements will be surrounded with [UIRow] and [UICol] each, otherwise not.
     */
    fun add(layoutSettings: LayoutContext, vararg ids: String, createRowCol: Boolean = false): UILayout {
        ids.forEach {
            val element = LayoutUtils.buildLabelInputElement(layoutSettings, it)
            if (element != null) {
                add(LayoutUtils.prepareElementToAdd(element, createRowCol))
            }
        }
        return this
    }

    fun getAllElements(): List<Any> {
        val list = mutableListOf<Any>()
        addAllElements(list, layout)
        namedContainers.forEach { addAllElements(list, it.content) }
        addAllElements(list, actions)
        return list
    }

    fun getElementById(id: String): UIElement? {
        var element = getElementById(id, layout)
        if (element != null)
            return element
        return null
    }

    fun getTableColumnById(id: String): UITableColumn {
        return getElementById(id) as UITableColumn
    }

    fun getInputById(id: String): UIInput {
        return getElementById(id) as UIInput
    }

    fun getTextAreaById(id: String): UITextArea {
        return getElementById(id) as UITextArea
    }

    fun getLabelledElementById(id: String): UILabelledElement {
        return getElementById(id) as UILabelledElement
    }

    fun getNamedContainerById(id: String): UINamedContainer? {
        namedContainers.forEach {
            if (it.id == id) {
                return it
            }
        }
        return null
    }

    fun getMenuById(id: String): MenuItem? {
        pageMenu.forEach {
            val found = it.get(id)
            if (found != null) {
                return found
            }
        }
        return null
    }

    private fun getElementById(id: String, elements: List<UIElement>): UIElement? {
        elements.forEach {
            if (LayoutUtils.getId(it, followLabelReference = false) == id)
                return it
            val element = when (it) {
                is UIGroup -> getElementById(id, it.content)
                is UIRow -> getElementById(id, it.content)
                is UICol -> getElementById(id, it.content)
                is UITable -> getElementById(id, it.columns)
                is UIList -> getElementById(id, it.content)
                else -> null
            }
            if (element != null)
                return element
        }
        return null
    }

    private fun addAllElements(list: MutableList<Any>, elements: MutableList<UIElement>) {
        elements.forEach { addAllElements(list, it) }
    }

    private fun addAllCols(list: MutableList<Any>, cols: MutableList<UICol>) {
        cols.forEach { addAllElements(list, it) }
    }

    private fun addAllElements(list: MutableList<Any>, element: UIElement) {
        list.add(element)
        when (element) {
            is UIGroup -> addAllElements(list, element.content)
            is UIRow -> addAllCols(list, element.content)
            is UICol -> addAllElements(list, element.content)
            is UISelect<*> -> {
                val values = element.values
                if (values != null)
                    values.forEach { list.add(it) }
            }
            is UITable -> element.columns.forEach { list.add(it) }
            is UIList -> addAllElements(list, element.content)
        }
    }
}
