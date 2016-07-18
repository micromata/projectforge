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

package org.projectforge.framework.persistence.xstream;

import org.hibernate.proxy.HibernateProxy;

import com.thoughtworks.xstream.converters.Converter;
import com.thoughtworks.xstream.converters.ConverterLookup;
import com.thoughtworks.xstream.core.ReferenceByXPathMarshaller;
import com.thoughtworks.xstream.io.HierarchicalStreamWriter;
import com.thoughtworks.xstream.mapper.Mapper;

/**
 * http://jira.codehaus.org/browse/XSTR-226
 */
public class HibernateProxyXPathMarshaller extends ReferenceByXPathMarshaller
{

  public HibernateProxyXPathMarshaller(HierarchicalStreamWriter writer, ConverterLookup converterLookup, Mapper mapper, int mode)
  {
    super(writer, converterLookup, mapper, mode);
  }

  @Override
  public void convertAnother(Object item, Converter converter)
  {
    Object toConvert;
    if (HibernateProxy.class.isAssignableFrom(item.getClass())) {
      toConvert = ((HibernateProxy) item).getHibernateLazyInitializer().getImplementation();
    } else {
      toConvert = item;
    }
    super.convertAnother(toConvert, converter);
  }

}
