package org.projectforge.ui

interface UILabelledElement {
    var label: String?
    var additionalLabel: String?
    var tooltip : String?
    /**
     * Only the clazz property of layout setting is used for getting i18n keys from the entity fields if not given.
     */
    val layoutSettings : LayoutSettings?
}