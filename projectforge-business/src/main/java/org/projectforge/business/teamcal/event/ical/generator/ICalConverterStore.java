package org.projectforge.business.teamcal.event.ical.generator;

import java.util.HashMap;
import java.util.Map;

import org.projectforge.business.teamcal.event.ical.generator.converter.AlarmConverter;
import org.projectforge.business.teamcal.event.ical.generator.converter.AttendeeConverter;
import org.projectforge.business.teamcal.event.ical.generator.converter.CreatedConverter;
import org.projectforge.business.teamcal.event.ical.generator.converter.DTEndConverter;
import org.projectforge.business.teamcal.event.ical.generator.converter.DTStampConverter;
import org.projectforge.business.teamcal.event.ical.generator.converter.DTStartConverter;
import org.projectforge.business.teamcal.event.ical.generator.converter.DescriptionConverter;
import org.projectforge.business.teamcal.event.ical.generator.converter.ExDateConverter;
import org.projectforge.business.teamcal.event.ical.generator.converter.LastModifiedConverter;
import org.projectforge.business.teamcal.event.ical.generator.converter.LocationConverter;
import org.projectforge.business.teamcal.event.ical.generator.converter.OrganizerConverter;
import org.projectforge.business.teamcal.event.ical.generator.converter.RRuleConverter;
import org.projectforge.business.teamcal.event.ical.generator.converter.SequenceConverter;
import org.projectforge.business.teamcal.event.ical.generator.converter.SummaryConverter;
import org.projectforge.business.teamcal.event.ical.generator.converter.TransparencyConverter;
import org.projectforge.business.teamcal.event.ical.generator.converter.UidConverter;

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
    this.registerVEventConverter(ICalGenerator.VEVENT_DTSTART, new DTStartConverter());
    this.registerVEventConverter(ICalGenerator.VEVENT_DTEND, new DTEndConverter());
    this.registerVEventConverter(ICalGenerator.VEVENT_SUMMARY, new SummaryConverter());
    this.registerVEventConverter(ICalGenerator.VEVENT_UID, new UidConverter());
    this.registerVEventConverter(ICalGenerator.VEVENT_LOCATION, new LocationConverter());
    this.registerVEventConverter(ICalGenerator.VEVENT_CREATED, new CreatedConverter());
    this.registerVEventConverter(ICalGenerator.VEVENT_DTSTAMP, new DTStampConverter());
    this.registerVEventConverter(ICalGenerator.VEVENT_LAST_MODIFIED, new LastModifiedConverter());
    this.registerVEventConverter(ICalGenerator.VEVENT_SEQUENCE, new SequenceConverter());
    this.registerVEventConverter(ICalGenerator.VEVENT_ORGANIZER, new OrganizerConverter(false));
    this.registerVEventConverter(ICalGenerator.VEVENT_ORGANIZER_EDITABLE, new OrganizerConverter(true));
    this.registerVEventConverter(ICalGenerator.VEVENT_TRANSP, new TransparencyConverter());
    this.registerVEventConverter(ICalGenerator.VEVENT_ALARM, new AlarmConverter());
    this.registerVEventConverter(ICalGenerator.VEVENT_DESCRIPTION, new DescriptionConverter());
    this.registerVEventConverter(ICalGenerator.VEVENT_ATTENDEES, new AttendeeConverter());
    this.registerVEventConverter(ICalGenerator.VEVENT_RRULE, new RRuleConverter());
    this.registerVEventConverter(ICalGenerator.VEVENT_EX_DATE, new ExDateConverter());
  }
}
