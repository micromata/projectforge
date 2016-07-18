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

package org.projectforge.web.wicket;

import java.io.ObjectStreamException;
import java.io.Serializable;
import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;

import org.apache.wicket.model.IModel;
import org.apache.wicket.util.lang.Objects;

/**
 * From the book "Wicket in Action" by Martijn Dashorst and Eelco Hillenius.<br/>
 * ©2009 by Manning Publications Co. All rights reserved.
 * 
 */
public final class EqualsDecorator
{
  private EqualsDecorator()
  {
  }

  public static IModel< ? > decorate(final IModel< ? > model)
  {
    return (IModel< ? >) Proxy.newProxyInstance(model.getClass().getClassLoader(), model.getClass().getInterfaces(), new Decorator(model));
  }

  @SuppressWarnings("serial")
  private static class Decorator implements InvocationHandler, Serializable
  {
    private final IModel< ? > model;

    Decorator(IModel< ? > model)
    {
      this.model = model;
    }

    public Object invoke(Object proxy, Method method, Object[] args) throws Throwable
    {
      String methodName = method.getName();
      if (methodName.equals("equals")) {
        if (args[0] instanceof IModel< ? >) {
          return Objects.equal(model.getObject(), ((IModel< ? >) args[0]).getObject());
        }
      } else if (methodName.equals("hashCode")) {
        Object val = model.getObject();
        return Objects.hashCode(val);
      } else if (methodName.equals("writeReplace")) {
        return new SerializableReplacement(model);
      }
      return method.invoke(model, args);
    }
  }

  @SuppressWarnings("serial")
  private static class SerializableReplacement implements Serializable
  {
    private final IModel< ? > model;

    SerializableReplacement(IModel< ? > model)
    {
      this.model = model;
    }

    private Object readResolve() throws ObjectStreamException
    {
      return decorate(model);
    }
  }
}
