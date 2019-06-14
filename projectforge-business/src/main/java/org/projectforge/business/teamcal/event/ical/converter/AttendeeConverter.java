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

package org.projectforge.business.teamcal.event.ical.converter;

import java.net.URI;
import java.util.Arrays;
import java.util.Iterator;
import java.util.List;

import org.apache.commons.validator.routines.EmailValidator;
import org.projectforge.business.teamcal.event.TeamEventService;
import org.projectforge.business.teamcal.event.model.TeamEventAttendeeDO;
import org.projectforge.business.teamcal.event.model.TeamEventAttendeeStatus;
import org.projectforge.business.teamcal.event.model.TeamEventDO;
import org.springframework.beans.factory.annotation.Autowired;

import net.fortuna.ical4j.model.Parameter;
import net.fortuna.ical4j.model.PropertyList;
import net.fortuna.ical4j.model.component.VEvent;
import net.fortuna.ical4j.model.parameter.Cn;
import net.fortuna.ical4j.model.parameter.CuType;
import net.fortuna.ical4j.model.parameter.PartStat;
import net.fortuna.ical4j.model.parameter.Role;
import net.fortuna.ical4j.model.parameter.Rsvp;
import net.fortuna.ical4j.model.property.Attendee;

public class AttendeeConverter extends PropertyConverter
{
  private static final List<String> STEP_OVER = Arrays.asList(Parameter.CN, Parameter.CUTYPE, Parameter.PARTSTAT, Parameter.RSVP, Parameter.ROLE);

  @Autowired
  private TeamEventService teamEventService;

  @Override
  public boolean toVEvent(TeamEventDO event, VEvent vEvent)
  {
    if (event.getAttendees() == null) {
      return false;
    }

    // TODO add organizer user, most likely as chair
    for (TeamEventAttendeeDO a : event.getAttendees()) {
      String email = "mailto:" + (a.getAddress() != null ? a.getAddress().getEmail() : a.getUrl());

      Attendee attendee = new Attendee(URI.create(email));

      // set common name
      if (a.getAddress() != null) {
        attendee.getParameters().add(new Cn(a.getAddress().getFullName()));
      } else if (a.getCommonName() != null) {
        attendee.getParameters().add(new Cn(a.getCommonName()));
      } else {
        attendee.getParameters().add(new Cn(a.getUrl()));
      }

      attendee.getParameters().add(a.getCuType() != null ? new CuType(a.getCuType()) : CuType.INDIVIDUAL);
      attendee.getParameters().add(a.getRole() != null ? new Role(a.getRole()) : Role.REQ_PARTICIPANT);
      if (a.getRsvp() != null) {
        attendee.getParameters().add(new Rsvp(a.getRsvp()));
      }
      attendee.getParameters().add(a.getStatus() != null ? a.getStatus().getPartStat() : PartStat.NEEDS_ACTION);
      this.parseAdditionalParameters(attendee.getParameters(), a.getAdditionalParams());

      vEvent.getProperties().add(attendee);
    }

    return true;
  }

  @Override
  public boolean fromVEvent(final TeamEventDO event, final VEvent vEvent)
  {
    final PropertyList eventAttendees = vEvent.getProperties(Attendee.ATTENDEE);
    if (eventAttendees == null || eventAttendees.isEmpty()) {
      return false;
    }

    for (int i = 0; i < eventAttendees.size(); i++) {
      Attendee attendee = (Attendee) eventAttendees.get(i);
      URI attendeeUri = attendee.getCalAddress();
      final String email = (attendeeUri != null) ? attendeeUri.getSchemeSpecificPart() : null;

      if (email != null && EmailValidator.getInstance().isValid(email) == false) {
        continue; // TODO maybe validation is not necessary, could also be en url? check rfc
      }

      TeamEventAttendeeDO attendeeDO = new TeamEventAttendeeDO();
      attendeeDO.setUrl(email);

      // set additional fields
      final Cn cn = (Cn) attendee.getParameter(Parameter.CN);
      final CuType cuType = (CuType) attendee.getParameter(Parameter.CUTYPE);
      final PartStat partStat = (PartStat) attendee.getParameter(Parameter.PARTSTAT);
      final Rsvp rsvp = (Rsvp) attendee.getParameter(Parameter.RSVP);
      final Role role = (Role) attendee.getParameter(Parameter.ROLE);

      attendeeDO.setCommonName(cn != null ? cn.getValue() : null);
      attendeeDO.setStatus(partStat != null ? TeamEventAttendeeStatus.getStatusForPartStat(partStat.getValue()) : null);
      attendeeDO.setCuType(cuType != null ? cuType.getValue() : null);
      attendeeDO.setRsvp(rsvp != null ? rsvp.getRsvp() : null);
      attendeeDO.setRole(role != null ? role.getValue() : null);

      // further params
      StringBuilder sb = new StringBuilder();
      Iterator<Parameter> iter = attendee.getParameters().iterator();

      while (iter.hasNext()) {
        final Parameter param = iter.next();
        if (param.getName() == null || STEP_OVER.contains(param.getName())) {
          continue;
        }

        sb.append(";");
        sb.append(param.toString());
      }

      if (sb.length() > 0) {
        // remove first ';'
        attendeeDO.setAdditionalParams(sb.substring(1));
      }

      event.addAttendee(attendeeDO);
    }

    return true;
  }
}
