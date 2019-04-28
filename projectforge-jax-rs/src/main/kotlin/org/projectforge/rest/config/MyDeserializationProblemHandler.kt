package org.projectforge.rest.config

import com.fasterxml.jackson.core.JsonParser
import com.fasterxml.jackson.core.JsonToken
import com.fasterxml.jackson.databind.DeserializationContext
import com.fasterxml.jackson.databind.JavaType
import com.fasterxml.jackson.databind.JsonDeserializer
import com.fasterxml.jackson.databind.deser.DeserializationProblemHandler
import com.fasterxml.jackson.databind.deser.ValueInstantiator
import com.fasterxml.jackson.databind.jsontype.TypeIdResolver
import org.projectforge.framework.i18n.translate
import org.projectforge.ui.ValidationError

class MyDeserializationProblemHandler : DeserializationProblemHandler() {
    private val log = org.slf4j.LoggerFactory.getLogger(MyDeserializationProblemHandler::class.java)

    val validationErrors = mutableListOf<ValidationError>()

    override fun handleWeirdNativeValue(ctxt: DeserializationContext, targetType: JavaType?, valueToConvert: Any?, p: JsonParser?): Any {
        logError(ctxt, "handleWeirdNativeValue", valueToConvert)
        return super.handleWeirdNativeValue(ctxt, targetType, valueToConvert, p)
    }

    override fun handleWeirdNumberValue(ctxt: DeserializationContext, targetType: Class<*>?, valueToConvert: Number?, failureMsg: String?): Any {
        logError(ctxt, "handleWeirdNumberValue", valueToConvert)
        return super.handleWeirdNumberValue(ctxt, targetType, valueToConvert, failureMsg)
    }

    override fun handleUnexpectedToken(ctxt: DeserializationContext, targetType: Class<*>?, t: JsonToken?, p: JsonParser?, failureMsg: String?): Any {
        logError(ctxt, "handleUnexpectedToken", t)
        return super.handleUnexpectedToken(ctxt, targetType, t, p, failureMsg)
    }

    override fun handleWeirdStringValue(ctxt: DeserializationContext, targetType: Class<*>?, valueToConvert: String?, failureMsg: String?): Any {
        var i18nKey: String? = null
        var result: Any? = null
        when (targetType) {
            Integer::class.java -> {
                i18nKey = "validation.error.format.integer"
                result = 0
            }
            java.lang.Long::class.java -> {
                i18nKey = "validation.error.format.integer"
                result = 0L
            }
            Long::class.java -> {
                i18nKey = "validation.error.format.integer"
                result = 0L
            }
            java.lang.Double::class.java -> {
                i18nKey = "validation.error.generic"
                result = 0.0
            }
            Double::class.java -> {
                i18nKey = "validation.error.generic"
                result = 0.0
            }
            else -> null
        }
        if (result != null) {
            val field = ctxt.parser.currentName
            //val clazz = ctxt.parser.currentValue::class.java
            validationErrors.add(ValidationError(translate(i18nKey), field, i18nKey))
            return result
        }
        logError(ctxt, "handleWeirdStringValue", valueToConvert)
        return super.handleWeirdStringValue(ctxt, targetType, valueToConvert, failureMsg)
    }

    override fun handleInstantiationProblem(ctxt: DeserializationContext, instClass: Class<*>?, argument: Any?, t: Throwable?): Any {
        logError(ctxt, "handleInstantiationProblem", null)
        return super.handleInstantiationProblem(ctxt, instClass, argument, t)
    }

    override fun handleWeirdKey(ctxt: DeserializationContext, rawKeyType: Class<*>?, keyValue: String?, failureMsg: String?): Any {
        logError(ctxt, "handleWeirdKey", keyValue)
        return super.handleWeirdKey(ctxt, rawKeyType, keyValue, failureMsg)
    }

    override fun handleUnknownProperty(ctxt: DeserializationContext, p: JsonParser?, deserializer: JsonDeserializer<*>?, beanOrClass: Any?, propertyName: String?): Boolean {
        logError(ctxt, "handleUnknownProperty", propertyName)
        return true // Mark the problem as solved
    }

    override fun handleMissingInstantiator(ctxt: DeserializationContext, instClass: Class<*>?, valueInsta: ValueInstantiator?, p: JsonParser?, msg: String?): Any {
        logError(ctxt, "handleMissingInstantiator", msg)
        return super.handleMissingInstantiator(ctxt, instClass, valueInsta, p, msg)
    }

    override fun handleMissingTypeId(ctxt: DeserializationContext, baseType: JavaType?, idResolver: TypeIdResolver?, failureMsg: String?): JavaType {
        logError(ctxt, "handleMissingTypeId", failureMsg)
        return super.handleMissingTypeId(ctxt, baseType, idResolver, failureMsg)
    }

    override fun handleUnknownTypeId(ctxt: DeserializationContext, baseType: JavaType?, subTypeId: String?, idResolver: TypeIdResolver?, failureMsg: String?): JavaType {
        logError(ctxt, "handleUnknownTypeId", failureMsg)
        return super.handleUnknownTypeId(ctxt, baseType, subTypeId, idResolver, failureMsg)
    }

    private fun logError(ctxt: DeserializationContext, method:String, valueToConvert: Any?) {
        val field = ctxt.parser.currentName
        val clazz = ctxt.parser.currentValue::class.java
        log.warn("Json parse error ($method) for $clazz.$field: $valueToConvert")
    }
}
