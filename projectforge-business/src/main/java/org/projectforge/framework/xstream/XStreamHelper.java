/////////////////////////////////////////////////////////////////////////////
//
// Project ProjectForge Community Edition
//         www.projectforge.org
//
// Copyright (C) 2001-2014 Kai Reinhard (k.reinhard@micromata.de)
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

package org.projectforge.framework.xstream;

import com.thoughtworks.xstream.XStream;
import com.thoughtworks.xstream.io.xml.DomDriver;

/**
 * Some helpers for using XStream (with proper UTF-8 encoding).
 * 
 * @author Kai Reinhard (k.reinhard@micromata.de)
 * 
 */
public class XStreamHelper
{

  private static final org.slf4j.Logger log = org.slf4j.LoggerFactory.getLogger(XStreamHelper.class);

  /**
   * @return new XStream initialized with UTF-8 DomDriver.
   */
  public static XStream createXStream()
  {
    return new XStream(new DomDriver("UTF-8"));
  }

  public static String toXml(final XStream xstream, final Object object)
  {
    return xstream.toXML(object);
  }

  public static Object fromXml(final XStream xstream, final String xml)
  {
    return xstream.fromXML(xml);
  }

  public static Object fromXml(XStream xstream, String xml, String packageNameOld, String packageNameNew)
  {
    xstream.aliasPackage(packageNameOld, packageNameNew);
    return fromXml(xstream, xml);
  }
}
