/////////////////////////////////////////////////////////////////////////////
//
// Project ProjectForge Community Edition
//         www.projectforge.org
//
// Copyright (C) 2001-2019 Micromata GmbH, Germany (www.micromata.com)
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

import org.springframework.cglib.proxy.Enhancer;

import com.thoughtworks.xstream.converters.Converter;
import com.thoughtworks.xstream.converters.ConverterLookup;
import com.thoughtworks.xstream.core.ReferenceByIdMarshaller;
import com.thoughtworks.xstream.core.SequenceGenerator;
import com.thoughtworks.xstream.core.util.ObjectIdDictionary;
import com.thoughtworks.xstream.io.HierarchicalStreamWriter;
import com.thoughtworks.xstream.mapper.Mapper;


public class ProxyIdRefMarshaller extends ReferenceByIdMarshaller
{
  /** The logger */
  private static final org.slf4j.Logger log = org.slf4j.LoggerFactory.getLogger(ProxyIdRefMarshaller.class);

  private ObjectIdDictionary references = new ObjectIdDictionary();

  private IDGenerator idGenerator;
  private Mapper classMapper;

  public ProxyIdRefMarshaller(HierarchicalStreamWriter writer, ConverterLookup converterLookup, Mapper classMapper,
      IDGenerator idGenerator)
  {
    super(writer, converterLookup, classMapper);
    this.idGenerator = idGenerator;
    this.classMapper = classMapper;
  }

  public ProxyIdRefMarshaller(HierarchicalStreamWriter writer, ConverterLookup converterLookup, Mapper classMapper)
  {
    this(writer, converterLookup, classMapper, new SequenceGenerator(1));
    this.classMapper = classMapper;
  }

  @Override
  public void convertAnother(Object item)
  {
    Class<?> targetClass = item.getClass();
    while (Enhancer.isEnhanced(targetClass) == true) {
      targetClass = targetClass.getSuperclass();
    }

    Converter converter = converterLookup.lookupConverterForType(targetClass);
    Object realItem = HibernateProxyHelper.get(item);

    if (classMapper.isImmutableValueType(realItem.getClass())) {
      // strings, ints, dates, etc... don't bother using references.
      converter.marshal(item, writer, this);
    } else {
      Object idOfExistingReference = references.lookupId(realItem);
      if (idOfExistingReference != null) {
        writer.addAttribute("reference", idOfExistingReference.toString());
        return;
      }
      String newId = idGenerator.next(realItem);
      writer.addAttribute("id", newId);
      references.associateId(realItem, newId);
      if (log.isDebugEnabled()) {
        log.debug("marshalling object " + realItem.getClass() + " to stream");
      }

      converter.marshal(realItem, writer, this);
    }
  }

}
