//
// Copyright (C) 2010-2016 Micromata GmbH
//
// Licensed under the Apache License, Version 2.0 (the "License");
// you may not use this file except in compliance with the License.
// You may obtain a copy of the License at
//
//  http://www.apache.org/licenses/LICENSE-2.0
//
// Unless required by applicable law or agreed to in writing, software
// distributed under the License is distributed on an "AS IS" BASIS,
// WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
// See the License for the specific language governing permissions and
// limitations under the License.
//

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
