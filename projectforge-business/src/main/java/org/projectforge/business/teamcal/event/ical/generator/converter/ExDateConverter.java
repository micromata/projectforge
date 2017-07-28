package org.projectforge.business.teamcal.event.ical.generator.converter;

import java.util.List;

import org.projectforge.business.teamcal.event.ical.generator.VEventConverter;
import org.projectforge.business.teamcal.event.model.TeamEventDO;
import org.projectforge.framework.calendar.ICal4JUtils;
import org.springframework.util.CollectionUtils;

import net.fortuna.ical4j.model.Date;
import net.fortuna.ical4j.model.DateList;
import net.fortuna.ical4j.model.component.VEvent;
import net.fortuna.ical4j.model.parameter.Value;
import net.fortuna.ical4j.model.property.ExDate;

public class ExDateConverter implements VEventConverter
{

  @Override
  public boolean convert(final TeamEventDO event, final VEvent vEvent)
  {
    if (event.hasRecurrence() == false || event.getRecurrenceExDate() == null) {
      return false;
    }

    final List<Date> exDates = ICal4JUtils.parseCSVDatesAsICal4jDates(event.getRecurrenceExDate(), (false == event.isAllDay()), ICal4JUtils.getUTCTimeZone());

    if (CollectionUtils.isEmpty(exDates)) {
      return false;
    }

    for (final Date date : exDates) {
      final DateList dateList;
      if (event.isAllDay() == true) {
        dateList = new DateList(Value.DATE);
      } else {
        dateList = new DateList();
        dateList.setUtc(true);
      }

      dateList.add(date);
      ExDate exDate;
      exDate = new ExDate(dateList);
      vEvent.getProperties().add(exDate);
    }

    return true;
  }
}
