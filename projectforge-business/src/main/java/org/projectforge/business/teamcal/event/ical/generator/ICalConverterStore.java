package org.projectforge.business.teamcal.event.ical.generator;

import java.util.HashMap;
import java.util.Map;

import org.apache.commons.lang.StringUtils;
import org.projectforge.business.teamcal.event.ical.generator.converter.AlarmConverter;
import org.projectforge.business.teamcal.event.ical.generator.converter.AttendeeConverter;
import org.projectforge.business.teamcal.event.ical.generator.converter.DTEndConverter;
import org.projectforge.business.teamcal.event.ical.generator.converter.DTStartConverter;
import org.projectforge.business.teamcal.event.ical.generator.converter.ExDateConverter;
import org.projectforge.business.teamcal.event.ical.generator.converter.OrganizerConverter;
import org.projectforge.business.teamcal.event.ical.generator.converter.PropertyConverter;
import org.projectforge.business.teamcal.event.ical.generator.converter.SummaryConverter;
import org.projectforge.business.teamcal.event.ical.generator.converter.UidConverter;
import org.projectforge.business.teamcal.event.model.TeamEventDO;

import net.fortuna.ical4j.model.DateTime;
import net.fortuna.ical4j.model.Property;
import net.fortuna.ical4j.model.property.Created;
import net.fortuna.ical4j.model.property.Description;
import net.fortuna.ical4j.model.property.DtStamp;
import net.fortuna.ical4j.model.property.LastModified;
import net.fortuna.ical4j.model.property.Location;
import net.fortuna.ical4j.model.property.Sequence;
import net.fortuna.ical4j.model.property.Transp;

public class ICalConverterStore
{
  private static ICalConverterStore ourInstance = new ICalConverterStore();

  public static ICalConverterStore getInstance()
  {
    return ourInstance;
  }

  private Map<String, VEventConverter> vEventConverters;

  private ICalConverterStore()
  {
    this.vEventConverters = new HashMap<>();

    this.registerVEventConverters();
  }

  public void registerVEventConverter(final String name, final VEventConverter converter)
  {
    if (this.vEventConverters.containsKey(name)) {
      throw new IllegalArgumentException(String.format("A converter with name '%s' already exisits", name));
    }

    this.vEventConverters.put(name, converter);
  }

  public VEventConverter getVEventConverter(final String name)
  {
    return this.vEventConverters.get(name);
  }

  private void registerVEventConverters()
  {
    // DT start
    this.registerVEventConverter(ICalGenerator.VEVENT_DTSTART, new DTStartConverter());

    // DT end
    this.registerVEventConverter(ICalGenerator.VEVENT_DTEND, new DTEndConverter());

    // summary
    this.registerVEventConverter(ICalGenerator.VEVENT_SUMMARY, new SummaryConverter());

    // uid
    this.registerVEventConverter(ICalGenerator.VEVENT_UID, new UidConverter());

    // location
    this.registerVEventConverter(ICalGenerator.VEVENT_LOCATION, new PropertyConverter()
    {
      @Override
      public Property convert(final TeamEventDO event)
      {
        if (StringUtils.isNotBlank(event.getLocation()) == true) {
          return new Location(event.getLocation());
        }

        return null;
      }
    });

    // created
    this.registerVEventConverter(ICalGenerator.VEVENT_CREATED, new PropertyConverter()
    {
      @Override
      public Property convert(final TeamEventDO event)
      {
        DateTime created = new DateTime(event.getCreated());
        created.setUtc(true);
        return new Created(created);
      }
    });

    // DT stamp
    this.registerVEventConverter(ICalGenerator.VEVENT_DTSTAMP, new PropertyConverter()
    {
      @Override
      public Property convert(final TeamEventDO event)
      {
        DateTime dtStampValue = new DateTime(event.getDtStamp());
        dtStampValue.setUtc(true);
        return new DtStamp(dtStampValue);
      }
    });

    // last modified
    this.registerVEventConverter(ICalGenerator.VEVENT_LAST_MODIFIED, new PropertyConverter()
    {
      @Override
      public Property convert(final TeamEventDO event)
      {
        DateTime lastModified = new DateTime(event.getDtStamp() != null ? event.getDtStamp() : event.getCreated());
        lastModified.setUtc(true);
        return new LastModified(lastModified);
      }
    });

    // sequence number
    this.registerVEventConverter(ICalGenerator.VEVENT_SEQUENCE, new PropertyConverter()
    {
      @Override
      public Property convert(final TeamEventDO event)
      {
        return new Sequence(event.getSequence() != null ? event.getSequence() : 0);
      }
    });

    // organizer
    this.registerVEventConverter(ICalGenerator.VEVENT_ORGANIZER, new OrganizerConverter(false));
    this.registerVEventConverter(ICalGenerator.VEVENT_ORGANIZER_EDITABLE, new OrganizerConverter(true));

    // transparency
    this.registerVEventConverter(ICalGenerator.VEVENT_TRANP, new PropertyConverter()
    {
      @Override
      public Property convert(final TeamEventDO event)
      {
        return Transp.OPAQUE; // TODO
      }
    });

    // alarm
    this.registerVEventConverter(ICalGenerator.VEVENT_ALARM, new AlarmConverter());

    // description
    this.registerVEventConverter(ICalGenerator.VEVENT_DESCRIPTION, new PropertyConverter()
    {
      @Override
      public Property convert(final TeamEventDO event)
      {
        if (StringUtils.isNotBlank(event.getNote())) {
          return new Description(event.getNote());
        }

        return null;
      }
    });

    // attendees
    this.registerVEventConverter(ICalGenerator.VEVENT_ATTENDEES, new AttendeeConverter());

    // RRule
    this.registerVEventConverter(ICalGenerator.VEVENT_RRULE, new PropertyConverter()
    {
      @Override
      public Property convert(final TeamEventDO event)
      {
        if (event.hasRecurrence()) {
          return event.getRecurrenceRuleObject();
        }

        return null;
      }
    });

    // ex dates
    this.registerVEventConverter(ICalGenerator.VEVENT_EX_DATE, new ExDateConverter());
  }
}
