package org.projectforge.business.teamcal.event.diff;

import java.sql.Timestamp;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;
import java.util.TimeZone;

import org.junit.Assert;
import org.junit.BeforeClass;
import org.junit.Test;
import org.projectforge.business.teamcal.admin.model.TeamCalDO;
import org.projectforge.business.teamcal.event.model.TeamEventAttendeeDO;
import org.projectforge.business.teamcal.event.model.TeamEventDO;
import org.projectforge.framework.persistence.user.entities.PFUserDO;

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

    Assert.assertNotNull(diff);
    Assert.assertEquals(TeamEventDiffType.UPDATED, diff.getDiffType());
    Assert.assertTrue(diff.isDiff());

    Assert.assertEquals(0, diff.getFieldDiffs().size());

    Assert.assertEquals(2, diff.getAttendeesAdded().size());
    Assert.assertEquals(0, diff.getAttendeesRemove().size());
  }

  @Test
  public void computeDiffAttendees2()
  {
    TeamEventDO newEvent = EVENT_1.clone();
    TeamEventDO oldEvent = EVENT_1;

    newEvent.getAttendees().clear();

    final TeamEventDiff diff = TeamEventDiff.compute(newEvent, oldEvent, Collections.EMPTY_SET);

    Assert.assertNotNull(diff);
    Assert.assertEquals(TeamEventDiffType.UPDATED, diff.getDiffType());
    Assert.assertTrue(diff.isDiff());

    Assert.assertEquals(0, diff.getFieldDiffs().size());

    Assert.assertEquals(0, diff.getAttendeesAdded().size());
    Assert.assertEquals(2, diff.getAttendeesRemove().size());
  }

  @Test
  public void computeDiffAttendees3()
  {
    TeamEventDO newEvent = EVENT_1.clone();
    TeamEventDO oldEvent = EVENT_1.clone();

    TeamEventAttendeeDO attendeeRemoved = (TeamEventAttendeeDO) newEvent.getAttendees().toArray()[0];
    newEvent.getAttendees().remove(attendeeRemoved);
    TeamEventAttendeeDO attendee3 = new TeamEventAttendeeDO();
    attendee3.setUrl("url3");
    newEvent.addAttendee(attendee3);
    TeamEventAttendeeDO attendee4 = new TeamEventAttendeeDO();
    attendee4.setUrl("url4");
    newEvent.addAttendee(attendee4);

    final TeamEventDiff diff = TeamEventDiff.compute(newEvent, oldEvent, Collections.EMPTY_SET);

    Assert.assertNotNull(diff);
    Assert.assertEquals(TeamEventDiffType.UPDATED, diff.getDiffType());
    Assert.assertTrue(diff.isDiff());

    Assert.assertEquals(0, diff.getFieldDiffs().size());

    Assert.assertEquals(2, diff.getAttendeesAdded().size());
    Assert.assertEquals(1, diff.getAttendeesRemove().size());

    Assert.assertTrue(diff.getAttendeesAdded().contains(attendee3));
    Assert.assertTrue(diff.getAttendeesAdded().contains(attendee4));

    Assert.assertTrue(diff.getAttendeesRemove().contains(attendeeRemoved));
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

    Assert.assertNotNull(diff);
    Assert.assertEquals(TeamEventDiffType.UPDATED, diff.getDiffType());
    Assert.assertTrue(diff.isDiff());

    Assert.assertEquals(2, diff.getFieldDiffs().size());

    TeamEventFieldDiff<String> subject = diff.getFieldDiff(TeamEventField.SUBJECT);
    Assert.assertNotNull(subject);
    Assert.assertEquals(TeamEventFieldDiffType.UPDATED, subject.getType());
    Assert.assertTrue(subject.isDiff());

    TeamEventFieldDiff<TeamCalDO> calendar = diff.getFieldDiff(TeamEventField.CALENDAR);
    Assert.assertNotNull(calendar);
    Assert.assertEquals(TeamEventFieldDiffType.UPDATED, calendar.getType());
    Assert.assertTrue(calendar.isDiff());

    TeamEventFieldDiff<PFUserDO> creator = diff.getFieldDiff(TeamEventField.CREATOR);
    Assert.assertNull(creator);
  }

  @Test
  public void computeDiffFilter2()
  {
    TeamEventDO newEvent = EVENT_1;
    TeamEventDO oldEvent = EVENT_3;
    Set<TeamEventField> filter = new HashSet<>();
    filter.add(TeamEventField.RECURRENCE_UNTIL);

    final TeamEventDiff diff = TeamEventDiff.compute(newEvent, oldEvent, filter);

    Assert.assertNotNull(diff);
    Assert.assertEquals(TeamEventDiffType.NONE, diff.getDiffType());
    Assert.assertFalse(diff.isDiff());

    Assert.assertEquals(0, diff.getFieldDiffs().size());

    TeamEventFieldDiff<String> subject = diff.getFieldDiff(TeamEventField.SUBJECT);
    Assert.assertNull(subject);

    TeamEventFieldDiff<TeamCalDO> calendar = diff.getFieldDiff(TeamEventField.CALENDAR);
    Assert.assertNull(calendar);

    TeamEventFieldDiff<PFUserDO> creator = diff.getFieldDiff(TeamEventField.CREATOR);
    Assert.assertNull(creator);
  }

  @Test
  public void computeDiff1()
  {
    TeamEventDO newEvent = EVENT_1;
    TeamEventDO oldEvent = EVENT_2;

    final TeamEventDiff diff = TeamEventDiff.compute(newEvent, oldEvent, Collections.EMPTY_SET);

    Assert.assertNotNull(diff);
    Assert.assertEquals(TeamEventDiffType.UPDATED, diff.getDiffType());
    Assert.assertTrue(diff.isDiff());

    Assert.assertEquals(1, diff.getFieldDiffs().size());

    TeamEventFieldDiff<String> fieldDiff = diff.getFieldDiff(TeamEventField.SUBJECT);
    Assert.assertNotNull(fieldDiff);
    Assert.assertEquals(TeamEventFieldDiffType.UPDATED, fieldDiff.getType());
    Assert.assertTrue(fieldDiff.isDiff());
  }

  @Test
  public void computeDiff2()
  {
    TeamEventDO newEvent = EVENT_1;
    TeamEventDO oldEvent = EVENT_3;

    final TeamEventDiff diff = TeamEventDiff.compute(newEvent, oldEvent, Collections.EMPTY_SET);

    Assert.assertNotNull(diff);
    Assert.assertEquals(TeamEventDiffType.UPDATED, diff.getDiffType());
    Assert.assertTrue(diff.isDiff());

    Assert.assertEquals(3, diff.getFieldDiffs().size());

    TeamEventFieldDiff<String> subject = diff.getFieldDiff(TeamEventField.SUBJECT);
    Assert.assertNotNull(subject);
    Assert.assertEquals(TeamEventFieldDiffType.UPDATED, subject.getType());
    Assert.assertTrue(subject.isDiff());

    TeamEventFieldDiff<TeamCalDO> calendar = diff.getFieldDiff(TeamEventField.CALENDAR);
    Assert.assertNotNull(calendar);
    Assert.assertEquals(TeamEventFieldDiffType.UPDATED, calendar.getType());
    Assert.assertTrue(calendar.isDiff());

    TeamEventFieldDiff<PFUserDO> creator = diff.getFieldDiff(TeamEventField.CREATOR);
    Assert.assertNotNull(creator);
    Assert.assertEquals(TeamEventFieldDiffType.UPDATED, creator.getType());
    Assert.assertTrue(creator.isDiff());
  }

  @Test
  public void computeDiff3()
  {
    TeamEventDO newEvent = EVENT_1;
    TeamEventDO oldEvent = EVENT_5;

    final TeamEventDiff diff = TeamEventDiff.compute(newEvent, oldEvent, Collections.EMPTY_SET);

    Assert.assertNotNull(diff);
    Assert.assertEquals(TeamEventDiffType.UPDATED, diff.getDiffType());
    Assert.assertTrue(diff.isDiff());

    Assert.assertEquals(2, diff.getFieldDiffs().size());

    TeamEventFieldDiff<String> note = diff.getFieldDiff(TeamEventField.NOTE);
    Assert.assertNotNull(note);
    Assert.assertEquals(TeamEventFieldDiffType.REMOVED, note.getType());
    Assert.assertTrue(note.isDiff());

    TeamEventFieldDiff<String> organizer = diff.getFieldDiff(TeamEventField.ORGANIZER);
    Assert.assertNotNull(organizer);
    Assert.assertEquals(TeamEventFieldDiffType.REMOVED, organizer.getType());
    Assert.assertTrue(organizer.isDiff());
  }

  @Test
  public void computeDiff4()
  {
    TeamEventDO newEvent = EVENT_1;
    TeamEventDO oldEvent = EVENT_6;

    final TeamEventDiff diff = TeamEventDiff.compute(newEvent, oldEvent, Collections.EMPTY_SET);

    Assert.assertNotNull(diff);
    Assert.assertEquals(TeamEventDiffType.UPDATED, diff.getDiffType());
    Assert.assertTrue(diff.isDiff());

    Assert.assertEquals(2, diff.getFieldDiffs().size());

    TeamEventFieldDiff<String> subject = diff.getFieldDiff(TeamEventField.SUBJECT);
    Assert.assertNotNull(subject);
    Assert.assertEquals(TeamEventFieldDiffType.SET, subject.getType());
    Assert.assertTrue(subject.isDiff());

    TeamEventFieldDiff<String> location = diff.getFieldDiff(TeamEventField.LOCATION);
    Assert.assertNotNull(location);
    Assert.assertEquals(TeamEventFieldDiffType.SET, location.getType());
    Assert.assertTrue(location.isDiff());
  }

  @Test
  public void computeDiffNoDiff1()
  {
    TeamEventDO newEvent = new TeamEventDO();
    TeamEventDO oldEvent = new TeamEventDO();

    final TeamEventDiff diff = TeamEventDiff.compute(newEvent, oldEvent, Collections.EMPTY_SET);

    Assert.assertNotNull(diff);
    Assert.assertEquals(TeamEventDiffType.NONE, diff.getDiffType());
    Assert.assertFalse(diff.isDiff());
  }

  @Test
  public void computeDiffNoDiff2()
  {
    TeamEventDO newEvent = EVENT_1;
    TeamEventDO oldEvent = EVENT_1;

    final TeamEventDiff diff = TeamEventDiff.compute(newEvent, oldEvent, Collections.EMPTY_SET);

    Assert.assertNotNull(diff);
    Assert.assertEquals(TeamEventDiffType.NONE, diff.getDiffType());
    Assert.assertFalse(diff.isDiff());
  }

  @Test
  public void computeDiffNew()
  {
    TeamEventDO newEvent = EVENT_1;

    final TeamEventDiff diff = TeamEventDiff.compute(newEvent, null, Collections.EMPTY_SET);

    Assert.assertNotNull(diff);
    Assert.assertEquals(TeamEventDiffType.NEW, diff.getDiffType());
    Assert.assertTrue(diff.isDiff());
  }

  @Test
  public void computeDiffDeleted()
  {
    TeamEventDO newEvent = EVENT_1.clone();
    TeamEventDO oldEvent = EVENT_1.clone();

    newEvent.setDeleted(true);
    oldEvent.setDeleted(false);

    final TeamEventDiff diff = TeamEventDiff.compute(newEvent, oldEvent, Collections.EMPTY_SET);

    Assert.assertNotNull(diff);
    Assert.assertEquals(TeamEventDiffType.DELETED, diff.getDiffType());
    Assert.assertTrue(diff.isDiff());
  }

  @Test
  public void computeDiffRestored()
  {
    TeamEventDO newEvent = EVENT_1.clone();
    TeamEventDO oldEvent = EVENT_1.clone();

    newEvent.setDeleted(false);
    oldEvent.setDeleted(true);

    final TeamEventDiff diff = TeamEventDiff.compute(newEvent, oldEvent, Collections.EMPTY_SET);

    Assert.assertNotNull(diff);
    Assert.assertEquals(TeamEventDiffType.RESTORED, diff.getDiffType());
    Assert.assertTrue(diff.isDiff());
  }

  @BeforeClass
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
    EVENT_1.setTimeZone(TimeZone.getTimeZone("UTC"));

    EVENT_1.setCreator(user1);
    EVENT_1.setCalendar(calendar1);
    EVENT_1.setStartDate(new Timestamp(System.currentTimeMillis()));
    EVENT_1.setEndDate(new Timestamp(System.currentTimeMillis()));
    EVENT_1.setAllDay(false);
    EVENT_1.setSubject("Subject 1");
    EVENT_1.setLocation("Location 1");
    EVENT_1.setRecurrenceExDate(null);
    EVENT_1.setRecurrenceRule("");
    EVENT_1.setRecurrenceDate("");
    EVENT_1.setRecurrenceReferenceId("");
    EVENT_1.setRecurrenceUntil(null);
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
