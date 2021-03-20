package org.projectforge.jcr

import com.fasterxml.jackson.annotation.JsonAutoDetect
import com.fasterxml.jackson.annotation.JsonInclude
import com.fasterxml.jackson.annotation.PropertyAccessor
import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.databind.SerializationFeature
import com.fasterxml.jackson.databind.module.SimpleModule
import mu.KotlinLogging
import java.io.IOException
import java.math.BigDecimal
import java.math.RoundingMode
import java.text.NumberFormat
import java.time.Instant
import java.time.ZoneOffset
import java.time.format.DateTimeFormatter
import java.util.*

private val log = KotlinLogging.logger {}

object PFJcrUtils {

    private val mapper = ObjectMapper()

    fun toJson(obj: Any): String {
        return try {
            mapper.writeValueAsString(obj)
        } catch (ex: Exception) {
            val id = System.currentTimeMillis()
            log.error("Exception while serializing object of type '${obj::class.java.simpleName}' #$id: ${ex.message}", ex)
            "[*** Exception while serializing object of type '${obj::class.java.simpleName}', see log files #$id for more details.]"
        }
    }

    @Throws(IOException::class)
    fun <T> fromJson(json: String?, classOfT: Class<T>?): T {
        return mapper.readValue(json, classOfT)
    }

    fun convertToDate(isoString: String?): Date? {
        if (isoString.isNullOrBlank()) {
            return null
        }
        return Date.from(Instant.from(jsDateTimeFormatter.parse(isoString)))
    }

    fun convertToString(date: Date?): String? {
        date ?: return null
        return jsDateTimeFormatter.format(date.toInstant())
    }

    init {
        mapper.setVisibility(PropertyAccessor.ALL, JsonAutoDetect.Visibility.NONE)
        mapper.setVisibility(PropertyAccessor.FIELD, JsonAutoDetect.Visibility.ANY)
        mapper.setSerializationInclusion(JsonInclude.Include.NON_NULL)
        // mapper.setSerializationInclusion(JsonInclude.Include.NON_DEFAULT)
        mapper.configure(SerializationFeature.FAIL_ON_SELF_REFERENCES, false)
        val module = SimpleModule()
        mapper.registerModule(module)
    }

    fun createSafeFilename(fileObject: FileObject): String {
        val fileName = fileObject.fileName
        if (fileName.isNullOrBlank() || !fileName.contains('.') || fileName.endsWith('.')) {
            return "${fileObject.fileId}.file"
        }
        val extension = fileName.substring(fileName.lastIndexOf('.') + 1)
        return "${fileObject.fileId}.${convertToSafeFilenameExtension(extension)}"
    }

    fun convertToSafeFilenameExtension(extension: String): String {
        val sb = StringBuilder()
        extension.forEach {
            if (it.isLetterOrDigit()) {
                sb.append(it)
            } else {
                sb.append('_')
            }
        }
        return sb.toString()
    }

    private val jsDateTimeFormatter = DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'").withZone(ZoneOffset.UTC)
}
