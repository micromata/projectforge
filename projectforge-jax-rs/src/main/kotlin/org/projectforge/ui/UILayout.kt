package org.projectforge.ui

import org.projectforge.framework.i18n.translate
import org.projectforge.framework.persistence.api.ExtendedBaseDO

class UILayout {
    constructor(title: String) {
        this.title = LayoutUtils.getLabelTransformation(title)
    }

    companion object {
        fun UIEditLayout(i18nPrefix:String, dataObject: ExtendedBaseDO<Int>?): UILayout {
            val titleKey = if (dataObject?.id != null) "$i18nPrefix.edit" else "$i18nPrefix.add"
            return UILayout(titleKey)
        }
    }

    var title: String?
    val layout: MutableList<UIElement> = mutableListOf()
    val namedContainers: MutableList<UINamedContainer> = mutableListOf()
    /**
     * The action buttons.
     */
    val actions = mutableListOf<UIElement>()
    /**
     * All required translations for the frontend dependent on the logged-in-user's language.
     */
    val translations = mutableMapOf<String, String>()

    /**
     * @param i18nKey The translation i18n key. The translation for the logged-in-user will be added.
     * @return this for chaining.
     */
    fun addTranslation(i18nKey: String): UILayout {
        translations.put(i18nKey, translate(i18nKey))
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

    /**
     * Convenient method for adding a bunch of UIInput fields with the given ids.
     */
    fun add(layoutSettings: LayoutContext, vararg ids: String): UILayout {
        ids.forEach {
            val element = LayoutUtils.buildLabelInputElement(layoutSettings, it)
            if (element != null)
                add(element)
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

    fun getNamedContainerById(id: String): UINamedContainer? {
        namedContainers.forEach {
            if (it.id == id) {
                return it
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
            is UISelect -> element.values.forEach { list.add(it) }
            is UITable -> element.columns.forEach { list.add(it) }
        }
    }
}