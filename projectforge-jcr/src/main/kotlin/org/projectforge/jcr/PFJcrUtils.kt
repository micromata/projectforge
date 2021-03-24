/////////////////////////////////////////////////////////////////////////////
//
// Project ProjectForge Community Edition
//         www.projectforge.org
//
// Copyright (C) 2001-2021 Micromata GmbH, Germany (www.micromata.com)
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

package org.projectforge.jcr

import com.fasterxml.jackson.annotation.JsonAutoDetect
import com.fasterxml.jackson.annotation.JsonInclude
import com.fasterxml.jackson.annotation.PropertyAccessor
import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.databind.SerializationFeature
import com.fasterxml.jackson.databind.module.SimpleModule
import mu.KotlinLogging
import java.io.IOException
import java.time.Instant
import java.time.ZoneOffset
import java.time.format.DateTimeFormatter
import java.util.*
import javax.jcr.Node

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

  fun matchAnyPath(node: Node, expectedPathList: List<String>?): Boolean {
    expectedPathList ?: return false
    expectedPathList.forEach {
      if (matchPath(node, it)) {
        return true
      }
    }
    return false
  }

  fun matchPath(node: Node, expectedPath: String): Boolean {
    val pathList = getPath(node, mutableListOf())
    val expectedPathList = expectedPath.split('/').filter { !it.isBlank() }
    if (expectedPathList.size > pathList.size) {
      return false
    }
    expectedPathList.forEachIndexed { index, path ->
      if (pathList[index] != path) {
        return false
      }
    }
    return true
  }

  private fun getPath(node: Node, pathList: MutableList<String>): List<String> {
    if (isMainNode(node) || isRootNode(node)) {
      return pathList
    }
    node.parent?.let { parent ->
      getPath(parent, pathList)
      pathList.add(node.name) // Don't add top level node (parent is null).
    }
    return pathList
  }

  fun isRootNode(node: Node): Boolean {
    return node.path.length <= 1 // node.path for root is '/'
  }

  fun isMainNode(node: Node): Boolean {
    return !isRootNode(node) && isRootNode(node.parent)
  }

  private val jsDateTimeFormatter =
    DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'").withZone(ZoneOffset.UTC)
}
