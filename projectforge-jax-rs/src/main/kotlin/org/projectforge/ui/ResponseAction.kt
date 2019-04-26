package org.projectforge.ui

/**
 * Given as response of a rest call to inform the client on how to proceed.
 */
class ResponseAction(val url: String?,
                     val targetType: TargetType? = TargetType.REDIRECT) {
    internal var variables: MutableMap<String, Any>? = null

    /**
     * @return this for chaining.
     */
    fun addVariable(variable: String, value: Any?) :ResponseAction {
        if (value != null) {
            if (variables == null) {
                variables = mutableMapOf()
            }
            variables!![variable] = value
        }
        return this
    }
}

enum class TargetType {
    /**
     * The client should redirect to the given url. If no type is given, REDIRECT is used as default.
     */
    REDIRECT,
    /**
     * The client will receive a download file after calling the rest service with the given url.
     */
    DOWNLOAD,
    /**
     * The client calls the rest service with the given url and will receive a response.
     */
    RESTCALL
}
