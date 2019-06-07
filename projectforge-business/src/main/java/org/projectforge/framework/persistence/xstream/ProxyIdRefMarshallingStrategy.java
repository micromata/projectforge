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
