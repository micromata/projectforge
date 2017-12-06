/////////////////////////////////////////////////////////////////////////////
//
// Project ProjectForge Community Edition
//         www.projectforge.org
//
// Copyright (C) 2001-2014 Kai Reinhard (k.reinhard@micromata.de)
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

package org.projectforge.framework.utils;

import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;

public class ReflectionHelper
{
  private static final org.slf4j.Logger log = org.slf4j.LoggerFactory.getLogger(ReflectionHelper.class);

  /**
   * Convenient method.
   * @param clazz
   * @param argClass
   * @param arg
   * @see #newInstance(Class, Class[], Object[])
   */
  public static Object newInstance(final Class< ? > clazz, final Class< ? > argClass, final Object arg)
  {
    final Class< ? >[] argClasses = new Class[1];
    argClasses[0] = argClass;
    final Object[] args = new Object[1];
    args[0] = arg;
    return newInstance(clazz, argClasses, args);
  }

  /**
   * Convenient method.
   * @param clazz
   * @param argClass0
   * @param argClass1
   * @param arg0
   * @param arg1
   * @see #newInstance(Class, Class[], Object[])
   */
  public static Object newInstance(final Class< ? > clazz, final Class< ? > argClass0, final Class< ? > argClass1, final Object arg0, final Object arg1)
  {
    final Class< ? >[] argClasses = new Class[2];
    argClasses[0] = argClass0;
    argClasses[1] = argClass1;
    final Object[] args = new Object[2];
    args[0] = arg0;
    args[1] = arg1;
    return newInstance(clazz, argClasses, args);
  }

  /**
   * Creates a new instance using the constructor with arguments of class argClasses.
   * @param clazz Class of object to instantiate.
   * @param argClasses Argument list of constructor to use.
   * @param args Arguments given to constructor.
   * @return
   */
  public static Object newInstance(final Class< ? > clazz, final Class< ? >[] argClasses, final Object[] args)
  {
    try {
      final Constructor< ? > constructor = clazz.getConstructor(argClasses);
      return constructor.newInstance(args);
    } catch (NoSuchMethodException ex) {
      log.error(ex.getMessage(), ex);
      throw new RuntimeException(ex);
    } catch (IllegalAccessException ex) {
      log.error(ex.getMessage(), ex);
      throw new RuntimeException(ex);
    } catch (InstantiationException ex) {
      log.error(ex.getMessage(), ex);
      throw new RuntimeException(ex);
    } catch (InvocationTargetException ex) {
      log.error(ex.getMessage(), ex);
      throw new RuntimeException(ex);
    }
  }
}
