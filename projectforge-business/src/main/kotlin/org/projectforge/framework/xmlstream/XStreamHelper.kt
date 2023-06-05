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

package org.projectforge.framework.xmlstream

import com.thoughtworks.xstream.XStream
import com.thoughtworks.xstream.security.AnyTypePermission
import com.thoughtworks.xstream.security.NullPermission
import com.thoughtworks.xstream.security.PrimitiveTypePermission
import mu.KotlinLogging

private val log = KotlinLogging.logger {}

/**
 * Some helpers for using XStream (with proper UTF-8 encoding).
 *
 * @author Kai Reinhard (k.reinhard@micromata.de)
 */
object XStreamHelper {
  /**
   * @param types: types will be declared as [XStream.allowTypes] and used for processing annotations as well via
   * [XStream.processAnnotations].
   * @return new XStream initialized with UTF-8 DomDriver.
   */
  @JvmStatic
  @JvmOverloads
  fun createXStream(vararg types: Class<*>): XStream {
    val xstream = XStream()
    xstream.addPermission(AnyTypePermission.ANY) // allow everything
    xstream.addPermission(NullPermission.NULL) // allow "null"
    xstream.addPermission(PrimitiveTypePermission.PRIMITIVES) // allow primitive types
    xstream.allowTypesByWildcard(arrayOf("java.lang.*", "java.util.*", "org.projectforge.*"))
    xstream.allowTypes(types)
    xstream.processAnnotations(types)
    return xstream
  }

  @JvmStatic
  fun toXml(xstream: XStream, obj: Any?): String {
    return xstream.toXML(obj)
  }

  @JvmStatic
  fun fromXml(xstream: XStream, xml: String?): Any? {
    xml ?: return null
    return xstream.fromXML(xml)
  }

  @JvmStatic
  fun fromXml(xstream: XStream, xml: String?, packageNameOld: String?, packageNameNew: String?): Any? {
    xstream.aliasPackage(packageNameOld, packageNameNew)
    return fromXml(xstream, xml)
  }
}
