package org.projectforge.business.teamcal;

import java.io.ByteArrayOutputStream;
import java.io.IOException;

import org.projectforge.business.configuration.ConfigurationService;
import org.projectforge.business.teamcal.event.model.ReminderActionType;
import org.projectforge.business.teamcal.event.model.TeamEventDO;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import net.fortuna.ical4j.data.CalendarOutputter;
import net.fortuna.ical4j.model.DateTime;
import net.fortuna.ical4j.model.Dur;
import net.fortuna.ical4j.model.Property;
import net.fortuna.ical4j.model.TimeZone;
import net.fortuna.ical4j.model.TimeZoneRegistry;
import net.fortuna.ical4j.model.TimeZoneRegistryFactory;
import net.fortuna.ical4j.model.ValidationException;
import net.fortuna.ical4j.model.component.VAlarm;
import net.fortuna.ical4j.model.component.VEvent;
import net.fortuna.ical4j.model.parameter.Value;
import net.fortuna.ical4j.model.property.Action;
import net.fortuna.ical4j.model.property.CalScale;
import net.fortuna.ical4j.model.property.Description;
import net.fortuna.ical4j.model.property.Duration;
import net.fortuna.ical4j.model.property.Location;
import net.fortuna.ical4j.model.property.ProdId;
import net.fortuna.ical4j.model.property.RRule;
import net.fortuna.ical4j.model.property.Repeat;
import net.fortuna.ical4j.model.property.Uid;
import net.fortuna.ical4j.model.property.Version;

@Service
public class ICSGenerator
{
  private static final org.apache.log4j.Logger log = org.apache.log4j.Logger.getLogger(ICSGenerator.class);

  @Autowired
  private ConfigurationService configService;

  public ByteArrayOutputStream getIcsFile(TeamEventDO data)
  {
    ByteArrayOutputStream baos = null;

    try {
      baos = new ByteArrayOutputStream();

      net.fortuna.ical4j.model.Calendar cal = new net.fortuna.ical4j.model.Calendar();
      cal.getProperties().add(new ProdId("-//ProjectForge//iCal4j 1.0//EN"));
      cal.getProperties().add(Version.VERSION_2_0);
      cal.getProperties().add(CalScale.GREGORIAN);

      TimeZoneRegistry registry = TimeZoneRegistryFactory.getInstance().createRegistry();
      TimeZone timezone = registry.getTimeZone(configService.getTimezone().getID());

      DateTime start = new net.fortuna.ical4j.model.DateTime(data.getStartDate().getTime());
      start.setTimeZone(timezone);
      DateTime stop = new net.fortuna.ical4j.model.DateTime(data.getEndDate().getTime());
      stop.setTimeZone(timezone);

      VEvent event = new VEvent(start, stop, data.getSubject());
      event.getProperties().add(new Description(data.getNote()));
      event.getProperties().add(new Location(data.getLocation()));
      if (data.isAllDay()) {
        event.getProperties().getProperty(Property.DTSTART).getParameters().add(Value.DATE);
      }
      if (data.getRecurrenceRuleObject() != null) {
        RRule rule = data.getRecurrenceRuleObject();
        event.getProperties().add(rule);
      }

      Uid uid = new Uid(data.getUid());
      event.getProperties().add(uid);

      cal.getComponents().add(event);

      if (data.getReminderActionType() != null) {
        VAlarm alarm = null;
        Dur dur = null;
        switch (data.getReminderDurationUnit()) {
          case DAYS:
            dur = new Dur(data.getReminderDuration(), 0, 0, 0);
            alarm = new VAlarm(dur);
            break;
          case HOURS:
            dur = new Dur(0, data.getReminderDuration(), 0, 0);
            alarm = new VAlarm(dur);
            break;
          case MINUTES:
            dur = new Dur(0, 0, data.getReminderDuration(), 0);
            alarm = new VAlarm(dur);
            break;
          default:
            log.info("No valid reminder duration unit.");

        }
        if (alarm != null) {
          if (data.getReminderActionType().equals(ReminderActionType.MESSAGE)) {
            alarm.getProperties().add(Action.DISPLAY);
          } else if (data.getReminderActionType().equals(ReminderActionType.MESSAGE_SOUND)) {
            alarm.getProperties().add(Action.AUDIO);
          }
          alarm.getProperties().add(new Repeat(1));
          alarm.getProperties().add(new Duration(dur));
          event.getAlarms().add(alarm);
        }
      }

      CalendarOutputter outputter = new CalendarOutputter();
      //      outputter.setValidating(false);
      outputter.output(cal, baos);
    } catch (IOException | ValidationException e) {
      log.error("Error while exporting calendar event. " + e.getMessage());
      return null;
    }
    return baos;
  }

}
