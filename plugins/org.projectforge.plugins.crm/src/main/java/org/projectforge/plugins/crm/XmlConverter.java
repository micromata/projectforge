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

package org.projectforge.plugins.crm;

import java.util.Iterator;
import java.util.List;

import org.apache.commons.lang.StringUtils;
import org.projectforge.framework.xstream.AliasMap;
import org.projectforge.framework.xstream.XmlObjectReader;
import org.projectforge.framework.xstream.XmlObjectWriter;

/**
 * @author Werner Feder (werner.feder@t-online.de)
 *
 */
public class XmlConverter<T>
{
  private static final String ENCLOSING_ENTITY ="values";

  private final T value;
  public XmlConverter(final T value) { this.value = value; }

  public List<T> readValues(final String valuesAsXml)
  {
    if (StringUtils.isBlank(valuesAsXml) == true) {
      return null;
    }
    final XmlObjectReader reader = new XmlObjectReader();
    final AliasMap aliasMap = new AliasMap();
    aliasMap.put(List.class, ENCLOSING_ENTITY);
    reader.setAliasMap(aliasMap).initialize(this.value.getClass());;
    @SuppressWarnings("unchecked")
    final List<T> list = (List<T>) reader.read(valuesAsXml);
    return list;
  }

  public String getValuesAsXml(final T... values)
  {
    if (values == null)
      return "";
    String xml =  "<" + ENCLOSING_ENTITY + ">";
    for (final T value : values) {
      xml += XmlObjectWriter.writeAsXml(value);
    }
    xml += "</" + ENCLOSING_ENTITY + ">";
    return xml;
  }

  public String getValuesAsXml(final List<T> values)
  {
    if (values == null)
      return "";
    String xml =  "<" + ENCLOSING_ENTITY + ">";
    final Iterator<T> it = values.iterator();
    while (it.hasNext() == true) {
      final T value = it.next();
      xml += XmlObjectWriter.writeAsXml(value);
    }
    xml += "</" + ENCLOSING_ENTITY + ">";
    return xml;
  }
}
