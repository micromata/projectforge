package org.projectforge.ui

import org.projectforge.framework.i18n.addTranslations
import org.projectforge.framework.i18n.translate
import org.projectforge.menu.MenuItem

class UILayout {
    constructor(title: String) {
        this.title = LayoutUtils.getLabelTransformation(title)
    }

    var title: String?
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
    fun add(layoutSettings: LayoutContext, vararg ids: String, createRowCol: Boolean = true): UILayout {
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
            is UISelect<*> -> element.values.forEach { list.add(it) }
            is UITable -> element.columns.forEach { list.add(it) }
        }
    }
}
