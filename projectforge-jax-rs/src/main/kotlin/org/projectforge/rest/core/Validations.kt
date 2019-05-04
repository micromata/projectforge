package org.projectforge.rest.core

import com.fasterxml.jackson.annotation.JsonFormat
import com.fasterxml.jackson.databind.exc.InvalidFormatException
import org.projectforge.ui.ResponseAction
import org.projectforge.ui.ValidationError
import org.springframework.beans.TypeMismatchException
import org.springframework.core.Ordered
import org.springframework.core.annotation.Order
import org.springframework.http.HttpHeaders
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.http.converter.HttpMessageNotReadableException
import org.springframework.validation.BindException
import org.springframework.validation.FieldError
import org.springframework.validation.ObjectError
import org.springframework.web.HttpMediaTypeNotSupportedException
import org.springframework.web.HttpRequestMethodNotSupportedException
import org.springframework.web.bind.MethodArgumentNotValidException
import org.springframework.web.bind.MissingServletRequestParameterException
import org.springframework.web.bind.annotation.ControllerAdvice
import org.springframework.web.bind.annotation.ExceptionHandler
import org.springframework.web.context.request.WebRequest
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException
import org.springframework.web.multipart.support.MissingServletRequestPartException
import org.springframework.web.servlet.NoHandlerFoundException
import org.springframework.web.servlet.mvc.method.annotation.ResponseEntityExceptionHandler
import java.time.LocalDateTime
import javax.validation.ConstraintViolationException

/**
 * Thanx to: https://www.baeldung.com/global-error-handler-in-a-spring-rest-api
 * Thanx to: https://github.com/eugenp/tutorials/blob/master/spring-security-rest/src/main/java/org/baeldung/web/CustomRestExceptionHandler.java
 */
@Order(Ordered.HIGHEST_PRECEDENCE)
@ControllerAdvice
class CustomRestExceptionHandler : ResponseEntityExceptionHandler() {
    private val log = org.slf4j.LoggerFactory.getLogger(CustomRestExceptionHandler::class.java)

// 400

    override fun handleMethodArgumentNotValid(ex: MethodArgumentNotValidException, headers: HttpHeaders, status: HttpStatus, request: WebRequest): ResponseEntity<Any> {
        logger.info(ex.javaClass.name)
        val apiError = ApiError(HttpStatus.BAD_REQUEST, ex.localizedMessage)
        apiError.addFieldErrors(ex.bindingResult.fieldErrors)
        apiError.addObjectErrors(ex.bindingResult.globalErrors)
        return handleExceptionInternal(ex, apiError, headers, apiError.status, request)
    }

    override fun handleBindException(ex: BindException, headers: HttpHeaders, status: HttpStatus, request: WebRequest): ResponseEntity<Any> {
        logger.info(ex.javaClass.name)
        val apiError = ApiError(HttpStatus.BAD_REQUEST, ex.localizedMessage)
        apiError.addFieldErrors(ex.bindingResult.fieldErrors)
        apiError.addObjectErrors(ex.bindingResult.globalErrors)
        return handleExceptionInternal(ex, apiError, headers, apiError.status, request)
    }

    override fun handleTypeMismatch(ex: TypeMismatchException, headers: HttpHeaders, status: HttpStatus, request: WebRequest): ResponseEntity<Any> {
        logger.info(ex.javaClass.name)
        val error = "${ex.getValue()} value for '${ex.getPropertyName()}' should be of type '${ex.getRequiredType()}'"
        val apiError = ApiError(HttpStatus.BAD_REQUEST, ex.getLocalizedMessage(), error)
        return ResponseEntity(apiError, HttpHeaders(), apiError.status)
    }

    override fun handleMissingServletRequestPart(ex: MissingServletRequestPartException, headers: HttpHeaders, status: HttpStatus, request: WebRequest): ResponseEntity<Any> {
        logger.info(ex.javaClass.name)
        val error = ex.requestPartName + " part is missing"
        val apiError = ApiError(HttpStatus.BAD_REQUEST, ex.localizedMessage, error)
        return ResponseEntity(apiError, HttpHeaders(), apiError.status)
    }

    override fun handleMissingServletRequestParameter(ex: MissingServletRequestParameterException, headers: HttpHeaders, status: HttpStatus, request: WebRequest): ResponseEntity<Any> {
        logger.info(ex.javaClass.name)
        //
        val error = ex.parameterName + " parameter is missing"
        val apiError = ApiError(HttpStatus.BAD_REQUEST, ex.localizedMessage, error)
        return ResponseEntity(apiError, HttpHeaders(), apiError.status)
    }

    //

    @ExceptionHandler(MethodArgumentTypeMismatchException::class)
    fun handleMethodArgumentTypeMismatch(ex: MethodArgumentTypeMismatchException, request: WebRequest): ResponseEntity<Any> {
        logger.info(ex.javaClass.name)
        //
        val error = ex.name + " should be of type " + ex.requiredType!!.name

        val apiError = ApiError(HttpStatus.BAD_REQUEST, ex.localizedMessage, error)
        return ResponseEntity(apiError, HttpHeaders(), apiError.status)
    }

    @ExceptionHandler(ConstraintViolationException::class)
    fun handleConstraintViolation(ex: ConstraintViolationException, request: WebRequest): ResponseEntity<Any> {
        logger.info(ex.javaClass.name)
        val apiError = ApiError(HttpStatus.BAD_REQUEST, ex.getLocalizedMessage())
        ex.getConstraintViolations()?.forEach {
            apiError.add(ApiSubError(it.getRootBeanClass().getName() + " " + it.getPropertyPath() + ": " + it.getMessage()))
        }

        return ResponseEntity(apiError, HttpHeaders(), apiError.status)
    }

    override fun handleHttpMessageNotReadable(ex: HttpMessageNotReadableException, headers: HttpHeaders, status: HttpStatus, request: WebRequest): ResponseEntity<Any> {
        val cause = ex.cause
        if (cause is InvalidFormatException) {
            val value = cause.value
            val path = cause.path
            val field = cause.path[0].fieldName
            val validationErrors = mutableListOf<ValidationError>()
            validationErrors.add(ValidationError(cause.message, field))
            return ResponseEntity(ResponseAction(validationErrors = validationErrors), HttpStatus.NOT_ACCEPTABLE)
        }
        log.error(ex.message)
        return super.handleHttpMessageNotReadable(ex, headers, status, request)
    }

    // 404

    override fun handleNoHandlerFoundException(ex: NoHandlerFoundException, headers: HttpHeaders, status: HttpStatus, request: WebRequest): ResponseEntity<Any> {
        logger.info(ex.javaClass.name)
        //
            val error = "No handler found for " + ex.httpMethod + " " + ex.requestURL

        val apiError = ApiError(HttpStatus.NOT_FOUND, ex.localizedMessage, error)
        return ResponseEntity(apiError, HttpHeaders(), apiError.status)
    }

    // 405

    override fun handleHttpRequestMethodNotSupported(ex: HttpRequestMethodNotSupportedException, headers: HttpHeaders, status: HttpStatus, request: WebRequest): ResponseEntity<Any> {
        logger.info(ex.javaClass.name)
        //
        val builder = StringBuilder()
        builder.append(ex.method)
        builder.append(" method is not supported for this request. Supported methods are ")
        ex.supportedHttpMethods!!.forEach { t -> builder.append("$t ") }

        val apiError = ApiError(HttpStatus.METHOD_NOT_ALLOWED, ex.localizedMessage, builder.toString())
        return ResponseEntity(apiError, HttpHeaders(), apiError.status)
    }

    // 415

    override fun handleHttpMediaTypeNotSupported(ex: HttpMediaTypeNotSupportedException, headers: HttpHeaders, status: HttpStatus, request: WebRequest): ResponseEntity<Any> {
        logger.info(ex.javaClass.name)
        //
        val builder = StringBuilder()
        builder.append(ex.contentType)
        builder.append(" media type is not supported. Supported media types are ")
        ex.supportedMediaTypes.forEach { t -> builder.append("$t ") }

        val apiError = ApiError(HttpStatus.UNSUPPORTED_MEDIA_TYPE, ex.localizedMessage, builder.substring(0, builder.length - 2))
        return ResponseEntity(apiError, HttpHeaders(), apiError.status)
    }

    // 500

    @ExceptionHandler(Exception::class)
    fun handleAll(ex: Exception, request: WebRequest): ResponseEntity<Any> {
        logger.info(ex.javaClass.name)
        logger.error("error", ex)
        //
        val apiError = ApiError(HttpStatus.INTERNAL_SERVER_ERROR, ex.localizedMessage)
        return ResponseEntity(apiError, HttpHeaders(), apiError.status)
    }
}

class ApiError(var status: HttpStatus,
               var message: String? = null,
               var debugMessage: String? = null
) {
    val subErrors = mutableListOf<ApiSubError>()
    @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd hh:mm:ss")
    val timestamp: LocalDateTime

    init {
        timestamp = LocalDateTime.now()
    }

    fun addFieldErrors(fieldErrors: List<FieldError>) {
        fieldErrors.forEach {
            add(ApiValidationFieldError(it.field, it.rejectedValue, it.defaultMessage))
        }
    }

    fun addObjectErrors(fieldErrors: List<ObjectError>) {
        fieldErrors.forEach {
            add(ApiValidationObjectError(objectName = it.objectName, message = it.defaultMessage))
        }
    }

    fun add(subError: ApiSubError) {
        subErrors.add(subError)
    }
}

open class ApiSubError(var messaage: String?)

class ApiValidationFieldError(
        var field: String?,
        var rejectedValue: Any?,
        message: String?
) : ApiSubError(message)

class ApiValidationObjectError(
        var objectName: String?,
        message: String?
) : ApiSubError(message)
