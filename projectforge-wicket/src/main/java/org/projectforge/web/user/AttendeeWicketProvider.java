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

package org.projectforge.web.user;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Set;

import org.projectforge.business.teamcal.event.TeamEventService;
import org.projectforge.business.teamcal.event.model.TeamEventAttendeeDO;
import org.projectforge.business.teamcal.event.model.TeamEventDO;
import org.projectforge.business.user.I18nHelper;
import org.projectforge.framework.utils.NumberHelper;

import com.vaynberg.wicket.select2.Response;
import com.vaynberg.wicket.select2.TextChoiceProvider;

public class AttendeeWicketProvider extends TextChoiceProvider<TeamEventAttendeeDO>
{
  private static final org.apache.log4j.Logger log = org.apache.log4j.Logger.getLogger(AttendeeWicketProvider.class);

  private static final long serialVersionUID = 6228672635966093257L;

  final private TeamEventDO event;

  private List<TeamEventAttendeeDO> sortedAttendees;

  private List<TeamEventAttendeeDO> customAttendees = new ArrayList<>();

  private transient TeamEventService teamEventService;

  private int pageSize = 20;

  private Integer internalNewAttendeeSequence = -1;

  public AttendeeWicketProvider(TeamEventDO event, TeamEventService teamEventService)
  {
    this.event = event;
    this.teamEventService = teamEventService;
  }

  /**
   * @param pageSize the pageSize to set
   * @return this for chaining.
   */
  public AttendeeWicketProvider setPageSize(final int pageSize)
  {
    this.pageSize = pageSize;
    return this;
  }

  /**
   * @see com.vaynberg.wicket.select2.TextChoiceProvider#getDisplayText(java.lang.Object)
   */
  @Override
  protected String getDisplayText(final TeamEventAttendeeDO choice)
  {
    String name = "";
    if (choice.getAddress() != null) {
      if (choice.getUser() != null) {
        name = "[" + I18nHelper.getLocalizedString("user") + "] " + choice.getAddress().getFullName();
      } else {
        name = "[" + I18nHelper.getLocalizedString("address.addressText") + "] " + choice.getAddress().getFullName();
      }
    }
    String mail = choice.getAddress() != null ? choice.getAddress().getEmail() : choice.getUrl();
    if (mail == null) {
      mail = "";
    }
    return name + " (" + mail + ")";
  }

  /**
   * @see com.vaynberg.wicket.select2.TextChoiceProvider#getId(java.lang.Object)
   */
  @Override
  protected Object getId(final TeamEventAttendeeDO choice)
  {
    return choice.getId();
  }

  /**
   * @see com.vaynberg.wicket.select2.ChoiceProvider#query(java.lang.String, int, com.vaynberg.wicket.select2.Response)
   */
  @Override
  public void query(String term, final int page, final Response<TeamEventAttendeeDO> response)
  {
    if (sortedAttendees == null) {
      sortedAttendees = teamEventService.getAddressesAndUserAsAttendee();
      Set<TeamEventAttendeeDO> assignedAttendees = event.getAttendees();
      List<TeamEventAttendeeDO> removeAddressAttendeeList = new ArrayList<>();
      if (assignedAttendees != null) {
        for (TeamEventAttendeeDO addressAttendee : sortedAttendees) {
          for (TeamEventAttendeeDO alreadyAssignedAttendee : assignedAttendees) {
            if (addressAttendee.equals(alreadyAssignedAttendee)) {
              removeAddressAttendeeList.add(addressAttendee);
            }
          }
        }
        sortedAttendees.removeAll(removeAddressAttendeeList);
        sortedAttendees.addAll(assignedAttendees);
      }
    }
    final List<TeamEventAttendeeDO> result = new ArrayList<>();
    term = term.toLowerCase();

    final int offset = page * pageSize;

    int matched = 0;
    boolean hasMore = false;
    for (final TeamEventAttendeeDO attendee : sortedAttendees) {
      if (attendee.getId() == null) {
        attendee.setId(internalNewAttendeeSequence);
        internalNewAttendeeSequence--;
      }
      if (result.size() == pageSize) {
        hasMore = true;
        break;
      }
      if ((attendee.getAddress() != null && attendee.getAddress().getFullName().toLowerCase().contains(term) == true)
          || (attendee.getUrl() != null && attendee.getUrl().toLowerCase().contains(term) == true)) {
        matched++;
        if (matched > offset) {
          result.add(attendee);
        }
      }
    }

    if (result.size() == 0) {
      TeamEventAttendeeDO newAttendee = new TeamEventAttendeeDO().setUrl(term);
      newAttendee.setId(internalNewAttendeeSequence);
      internalNewAttendeeSequence--;
      customAttendees.add(newAttendee);
      result.add(newAttendee);
    }

    response.addAll(result);
    response.setHasMore(hasMore);
  }

  /**
   * @see com.vaynberg.wicket.select2.ChoiceProvider#toChoices(java.util.Collection)
   */
  @Override
  public Collection<TeamEventAttendeeDO> toChoices(final Collection<String> ids)
  {
    final List<TeamEventAttendeeDO> list = new ArrayList<>();
    if (ids == null) {
      return list;
    }
    for (final String str : ids) {
      final Integer attendeeId = NumberHelper.parseInteger(str);
      if (attendeeId == null) {
        continue;
      }
      TeamEventAttendeeDO attendee = null;
      if (attendeeId < 0) {
        attendee = sortedAttendees
            .stream()
            .filter(att -> att.getId().equals(attendeeId))
            .findFirst().orElse(null);
        if (attendee == null) {
          attendee = customAttendees
              .stream()
              .filter(att -> att.getId().equals(attendeeId))
              .findFirst().orElse(null);
        }
      } else {
        attendee = teamEventService.getAttendee(attendeeId);
      }
      if (attendee != null) {
        list.add(attendee);
      }
    }
    return list;
  }

}