package org.projectforge.framework.persistence.api;

import org.projectforge.common.i18n.I18nEnum;

/**
 * An User right.
 * 
 * @author Roger Rene Kommer (r.kommer.extern@micromata.de)
 *
 */
public interface IUserRightId extends I18nEnum
{
  /**
   * ID of the right
   * 
   * @return
   */
  String getId();

  /**
   * how to order.
   * 
   * @return
   */
  String getOrderString();

  int compareTo(IUserRightId o);

}
