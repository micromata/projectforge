/////////////////////////////////////////////////////////////////////////////
//
// Project ProjectForge Community Edition
//         www.projectforge.org
//
// Copyright (C) 2001-2014 Kai Reinhard (k.reinhard@micromata.de)
//
// ProjectForge is dual-licensed.
//
// This community edition is free software; you can redistribute it and/or
// modify it under the terms of the GNU General Public License as published
// by the Free Software Foundation; version 3 of the License.
//
// This community edition is distributed in the hope that it will be useful,
// but WITHOUT ANY WARRANTY; without even the implied warranty of
// MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU General
// Public License for more details.
//
// You should have received a copy of the GNU General Public License along
// with this program; if not, see http://www.gnu.org/licenses/.
//
/////////////////////////////////////////////////////////////////////////////

package org.projectforge.business.teamcal.event.diff;

import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Set;

import org.projectforge.business.teamcal.admin.model.TeamCalDO;
import org.projectforge.business.teamcal.event.model.ReminderActionType;
import org.projectforge.business.teamcal.event.model.ReminderDurationUnit;
import org.projectforge.business.teamcal.event.model.TeamEventAttendeeDO;
import org.projectforge.business.teamcal.event.model.TeamEventDO;
import org.projectforge.framework.persistence.user.entities.PFUserDO;

/**
 * @author Stefan Niemczyk (s.niemczyk@micromata.de)
 */
public class TeamEventDiff
{
  /**
   * Computes the diff between old and new state of an event.
   *
   * @param eventNewState
   * @param eventOldState
   * @param fieldFilter
   * @return the computed diff state
   */
  public static TeamEventDiff compute(final TeamEventDO eventNewState, final TeamEventDO eventOldState, final Set<TeamEventField> fieldFilter)
  {
    if (eventNewState == null) {
      return null;
    }

    TeamEventDiff diff = new TeamEventDiff();

    // old is null -> new event, no further diff
    if (eventOldState == null) {
      diff.type = TeamEventDiffType.NEW;
      return diff;
    }

    // event is deleted, no further diff
    if (eventNewState.isDeleted() && eventOldState.isDeleted() == false) {
      diff.type = TeamEventDiffType.DELETED;
      return diff;
    }

    // event is restored, no further diff
    if (eventNewState.isDeleted() == false && eventOldState.isDeleted()) {
      diff.type = TeamEventDiffType.RESTORED;
      return diff;
    }

    // ---------------------------------------------------------------------------------------------------
    // compute diff
    // ---------------------------------------------------------------------------------------------------

    // check fields
    diff.calendar = computeFieldDiff(diff, TeamEventField.CALENDAR, fieldFilter, eventNewState.getCalendar(), eventOldState.getCalendar());
    diff.creator = computeFieldDiff(diff, TeamEventField.CREATOR, fieldFilter, eventNewState.getCreator(), eventOldState.getCreator());
    diff.startDate = computeFieldDiff(diff, TeamEventField.START_DATE, fieldFilter, eventNewState.getStartDate(), eventOldState.getStartDate());
    diff.endDate = computeFieldDiff(diff, TeamEventField.END_DATE, fieldFilter, eventNewState.getEndDate(), eventOldState.getEndDate());
    diff.allDay = computeFieldDiff(diff, TeamEventField.ALL_DAY, fieldFilter, eventNewState.isAllDay(), eventOldState.isAllDay());
    diff.subject = computeFieldDiff(diff, TeamEventField.SUBJECT, fieldFilter, eventNewState.getSubject(), eventOldState.getSubject());
    diff.location = computeFieldDiff(diff, TeamEventField.LOCATION, fieldFilter, eventNewState.getLocation(), eventOldState.getLocation());

    diff.organizer = computeFieldDiff(diff, TeamEventField.ORGANIZER, fieldFilter, eventNewState.getOrganizer(), eventOldState.getOrganizer());
    diff.note = computeFieldDiff(diff, TeamEventField.NOTE, fieldFilter, eventNewState.getNote(), eventOldState.getNote());
    diff.lastEmail = computeFieldDiff(diff, TeamEventField.LAST_MAIL, fieldFilter, eventNewState.getLastEmail(), eventOldState.getLastEmail());

    diff.recurrenceExDate = computeFieldDiff(diff, TeamEventField.RECURRENCE_EX_DATES, fieldFilter, eventNewState.getRecurrenceExDate(),
        eventOldState.getRecurrenceExDate());
    diff.recurrenceRule = computeFieldDiff(diff, TeamEventField.RECURRENCE_RULE, fieldFilter, eventNewState.getRecurrenceRule(),
        eventOldState.getRecurrenceRule());
    diff.recurrenceReferenceDate = computeFieldDiff(diff, TeamEventField.RECURRENCE_REFERENCE_DATE, fieldFilter, eventNewState.getRecurrenceDate(),
        eventOldState.getRecurrenceDate());
    diff.recurrenceReferenceId = computeFieldDiff(diff, TeamEventField.RECURRENCE_REFERENCE_ID, fieldFilter, eventNewState.getRecurrenceReferenceId(),
        eventOldState.getRecurrenceReferenceId());
    diff.recurrenceUntil = computeFieldDiff(diff, TeamEventField.RECURRENCE_UNTIL, fieldFilter, eventNewState.getRecurrenceUntil(),
        eventOldState.getRecurrenceUntil());
    diff.sequence = computeFieldDiff(diff, TeamEventField.SEQUENCE, fieldFilter, eventNewState.getSequence(), eventOldState.getSequence());

    diff.reminderDuration = computeFieldDiff(diff, TeamEventField.REMINDER_DURATION, fieldFilter, eventNewState.getReminderDuration(),
        eventOldState.getReminderDuration());
    diff.reminderDurationType = computeFieldDiff(diff, TeamEventField.REMINDER_DURATION_TYPE, fieldFilter, eventNewState.getReminderDurationUnit(),
        eventOldState.getReminderDurationUnit());
    diff.reminderActionType = computeFieldDiff(diff, TeamEventField.REMINDER_ACTION_TYPE, fieldFilter, eventNewState.getReminderActionType(),
        eventOldState.getReminderActionType());

    // check attendees
    final Set<TeamEventAttendeeDO> attendeesNewState = eventNewState.getAttendees();
    final Set<TeamEventAttendeeDO> attendeesOldState = eventOldState.getAttendees();

    if (attendeesNewState != null && attendeesNewState.isEmpty() == false) {
      if (attendeesOldState == null || attendeesOldState.isEmpty()) {
        diff.attendeesAdded.addAll(attendeesNewState);
      } else {
        for (TeamEventAttendeeDO attendee : attendeesNewState) {
          if (attendeesOldState.contains(attendee) == false) {
            diff.attendeesAdded.add(attendee);
          }
        }
      }
    }

    if (attendeesOldState != null && attendeesOldState.isEmpty() == false) {
      if (attendeesNewState == null || attendeesNewState.isEmpty()) {
        diff.attendeesRemoved.addAll(attendeesOldState);
      } else {
        for (TeamEventAttendeeDO attendee : attendeesOldState) {
          if (attendeesNewState.contains(attendee) == false) {
            diff.attendeesRemoved.add(attendee);
          }
        }
      }
    }

    // check attachments
    //    if (this.attachments != null && this.attachments.isEmpty() == false) {
    //      clone.attachments = clone.ensureAttachments();
    //      for (final TeamEventAttachmentDO attachment : this.getAttachments()) {
    //        TeamEventAttachmentDO cloneAttachment = new TeamEventAttachmentDO();
    //        cloneAttachment.setFilename(attachment.getFilename());
    //        cloneAttachment.setContent(attachment.getContent());
    //        clone.addAttachment(cloneAttachment);
    //      }
    //    }

    if (diff.fieldDiffs.isEmpty() && diff.attendeesRemoved.isEmpty() && diff.attendeesAdded.isEmpty()) {
      diff.type = TeamEventDiffType.NONE;
    } else {
      diff.type = TeamEventDiffType.UPDATED;
    }

    return diff;
  }

  private static <F> TeamEventFieldDiff<F> computeFieldDiff(final TeamEventDiff diff, final TeamEventField field, final Set<TeamEventField> fieldFilter,
      final F newFieldValue, final F oldFieldValue)
  {
    if (fieldFilter.isEmpty() == false && fieldFilter.contains(field) == false) {
      return null;
    }

    final TeamEventFieldDiff<F> fieldDiff = new TeamEventFieldDiff<>(field, newFieldValue, oldFieldValue);
    diff.allFields.add(fieldDiff);

    if (fieldDiff.isDiff()) {
      diff.fieldDiffs.add(fieldDiff);
    }

    return fieldDiff;
  }

  // meta information
  private TeamEventDiffType type;
  private List<TeamEventFieldDiff> allFields;
  private List<TeamEventFieldDiff> fieldDiffs;

  // fields
  private TeamEventFieldDiff<TeamCalDO> calendar;
  private TeamEventFieldDiff<PFUserDO> creator;
  private TeamEventFieldDiff<Timestamp> startDate;
  private TeamEventFieldDiff<Timestamp> endDate;
  private TeamEventFieldDiff<Boolean> allDay;
  private TeamEventFieldDiff<String> subject;
  private TeamEventFieldDiff<String> location;
  private TeamEventFieldDiff<String> recurrenceExDate;
  private TeamEventFieldDiff<String> recurrenceRule;
  private TeamEventFieldDiff<String> recurrenceReferenceDate;
  private TeamEventFieldDiff<String> recurrenceReferenceId;
  private TeamEventFieldDiff<Date> recurrenceUntil;
  private TeamEventFieldDiff<String> organizer;
  private TeamEventFieldDiff<String> note;
  private TeamEventFieldDiff<Timestamp> lastEmail;
  private TeamEventFieldDiff<Integer> sequence;
  private TeamEventFieldDiff<Integer> reminderDuration;
  private TeamEventFieldDiff<ReminderDurationUnit> reminderDurationType;
  private TeamEventFieldDiff<ReminderActionType> reminderActionType;

  // attendees
  private List<TeamEventAttendeeDO> attendeesAdded;
  private List<TeamEventAttendeeDO> attendeesRemoved;

  /**
   * Use compute method @see org.projectforge.business.teamcal.event.TeamEventDiff#compute(TeamEventDO, TeamEventDO)
   */
  private TeamEventDiff()
  {
    // do not call from outside
    this.allFields = new ArrayList<>();
    this.fieldDiffs = new ArrayList<>();
    this.attendeesAdded = new ArrayList<>();
    this.attendeesRemoved = new ArrayList<>();
  }

  public <F> TeamEventFieldDiff<F> getFieldDiff(final TeamEventField field)
  {
    for (TeamEventFieldDiff fieldDiff : this.allFields) {
      if (fieldDiff.getTeamEventField() == field) {
        return fieldDiff;
      }
    }

    return null;
  }

  public TeamEventDiffType getDiffType()
  {
    return this.type;
  }

  public boolean isDiff()
  {
    return this.type != TeamEventDiffType.NONE;
  }

  public List<TeamEventFieldDiff> getFieldDiffs()
  {
    return this.fieldDiffs;
  }

  public List<TeamEventAttendeeDO> getAttendeesRemove()
  {
    return this.attendeesRemoved;
  }

  public List<TeamEventAttendeeDO> getAttendeesAdded()
  {
    return this.attendeesAdded;
  }

}
