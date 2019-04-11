package org.projectforge.business.teamcal.event.diff;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.projectforge.business.teamcal.admin.model.TeamCalDO;
import org.projectforge.business.teamcal.event.model.TeamEventAttendeeDO;
import org.projectforge.business.teamcal.event.model.TeamEventDO;
import org.projectforge.framework.persistence.user.entities.PFUserDO;

import java.sql.Timestamp;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

/**
 * Created by sniemczyk on 20.06.17.
 */
public class TeamEventDiffTest
{
  private static TeamEventDO EVENT_1;
  private static TeamEventDO EVENT_2;
  private static TeamEventDO EVENT_3;
  private static TeamEventDO EVENT_4;
  private static TeamEventDO EVENT_5;
  private static TeamEventDO EVENT_6;

  @Test
  public void computeDiffAttendees1()
  {
    TeamEventDO newEvent = EVENT_1;
    TeamEventDO oldEvent = EVENT_1.clone();

    oldEvent.getAttendees().clear();

    final TeamEventDiff diff = TeamEventDiff.compute(newEvent, oldEvent, Collections.EMPTY_SET);

    Assertions.assertNotNull(diff);
    Assertions.assertEquals(TeamEventDiffType.ATTENDEES, diff.getDiffType());
    Assertions.assertTrue(diff.isDiff());

    Assertions.assertEquals(0, diff.getFieldDiffs().size());

    Assertions.assertEquals(2, diff.getAttendeesAdded().size());
    Assertions.assertEquals(0, diff.getAttendeesRemoved().size());
  }

  @Test
  public void computeDiffAttendees2()
  {
    TeamEventDO newEvent = EVENT_1.clone();
    TeamEventDO oldEvent = EVENT_1;

    newEvent.getAttendees().clear();

    final TeamEventDiff diff = TeamEventDiff.compute(newEvent, oldEvent, Collections.EMPTY_SET);

    Assertions.assertNotNull(diff);
    Assertions.assertEquals(TeamEventDiffType.ATTENDEES, diff.getDiffType());
    Assertions.assertTrue(diff.isDiff());

    Assertions.assertEquals(0, diff.getFieldDiffs().size());

    Assertions.assertEquals(0, diff.getAttendeesAdded().size());
    Assertions.assertEquals(2, diff.getAttendeesRemoved().size());
  }

  @Test
  public void computeDiffAttendees3()
  {
    TeamEventDO newEvent = EVENT_1.clone();
    TeamEventDO oldEvent = EVENT_1.clone();

    TeamEventAttendeeDO attendeeRemoved = (TeamEventAttendeeDO) newEvent.getAttendees().toArray()[0];
    TeamEventAttendeeDO attendeeNotChanged = (TeamEventAttendeeDO) newEvent.getAttendees().toArray()[1];
    newEvent.getAttendees().remove(attendeeRemoved);
    TeamEventAttendeeDO attendee3 = new TeamEventAttendeeDO();
    attendee3.setUrl("url3");
    newEvent.addAttendee(attendee3);
    TeamEventAttendeeDO attendee4 = new TeamEventAttendeeDO();
    attendee4.setUrl("url4");
    newEvent.addAttendee(attendee4);

    final TeamEventDiff diff = TeamEventDiff.compute(newEvent, oldEvent, Collections.EMPTY_SET);

    Assertions.assertNotNull(diff);
    Assertions.assertEquals(TeamEventDiffType.ATTENDEES, diff.getDiffType());
    Assertions.assertTrue(diff.isDiff());

    Assertions.assertEquals(0, diff.getFieldDiffs().size());

    Assertions.assertEquals(2, diff.getAttendeesAdded().size());
    Assertions.assertEquals(1, diff.getAttendeesRemoved().size());
    Assertions.assertEquals(1, diff.getAttendeesNotChanged().size());

    Assertions.assertTrue(diff.getAttendeesAdded().contains(attendee3));
    Assertions.assertTrue(diff.getAttendeesAdded().contains(attendee4));

    Assertions.assertTrue(diff.getAttendeesRemoved().contains(attendeeRemoved));
    Assertions.assertTrue(diff.getAttendeesNotChanged().contains(attendeeNotChanged));
  }

  @Test
  public void computeDiffFilter1()
  {
    TeamEventDO newEvent = EVENT_1;
    TeamEventDO oldEvent = EVENT_3;
    Set<TeamEventField> filter = new HashSet<>();
    filter.add(TeamEventField.SUBJECT);
    filter.add(TeamEventField.CALENDAR);

    final TeamEventDiff diff = TeamEventDiff.compute(newEvent, oldEvent, filter);

    Assertions.assertNotNull(diff);
    Assertions.assertEquals(TeamEventDiffType.UPDATED, diff.getDiffType());
    Assertions.assertTrue(diff.isDiff());

    Assertions.assertEquals(2, diff.getFieldDiffs().size());

    TeamEventFieldDiff<String> subject = diff.getFieldDiff(TeamEventField.SUBJECT);
    Assertions.assertNotNull(subject);
    Assertions.assertEquals(TeamEventFieldDiffType.UPDATED, subject.getType());
    Assertions.assertTrue(subject.isDiff());

    TeamEventFieldDiff<TeamCalDO> calendar = diff.getFieldDiff(TeamEventField.CALENDAR);
    Assertions.assertNotNull(calendar);
    Assertions.assertEquals(TeamEventFieldDiffType.UPDATED, calendar.getType());
    Assertions.assertTrue(calendar.isDiff());

    TeamEventFieldDiff<PFUserDO> creator = diff.getFieldDiff(TeamEventField.CREATOR);
    Assertions.assertNull(creator);
  }

  @Test
  public void computeDiffFilter2()
  {
    TeamEventDO newEvent = EVENT_1;
    TeamEventDO oldEvent = EVENT_3;
    Set<TeamEventField> filter = new HashSet<>();
    filter.add(TeamEventField.RECURRENCE_UNTIL);

    final TeamEventDiff diff = TeamEventDiff.compute(newEvent, oldEvent, filter);

    Assertions.assertNotNull(diff);
    Assertions.assertEquals(TeamEventDiffType.NONE, diff.getDiffType());
    Assertions.assertFalse(diff.isDiff());

    Assertions.assertEquals(0, diff.getFieldDiffs().size());

    TeamEventFieldDiff<String> subject = diff.getFieldDiff(TeamEventField.SUBJECT);
    Assertions.assertNull(subject);

    TeamEventFieldDiff<TeamCalDO> calendar = diff.getFieldDiff(TeamEventField.CALENDAR);
    Assertions.assertNull(calendar);

    TeamEventFieldDiff<PFUserDO> creator = diff.getFieldDiff(TeamEventField.CREATOR);
    Assertions.assertNull(creator);
  }

  @Test
  public void computeDiff1()
  {
    TeamEventDO newEvent = EVENT_1;
    TeamEventDO oldEvent = EVENT_2;

    final TeamEventDiff diff = TeamEventDiff.compute(newEvent, oldEvent, Collections.EMPTY_SET);

    Assertions.assertNotNull(diff);
    Assertions.assertEquals(TeamEventDiffType.UPDATED, diff.getDiffType());
    Assertions.assertTrue(diff.isDiff());

    Assertions.assertEquals(1, diff.getFieldDiffs().size());

    TeamEventFieldDiff<String> fieldDiff = diff.getFieldDiff(TeamEventField.SUBJECT);
    Assertions.assertNotNull(fieldDiff);
    Assertions.assertEquals(TeamEventFieldDiffType.UPDATED, fieldDiff.getType());
    Assertions.assertTrue(fieldDiff.isDiff());
  }

  @Test
  public void computeDiff2()
  {
    TeamEventDO newEvent = EVENT_1;
    TeamEventDO oldEvent = EVENT_3;

    final TeamEventDiff diff = TeamEventDiff.compute(newEvent, oldEvent, Collections.EMPTY_SET);

    Assertions.assertNotNull(diff);
    Assertions.assertEquals(TeamEventDiffType.UPDATED, diff.getDiffType());
    Assertions.assertTrue(diff.isDiff());

    Assertions.assertEquals(3, diff.getFieldDiffs().size());

    TeamEventFieldDiff<String> subject = diff.getFieldDiff(TeamEventField.SUBJECT);
    Assertions.assertNotNull(subject);
    Assertions.assertEquals(TeamEventFieldDiffType.UPDATED, subject.getType());
    Assertions.assertTrue(subject.isDiff());

    TeamEventFieldDiff<TeamCalDO> calendar = diff.getFieldDiff(TeamEventField.CALENDAR);
    Assertions.assertNotNull(calendar);
    Assertions.assertEquals(TeamEventFieldDiffType.UPDATED, calendar.getType());
    Assertions.assertTrue(calendar.isDiff());

    TeamEventFieldDiff<PFUserDO> creator = diff.getFieldDiff(TeamEventField.CREATOR);
    Assertions.assertNotNull(creator);
    Assertions.assertEquals(TeamEventFieldDiffType.UPDATED, creator.getType());
    Assertions.assertTrue(creator.isDiff());
  }

  @Test
  public void computeDiff3()
  {
    TeamEventDO newEvent = EVENT_1;
    TeamEventDO oldEvent = EVENT_5;

    final TeamEventDiff diff = TeamEventDiff.compute(newEvent, oldEvent, Collections.EMPTY_SET);

    Assertions.assertNotNull(diff);
    Assertions.assertEquals(TeamEventDiffType.UPDATED, diff.getDiffType());
    Assertions.assertTrue(diff.isDiff());

    Assertions.assertEquals(2, diff.getFieldDiffs().size());

    TeamEventFieldDiff<String> note = diff.getFieldDiff(TeamEventField.NOTE);
    Assertions.assertNotNull(note);
    Assertions.assertEquals(TeamEventFieldDiffType.REMOVED, note.getType());
    Assertions.assertTrue(note.isDiff());

    TeamEventFieldDiff<String> organizer = diff.getFieldDiff(TeamEventField.ORGANIZER);
    Assertions.assertNotNull(organizer);
    Assertions.assertEquals(TeamEventFieldDiffType.REMOVED, organizer.getType());
    Assertions.assertTrue(organizer.isDiff());
  }

  @Test
  public void computeDiff4()
  {
    TeamEventDO newEvent = EVENT_1;
    TeamEventDO oldEvent = EVENT_6;

    final TeamEventDiff diff = TeamEventDiff.compute(newEvent, oldEvent, Collections.EMPTY_SET);

    Assertions.assertNotNull(diff);
    Assertions.assertEquals(TeamEventDiffType.UPDATED, diff.getDiffType());
    Assertions.assertTrue(diff.isDiff());

    Assertions.assertEquals(2, diff.getFieldDiffs().size());

    TeamEventFieldDiff<String> subject = diff.getFieldDiff(TeamEventField.SUBJECT);
    Assertions.assertNotNull(subject);
    Assertions.assertEquals(TeamEventFieldDiffType.SET, subject.getType());
    Assertions.assertTrue(subject.isDiff());

    TeamEventFieldDiff<String> location = diff.getFieldDiff(TeamEventField.LOCATION);
    Assertions.assertNotNull(location);
    Assertions.assertEquals(TeamEventFieldDiffType.SET, location.getType());
    Assertions.assertTrue(location.isDiff());
  }

  @Test
  public void computeDiffNoDiff1()
  {
    TeamEventDO newEvent = new TeamEventDO();
    TeamEventDO oldEvent = new TeamEventDO();

    final TeamEventDiff diff = TeamEventDiff.compute(newEvent, oldEvent, Collections.EMPTY_SET);

    Assertions.assertNotNull(diff);
    Assertions.assertEquals(TeamEventDiffType.NONE, diff.getDiffType());
    Assertions.assertFalse(diff.isDiff());
  }

  @Test
  public void computeDiffNoDiff2()
  {
    TeamEventDO newEvent = EVENT_1;
    TeamEventDO oldEvent = EVENT_1;

    final TeamEventDiff diff = TeamEventDiff.compute(newEvent, oldEvent, Collections.EMPTY_SET);

    Assertions.assertNotNull(diff);
    Assertions.assertEquals(TeamEventDiffType.NONE, diff.getDiffType());
    Assertions.assertFalse(diff.isDiff());
  }

  @Test
  public void computeDiffNew()
  {
    TeamEventDO newEvent = EVENT_1;

    final TeamEventDiff diff = TeamEventDiff.compute(newEvent, null, Collections.EMPTY_SET);

    Assertions.assertNotNull(diff);
    Assertions.assertEquals(TeamEventDiffType.NEW, diff.getDiffType());
    Assertions.assertTrue(diff.isDiff());
  }

  @Test
  public void computeDiffDeleted()
  {
    TeamEventDO newEvent = EVENT_1.clone();
    TeamEventDO oldEvent = EVENT_1.clone();

    newEvent.setDeleted(true);
    oldEvent.setDeleted(false);

    final TeamEventDiff diff = TeamEventDiff.compute(newEvent, oldEvent, Collections.EMPTY_SET);

    Assertions.assertNotNull(diff);
    Assertions.assertEquals(TeamEventDiffType.DELETED, diff.getDiffType());
    Assertions.assertTrue(diff.isDiff());
  }

  @Test
  public void computeDiffRestored()
  {
    TeamEventDO newEvent = EVENT_1.clone();
    TeamEventDO oldEvent = EVENT_1.clone();

    newEvent.setDeleted(false);
    oldEvent.setDeleted(true);

    final TeamEventDiff diff = TeamEventDiff.compute(newEvent, oldEvent, Collections.EMPTY_SET);

    Assertions.assertNotNull(diff);
    Assertions.assertEquals(TeamEventDiffType.RESTORED, diff.getDiffType());
    Assertions.assertTrue(diff.isDiff());
  }

  @BeforeAll
  public static void init()
  {
    PFUserDO user1 = new PFUserDO();
    user1.setUsername("Name 1");
    PFUserDO user2 = new PFUserDO();
    user1.setUsername("Name 2");

    TeamCalDO calendar1 = new TeamCalDO();
    calendar1.setId(1);
    calendar1.setTitle("Title 1");
    TeamCalDO calendar2 = new TeamCalDO();
    calendar2.setId(2);
    calendar1.setTitle("Title 2");

    EVENT_1 = new TeamEventDO();

    EVENT_1.setCreator(user1);
    EVENT_1.setCalendar(calendar1);
    EVENT_1.setStartDate(new Timestamp(System.currentTimeMillis()));
    EVENT_1.setEndDate(new Timestamp(System.currentTimeMillis()));
    EVENT_1.setAllDay(false);
    EVENT_1.setSubject("Subject 1");
    EVENT_1.setLocation("Location 1");
    EVENT_1.setRecurrenceExDate(null);
    EVENT_1.setRecurrenceDate("");
    EVENT_1.setRecurrenceReferenceId("");
    EVENT_1.setLastEmail(null);
    EVENT_1.setSequence(null);
    EVENT_1.setReminderDuration(null);
    EVENT_1.setReminderDurationUnit(null);
    EVENT_1.setReminderActionType(null);

    TeamEventAttendeeDO attendee1 = new TeamEventAttendeeDO();
    attendee1.setUrl("url1");
    EVENT_1.addAttendee(attendee1);
    TeamEventAttendeeDO attendee2 = new TeamEventAttendeeDO();
    attendee2.setUrl("url2");
    EVENT_1.addAttendee(attendee2);

    EVENT_2 = EVENT_1.clone();
    EVENT_2.setSubject("Subject 2");

    EVENT_3 = EVENT_1.clone();
    EVENT_3.setCreator(user2);
    EVENT_3.setCalendar(calendar2);
    EVENT_3.setSubject("Subject 2");

    EVENT_4 = EVENT_1.clone();
    EVENT_4.setReminderDuration(2);

    EVENT_5 = EVENT_1.clone();
    EVENT_5.setNote("Note");
    EVENT_5.setOrganizer("organizer");

    EVENT_6 = EVENT_1.clone();
    EVENT_6.setLocation(null);
    EVENT_6.setSubject(null);
  }
}
