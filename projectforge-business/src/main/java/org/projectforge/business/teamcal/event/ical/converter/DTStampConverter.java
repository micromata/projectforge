package org.projectforge.business.teamcal.event.ical.converter;

import java.sql.Timestamp;

import org.projectforge.business.teamcal.event.model.TeamEventDO;

import net.fortuna.ical4j.model.DateTime;
import net.fortuna.ical4j.model.Property;
import net.fortuna.ical4j.model.component.VEvent;
import net.fortuna.ical4j.model.property.DtStamp;

public class DTStampConverter extends PropertyConverter
{
  @Override
  public Property toVEvent(final TeamEventDO event)
  {
    DateTime dtStampValue = new DateTime(event.getDtStamp());
    dtStampValue.setUtc(true);
    return new DtStamp(dtStampValue);
  }

  @Override
  public boolean fromVEvent(final TeamEventDO event, final VEvent vEvent)
  {
    if (vEvent.getDateStamp() != null) {
      event.setDtStamp(new Timestamp(vEvent.getDateStamp().getDate().getTime()));
      return true;
    }

    return false;
  }
}
