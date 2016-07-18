package org.projectforge.framework.persistence.api;

import java.util.List;

import org.projectforge.business.user.UserRight;

/**
 * Gather user Rights.
 * 
 * @author Roger Rene Kommer (r.kommer.extern@micromata.de)
 *
 */
public interface UserRightService
{
  UserRight getRight(String userRightId);

  UserRight getRight(IUserRightId id);

  /**
   * 
   * @param userRightId
   * @return
   * @throws IllegalArgumentException if not found
   */
  IUserRightId getRightId(final String userRightId) throws IllegalArgumentException;

  List<UserRight> getOrderedRights();

  void addRight(final UserRight right);
}
