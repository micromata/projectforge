package org.projectforge.rest.ui

data class LayoutSettings(
        /**
         * Data class for auto-detecting JPA-property (@Column), PropertyInfo and property type.
         */
        val dataObjectClazz: Class<*>?, val useInlineLabels: Boolean = true)