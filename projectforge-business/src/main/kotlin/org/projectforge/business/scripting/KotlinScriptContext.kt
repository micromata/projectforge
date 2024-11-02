package org.projectforge.business.scripting

/**
 * Workarround for bindings.
 */
class KotlinScriptContext {
    private val propertyValues = mutableMapOf<String, Any?>()

    fun setProperty(name: String, value: Any?) {
        propertyValues[name] = value
    }

    fun getProperty(name: String): Any? {
        return propertyValues[name]
    }
}
