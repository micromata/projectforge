/////////////////////////////////////////////////////////////////////////////
//
// Project ProjectForge Community Edition
//         www.projectforge.org
//
// Copyright (C) 2001-2025 Micromata GmbH, Germany (www.micromata.com)
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

package org.projectforge.rest.core

import jakarta.servlet.http.HttpServletRequest
import mu.KotlinLogging
import org.projectforge.common.MaxFileSizeExceeded
import org.projectforge.common.i18n.UserException
import org.projectforge.common.logging.LogLevel
import org.projectforge.framework.api.TechnicalException
import org.projectforge.framework.i18n.translateMsg
import org.projectforge.framework.utils.ExceptionStackTracePrinter
import org.projectforge.rest.utils.RequestLog
import org.projectforge.ui.UIToast
import org.springframework.core.annotation.AnnotationUtils
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.ControllerAdvice
import org.springframework.web.bind.annotation.ExceptionHandler
import org.springframework.web.bind.annotation.ResponseStatus
import org.springframework.web.multipart.MaxUploadSizeExceededException

private val log = KotlinLogging.logger {}

@ControllerAdvice
internal class GlobalDefaultExceptionHandler {
    @ExceptionHandler(value = [(Exception::class)])
    @Throws(Exception::class)
    fun defaultErrorHandler(request: HttpServletRequest, ex: Throwable): Any {
        // If the exception is annotated with @ResponseStatus rethrow it and let
        // the framework handle it.
        if (AnnotationUtils.findAnnotation(ex.javaClass, ResponseStatus::class.java) != null) {
            throw ex
        }
        if (ex is MaxUploadSizeExceededException) {
            val userEx =
                MaxFileSizeExceeded(
                    ex.maxUploadSize,
                    -1,
                    maxFileSizeSpringProperty = "spring.servlet.multipart.max-file-size|spring.servlet.multipart.max-request-size"
                )
            log.error("${translateMsg(userEx)} ${userEx.logHintMessage}")
            return ResponseEntity.badRequest().body(UIToast.createExceptionToast(userEx))
        }
        if (ex is UserException) {
            if (ex.logHintMessage.isNullOrBlank()) {
                log.error(translateMsg(ex))
            } else {
                log.error("${translateMsg(ex)} ${ex.logHintMessage}")
            }
            return if (ex.displayUserMessage) {
                ResponseEntity.ok().body(UIToast.createExceptionToast(ex))
            } else {
                ResponseEntity.badRequest().body(UIToast.createExceptionToast(ex))
            }
        }
        GlobalExceptionRegistry.findExInfo(ex)?.let {
            return handleKnownException(ex, it)
        }
        val additionalExcptionMessage =
            if (ex is TechnicalException && ex.technicalMessage != null) " technical=${ex.technicalMessage}" else ""
        val exceptionMessage = "${ex::class.java.name}: ${ex.message}$additionalExcptionMessage"
        log.error(
            "Exception while processing request: ${ex.message} Request: ${RequestLog.asJson(request)},\nexception=$exceptionMessage\n${
                ExceptionStackTracePrinter.toString(
                    ex,
                    showExceptionMessage = false,
                    stopBeforeForeignPackages = false,
                    depth = 15
                )
            }"
        )
        log.error(ex.message, ex)
        return ResponseEntity("Internal error.", HttpStatus.BAD_REQUEST)
    }

    private fun handleKnownException(ex: Throwable, exInfo: GlobalExceptionRegistry.ExInfo): Any {
        val msg = if (exInfo.message.isNullOrBlank()) exInfo.message else "${exInfo.message}: ${ex.message}"
        when (exInfo.logLevel) {
            LogLevel.INFO -> log.info(msg)
            else -> log.error(msg)
        }
        return ResponseEntity(msg, exInfo.status)
    }
}
