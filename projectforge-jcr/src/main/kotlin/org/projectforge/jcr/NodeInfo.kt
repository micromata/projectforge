/////////////////////////////////////////////////////////////////////////////
//
// Project ProjectForge Community Edition
//         www.projectforge.org
//
// Copyright (C) 2001-2023 Micromata GmbH, Germany (www.micromata.com)
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

import mu.KotlinLogging
import javax.jcr.Node

private val log = KotlinLogging.logger {}

/**
 * For information.
 */
class NodeInfo() {
  internal constructor(node: Node, recursive: Boolean = true, listOfIgnoredNodePaths: List<String>? = null) : this() {
    name = node.name
    path = if (PFJcrUtils.isRootNode(node)) {
      ""
    } else {
      node.parent.path
    }
    if (recursive) {
      node.nodes?.let {
        val nodes = mutableListOf<NodeInfo>()
        while (it.hasNext()) {
          val child = it.nextNode()
          if (PFJcrUtils.matchAnyPath(child, listOfIgnoredNodePaths)) {
            log.info { "Ignore path=${child.path} as configured." }
            continue
          }
          nodes.add(NodeInfo(child))
        }
        children = nodes
      }
    }
    if (node.properties?.hasNext() == true) {
      val props = mutableListOf<PropertyInfo>()
      properties = props
      node.properties.let {
        while (it.hasNext()) {
          props.add(PropertyInfo(it.nextProperty()))
        }
      }
    }
  }

  var name: String? = null
  var children: List<NodeInfo>? = null
  var properties: List<PropertyInfo>? = null
  lateinit var path: String

  fun getProperty(propertyName: String): PropertyInfo? {
    return properties?.firstOrNull { it.name == propertyName }
  }

  fun hasProperty(propertyName: String): Boolean {
    return properties?.any { it.name == propertyName } == true
  }

  fun findDescendant(vararg path: String): NodeInfo? {
    var node: NodeInfo? = this
    path.forEach { name ->
      node = node?.children?.find { it.name == name } ?: return null
    }
    return node
  }

  override fun toString(): String {
    return NodeLogInfo.copyFrom(this).toString()
  }

  /**
   * For logging purposes only (short form of NodeInfo).
   */
  @Suppress("unused")
  private class NodeLogInfo(
    val name: String?,
    val children: List<NodeLogInfo>? = null,
    val properties: Map<String, Any?>? = null
  ) {
    override fun toString(): String {
      return PFJcrUtils.toJson(this)
    }

    companion object {
      fun copyFrom(node: NodeInfo): NodeLogInfo {
        val children = node.children?.map { copyFrom(it) }
        return NodeLogInfo(
          node.name,
          if (children.isNullOrEmpty()) null else children,
          propsToMap(node)
        )
      }

      private fun propsToMap(node: NodeInfo): Map<String, Any?>? {
        val properties = node.properties ?: return null
        val propsMap = mutableMapOf<String, Any?>()
        properties.forEach { prop ->
          if (prop.name?.startsWith("jcr:") != true) { // Ignore jcr specific props.
            if (prop.values.isNullOrEmpty()) {
              propsMap.put(prop.name ?: "???", prop.value?.toString())
            } else {
              val values = mutableListOf<String>()
              prop.values?.forEach { value ->
                values.add(value.toString())
              }
              if (values.isNotEmpty()) {
                propsMap[prop.name ?: "???"] = values
              }
            }
          }
        }
        return if (propsMap.isEmpty()) null else propsMap
      }
    }
  }
}
