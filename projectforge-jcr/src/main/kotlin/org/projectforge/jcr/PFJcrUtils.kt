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
            return "${fileObject.id}.file"
        }
        val extension = fileName.substring(fileName.lastIndexOf('.')  + 1)
        return "${fileObject.id}.${convertToSafeFilenameExtension(extension)}"
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

    fun formatBytes(bytes: Int?): String {
        return formatBytes(bytes?.toLong())
    }

    /**
     * Pretty output of bytes, "1023 bytes", "1.1 kb", "523 kb", "1.7 Mb", "143 Gb" etc.
     *
     * @param bytes
     * @return
     */
    fun formatBytes(bytes: Long?): String {
        bytes ?: return "? bytes"
        if (bytes < KILO_BYTES) {
            return "$bytes bytes"
        }
        if (bytes < MEGA_BYTES) {
            var no = BigDecimal(bytes).divide(KB_BD, 1, RoundingMode.HALF_UP)
            if (no.toLong() >= 100) {
                no = no.setScale(0, RoundingMode.HALF_UP)
            }
            return NumberFormat.getInstance().format(no) + " kb"
        }
        if (bytes < GIGA_BYTES) {
            var no = BigDecimal(bytes).divide(MB_BD, 1, RoundingMode.HALF_UP)
            if (no.toLong() >= 100) {
                no = no.setScale(0, RoundingMode.HALF_UP)
            }
            return NumberFormat.getInstance().format(no) + " Mb"
        }
        var no = BigDecimal(bytes).divide(GB_BD, 1, RoundingMode.HALF_UP)
        if (no.toLong() >= 100) {
            no = no.setScale(0, RoundingMode.HALF_UP)
        }
        return NumberFormat.getInstance().format(no) + " Gb"
    }

    private const val KILO_BYTES = 1024
    private val KB_BD = BigDecimal(KILO_BYTES)
    private const val MEGA_BYTES = KILO_BYTES * 1024
    private val MB_BD = BigDecimal(MEGA_BYTES)
    private const val GIGA_BYTES = MEGA_BYTES * 1024
    private val GB_BD = BigDecimal(GIGA_BYTES)
    private val TWENTY = BigDecimal(20)
}
