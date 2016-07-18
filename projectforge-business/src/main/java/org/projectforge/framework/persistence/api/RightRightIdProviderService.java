package org.projectforge.framework.persistence.api;

import java.util.Collection;

/**
 * Provides the user Rights.
 * 
 * @author Roger Rene Kommer (r.kommer.extern@micromata.de)
 *
 */
public interface RightRightIdProviderService
{

  /**
   * Gets the user right ids.
   *
   * @return the user right ids
   */
  Collection<IUserRightId> getUserRightIds();
}
