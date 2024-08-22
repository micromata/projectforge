/////////////////////////////////////////////////////////////////////////////
//
// Project ProjectForge Community Edition
//         www.projectforge.org
//
// Copyright (C) 2001-2024 Micromata GmbH, Germany (www.micromata.com)
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

package org.projectforge.common.mgc;

import org.apache.log4j.Logger;

import java.lang.reflect.Field;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.lang.reflect.TypeVariable;
import java.util.HashMap;
import java.util.Map;

/**
 * Calculate a tree of generic type.
 *
 * @author Roger Rene Kommer (r.kommer.extern@micromata.de)
 *
 */
public class GenericTypeTrail
{
  static final Logger LOG = Logger.getLogger(GenericTypeTrail.class);
  /**
   * the link to the derived class.
   */
  public GenericTypeTrail derivedTrail;
  /**
   * the class in derive tree.
   */
  public Class<?> clazz;
  /**
   * Generic Types decla
   */
  public Map<String, Type> typeNames = new HashMap<>();

  public Class<?> getContreteFieldType(Field field)
  {
    Type genType = field.getGenericType();
    if (genType == null || genType instanceof Class) {
      return (Class<?>) genType;
    }
    if (genType instanceof TypeVariable) {
      TypeVariable<?> tvar = (TypeVariable<?>) genType;
      String typeName = tvar.getTypeName();
      Class<?> ret = getConcreteTypeForTypeName(typeName);
      if (ret != null) {
        return ret;
      }
      LOG.warn("Cannot find concreate type for : " + clazz.getName() + "<" + typeName + ">");
    }
    return field.getType();
  }

  public Class<?> getConcreteTypeForTypeName(String typeName)
  {
    if (derivedTrail == null) {
      return null;
    }
    Type tp = derivedTrail.typeNames.get(typeName);
    tp.toString();
    if (tp instanceof Class) {
      return (Class<?>) tp;
    }
    if (tp instanceof TypeVariable) {
      TypeVariable<?> tv = (TypeVariable<?>) tp;
      String supername = tv.getName();
      return derivedTrail.getConcreteTypeForTypeName(supername);
    }
    return null;
  }

  static Map<Class<?>, GenericTypeTrail> buildTypeTransMap(Class<?> clazz)
  {
    Map<Class<?>, GenericTypeTrail> ret = new HashMap<>();
    buildTypeTransMap(clazz, ret, null);
    return ret;
  }

  public static void buildTypeTransMap(Class<?> clazz, Map<Class<?>, GenericTypeTrail> ret, GenericTypeTrail superTrail)
  {
    GenericTypeTrail typeTrail = new GenericTypeTrail();
    typeTrail.clazz = clazz;
    typeTrail.derivedTrail = superTrail;
    Type gensuper = clazz.getGenericSuperclass();
    Class<?> superclazz = clazz.getSuperclass();
    if (superclazz == null) {
      return;
    }
    ret.put(clazz, typeTrail);

    TypeVariable<?>[] typeParams = superclazz.getTypeParameters();

    if (gensuper instanceof ParameterizedType) {
      ParameterizedType pgensu = (ParameterizedType) gensuper;
      pgensu.getTypeName();
      Type[] acttypes = pgensu.getActualTypeArguments();
      if (typeParams.length != acttypes.length) {
        System.out.println("UUps typeParams.length != acttypes.length");
      }

      for (int i = 0; i < acttypes.length; ++i) {
        String typename = typeParams[i].getName();
        Type actType = acttypes[i];
        typeTrail.typeNames.put(typename, actType);
      }
    }

    if (superclazz != null && superclazz instanceof Object) {
      buildTypeTransMap(superclazz, ret, typeTrail);
    }
  }
}
