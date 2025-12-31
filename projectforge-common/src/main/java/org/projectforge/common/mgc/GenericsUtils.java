/////////////////////////////////////////////////////////////////////////////
//
// Project ProjectForge Community Edition
//         www.projectforge.org
//
// Copyright (C) 2001-2026 Micromata GmbH, Germany (www.micromata.com)
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

import java.lang.reflect.*;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Utils to retrieve Generic Type information
 *
 * @author roger
 *
 */
public class GenericsUtils
{
  private static final org.slf4j.Logger LOG = org.slf4j.LoggerFactory.getLogger(GenericsUtils.class);

  public static Class<?> getClass(Type type)
  {
    if (type instanceof Class) {
      return (Class<?>) type;
    } else if (type instanceof ParameterizedType) {
      return getClass(((ParameterizedType) type).getRawType());
    } else if (type instanceof GenericArrayType) {
      Type componentType = ((GenericArrayType) type).getGenericComponentType();
      Class<?> componentClass = getClass(componentType);
      if (componentClass != null) {
        return Array.newInstance(componentClass, 0).getClass();
      } else {
        return null;
      }
    } else {
      return null;
    }
  }

  public static Class<?> getMethodExceptionType(Class<?> cls, String methodName, Class<?>[] argTypes,
      int methodPosition,
      int classPosition, Class<?> defaultType)
  {
    try {

      Method m = cls.getMethod(methodName, argTypes);
      return getMethodExceptionType(cls, m, methodPosition, classPosition, defaultType);
    } catch (Exception ex) {
      throw new RuntimeException(ex);
    }
  }

  public static Class<?> getClassGenericTypeFromSuperClass(Class<?> cls, int typePosition, Class<?> defaultClass)
  {
    Type t = cls.getGenericSuperclass();
    if (t instanceof ParameterizedType) {
      ParameterizedType ptt = (ParameterizedType) t;
      Type optt = ptt.getOwnerType();

      Type[] pts = ptt.getActualTypeArguments();
      if (pts.length <= typePosition) {
        return defaultClass;
      }
      Type pt = pts[typePosition];
      Class<?> fcls = getClass(pt);
      if (pt instanceof Class<?>) {
        return (Class<?>) pt;
      }

    }
    return defaultClass;
  }

  public static Class<?> getClassGenericTypeFromInterface(Class<?> cls, int interfacePosition, int typePosition,
      Class<?> defaultClass)
  {
    Type[] tps = cls.getGenericInterfaces();
    if (tps.length <= interfacePosition) {
      return defaultClass;
    }
    Type t = tps[interfacePosition];
    if (t instanceof ParameterizedType) {
      ParameterizedType ptt = (ParameterizedType) t;
      Type[] pts = ptt.getActualTypeArguments();
      if (pts.length <= typePosition) {
        return defaultClass;
      }
      Type pt = pts[typePosition];

      if (pt instanceof Class<?>) {
        return (Class<?>) pt;
      }
      Class<?> fcls = getClass(pt);
    }
    return defaultClass;
  }

  public static int findParameterPosition(Class<?> rawClass, String typeName)
  {
    TypeVariable<?>[] tps = rawClass.getTypeParameters();
    for (int i = 0; i < tps.length; ++i) {
      TypeVariable<?> rtc = tps[i];
      if (rtc.getName().equals(typeName) == true) {
        return i;
      }
    }
    return -1;
  }

  public static Class<?> getClassGenericTypeFromSuperClass(Class<?> rtClass, Class<?> declClass, String typeName,
      Class<?> defaultClass)
  {
    // printGenericClass(rtClass);
    TypeVariable<?>[] orgParamTypes = rtClass.getTypeParameters();
    Class<?> cls = rtClass.getSuperclass();
    Type gsc = rtClass.getGenericSuperclass();

    Type[] ifaces = cls.getGenericInterfaces();
    if (gsc instanceof ParameterizedType) {
      ParameterizedType tp = (ParameterizedType) gsc;
      Type rt = tp.getRawType();
      Type owner = tp.getOwnerType();
      if (rt instanceof Class<?>) {
        Class<?> rawClass = (Class<?>) rt;
        int paramPos = findParameterPosition(rawClass, typeName);
        if (paramPos == -1) {
          return defaultClass;
        }
        Type pp = tp.getActualTypeArguments()[paramPos];
        return getClass(pp);
      }
    }
    return defaultClass;
  }

  public static Class<?> getMethodType(String tp, Method m)
  {
    for (Type pt : m.getGenericParameterTypes()) {
      printType(pt);
    }
    return null;
  }

  public static Class<?> getMethodType(Class<?> cls, String methodName, String typeName)
  {
    Method m = MGCClassUtils.findFirstMethod(cls, methodName);
    if (m == null) {
      return null;
    }
    return getMethodType(typeName, m);
  }

  public static Class<?> getMethodExceptionType(Class<?> cls, Method m, int methodPosition, int classPosition,
      Class<?> defaultType)
  {
    Class<?> fcls = null;
    Type[] types = m.getGenericExceptionTypes();
    if (types.length <= methodPosition) {
      return defaultType;
    }
    Type tp = types[methodPosition];
    if (tp instanceof Class<?>) {
      return (Class<?>) tp;
    } else if (tp instanceof TypeVariable<?>) {
      TypeVariable<?> tv = (TypeVariable<?>) tp;
      String name = tv.getName();
      Class<?> declClass = m.getDeclaringClass();
      fcls = getClassGenericTypeFromSuperClass(cls, declClass, name, null);
      if (fcls != null) {
        return fcls;
      }
    }
    fcls = getClassGenericTypeFromSuperClass(cls, classPosition, null);
    if (fcls != null) {
      return fcls;
    }
    printType("", tp);
    return defaultType;
  }

  // from http://www.artima.com/weblogs/viewpost.jsp?thread=208860

  public static List<Class<?>> getTypeArguments(Class<?> baseClass, Class<?> childClass)
  {
    Map<Type, Type> resolvedTypes = new HashMap<Type, Type>();
    Type type = childClass;
    // start walking up the inheritance hierarchy until we hit baseClass
    while (!getClass(type).equals(baseClass)) {
      if (type instanceof Class) {
        // there is no useful information for us in raw types, so just keep going.
        type = ((Class<?>) type).getGenericSuperclass();
      } else {
        ParameterizedType parameterizedType = (ParameterizedType) type;
        Class<?> rawType = (Class<?>) parameterizedType.getRawType();

        Type[] actualTypeArguments = parameterizedType.getActualTypeArguments();
        TypeVariable<?>[] typeParameters = rawType.getTypeParameters();
        for (int i = 0; i < actualTypeArguments.length; i++) {
          resolvedTypes.put(typeParameters[i], actualTypeArguments[i]);
        }

        if (!rawType.equals(baseClass)) {
          type = rawType.getGenericSuperclass();
        }
      }
    }

    // finally, for each actual type argument provided to baseClass, determine (if possible)
    // the raw class for that type argument.
    Type[] actualTypeArguments;
    if (type instanceof Class) {
      actualTypeArguments = ((Class) type).getTypeParameters();
    } else {
      actualTypeArguments = ((ParameterizedType) type).getActualTypeArguments();
    }
    List<Class<?>> typeArgumentsAsClasses = new ArrayList<Class<?>>();
    // resolve types by chasing down type variables.
    for (Type baseType : actualTypeArguments) {
      while (resolvedTypes.containsKey(baseType)) {
        baseType = resolvedTypes.get(baseType);
      }
      typeArgumentsAsClasses.add(getClass(baseType));
    }
    return typeArgumentsAsClasses;
  }

  public static void printGenericClass(Class<?> cls)
  {
    StringBuilder sb = new StringBuilder();
    printGenericClass("", cls, sb);
    System.out.println(sb.toString());
  }

  public static void printGenericClass(String indent, Class<?> cls, StringBuilder sb)
  {
    sb.append(indent).append("class: ").append(cls).append("\n");
    TypeVariable<?>[] orgParamTypes = cls.getTypeParameters();
    indent = indent + "  ";
    if (orgParamTypes.length > 0) {
      sb.append(indent).append("ParamTypes:\n");
      for (Type pt : orgParamTypes) {
        printType(indent + "  ", pt, sb);
      }
    }
    Type st = cls.getGenericSuperclass();
    if (st != null && st != Object.class) {
      sb.append(indent).append("GenericSuperclass:\n");
      if (st instanceof Class<?>) {
        printGenericClass(indent + "  ", (Class<?>) st, sb);
      } else {
        printType(indent + "  ", st, sb);
      }
    }
    Type[] ift = cls.getGenericInterfaces();
    if (ift != null && ift.length > 0) {
      sb.append(indent).append("GenericInterfaces:\n");
      for (Type t : ift) {
        sb.append(indent + " ").append(" iface:\n");
        printType(indent + "  ", t, sb);
      }
    }
    if (cls.getSuperclass() != null && cls.getSuperclass() != Object.class) {
      sb.append(indent).append("Super:\n");
      printGenericClass(indent + "  ", cls.getSuperclass(), sb);
    }
    Class<?>[] ifaces = cls.getInterfaces();
    if (ifaces.length > 0) {
      for (Class<?> iface : ifaces) {
        sb.append(indent).append("Interface:\n");
        printGenericClass(indent + "  ", iface, sb);
      }
    }
  }

  public static void printType(Type type)
  {
    printType("", type);
  }

  public static void printType(String indent, Type type)
  {
    StringBuilder sb = new StringBuilder();
    printType(indent, type, sb);
    System.out.println(sb.toString());
  }

  public static void printType(String indent, Type type, StringBuilder sb)
  {
    if (type instanceof Class<?>) {
      sb.append(indent + "Class: " + type.toString() + "\n");
    } else if (type instanceof ParameterizedType) {
      ParameterizedType pt = (ParameterizedType) type;
      sb.append(indent + "ParameterizedType: " + pt.toString() + "\n").append(indent + " args:\n");
      for (Type t : pt.getActualTypeArguments()) {
        printType(indent + "  ", t, sb);
      }
      sb.append(indent + " rawType:\n");
      Type rp = pt.getRawType();
      printType(indent + "  ", rp, sb);
      Type owner = pt.getOwnerType();
      if (owner != null) {
        sb.append(indent + " owner:\n");
        printType(indent + "  ", owner, sb);
      }
    } else if (type instanceof TypeVariable) {
      TypeVariable tp = (TypeVariable) type;
      sb.append(indent + "TypeVariable: " + tp.toString() + "\n").append(indent + "  Bounds:\n");
      GenericDeclaration gd = tp.getGenericDeclaration();
      TypeVariable<?>[] gdtv = gd.getTypeParameters();
      // tp.get
      for (Type b : tp.getBounds()) {
        printType(indent + "    ", b, sb);
      }
      // cause recursion
      // GenericDeclaration gd = tp.getGenericDeclaration();
      // sb.append(indent + " GenericDeclaration: " + gd + "\n")//
      // .append(indent + " types:\n");
      // for (TypeVariable tv : gd.getTypeParameters()) {
      // printType(indent + " ", tv, sb);
      // }
    } else {
      sb.append(indent + "Unknown: " + type.toString() + "\n");
    }
  }

  /**
   * Retrieve a concrete type of a generic parameter
   *
   * @param desiredType generic Interface or class containing generic type
   * @param concretType concrete class. Note List String is not sufficient. use new List String () {} instead
   * @param position position of the generic type
   * @return null if none found
   */
  public static Class<?> getConcretTypeParameter(Class<?> desiredType, Class<?> concretType, int position)
  {
    Type[] types = getTypeParameters(desiredType, concretType);
    if (types == null || types.length <= position) {
      return null;
    }
    Type typ = types[position];
    if (typ instanceof Class<?>) {
      return (Class<?>) typ;
    }
    return null;
  }

  /**
   * See getConcretTypeParameter with position
   *
   * @param desiredType the type we desire
   * @param concretType the correct type
   * @param genTypeName the name of the generic
   * @return null if none found
   */
  public static Class<?> getConcretTypeParameter(Class<?> desiredType, Class<?> concretType, String genTypeName)
  {
    // Type[] types = getTypeParameters(desiredType, concretType);
    Type genSuperCls = concretType.getGenericSuperclass();
    if (genSuperCls instanceof ParameterizedType) {
      ParameterizedType pt = (ParameterizedType) genSuperCls;
      Class<?> rawType = (Class<?>) pt.getRawType();
      TypeVariable<?>[] rawTypeParams = rawType.getTypeParameters();
      int foundPos = -1;
      for (int i = 0; i < rawTypeParams.length; ++i) {
        TypeVariable<?> tv = rawTypeParams[i];
        if (tv.getName().equals(genTypeName) == true) {
          foundPos = i;
          break;
        }
      }
      if (foundPos != -1) {
        return getConcretTypeParameter(desiredType, concretType, foundPos);
      }
    }
    for (Type superType : concretType.getGenericInterfaces()) {
      if (superType instanceof ParameterizedType) {
        ParameterizedType pt = (ParameterizedType) superType;
        Class<?> rawType = (Class<?>) pt.getRawType();
        TypeVariable<?>[] rawTypeParams = rawType.getTypeParameters();
        int foundPos = -1;
        for (int i = 0; i < rawTypeParams.length; ++i) {
          TypeVariable<?> tv = rawTypeParams[i];
          if (tv.getName().equals(genTypeName) == true) {
            foundPos = i;
            break;
          }
        }
        if (foundPos != -1) {
          return getConcretTypeParameter(desiredType, concretType, foundPos);
        }
      }
    }
    return null;

  }

  // from org/apache/xbean/recipe/RecipeHelper.java
  public static Type[] getTypeParameters(Class<?> desiredType, Type type)
  {
    if (type instanceof Class) {
      Class<?> rawClass = (Class<?>) type;

      // if this is the collection class we're done
      if (desiredType.equals(type)) {
        return null;
      }

      for (Type intf : rawClass.getGenericInterfaces()) {
        Type[] collectionType = getTypeParameters(desiredType, intf);
        if (collectionType != null) {
          return collectionType;
        }
      }

      Type[] collectionType = getTypeParameters(desiredType, rawClass.getGenericSuperclass());
      return collectionType;
    } else if (type instanceof ParameterizedType) {
      ParameterizedType parameterizedType = (ParameterizedType) type;

      Type rawType = parameterizedType.getRawType();
      if (desiredType.equals(rawType)) {
        Type[] argument = parameterizedType.getActualTypeArguments();
        return argument;
      }
      Type[] collectionTypes = getTypeParameters(desiredType, rawType);
      if (collectionTypes != null) {
        for (int i = 0; i < collectionTypes.length; i++) {
          if (collectionTypes[i] instanceof TypeVariable) {
            TypeVariable typeVariable = (TypeVariable) collectionTypes[i];
            TypeVariable[] rawTypeParams = ((Class) rawType).getTypeParameters();
            for (int j = 0; j < rawTypeParams.length; j++) {
              if (typeVariable.getName().equals(rawTypeParams[j].getName())) {
                collectionTypes[i] = parameterizedType.getActualTypeArguments()[j];
              }
            }
          }
        }
      }
      return collectionTypes;
    }
    return null;
  }

  /**
   * Figure out the concrete type of the field based on the concrete class clazz.
   *
   * @param clazz the class
   * @param field the field
   * @return the class of the concrete field
   */
  public static Class<?> getConcreteFieldType(Class<?> clazz, Field field)
  {
    Type genType = field.getGenericType();
    if (genType instanceof Class) {
      return (Class<?>) genType;
    }
    Map<Class<?>, GenericTypeTrail> typetrails = GenericTypeTrail.buildTypeTransMap(clazz);
    GenericTypeTrail baseTrail = typetrails.get(field.getDeclaringClass());
    if (baseTrail == null) {
      LOG.info("Cannot determine field type: " + clazz.getName() + "." + field.getName());
      return field.getType();
    }
    Class<?> type = baseTrail.getContreteFieldType(field);
    return type;

  }
}
