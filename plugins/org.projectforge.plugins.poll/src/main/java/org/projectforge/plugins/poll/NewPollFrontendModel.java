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

package org.projectforge.plugins.poll;

import org.apache.wicket.injection.Injector;
import org.apache.wicket.spring.injection.annot.SpringBean;
import org.projectforge.framework.persistence.user.entities.GroupDO;
import org.projectforge.framework.persistence.user.entities.PFUserDO;
import org.projectforge.plugins.poll.attendee.PollAttendeeDO;
import org.projectforge.plugins.poll.attendee.PollAttendeeDao;
import org.projectforge.plugins.poll.event.PollEventDO;
import org.projectforge.plugins.poll.event.PollEventDao;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;

/**
 * Data object which is <b>only</b> used for the model chaining in the "new poll" workflow which contains:<br/>
 * - Setting meta information<br/>
 * - Setting poll related date<br/>
 * - Setting poll related attendees<br/>
 * 
 * @author Johannes Unterstein (j.unterstein@micromata.de)
 * @author M. Lauterbach (m.lauterbach@micromata.de)
 * 
 */
public class NewPollFrontendModel implements Serializable
{
  private static final long serialVersionUID = -6709402512895730321L;

  private PollDO pollDo;

  private List<PollEventDO> allEvents;

  private List<PollAttendeeDO> pollAttendeeList;

  private final List<GroupDO> pollGroupList;

  @SpringBean
  private PollAttendeeDao pollAttendeeDao;

  @SpringBean
  private PollEventDao pollEventDao;

  @SpringBean
  private PollDao pollDao;

  /**
   * 
   */
  public NewPollFrontendModel(final PollDO pollDo)
  {
    Injector.get().inject(this);
    this.pollDo = pollDo;
    this.allEvents = new LinkedList<>();
    this.pollAttendeeList = new LinkedList<>();
    this.pollGroupList = new LinkedList<>();
  }

  public void initModelByPoll()
  {
    try {
      pollDo = pollDao.getById(pollDo.getId());
      if (pollDo != null) {
        pollAttendeeList = pollAttendeeDao.getListByPoll(pollDo);
        allEvents = pollEventDao.getListByPoll(pollDo);
      }
    } catch (final Exception ex) {
      // TODO log entry
    }
  }

  public boolean isNew()
  {
    return pollDo.getId() == null;
  }

  /**
   * @return the pollDo
   */
  public PollDO getPollDo()
  {
    return pollDo;
  }

  /**
   * @return the allEvents
   */
  public List<PollEventDO> getAllEvents()
  {
    return allEvents;
  }

  /**
   * @return the pollAttendeeList
   */
  public List<PollAttendeeDO> getPollAttendeeList()
  {
    return pollAttendeeList;
  }

  /**
   * @return the pollGroupList
   */
  public List<GroupDO> getPollGroupList()
  {
    return pollGroupList;
  }

  public List<PFUserDO> getUserDoFromAttendees()
  {
    final List<PFUserDO> result = new LinkedList<>();
    for (final PollAttendeeDO attendee : getPollAttendeeList()) {
      if (attendee.getUser() != null) {
        result.add(attendee.getUser());
      }
    }
    return result;
  }

  /**
   * Get user or email list.
   * 
   * @param choice true for user list, false for email list
   * @return
   */
  public List<PollAttendeeDO> getUserOrEmailList(final boolean choice) {
    final List<PollAttendeeDO> list = new ArrayList<>();
    if (pollAttendeeList != null) {
      for (final PollAttendeeDO attendee : pollAttendeeList) {
        if (choice) {
          if (attendee.getUser() != null) {
            list.add(attendee);
          }
        } else {
          if (attendee.getEmail() != null) {
            list.add(attendee);
          }
        }
      }
    }
    return list;
  }
}
