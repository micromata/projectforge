package org.projectforge.caldav.rest;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import org.apache.commons.codec.binary.Base64;
import org.projectforge.caldav.cache.UserCache;
import org.projectforge.caldav.model.Calendar;
import org.projectforge.caldav.model.Meeting;
import org.projectforge.caldav.model.User;
import org.projectforge.model.rest.CalendarEventObject;
import org.projectforge.model.rest.CalendarObject;
import org.projectforge.model.rest.RestPaths;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.util.UriComponentsBuilder;

import com.fasterxml.jackson.databind.ObjectMapper;

@Service
public class CalendarRest
{
  @Autowired
  private UserCache userCache;

  @Autowired
  private RestTemplate restTemplate;

  @Value("${projectforge.server.address}")
  private String projectforgeServerAddress;

  @Value("${projectforge.server.port}")
  private String projectforgeServerPort;

  private static Logger log = LoggerFactory.getLogger(CalendarRest.class);

  public List<Calendar> getCalendarList(User user)
  {
    List<Calendar> result = new ArrayList<>();
    try {
      String url = projectforgeServerAddress + ":" + projectforgeServerPort + RestPaths.buildListPath(RestPaths.TEAMCAL);
      HttpHeaders headers = new HttpHeaders();
      headers.set("authenticationUserId", String.valueOf(user.getPk()));
      headers.set("authenticationToken", user.getAuthenticationToken());
      headers.set("Accept", MediaType.APPLICATION_JSON_VALUE);
      UriComponentsBuilder builder = UriComponentsBuilder.fromHttpUrl(url)
          .queryParam("fullAccess", true);
      HttpEntity<?> entity = new HttpEntity<>(headers);
      HttpEntity<CalendarObject[]> response = restTemplate.exchange(builder.build().encode().toUri(), HttpMethod.GET, entity, CalendarObject[].class);
      CalendarObject[] calendarArray = response.getBody();
      log.info("Result of rest call (" + RestPaths.TEAMCAL + "): " + calendarArray);
      result = convertRestResponse(user, calendarArray);
    } catch (HttpClientErrorException e) {
      if (e.getStatusCode() == HttpStatus.UNAUTHORIZED) {
        // do not log the exception message if just unauthorized
        log.warn("Unauthorized to access calenders for user '{}'", user.getUsername());
      } else {
        log.error("Exception while getting calendars for user: " + user.getUsername(), e);
      }
    } catch (Exception e) {
      log.error("Exception while getting calendars for user: " + user.getUsername(), e);
    }
    return result;
  }

  public List<Meeting> getCalendarEvents(Calendar cal)
  {
    List<Meeting> result = new ArrayList<>();
    try {
      String url = projectforgeServerAddress + ":" + projectforgeServerPort + RestPaths.buildPath(RestPaths.TEAMEVENTS);
      HttpHeaders headers = new HttpHeaders();
      headers.set("Accept", MediaType.APPLICATION_JSON_VALUE);
      headers.set("authenticationUserId", String.valueOf(cal.getUser().getPk()));
      headers.set("authenticationToken", cal.getUser().getAuthenticationToken());
      UriComponentsBuilder builder = UriComponentsBuilder.fromHttpUrl(url)
          .queryParam("calendarIds", cal.getId());
      HttpEntity<?> entity = new HttpEntity<>(headers);
      HttpEntity<CalendarEventObject[]> response = restTemplate.exchange(builder.build().encode().toUri(), HttpMethod.GET, entity, CalendarEventObject[].class);
      CalendarEventObject[] calendarEventArray = response.getBody();
      log.info("Result of rest call (" + RestPaths.TEAMEVENTS + ") (Size: " + calendarEventArray.length + ") : " + calendarEventArray);
      result = convertRestResponse(cal, calendarEventArray);
    } catch (Exception e) {
      log.error("Exception while getting calendar events for calendar: " + cal.getName(), e);
    }
    return result;
  }

  public Meeting saveCalendarEvent(Meeting meeting)
  {
    return sendCalendarEvent(meeting, RestPaths.buildPath(RestPaths.TEAMEVENTS, RestPaths.SAVE));
  }

  public Meeting updateCalendarEvent(Meeting meeting)
  {
    return sendCalendarEvent(meeting, RestPaths.buildPath(RestPaths.TEAMEVENTS, RestPaths.UPDATE));
  }

  private Meeting sendCalendarEvent(Meeting meeting, String path)
  {
    try {
      CalendarEventObject request = convertRestRequest(meeting);
      ObjectMapper mapper = new ObjectMapper();
      final String json = mapper.writeValueAsString(request);
      String url = projectforgeServerAddress + ":" + projectforgeServerPort + path;
      HttpHeaders headers = new HttpHeaders();
      headers.set("Accept", MediaType.APPLICATION_JSON_VALUE);
      headers.setContentType(MediaType.APPLICATION_JSON);
      headers.set("authenticationUserId", String.valueOf(meeting.getCalendar().getUser().getPk()));
      headers.set("authenticationToken", meeting.getCalendar().getUser().getAuthenticationToken());
      HttpEntity<?> entity = new HttpEntity<>(json, headers);
      UriComponentsBuilder builder = UriComponentsBuilder.fromHttpUrl(url);
      ResponseEntity<CalendarEventObject> response = restTemplate
          .exchange(builder.build().encode().toUri(), HttpMethod.PUT, entity, CalendarEventObject.class);
      CalendarEventObject calendarEvent = response.getBody();
      log.info("Result of rest call: " + calendarEvent);
      return convertRestResponse(meeting.getCalendar(), calendarEvent);
    } catch (Exception e) {
      log.error("Exception while creating calendar event: " + meeting.getName(), e);
    }
    return null;
  }

  public void deleteCalendarEvent(Meeting meeting)
  {
    try {
      CalendarEventObject request = convertRestRequest(meeting);
      ObjectMapper mapper = new ObjectMapper();
      final String json = mapper.writeValueAsString(request);
      String url = projectforgeServerAddress + ":" + projectforgeServerPort + RestPaths.buildPath(RestPaths.TEAMEVENTS);
      HttpHeaders headers = new HttpHeaders();
      headers.set("Accept", MediaType.APPLICATION_JSON_VALUE);
      headers.setContentType(MediaType.APPLICATION_JSON);
      headers.set("authenticationUserId", String.valueOf(meeting.getCalendar().getUser().getPk()));
      headers.set("authenticationToken", meeting.getCalendar().getUser().getAuthenticationToken());
      HttpEntity<?> entity = new HttpEntity<>(json, headers);
      UriComponentsBuilder builder = UriComponentsBuilder.fromHttpUrl(url);
      ResponseEntity<CalendarEventObject> response = restTemplate
          .exchange(builder.build().encode().toUri(), HttpMethod.DELETE, entity, CalendarEventObject.class);
      CalendarEventObject calendarEvent = response.getBody();
      log.info("Result of rest call: " + calendarEvent);
    } catch (Exception e) {
      log.error("Exception while creating calendar event: " + meeting.getName(), e);
    }
  }

  private List<Calendar> convertRestResponse(User user, CalendarObject[] calendarArray)
  {
    List<Calendar> result = new ArrayList<>();
    List<CalendarObject> calObjList = Arrays.asList(calendarArray);
    calObjList.forEach(calObj -> {
      result.add(new Calendar(user, calObj.getId(), calObj.getTitle()));
    });
    return result;
  }

  private List<Meeting> convertRestResponse(Calendar cal, CalendarEventObject[] calendarEventArray)
  {
    List<Meeting> result = new ArrayList<>();
    List<CalendarEventObject> calEventObjList = Arrays.asList(calendarEventArray);
    calEventObjList.forEach(calEventObj -> {
      result.add(convertRestResponse(cal, calEventObj));
    });
    return result;
  }

  private Meeting convertRestResponse(Calendar cal, CalendarEventObject calendarEvent)
  {
    Meeting result = new Meeting(cal);
    result.setUid(calendarEvent.getUid());
    result.setCreateDate(calendarEvent.getCreated());
    result.setModifiedDate(calendarEvent.getLastUpdate());
    result.setName(calendarEvent.getUid() + ".ics");
    result.setIcalData(Base64.decodeBase64(calendarEvent.getIcsData()));
    return result;
  }

  private CalendarEventObject convertRestRequest(Meeting m)
  {
    CalendarEventObject result = new CalendarEventObject();
    result.setUid(m.getUid());
    result.setCalendarId(m.getCalendar().getId());
    result.setIcsData(Base64.encodeBase64String(m.getIcalData()));
    result.setCreated(m.getCreateDate());
    result.setLastUpdate(m.getModifiedDate());
    return result;
  }
}
