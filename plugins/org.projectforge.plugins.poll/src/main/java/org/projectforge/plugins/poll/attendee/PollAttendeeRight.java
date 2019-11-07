/////////////////////////////////////////////////////////////////////////////
//
// Project ProjectForge Community Edition
//         www.projectforge.org
//
// Copyright (C) 2001-2019 Micromata GmbH, Germany (www.micromata.com)
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

package org.projectforge.plugins.poll.attendee;

import org.projectforge.business.user.UserRightAccessCheck;
import org.projectforge.business.user.UserRightCategory;
import org.projectforge.business.user.UserRightValue;
import org.projectforge.framework.access.AccessChecker;
import org.projectforge.framework.access.OperationType;
import org.projectforge.framework.persistence.user.entities.PFUserDO;
import org.projectforge.plugins.poll.PollPluginUserRightId;
import org.projectforge.plugins.poll.PollRight;

/**
 * @author Johannes Unterstein (j.unterstein@micromata.de)
 * @author M. Lauterbach (m.lauterbach@micromata.de)
 */
public class PollAttendeeRight extends UserRightAccessCheck<PollAttendeeDO>
{
  private static final long serialVersionUID = 5546777247602641113L;

  private final PollRight pollRight;

  /**
   * @param id
   * @param category
   * @param rightValues
   */
  public PollAttendeeRight(AccessChecker accessChecker)
  {
    super(accessChecker, PollPluginUserRightId.PLUGIN_POLL_ATTENDEE, UserRightCategory.PLUGINS,
        UserRightValue.TRUE);
    pollRight = new PollRight(accessChecker);
  }

  /**
   * @see org.projectforge.business.user.UserRightAccessCheck#hasSelectAccess(org.projectforge.framework.persistence.user.entities.PFUserDO,
   *      java.lang.Object)
   */
  @Override
  public boolean hasSelectAccess(final PFUserDO user, final PollAttendeeDO obj)
  {
    // new poll
    if (obj == null || obj.getPoll() == null) {
      return true;
    }

    if (pollRight.isOwner(user, obj.getPoll())) {
      return true;
    } else {
      if (pollRight.isVerifiedUser(user, null, obj.getPoll())) {
        return true;
      } else {
        return false;
      }
    }
  }

  /**
   * @see org.projectforge.business.user.UserRightAccessCheck#hasInsertAccess(org.projectforge.framework.persistence.user.entities.PFUserDO,
   *      java.lang.Object)
   */
  @Override
  public boolean hasInsertAccess(final PFUserDO user, final PollAttendeeDO obj)
  {
    // new entry if obj.getPoll() == null
    if (obj == null
        || obj.getPoll() == null
        || pollRight.isOwner(user, obj.getPoll())) {
      return true;
    } else {
      return false;
    }
  }

  /**
   * @see org.projectforge.business.user.UserRightAccessCheck#hasAccess(org.projectforge.framework.persistence.user.entities.PFUserDO,
   *      java.lang.Object, java.lang.Object, org.projectforge.framework.access.OperationType)
   */
  @Override
  public boolean hasAccess(final PFUserDO user, final PollAttendeeDO obj, final PollAttendeeDO oldObj,
      final OperationType operationType)
  {
    // new entry if obj == null
    if (obj == null || pollRight.isOwner(user, obj.getPoll())) {
      return true;
    } else {
      return false;
    }
  }
}
