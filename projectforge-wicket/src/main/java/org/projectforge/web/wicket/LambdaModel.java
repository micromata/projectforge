package org.projectforge.web.wicket;

import java.util.function.Consumer;
import java.util.function.Supplier;

import org.apache.wicket.model.AbstractReadOnlyModel;
import org.apache.wicket.model.IModel;
import org.apache.wicket.util.lang.Args;

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
