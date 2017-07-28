package org.projectforge.business.teamcal.event.ical.converter;

import org.projectforge.business.teamcal.event.model.TeamEventDO;

import net.fortuna.ical4j.model.DateTime;
import net.fortuna.ical4j.model.Property;
import net.fortuna.ical4j.model.property.DtStamp;

public class DTStampConverter extends PropertyConverter
{
  @Override
  public Property convert(final TeamEventDO event)
  {
    DateTime dtStampValue = new DateTime(event.getDtStamp());
    dtStampValue.setUtc(true);
    return new DtStamp(dtStampValue);
  }
}
