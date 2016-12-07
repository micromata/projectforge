package org.projectforge.framework.persistence.api;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * @author Roger Rene Kommer (r.kommer.extern@micromata.de)
 */
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.TYPE)
public @interface AUserRightId
{

  String value();

  /**
   * If set to false you can skip the access check. This is useful if you have a DO which should be accessible from every user, not only the users which have the right.
   */
  boolean checkAccess() default true;

}
