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
import java.util.TreeMap;
import java.util.TreeSet;

import org.hibernate.collection.internal.PersistentList;
import org.hibernate.collection.internal.PersistentMap;
import org.hibernate.collection.internal.PersistentSet;
import org.hibernate.collection.internal.PersistentSortedMap;
import org.hibernate.collection.internal.PersistentSortedSet;

import com.thoughtworks.xstream.mapper.Mapper;
import com.thoughtworks.xstream.mapper.XmlFriendlyMapper;

/**
 * Replaces Hibernate 3 specific collections with java.util implementations.
 * 
 * <strong>NOTE</strong> This mapper takes care only of the writing to the XML (deflating) not the other way around
 * (inflating) because there is no need.
 * 
 * @author Costin Leau
 * 
 */

public class HibernateCollectionsMapper extends XmlFriendlyMapper// MapperWrapper
{
  private final static String[] hbClassNames = { PersistentList.class.getName(), PersistentSet.class.getName(),
      PersistentMap.class.getName(), PersistentSortedSet.class.getName(), PersistentSortedMap.class.getName() };

  private final static String[] jdkClassNames = { ArrayList.class.getName(), HashSet.class.getName(),
      HashMap.class.getName(),
      TreeSet.class.getName(), TreeMap.class.getName() };

  private final static Class[] hbClasses = { PersistentList.class, PersistentSet.class, PersistentMap.class,
      PersistentSortedSet.class,
      PersistentSortedMap.class };

  private final static Class[] jdkClasses = { ArrayList.class, HashSet.class, HashMap.class, TreeSet.class,
      TreeMap.class };

  public HibernateCollectionsMapper(final Mapper wrapped)
  {
    super(wrapped);
  }

  /**
   * @see com.thoughtworks.xstream.alias.ClassMapper#mapNameToXML(java.lang.String)
   */
  @Override
  public String mapNameToXML(final String javaName)
  {
    return super.mapNameToXML(replaceClasses(javaName));
  }

  /**
   * @see com.thoughtworks.xstream.mapper.Mapper#serializedClass(java.lang.Class)
   */
  @Override
  public String serializedClass(final Class type)
  {
    return super.serializedClass(replaceClasses(type));
  }

  /**
   * @see com.thoughtworks.xstream.mapper.Mapper#serializedMember(java.lang.Class, java.lang.String)
   */
  @Override
  public String serializedMember(final Class type, final String fieldName)
  {
    return super.serializedMember(replaceClasses(type), fieldName);
  }

  /**
   * Simple replacements between the HB 3 collections and their underlying collections from java.util.
   * 
   * @param name
   * @return the equivalent JDK class name
   */
  private String replaceClasses(final String name)
  {
    for (int i = 0; i < hbClassNames.length; i++) {
      if (name.equals(hbClassNames[i])) {
        return jdkClassNames[i];
      }
    }
    return name;
  }

  /**
   * Simple replacements between the HB 3 collections and their underlying collections from java.util.
   * 
   * @param clazz
   * @return the equivalent JDK class
   */
  private Class replaceClasses(final Class clazz)
  {
    for (int i = 0; i < hbClasses.length; i++) {
      if (clazz.equals(hbClasses[i])) {
        return jdkClasses[i];
      }
    }
    return clazz;
  }
}
