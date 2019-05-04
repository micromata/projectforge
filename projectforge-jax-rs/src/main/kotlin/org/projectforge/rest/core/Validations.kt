package org.projectforge.rest.core

import com.fasterxml.jackson.databind.exc.InvalidFormatException
import org.projectforge.framework.i18n.translate
import org.projectforge.ui.ResponseAction
import org.projectforge.ui.ValidationError
import org.springframework.core.Ordered
import org.springframework.core.annotation.Order
import org.springframework.http.HttpHeaders
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.http.converter.HttpMessageNotReadableException
import org.springframework.web.bind.annotation.ControllerAdvice
import org.springframework.web.bind.annotation.ExceptionHandler
import org.springframework.web.context.request.WebRequest
import org.springframework.web.servlet.mvc.method.annotation.ResponseEntityExceptionHandler

/**
 * Thanx to: https://www.baeldung.com/global-error-handler-in-a-spring-rest-api
 * Thanx to: https://github.com/eugenp/tutorials/blob/master/spring-security-rest/src/main/java/org/baeldung/web/CustomRestExceptionHandler.java
 */
@Order(Ordered.HIGHEST_PRECEDENCE)
@ControllerAdvice
class CustomRestExceptionHandler : ResponseEntityExceptionHandler() {
    private val log = org.slf4j.LoggerFactory.getLogger(CustomRestExceptionHandler::class.java)

    // 406 - NOT_ACCEPTABLE
    override fun handleHttpMessageNotReadable(ex: HttpMessageNotReadableException, headers: HttpHeaders, status: HttpStatus, request: WebRequest): ResponseEntity<Any> {
        val cause = ex.cause
        if (cause is InvalidFormatException) {
            val value = cause.value
            val path = cause.path
            val field = cause.path[0].fieldName
            val validationErrors = mutableListOf<ValidationError>()
            validationErrors.add(ValidationError(translate("validation.error.format.integer"), field))
            return ResponseEntity(ResponseAction(validationErrors = validationErrors), HttpStatus.NOT_ACCEPTABLE)
        }
        log.error(ex.message)
        return super.handleHttpMessageNotReadable(ex, headers, status, request)
    }

    // 500
    @ExceptionHandler(Exception::class)
    fun handleAll(ex: Exception, request: WebRequest): ResponseEntity<Any> {
        logger.info(ex.javaClass.name)
        logger.error("error", ex)
        //
        val apiError = ApiError(HttpStatus.INTERNAL_SERVER_ERROR, "An internal server error occurred. Please refer the server's log files.")
        return ResponseEntity(apiError, HttpHeaders(), apiError.status)
    }

    /**
     * Overriding this method due to security reasons, because the default implementation returns the error messages to the client. The error message may contain internal
     * server data, such as file paths etc.
     */
    override fun handleExceptionInternal(ex: java.lang.Exception, body: Any?, headers: HttpHeaders, status: HttpStatus, request: WebRequest): ResponseEntity<Any?> {
        log.error("An error while calling the rest service occured: '${ex.message}'.", ex)
        return ResponseEntity(body, headers, status)
    }
}

class ApiError(var status: HttpStatus, var message: String? = null)
