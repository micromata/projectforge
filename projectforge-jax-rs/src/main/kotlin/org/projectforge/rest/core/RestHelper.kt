package org.projectforge.rest.core

import org.projectforge.framework.time.PFDateTime
import org.projectforge.rest.converter.DateTimeFormat
import java.net.URI
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.time.format.DateTimeParseException
import javax.servlet.http.HttpServletRequest


class RestHelper {
    companion object {

        private val log = org.slf4j.LoggerFactory.getLogger(RestHelper::class.java)

        fun buildUri(request: HttpServletRequest, path: String): URI {
            return URI("${getRootUrl(request)}/$path")
        }

        fun getRootUrl(request: HttpServletRequest): String {
            val serverName = request.serverName
            val portNumber = request.serverPort
            return if (portNumber != 80 && portNumber != 443) "$serverName:$portNumber" else serverName
        }

        fun parseJSDateTime(jsString: String?): PFDateTime? {
            if (jsString.isNullOrBlank())
                return null
            try {
                val length = jsString.length
                val formatter =
                        when {
                            length > 19 -> jsonDateTimeFormatter
                            length > 16 -> jsonDateTimeSecondsFormatter
                            length > 10 -> jsonDateTimeMinutesFormatter
                            else -> jsonDateFormatter
                        }
                if (formatter != jsonDateFormatter)
                    return PFDateTime.parseUTCDate(jsString, formatter)
                val local = LocalDate.parse(jsString, jsonDateFormatter) // Parses UTC as local date.
                return PFDateTime.from(local)
            } catch (ex: DateTimeParseException) {
                log.error("Error while parsing date '$jsString': ${ex.message}.")
                return null
            }
        }

        fun parseLong(request: HttpServletRequest, parameter: String): Long? {
            val value = request.getParameter(parameter)
            if (value == null)
                return null
            try {
                return value.toLong()
            } catch (ex: DateTimeParseException) {
                log.error("Error while parsing date millis '$value': ${ex.message}.")
                return null
            }
        }

        private val jsonDateTimeFormatter = DateTimeFormatter.ofPattern(DateTimeFormat.JS_DATE_TIME_MILLIS.pattern)
        private val jsonDateTimeSecondsFormatter = DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss")
        private val jsonDateTimeMinutesFormatter = DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm")
        private val jsonDateFormatter = DateTimeFormatter.ofPattern("yyyy-MM-dd")
    }
}
