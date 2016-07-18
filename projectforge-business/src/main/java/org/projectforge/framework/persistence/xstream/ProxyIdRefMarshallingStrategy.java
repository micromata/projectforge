/////////////////////////////////////////////////////////////////////////////
//
// $RCSfile: ProxyIdRefMarshallingStrategy.java,v $
//
// Project   Hibernate3History
//
// Author    Wolfgang Jung (w.jung@micromata.de)
// Created   Jan 13, 2006
// Copyright Micromata Jan 13, 2006
//
// $Id: ProxyIdRefMarshallingStrategy.java,v 1.1 2007/03/08 22:50:48 wolle Exp $
// $Revision: 1.1 $
// $Date: 2007/03/08 22:50:48 $
//
/////////////////////////////////////////////////////////////////////////////
package org.projectforge.framework.persistence.xstream;

import com.thoughtworks.xstream.converters.ConverterLookup;
import com.thoughtworks.xstream.converters.DataHolder;
import com.thoughtworks.xstream.core.ReferenceByIdMarshallingStrategy;
import com.thoughtworks.xstream.core.ReferenceByIdUnmarshaller;
import com.thoughtworks.xstream.io.HierarchicalStreamReader;
import com.thoughtworks.xstream.io.HierarchicalStreamWriter;
import com.thoughtworks.xstream.mapper.Mapper;

public class ProxyIdRefMarshallingStrategy extends ReferenceByIdMarshallingStrategy
{

  @Override
  public Object unmarshal(Object root, HierarchicalStreamReader reader, DataHolder dataHolder,
      ConverterLookup converterLookup, Mapper classMapper)
  {
    return new ReferenceByIdUnmarshaller(
        root, reader, converterLookup,
        classMapper).start(dataHolder);
  }

  @Override
  public void marshal(HierarchicalStreamWriter writer, Object obj, ConverterLookup converterLookup,
      Mapper classMapper, DataHolder dataHolder)
  {
    new ProxyIdRefMarshaller(
        writer, converterLookup, classMapper).start(obj, dataHolder);
  }

}
