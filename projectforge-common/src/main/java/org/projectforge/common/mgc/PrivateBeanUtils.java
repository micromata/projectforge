/////////////////////////////////////////////////////////////////////////////
//
// Project ProjectForge Community Edition
//         www.projectforge.org
//
// Copyright (C) 2001-2025 Micromata GmbH, Germany (www.micromata.com)
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

import org.apache.commons.beanutils.ConvertUtils;
import org.apache.commons.beanutils.Converter;

import java.lang.annotation.Annotation;
import java.lang.reflect.*;
import java.util.*;

/**
 * Utility to access private/protected fields without public getter/setter. Reading/Write fields directly without
 * getter/setter.
 *
 * @author roger@micromata.de
 */
public class PrivateBeanUtils
{

  /**
   * The Class AccessibleScope.
   */
  public static class AccessibleScope
  {

    /**
     * The object.
     */
    private AccessibleObject object;

    /**
     * The object was accessible.
     */
    private boolean wasAccessable = false;

    /**
     * Instantiates a new accessible scope.
     *
     * @param object the object
     */
    public AccessibleScope(AccessibleObject object)
    {
      this.object = object;
      wasAccessable = object.isAccessible();
      if (wasAccessable == false) {
        object.setAccessible(true);
      }
    }

    /**
     * Restore.
     */
    public void restore()
    {
      if (wasAccessable == false) {
        wasAccessable = true;
        object.setAccessible(false);
      }
    }
  }

  /**
   * Find a field of given bean.
   *
   * @param bean the bean
   * @param fieldName the field name
   * @return null if not found
   */
  public static Field findField(Object bean, String fieldName)
  {
    return findField(bean.getClass(), fieldName);
  }

  /**
   * For a given getVariable return variable.
   *
   * @param getter the getter
   * @return null if not found
   */
  public static String getFieldNameFromGetter(String getter)
  {
    if (getter == null) {
      return null;
    }
    if (getter.startsWith("get") == true && getter.length() >= 4) {
      return getter.substring(3, 4).toLowerCase() + getter.substring(4);
    }
    if (getter.startsWith("is") == true && getter.length() >= 3) {
      return getter.substring(2, 3).toLowerCase() + getter.substring(3);
    }
    return null;
  }

  /**
   * Gets the getter from field name.
   *
   * @param fieldName the field name
   * @return the getter from field name
   */
  public static String getGetterMethodNameFromFieldName(String fieldName)
  {
    return getGetterOrSetterMethodNameFromFieldName(fieldName, "get");
  }

  /**
   * Gets the setter method name from field name.
   *
   * @param fieldName the field name
   * @return the setter method name from field name
   */
  public static String getSetterMethodNameFromFieldName(String fieldName)
  {
    return getGetterOrSetterMethodNameFromFieldName(fieldName, "set");
  }

  /**
   * Gets the getter from field name.
   *
   * @param fieldName the field name
   * @param prefix the prefix
   * @return the getter from field name
   */
  public static String getGetterOrSetterMethodNameFromFieldName(String fieldName, String prefix)
  {
    char fl = Character.toUpperCase(fieldName.charAt(0));
    String getter = prefix + fl;

    if (fieldName.length() < 2) {
      return getter;
    }
    return getter + fieldName.substring(1);
  }

  /**
   * Find getter from field.
   *
   * @param clazz the clazz
   * @param field the field
   * @return the method
   */
  public static Method findGetterFromField(Class<?> clazz, Field field)
  {
    return findGetterFromField(clazz, field.getName(), field.getType());
  }

  /**
   * Find getter from field.
   *
   * @param clazz the clazz
   * @param fieldName the field name
   * @param fieldType the field type
   * @return the method
   */
  public static Method findGetterFromField(Class<?> clazz, String fieldName, Class<?> fieldType)
  {

    String prefix = "get";
    if (fieldType == Boolean.TYPE) {
      prefix = "is";
    }
    String methodName = getGetterOrSetterMethodNameFromFieldName(fieldName, prefix);
    try {
      Method m = clazz.getMethod(methodName);
      return m;
    } catch (NoSuchMethodException ex) {
      return null;
    }
  }

  /**
   * Find setter from field.
   *
   * @param clazz the clazz
   * @param fieldName the field name
   * @param fieldType the field type
   * @return the method
   */
  public static Method findSetterFromField(Class<?> clazz, String fieldName, Class<?> fieldType)
  {
    String methodname = getSetterMethodNameFromFieldName(fieldName);
    try {
      Method m = clazz.getMethod(methodname, fieldType);
      return m;
    } catch (NoSuchMethodException ex) {
      return null;
    }

  }

  /**
   * Find field from getter.
   *
   * @param clazz the clazz
   * @param method the method
   * @return the field
   */
  public static Field findFieldFromGetter(Class<?> clazz, Method method)
  {
    String fieldName = getFieldNameFromGetter(method.getName());
    Field found = findField(clazz, fieldName);
    return found;
  }

  /**
   * Try finding a field with given name. Goes through class hierarchy.
   *
   * @param cls the cls
   * @param fieldName the field name
   * @return the field
   */
  public static Field findField(Class<?> cls, String fieldName)
  {
    Field f = null;
    try {
      f = cls.getDeclaredField(fieldName);
    } catch (SecurityException ex) {
    } catch (NoSuchFieldException ex) {
    }
    if (f != null) {
      return f;
    }
    if (cls == Object.class || cls.getSuperclass() == null) {
      return null;
    }
    return findField(cls.getSuperclass(), fieldName);
  }

  /**
   * Read a field.
   *
   * @param bean the bean
   * @param fieldName the field name
   * @return the value
   * @throws RuntimeException if field doesn't exists.
   */
  public static Object readField(Object bean, String fieldName)
  {
    Field f = findField(bean, fieldName);
    if (f == null) {
      throw new RuntimeException("No bean field found: " + bean.getClass().getName() + "." + fieldName);
    }
    return readField(bean, f);
  }

  /**
   * Read field.
   *
   * @param <T> the generic type
   * @param bean the bean
   * @param fieldName the field name
   * @param expectedClass the expected class
   * @return the t
   */
  public static <T> T readField(Object bean, String fieldName, Class<T> expectedClass)
  {
    Object o = readField(bean, fieldName);
    return (T) o;
  }

  /**
   * Read static field.
   *
   * @param beanClass the bean class
   * @param fieldName the field name
   * @return the object
   */
  public static Object readStaticField(Class<?> beanClass, String fieldName)
  {
    Field f = findField(beanClass, fieldName);
    if (f == null) {
      throw new RuntimeException("No bean field found: " + beanClass.getName() + "." + fieldName);
    }
    return readField(null, f);
  }

  /**
   * Read static field.
   *
   * @param <T> the generic type
   * @param beanClass the bean class
   * @param fieldName the field name
   * @param expectedClass the expected class
   * @return the t
   */
  public static <T> T readStaticField(Class<?> beanClass, String fieldName, Class<T> expectedClass)
  {
    return (T) readStaticField(beanClass, fieldName);
  }

  /**
   * Write static filed.
   *
   * @param beanClass the bean class
   * @param fieldName the field name
   * @param value the value
   */
  public static void writeStaticFiled(Class<?> beanClass, String fieldName, Object value)
  {
    Field f = findField(beanClass, fieldName);
    if (f == null) {
      throw new RuntimeException("No bean field found: " + beanClass.getName() + "." + fieldName);
    }
    writeField(null, f, value);
  }

  /**
   * Write final static field.
   *
   * @param field the field
   * @param value the value
   */
  public static void writeFinalStaticField(Field field, Object value)
  {
    AccessibleScope ascope = new AccessibleScope(field);
    try {
      Field modifiersField = Field.class.getDeclaredField("modifiers");
      AccessibleScope modscope = new AccessibleScope(field);
      try {
        int oldmods = field.getModifiers();
        modifiersField.setInt(field, oldmods & ~Modifier.FINAL);
        writeField(null, field, value);
        modifiersField.setInt(field, oldmods);
      } finally {
        modscope.restore();
      }
    } catch (RuntimeException ex) { // NOSONAR "Illegal Catch" framework
      throw ex; // NOSONAR "Illegal Catch" framework
    } catch (Exception ex) { // NOSONAR "Illegal Catch" framework
      throw new RuntimeException("Cannot write field: " + field.getDeclaringClass().getName() + "." + field.getName());// NOSONAR "Illegal Catch" framework
    } finally {
      ascope.restore();
    }

  }

  /**
   * Use this with care. This overwrites a final field.
   *
   * @param beanClass the bean class
   * @param fieldName the field name
   * @param value the value
   */
  public static void writeFinalStaticField(Class<?> beanClass, String fieldName, Object value)
  {
    Field f = findField(beanClass, fieldName);
    if (f == null) {
      throw new RuntimeException("No bean field found: " + beanClass.getName() + "." + fieldName);
    }
    writeFinalStaticField(f, value);
  }

  /**
   * Read a bean field.
   *
   * @param bean the bean
   * @param field the field
   * @return the object
   * @throws RuntimeException falls the bean filed can not be accessed
   */
  public static synchronized Object readField(Object bean, Field field)
  {
    AccessibleScope ascope = new AccessibleScope(field);
    try {
      Object o = field.get(bean);
      return o;
    } catch (Exception ex) {
      throw new RuntimeException(
          "Failure accessing bean field: " + bean.getClass().getName() + "." + field + "; " + ex.getMessage(), ex);
    } finally {
      ascope.restore();
    }
  }

  /**
   * Write the beanfield.
   *
   * @param bean the bean
   * @param fieldName the field name
   * @param value the value
   * @throws RuntimeException is the bean field can not be found
   */
  public static void writeField(Object bean, String fieldName, Object value)
  {
    Field f = findField(bean, fieldName);
    if (f == null) {
      throw new RuntimeException("No bean field found: " + bean.getClass().getName() + "." + fieldName);
    }
    writeField(bean, f, value);
  }

  /**
   * Get the type of the field.
   *
   * @param bean the bean
   * @param fieldName the field name
   * @return null if field not found.
   */
  public static Class<?> getFieldType(Object bean, String fieldName)
  {
    Field f = findField(bean, fieldName);
    if (f == null) {
      return null;
    }
    return GenericsUtils.getConcreteFieldType(bean.getClass(), f);
  }

  /**
   * Write a bean field.
   *
   * @param bean the bean
   * @param field the field
   * @param value the value
   * @throws RuntimeException if the bean field can not be accessed
   */
  public static synchronized void writeField(Object bean, Field field, Object value)
  {
    AccessibleScope asc = new AccessibleScope(field);
    try {
      field.set(bean, value);
    } catch (Exception ex) {
      throw new RuntimeException(
          "Failure accessing bean field: " + bean.getClass().getName() + "." + field + "; " + ex.getMessage(), ex);
    } finally {
      asc.restore();
    }
  }

  /**
   * Find fields with annotation.
   *
   * @param clazz the clazz
   * @param annotation the annotation
   * @param res the res
   */
  public static void findFieldsWithAnnotation(Class<?> clazz, Class<? extends Annotation> annotation,
      List<Pair<Field, ? extends Annotation>> res)
  {
    for (Field f : clazz.getDeclaredFields()) {
      Annotation an = f.getAnnotation(annotation);
      if (an == null) {
        continue;
      }
      res.add(new Pair<Field, Annotation>(f, an));
    }
    if (clazz == Object.class || clazz.getSuperclass() == null) {
      return;
    }
    findFieldsWithAnnotation(clazz.getSuperclass(), annotation, res);
  }

  /**
   * Find all fields of bean which implementing iface.
   *
   * @param <T> the generic type
   * @param bean the bean
   * @param iface the iface
   * @param result the result
   * @param depth recursion depth in search
   * @param includeStatics the include statics
   */
  public static <T> void findNestedImplementing(Object bean, Class<T> iface, List<T> result, int depth,
      boolean includeStatics)
  {
    if (bean == null) {
      return;
    }
    Class<?> clazz = bean.getClass();
    findNestedImplementing(bean, clazz, iface, result, depth, includeStatics);
  }

  /**
   * Find nested implementing.
   *
   * @param <T> the generic type
   * @param bean the bean
   * @param clazz the clazz
   * @param iface the iface
   * @param result the result
   * @param depth the depth
   * @param includeStatics the include statics
   */
  @SuppressWarnings("unchecked")
  public static <T> void findNestedImplementing(Object bean, Class<?> clazz, Class<T> iface, List<T> result, int depth,
      boolean includeStatics)
  {
    if (iface.isAssignableFrom(clazz) == true) {
      result.add((T) bean);
    }
    if (depth == 0) {
      return;
    }

    for (Field f : clazz.getDeclaredFields()) {
      if (includeStatics == false && (f.getModifiers() & Modifier.STATIC) == Modifier.STATIC) {
        continue;
      }
      Object o = readField(bean, f);
      findNestedImplementing(o, iface, result, depth - 1, includeStatics);
    }
    if (clazz == Object.class || clazz.getSuperclass() == null) {
      return;
    }
    findNestedImplementing(bean, clazz.getSuperclass(), iface, result, depth, includeStatics);
  }

  /**
   * Find fields with annotation.
   *
   * @param bean the bean
   * @param annotation the annotation
   * @return the list of pair of field,? extends annotation
   */
  public static List<Pair<Field, ? extends Annotation>> findFieldsWithAnnotation(Object bean,
      Class<? extends Annotation> annotation)
  {
    List<Pair<Field, ? extends Annotation>> ret = new ArrayList<Pair<Field, ? extends Annotation>>();
    findFieldsWithAnnotation(bean.getClass(), annotation, ret);
    return ret;
  }

  /**
   * Gets the bean size.
   *
   * @param bean the bean
   * @return the bean size
   */
  public static int getBeanSize(Object bean)
  {
    return getBeanSize(bean, new EveryMatcher<String>());
  }

  /**
   * Gets the bean size.
   *
   * @param bean the bean
   * @param classNameMatcher matches against class name
   * @return the bean size
   */
  public static int getBeanSize(Object bean, Matcher<String> classNameMatcher)
  {
    return getBeanSize(bean, classNameMatcher, new EveryMatcher<String>());
  }

  /**
   * Gets the bean size.
   *
   * @param bean the bean
   * @param classNameMatcher the class name matcher
   * @param fieldNameMatcher matches against (decl class name).fieldname
   * @return the bean size
   */
  public static int getBeanSize(Object bean, Matcher<String> classNameMatcher, Matcher<String> fieldNameMatcher)
  {
    IdentityHashMap<Object, Object> m = new IdentityHashMap<Object, Object>();
    return getBeanSize(bean, m, classNameMatcher, fieldNameMatcher);
  }

  /**
   * Gets the bean size.
   *
   * @param bean the bean
   * @param m the m
   * @param classNameMatcher the class name matcher
   * @param fieldNameMatcher the field name matcher
   * @return the bean size
   */
  public static int getBeanSize(Object bean, IdentityHashMap<Object, Object> m, Matcher<String> classNameMatcher,
      Matcher<String> fieldNameMatcher)
  {

    if (bean == null) {
      return 0;
    }
    if (m.containsKey(bean) == true) {
      return 0;
    }
    try {
      Class<?> clazz = bean.getClass();
      return getBeanSize(bean, clazz, m, classNameMatcher, fieldNameMatcher);
    } catch (NoClassDefFoundError ex) {
      return 0;
    }
  }

  /**
   * Gets the bean size.
   *
   * @param bean the bean
   * @param clazz the clazz
   * @param m the m
   * @param classNameMatcher the class name matcher
   * @param fieldNameMatcher the field name matcher
   * @return the bean size
   */
  public static int getBeanSize(Object bean, Class<?> clazz, IdentityHashMap<Object, Object> m,
      Matcher<String> classNameMatcher, Matcher<String> fieldNameMatcher)
  {
    if (m.containsKey(bean) == true) {
      return 0;
    }
    m.put(bean, null);
    return getBeanSizeIntern(bean, clazz, m, classNameMatcher, fieldNameMatcher);
  }

  /**
   * Gets the bean size intern.
   *
   * @param bean the bean
   * @param clazz the clazz
   * @param m the m
   * @param classNameMatcher the class name matcher
   * @param fieldNameMatcher the field name matcher
   * @return the bean size intern
   */
  public static int getBeanSizeIntern(Object bean, Class<?> clazz, IdentityHashMap<Object, Object> m,
      Matcher<String> classNameMatcher,
      Matcher<String> fieldNameMatcher)
  {
    if (classNameMatcher.match(clazz.getName()) == false) {
      return 0;
    }
    if (clazz.isArray() == true) {
      if (clazz == boolean[].class) {
        return (((boolean[]) bean).length * 4);
      } else if (clazz == char[].class) {
        return (((char[]) bean).length * 2);
      } else if (clazz == byte[].class) {
        return (((byte[]) bean).length * 1);
      } else if (clazz == short[].class) {
        return (((short[]) bean).length * 2);
      } else if (clazz == int[].class) {
        return (((int[]) bean).length * 4);
      } else if (clazz == long[].class) {
        return (((long[]) bean).length * 4);
      } else if (clazz == float[].class) {
        return (((float[]) bean).length * 4);
      } else if (clazz == double[].class) {
        return (((double[]) bean).length * 8);
      } else {
        int length = Array.getLength(bean);
        int ret = (length * 4);
        for (int i = 0; i < length; ++i) {
          ret += getBeanSize(Array.get(bean, i), m, classNameMatcher, fieldNameMatcher);
        }
        return ret;
      }
    }
    int ret = 0;
    try {
      for (Field f : clazz.getDeclaredFields()) {
        int mod = f.getModifiers();
        if (Modifier.isStatic(mod) == true) {
          continue;
        }
        if (fieldNameMatcher.match(clazz.getName() + "." + f.getName()) == false) {
          continue;
        }
        if (f.getType() == Boolean.TYPE) {
          ret += 4;
        } else if (f.getType() == Character.TYPE) {
          ret += 2;
        } else if (f.getType() == Byte.TYPE) {
          ret += 1;
        } else if (f.getType() == Short.TYPE) {
          ret += 2;
        } else if (f.getType() == Integer.TYPE) {
          ret += 4;
        } else if (f.getType() == Long.TYPE) {
          ret += 8;
        } else if (f.getType() == Float.TYPE) {
          ret += 4;
        } else if (f.getType() == Double.TYPE) {
          ret += 8;
        } else {

          ret += 4;
          Object o = null;
          try {
            o = readField(bean, f);
            if (o == null) {
              continue;
            }
          } catch (NoClassDefFoundError ex) {
            // nothing
            continue;
          }
          int nestedsize = getBeanSize(o, o.getClass(), m, classNameMatcher, fieldNameMatcher);
          ret += nestedsize;
        }
      }
    } catch (NoClassDefFoundError ex) {
      // ignore here.
    }
    if (clazz == Object.class || clazz.getSuperclass() == null) {
      return ret;
    }
    ret += getBeanSizeIntern(bean, clazz.getSuperclass(), m, classNameMatcher, fieldNameMatcher);
    return ret;
  }

  /**
   * Find method.
   *
   * @param bean the bean
   * @param clazz the clazz
   * @param method the method
   * @param args the args
   * @return null if method cannot be found
   */
  public static Method findMethod(Object bean, Class<?> clazz, String method, Object... args)
  {
    nextMethod: for (Method m : clazz.getDeclaredMethods()) {
      if (m.getName().equals(method) == false) {
        continue;
      }
      Class<?>[] argClazzes = m.getParameterTypes();
      if (argClazzes.length != args.length) {
        continue;
      }
      for (int i = 0; i < args.length; ++i) {
        Object a = args[i];
        Class<?> ac = argClazzes[i];
        if (a != null && ac.isAssignableFrom(a.getClass()) == false) {
          continue nextMethod;
        }
      }
      return m;
    }
    if (clazz != Object.class && clazz.getSuperclass() != null) {
      return findMethod(bean, clazz.getSuperclass(), method, args);
    }
    return null;
  }

  /**
   * Gets the args descriptor.
   *
   * @param args the args
   * @return the args descriptor
   */
  public static String getArgsDescriptor(Object[] args)
  {
    StringBuilder sb = new StringBuilder();
    sb.append("(");
    boolean first = true;
    for (Object arg : args) {
      if (first == true) {
        first = false;
      } else {
        sb.append(",");
      }
      if (arg != null) {
        sb.append(arg.getClass().getCanonicalName());
      } else {
        sb.append("<null>");
      }
    }
    sb.append(")");
    return sb.toString();
  }

  /**
   * Invokes a method.
   *
   * @param bean the bean
   * @param method the method
   * @param args the args
   * @return return value of method call
   * @throws RuntimeException if bean is null, method cannot be found
   */
  public static Object invokeMethod(Object bean, String method, Object... args)
  {
    Method m = findMethod(bean, bean.getClass(), method, args);
    if (m == null) {
      throw new RuntimeException(
          "Cannot find method to call: " + bean.getClass().getName() + "." + method + getArgsDescriptor(args));
    }
    return invokeMethod(bean, m, args);
  }

  /**
   * Invoke method.
   *
   * @param bean the bean
   * @param method the method
   * @param args the args
   * @return the object
   */
  public static Object invokeMethod(Object bean, Method method, Object... args)
  {
    AccessibleScope ascope = new AccessibleScope(method);
    try {
      return method.invoke(bean, args);
    } catch (InvocationTargetException ex) {
      if (ex.getTargetException() instanceof RuntimeException) {
        throw (RuntimeException) ex.getTargetException();
      }
      throw new RuntimeException(
          "Failure calling method: " + bean.getClass().getName() + "." + method.getName() + ": " + ex.getMessage(), ex);
    } catch (Exception ex) {
      throw new RuntimeException(
          "Failure calling method: " + bean.getClass().getName() + "." + method.getName() + ": " + ex.getMessage(), ex);
    } finally {
      ascope.restore();
    }
  }

  /**
   * Invokes a static method.
   *
   * @param clazz the clazz
   * @param method the method
   * @param args the args
   * @return return value of method call
   * @throws RuntimeException if bean is null, method cannot be found
   */
  public static Object invokeStaticMethod(Class<?> clazz, String method, Object... args)
  {
    Method m = findMethod(null, clazz, method, args);
    if (m == null) {
      throw new RuntimeException(
          "Cannot find method to call: " + clazz.getName() + "." + method + getArgsDescriptor(args));
    }
    AccessibleScope ascope = new AccessibleScope(m);
    try {
      return m.invoke(null, args);
    } catch (InvocationTargetException ex) {
      if (ex.getTargetException() instanceof RuntimeException) {
        throw (RuntimeException) ex.getTargetException();
      }
      throw new RuntimeException("Failure calling method: " + clazz.getName() + "." + method + ": " + ex.getMessage(),
          ex);
    } catch (Exception ex) {
      throw new RuntimeException("Failure calling method: " + clazz.getName() + "." + method + ": " + ex.getMessage(),
          ex);
    } finally {
      ascope.restore();
    }
  }

  /**
   * Invoke method.
   *
   * @param <T> the generic type
   * @param expectedReturnType the expected return type
   * @param bean the bean
   * @param method the method
   * @param args the args
   * @return the t
   */
  @SuppressWarnings("unchecked")
  public static <T> T invokeMethod(Class<T> expectedReturnType, Object bean, String method, Object... args)
  {
    return (T) invokeMethod(bean, method, args);
  }

  /**
   * Find constructor.
   *
   * @param <T> the generic type
   * @param clazz the clazz
   * @param args the args
   * @return the constructor
   */
  @SuppressWarnings("unchecked")
  public static <T> Constructor<T> findConstructor(Class<T> clazz, Object[] args)
  {
    nextMethod: for (Constructor<?> constructor : clazz.getDeclaredConstructors()) {

      Class<?>[] argClazzes = constructor.getParameterTypes();
      if (argClazzes.length != args.length) {
        continue;
      }
      for (int i = 0; i < args.length; ++i) {
        Object a = args[i];
        Class<?> ac = argClazzes[i];
        if (a != null && ac.isAssignableFrom(a.getClass()) == false) {
          continue nextMethod;
        }
      }
      return (Constructor<T>) constructor;
    }
    return null;
  }

  /**
   * Creates the instance calling the constructor with given arguments.
   *
   * @param <T> the generic type
   * @param clazz the clazz
   * @param args the args
   * @return the t
   */
  public static <T> T createInstance(Class<T> clazz, Object... args)
  {
    Constructor<T> constr = findConstructor(clazz, args);
    if (constr == null) {
      throw new RuntimeException("Cannot find constructor to call: " + clazz.getName() + "." + getArgsDescriptor(args));
    }
    AccessibleScope ascope = new AccessibleScope(constr);
    try {
      return constr.newInstance(args);
    } catch (Exception ex) {
      throw new RuntimeException("Failure calling constructor: " + clazz.getName() + "." + ex.getMessage(), ex);
    } finally {
      ascope.restore();
    }

  }

  /**
   * Create a new instance of source (default constructor) and then copy all fields from source to new instance.
   *
   * @param <T> the generic type
   * @param source the source
   * @return the t
   */
  @SuppressWarnings("unchecked")
  public static <T> T cloneWithInstanceProperties(T source)
  {
    T newInstance = createInstance((Class<T>) source.getClass());
    copyInstanceProperties(source, newInstance);
    return newInstance;
  }

  /**
   * Used for copy-Constructors.
   *
   * @param <T> the generic type
   * @param source the source
   * @param target the target
   */
  public static <T> void copyInstanceProperties(T source, T target)
  {
    copyInstanceProperties(source.getClass(), source, target);
  }

  /**
   * Copy all instance fields from source to target. static fields are not copied.
   *
   * @param <T> the generic type
   * @param targetClass the target class
   * @param source the source
   * @param target the target
   */
  public static <T> void copyInstanceProperties(Class<?> targetClass, T source, T target)
  {
    if (targetClass == null) {
      return;
    }

    for (Field f : targetClass.getDeclaredFields()) {
      int mod = f.getModifiers();
      if (Modifier.isStatic(mod) == true) {
        continue;
      }
      Object value = readField(source, f.getName());
      writeField(target, f, value);
    }
    copyInstanceProperties(targetClass.getSuperclass(), source, target);
  }

  /**
   * Copies the fields, where the field names are matches.
   *
   * @param <T> the generic type
   * @param targetClass the target class
   * @param source the source
   * @param target the target
   * @param fieldNameMatcher the field name matcher
   */
  public static <T> void copyInstanceProperties(Class<?> targetClass, T source, T target,
      Matcher<Field> fieldNameMatcher)
  {
    if (targetClass == null) {
      return;
    }

    for (Field f : targetClass.getDeclaredFields()) {
      int mod = f.getModifiers();
      if (Modifier.isStatic(mod) == true) {
        continue;
      }
      if (fieldNameMatcher.match(f) == false) {
        continue;
      }
      Object value = readField(source, f.getName());
      writeField(target, f, value);
    }
    copyInstanceProperties(targetClass.getSuperclass(), source, target, fieldNameMatcher);
  }

  /**
   * Find all fields.
   *
   * @param clazz the clazz
   * @param matcher the matcher
   * @return the list
   */
  public static List<Field> findAllFields(Class<?> clazz, Matcher<Field> matcher)
  {
    List<Field> ret = new ArrayList<>();
    fetchAllFields(clazz, matcher, ret);
    return ret;
  }

  /**
   * Fetch all fields.
   *
   * @param clazz the clazz
   * @param matcher the matcher
   * @param ret the ret
   */
  public static void fetchAllFields(Class<?> clazz, Matcher<Field> matcher, List<Field> ret)
  {
    if (clazz == null) {
      return;
    }
    for (Field f : clazz.getDeclaredFields()) {
      if (matcher.match(f) == true) {
        ret.add(f);
      }
    }
    fetchAllFields(clazz.getSuperclass(), matcher, ret);
  }

  /**
   * write all properties directly to fields, ignoring getter/setter. The types of the values must have the same of
   * target type of the properties
   *
   * @param bean target bean
   * @param properties the properties
   */
  public static void populate(Object bean, Map<String, Object> properties)
  {
    for (Map.Entry<String, Object> me : properties.entrySet()) {
      Field f = findField(bean, me.getKey());
      if (f == null) {
        continue;
      }
      writeField(bean, f, me.getValue());
    }
  }

  /**
   * Convert internal.
   *
   * @param value the value
   * @param type the type
   * @return the object
   */
  protected static Object convertInternal(Object value, Class<?> type)
  {

    Converter converter = ConvertUtils.lookup(type);
    if (converter != null) {
      return converter.convert(type, value);
    } else {
      return value;
    }
  }

  /**
   * Convert.
   *
   * @param value the value
   * @param type the type
   * @return the object
   */
  protected static Object convert(String value, Class<?> type)
  {
    Object newValue = null;
    if (type.isArray()) { // Indexed value into array
      newValue = ConvertUtils.convert(value, type.getComponentType());
    } else { // Value into scalar
      newValue = ConvertUtils.convert(value, type);
    }
    return newValue;
  }

  /**
   * write all properties directly to fields, ignoring getter/setter. It uses internal ConvertUtils to try to convert
   * strings to proper field types.
   *
   * @param bean target bean
   * @param properties the properties
   */
  public static void populate2(Object bean, Map<String, String> properties)
  {
    for (Map.Entry<String, String> me : properties.entrySet()) {
      Field f = findField(bean, me.getKey());
      if (f == null) {
        continue;
      }
      String val = me.getValue();
      Object nval = convert(val, f.getType());
      writeField(bean, f, nval);
    }
  }

  /**
   * Gets the all non static fields.
   *
   * @param bean the bean
   * @return the all non static fields
   */
  public static Map<String, Object> getAllNonStaticFields(Object bean)
  {
    Map<String, Object> ret = new HashMap<String, Object>();
    fetchAllNonStaticFields(ret, bean.getClass(), bean);
    return ret;
  }

  /**
   * Gets the all fields.
   *
   * @param bean the bean
   * @param modifiederMask the modifieder mask
   * @param negModifierMask the neg modifier mask
   * @return the all fields
   */
  public static Map<String, Object> getAllFields(Object bean, int modifiederMask, int negModifierMask)
  {
    Map<String, Object> ret = new HashMap<String, Object>();
    fetchAllFields(ret, bean.getClass(), bean, modifiederMask, negModifierMask);
    return ret;
  }

  /**
   * Read all field into a map.
   *
   * @param ret the ret
   * @param clz the clz
   * @param bean the bean
   * @param modifiederMask required Modified.XXX fields set.
   * @param negModifierMask disalowwed Modified.XXX fields set.
   */
  public static void fetchAllFields(Map<String, Object> ret, Class<?> clz, Object bean, int modifiederMask,
      int negModifierMask)
  {
    if (clz == null) {
      return;
    }
    for (Field f : clz.getDeclaredFields()) {
      int mod = f.getModifiers();
      if ((mod & negModifierMask) != 0) {
        continue;
      }
      if ((mod & modifiederMask) != modifiederMask) {
        continue;
      }
      if (ret.containsKey(f.getName()) == true) {
        continue;
      }
      Object value = readField(bean, f);
      ret.put(f.getName(), value);
    }
    fetchAllFields(ret, clz.getSuperclass(), bean, modifiederMask, negModifierMask);
  }

  /**
   * Fetch all non static fields.
   *
   * @param ret the ret
   * @param clz the clz
   * @param bean the bean
   */
  public static void fetchAllNonStaticFields(Map<String, Object> ret, Class<?> clz, Object bean)
  {
    fetchAllFields(ret, clz, bean, 0, Modifier.STATIC);
  }

  /**
   * Try to load from thread context class loader and checks if assignable.
   *
   * @param className name of class assigned to
   * @param source source.
   * @return true, if is assignable from
   */
  public static boolean isAssignableFrom(String className, Class<?> source)
  {
    try {
      Class<?> cls = Thread.currentThread().getContextClassLoader().loadClass(className);
      return cls.isAssignableFrom(source);
    } catch (ClassNotFoundException ex) {
      return false;
    }
  }

  /**
   * Gets the field attr getter.
   *
   * @param <BEAN> the generic type
   * @param <T> the generic type
   * @param beanClass the bean class
   * @param field the field
   * @param fieldType the field type
   * @return the field attr getter
   */
  public static <BEAN, T> AttrGetter<BEAN, T> getFieldAttrGetter(Class<BEAN> beanClass, Field field, Class<T> fieldType)
  {
    return (bean) -> (T) readField(bean, field);
  }

  /**
   * Gets the field attr setter.
   *
   * @param <BEAN> the generic type
   * @param <T> the generic type
   * @param beanClass the bean class
   * @param field the field
   * @param fieldType the field type
   * @return the field attr setter
   */

  public static <BEAN, T> AttrSetter<BEAN, T> getFieldAttrSetter(Class<BEAN> beanClass, Field field,
      Class<T> fieldType)
  {
    return (bean, value) -> writeField(bean, field, value);
  }

  /**
   * Gets the method attr getter.
   *
   * @param <BEAN> the generic type
   * @param <T> the generic type
   * @param beanClass the bean class
   * @param method the method
   * @param fieldType the field type
   * @return the method attr getter
   */
  public static <BEAN, T> AttrGetter<BEAN, T> getMethodAttrGetter(Class<BEAN> beanClass, Method method,
      Class<T> fieldType)
  {
    return (bean) -> (T) invokeMethod(bean, method);
  }

  /**
   * Gets the method attr setter.
   *
   * @param <BEAN> the generic type
   * @param <T> the generic type
   * @param beanClass the bean class
   * @param method the method
   * @param fieldType the field type
   * @return the method attr setter
   */
  public static <BEAN, T> AttrSetter<BEAN, T> getMethodAttrSetter(Class<BEAN> beanClass, Method method,
      Class<T> fieldType)
  {
    return (bean, value) -> invokeMethod(bean, method, value);
  }

}
