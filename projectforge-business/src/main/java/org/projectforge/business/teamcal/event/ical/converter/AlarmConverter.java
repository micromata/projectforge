package org.projectforge.business.teamcal.event.ical.converter;

import java.util.List;

import org.projectforge.business.teamcal.event.ical.VEventComponentConverter;
import org.projectforge.business.teamcal.event.model.ReminderActionType;
import org.projectforge.business.teamcal.event.model.ReminderDurationUnit;
import org.projectforge.business.teamcal.event.model.TeamEventDO;

import net.fortuna.ical4j.model.Dur;
import net.fortuna.ical4j.model.component.VAlarm;
import net.fortuna.ical4j.model.component.VEvent;
import net.fortuna.ical4j.model.property.Action;
import net.fortuna.ical4j.model.property.Trigger;

public class AlarmConverter implements VEventComponentConverter
{
  private static final int DURATION_OF_WEEK = 7;

  @Override
  public boolean toVEvent(final TeamEventDO event, final VEvent vEvent)
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

  @Override
  public boolean fromVEvent(final TeamEventDO event, final VEvent vEvent)
  {

    final List<VAlarm> alarms = vEvent.getAlarms();
    if (alarms == null || alarms.isEmpty()) {
      return false;
    }

    final VAlarm alarm = alarms.get(0);
    final Dur dur = alarm.getTrigger().getDuration();
    if (alarm.getAction() == null || dur == null) {
      return false;
    }

    if (Action.AUDIO.equals(alarm.getAction())) {
      event.setReminderActionType(ReminderActionType.MESSAGE_SOUND);
    } else {
      event.setReminderActionType(ReminderActionType.MESSAGE);
    }

    // consider weeks
    int weeksToDays = 0;
    if (dur.getWeeks() != 0) {
      weeksToDays = dur.getWeeks() * DURATION_OF_WEEK;
    }

    if (dur.getDays() != 0) {
      event.setReminderDuration(dur.getDays() + weeksToDays);
      event.setReminderDurationUnit(ReminderDurationUnit.DAYS);
    } else if (dur.getHours() != 0) {
      event.setReminderDuration(dur.getHours());
      event.setReminderDurationUnit(ReminderDurationUnit.HOURS);
    } else if (dur.getMinutes() != 0) {
      event.setReminderDuration(dur.getMinutes());
      event.setReminderDurationUnit(ReminderDurationUnit.MINUTES);
    } else {
      event.setReminderDuration(15);
      event.setReminderDurationUnit(ReminderDurationUnit.MINUTES);
    }

    return true;
  }
}
