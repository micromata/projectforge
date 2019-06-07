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

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.TreeMap;
import java.util.TreeSet;

import org.hibernate.collection.internal.PersistentBag;
import org.hibernate.collection.internal.PersistentList;
import org.hibernate.collection.internal.PersistentMap;
import org.hibernate.collection.internal.PersistentSet;
import org.hibernate.collection.internal.PersistentSortedMap;
import org.hibernate.collection.internal.PersistentSortedSet;
import org.hibernate.proxy.HibernateProxy;

import com.thoughtworks.xstream.mapper.Mapper;
import com.thoughtworks.xstream.mapper.MapperWrapper;

/**
 * Replaces Hibernate 3 specific collections with java.util implementations.
 * 
 * <strong>NOTE</strong> This mapper takes care only of the writing to the XML (deflating) not the other way around
 * (inflating) because there is no need.
 * 
 * @author Costin Leau
 * 
 */

public class HibernateMapper extends MapperWrapper
{

  private Map<Class<?>, Class<?>> collectionMap = new HashMap<Class<?>, Class<?>>();

  public HibernateMapper(MapperWrapper arg0)
  {
    super(arg0);
    init();
  }

  public void init()
  {
    collectionMap.put(PersistentBag.class, ArrayList.class);
    collectionMap.put(PersistentList.class, ArrayList.class);
    collectionMap.put(PersistentMap.class, HashMap.class);
    collectionMap.put(PersistentSet.class, HashSet.class);
    collectionMap.put(PersistentSortedMap.class, TreeMap.class);
    collectionMap.put(PersistentSortedSet.class, TreeSet.class);
  }

  public HibernateMapper(Mapper arg0)
  {
    super(arg0);
    init();
  }

  @SuppressWarnings("unchecked")
  @Override
  public Class defaultImplementationOf(Class clazz)
  {
    // System.err.println("checking class:" + clazz);
    if (collectionMap.containsKey(clazz)) {
      // System.err.println("** substituting " + clazz + " with " + collectionMap.get(clazz));
      return collectionMap.get(clazz);
    }

    return super.defaultImplementationOf(clazz);
  }

  @SuppressWarnings("unchecked")
  @Override
  public String serializedClass(Class clazz)
  {
    // check whether we are hibernate proxy and substitute real name
    for (int i = 0; i < clazz.getInterfaces().length; i++) {
      if (HibernateProxy.class.equals(clazz.getInterfaces()[i])) {
        // System.err.println("resolving to class name:" + clazz.getSuperclass().getName());
        return clazz.getSuperclass().getName();
      }
    }
    if (collectionMap.containsKey(clazz)) {
      // System.err.println("** substituting " + clazz + " with " + collectionMap.get(clazz));
      return ((Class) collectionMap.get(clazz)).getName();
    }

    return super.serializedClass(clazz);
  }

}
