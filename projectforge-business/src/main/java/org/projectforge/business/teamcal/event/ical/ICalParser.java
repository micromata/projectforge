package org.projectforge.business.teamcal.event.ical;

import static org.projectforge.business.teamcal.event.ical.ICalConverterStore.FULL_LIST;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.io.StringReader;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.TimeZone;

import org.apache.commons.lang.StringUtils;
import org.projectforge.business.teamcal.TeamCalConfig;
import org.projectforge.business.teamcal.event.model.TeamEventDO;
import org.projectforge.framework.persistence.user.api.ThreadLocalUserContext;
import org.projectforge.framework.persistence.user.entities.PFUserDO;

import net.fortuna.ical4j.data.CalendarBuilder;
import net.fortuna.ical4j.data.ParserException;
import net.fortuna.ical4j.model.Calendar;
import net.fortuna.ical4j.model.Component;
import net.fortuna.ical4j.model.component.CalendarComponent;
import net.fortuna.ical4j.model.component.VEvent;
import net.fortuna.ical4j.model.property.Method;

public class ICalParser
{
  private static final org.apache.log4j.Logger log = org.apache.log4j.Logger.getLogger(ICalParser.class);

  //------------------------------------------------------------------------------------------------------------
  // Static part
  //------------------------------------------------------------------------------------------------------------

  public static ICalParser parseAllFields()
  {
    final ICalParser parser = new ICalParser();
    parser.parseVEvent = new ArrayList<>(FULL_LIST);

    return parser;
  }

  //------------------------------------------------------------------------------------------------------------
  // None static part
  //------------------------------------------------------------------------------------------------------------

  private List<String> parseVEvent;
  private Calendar calendar;
  private List<VEvent> events;
  private List<TeamEventDO> extractedEvents;

  // TODO check if needed
  private PFUserDO user;
  private Locale locale;
  private TimeZone timeZone;
  private Method method;

  private ICalParser()
  {
    this.parseVEvent = new ArrayList<>();

    // set user, timezone, locale
    this.user = ThreadLocalUserContext.getUser();
    this.timeZone = ThreadLocalUserContext.getTimeZone();
    this.locale = ThreadLocalUserContext.getLocale();

    this.reset();
  }

  public void reset()
  {
    events = new ArrayList<>();
    this.extractedEvents = new ArrayList<>();
  }

  public boolean parse(final String iCalString)
  {
    return this.parse(new StringReader(iCalString));
  }

  public boolean parse(final InputStream iCalStream)
  {
    return this.parse(new InputStreamReader(iCalStream));
  }

  public boolean parse(final Reader iCalReader)
  {
    this.reset();
    final CalendarBuilder builder = new CalendarBuilder();

    try {
      // parse calendar
      this.calendar = builder.build(iCalReader);
    } catch (IOException | ParserException e) {
      log.error("An unknown error occurred while parsing an ICS file", e);
      return false;
    }
    return this.parse(this.calendar);
  }

  @Deprecated
  public boolean parse(final Calendar calendar)
  {
    this.method = this.calendar.getMethod();

    final List<CalendarComponent> list = calendar.getComponents(Component.VEVENT);
    if (list == null || list.size() == 0) {
      // no events found
      return true;
    }

    for (final Component c : list) {
      final VEvent vEvent = (VEvent) c;

      // skip setup event!
      if (vEvent.getSummary() != null && StringUtils.equals(vEvent.getSummary().getValue(), TeamCalConfig.SETUP_EVENT)) {
        continue;
      }

      final TeamEventDO event = this.parse(vEvent);

      if (event != null) {
        this.events.add(vEvent);
        this.extractedEvents.add(event);
      }
    }

    // sorting events
    this.extractedEvents.sort((o1, o2) -> {
      final Date startDate1 = o1.getStartDate();
      final Date startDate2 = o2.getStartDate();
      if (startDate1 == null) {
        if (startDate2 == null) {
          return 0;
        }
        return -1;
      }
      return startDate1.compareTo(startDate2);
    });

    return true;
  }

  private TeamEventDO parse(final VEvent vEvent)
  {
    final ICalConverterStore store = ICalConverterStore.getInstance();

    // create vEvent
    final TeamEventDO event = new TeamEventDO();
    event.setCreator(user);

    for (String extract : this.parseVEvent) {
      VEventComponentConverter converter = store.getVEventConverter(extract);

      if (converter == null) {
        log.warn(String.format("No converter found for '%s', converter is skipped", extract));
        continue;
      }

      converter.fromVEvent(event, vEvent);
    }

    return event;
  }

  public List<TeamEventDO> getExtractedEvents()
  {
    return this.extractedEvents;
  }

  @Deprecated
  public List<VEvent> getVEvents()
  {
    return this.events;
  }

  public Method getMethod()
  {
    return this.method;
  }
}
