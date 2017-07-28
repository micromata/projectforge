package org.projectforge.business.teamcal.event.ical.converter;

import java.net.URI;

import org.projectforge.business.teamcal.event.model.TeamEventAttendeeDO;
import org.projectforge.business.teamcal.event.model.TeamEventDO;

import net.fortuna.ical4j.model.component.VEvent;
import net.fortuna.ical4j.model.parameter.Cn;
import net.fortuna.ical4j.model.parameter.CuType;
import net.fortuna.ical4j.model.parameter.PartStat;
import net.fortuna.ical4j.model.parameter.Role;
import net.fortuna.ical4j.model.parameter.Rsvp;
import net.fortuna.ical4j.model.property.Attendee;

public class AttendeeConverter extends PropertyConverter
{
  @Override
  public boolean convert(TeamEventDO event, VEvent vEvent)
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
}
