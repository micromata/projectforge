/////////////////////////////////////////////////////////////////////////////
//
// Project ProjectForge Community Edition
//         www.projectforge.org
//
// Copyright (C) 2001-2023 Micromata GmbH, Germany (www.micromata.com)
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

import org.apache.wicket.model.AbstractReadOnlyModel;
import org.apache.wicket.model.IModel;
import org.apache.wicket.util.lang.Args;

import java.util.function.Consumer;
import java.util.function.Supplier;

/**
 * Backport of the LambdaModel of Wicket 8. If we upgrade to Wicket 8, we can remove this class.
 */
public class LambdaModel
{
  private LambdaModel()
  {
    // Private constructor to avoid instantiating this class.
  }

  /**
   * Usage: LambdaModel.of(foo::getBar)
   */
  public static <T> IModel<T> of(final Supplier<T> getter)
  {
    Args.notNull(getter, "getter");

    return new AbstractReadOnlyModel<T>()
    {
      @Override
      public T getObject()
      {
        return getter.get();
      }
    };
  }

  /**
   * Usage: LambdaModel.of(foo::getBar, foo::setBar)
   */
  public static <T> IModel<T> of(final Supplier<T> getter, final Consumer<T> setter)
  {
    Args.notNull(getter, "getter");
    Args.notNull(setter, "setter");

    return new IModel<T>()
    {
      @Override
      public T getObject()
      {
        return getter.get();
      }

      @Override
      public void setObject(T t)
      {
        setter.accept(t);
      }

      @Override
      public void detach()
      {
      }
    };
  }
}
