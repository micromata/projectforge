package org.projectforge.business.teamcal.event.ical.generator.converter;

import org.projectforge.business.teamcal.event.ical.generator.VEventConverter;
import org.projectforge.business.teamcal.event.model.ReminderDurationUnit;
import org.projectforge.business.teamcal.event.model.TeamEventDO;

import net.fortuna.ical4j.model.Dur;
import net.fortuna.ical4j.model.component.VAlarm;
import net.fortuna.ical4j.model.component.VEvent;
import net.fortuna.ical4j.model.property.Action;
import net.fortuna.ical4j.model.property.Trigger;

public class AlarmConverter implements VEventConverter
{
  @Override
  public boolean convert(final TeamEventDO event, final VEvent vEvent)
  {
    if (event.getReminderDuration() == null || event.getReminderActionType() == null) {
      return false;
    }

    final VAlarm alarm = new VAlarm();
    Dur dur = null;
    // (-1) * needed to set alert before
    if (ReminderDurationUnit.MINUTES.equals(event.getReminderDurationUnit())) {
      dur = new Dur(0, 0, (-1) * event.getReminderDuration(), 0);
    } else if (ReminderDurationUnit.HOURS.equals(event.getReminderDurationUnit())) {
      dur = new Dur(0, (-1) * event.getReminderDuration(), 0, 0);
    } else if (ReminderDurationUnit.DAYS.equals(event.getReminderDurationUnit())) {
      dur = new Dur((-1) * event.getReminderDuration(), 0, 0, 0);
    }

    if (dur == null) {
      return false;
    }

    alarm.getProperties().add(new Trigger(dur));
    alarm.getProperties().add(new Action(event.getReminderActionType().getType()));
    vEvent.getAlarms().add(alarm);

    return true;
  }
}
