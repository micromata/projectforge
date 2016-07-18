/////////////////////////////////////////////////////////////////////////////
//
// $RCSfile: ProxyIdRefMarshaller.java,v $
//
// Project   Hibernate3History
//
// Author    Wolfgang Jung (w.jung@micromata.de)
// Created   Jan 13, 2006
// Copyright Micromata Jan 13, 2006
//
// $Id: ProxyIdRefMarshaller.java,v 1.1 2007/03/08 22:50:48 wolle Exp $
// $Revision: 1.1 $
// $Date: 2007/03/08 22:50:48 $
//
/////////////////////////////////////////////////////////////////////////////
package org.projectforge.framework.persistence.xstream;

import com.thoughtworks.xstream.converters.Converter;
import com.thoughtworks.xstream.converters.ConverterLookup;
import com.thoughtworks.xstream.core.ReferenceByIdMarshaller;
import com.thoughtworks.xstream.core.SequenceGenerator;
import com.thoughtworks.xstream.core.util.ObjectIdDictionary;
import com.thoughtworks.xstream.io.HierarchicalStreamWriter;
import com.thoughtworks.xstream.mapper.Mapper;

import net.sf.cglib.proxy.Enhancer;

public class ProxyIdRefMarshaller extends ReferenceByIdMarshaller
{
  /** The logger */
  private static final org.apache.log4j.Logger log = org.apache.log4j.Logger.getLogger(ProxyIdRefMarshaller.class);

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
