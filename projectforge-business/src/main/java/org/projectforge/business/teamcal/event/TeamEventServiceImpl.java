/////////////////////////////////////////////////////////////////////////////
//
// Project ProjectForge Community Edition
//         www.projectforge.org
//
// Copyright (C) 2001-2026 Micromata GmbH, Germany (www.micromata.com)
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

package org.projectforge.business.teamcal.event;

import org.apache.commons.lang3.StringUtils;
import org.projectforge.business.address.AddressDO;
import org.projectforge.business.address.AddressDao;
import org.projectforge.business.calendar.event.model.ICalendarEvent;
import org.projectforge.business.configuration.ConfigurationService;
import org.projectforge.business.configuration.DomainService;
import org.projectforge.business.teamcal.admin.model.TeamCalDO;
import org.projectforge.business.teamcal.event.diff.TeamEventField;
import org.projectforge.business.teamcal.event.ical.ICalHandler;
import org.projectforge.business.teamcal.event.model.TeamEventAttendeeDO;
import org.projectforge.business.teamcal.event.model.TeamEventAttendeeDao;
import org.projectforge.business.teamcal.event.model.TeamEventAttendeeStatus;
import org.projectforge.business.teamcal.event.model.TeamEventDO;
import org.projectforge.business.teamcal.service.CryptService;
import org.projectforge.business.user.service.UserService;
import org.projectforge.framework.persistence.jpa.PfPersistenceService;
import org.projectforge.framework.persistence.user.entities.PFUserDO;
import org.projectforge.mail.SendMail;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

@Service
public class TeamEventServiceImpl implements TeamEventService {
    private static final org.slf4j.Logger log = org.slf4j.LoggerFactory.getLogger(TeamEventServiceImpl.class);

    private enum EventMailType {
        NEW, DELETED, UPDATED
    }

    @Autowired
    private AddressDao addressDao;

    @Autowired
    private TeamEventAttendeeDao teamEventAttendeeDao;

    @Autowired
    private TeamEventDao teamEventDao;

    @Autowired
    private SendMail sendMail;

    @Autowired
    private UserService userService;

    @Autowired
    private CryptService cryptService;

    @Autowired
    private ConfigurationService configService;

    @Autowired
    private DomainService domainService;

    @Autowired
    private PfPersistenceService persistenceService;

    // Set TeamCalEvent fields used for computing a diff in order to send notification mails
    private static final Set<TeamEventField> TEAM_EVENT_FIELD_FILTER = Stream.of(
            TeamEventField.START_DATE,
            TeamEventField.END_DATE,
            TeamEventField.ALL_DAY,
            TeamEventField.LOCATION,
            TeamEventField.NOTE,
            TeamEventField.SUBJECT,
            TeamEventField.RECURRENCE_EX_DATES,
            TeamEventField.RECURRENCE_RULE,
            TeamEventField.RECURRENCE_REFERENCE_DATE
    ).collect(Collectors.toCollection(HashSet::new));

    @Override
    public List<TeamEventAttendeeDO> getAddressesAndUserAsAttendee() {
        List<TeamEventAttendeeDO> resultList = new ArrayList<>();
        List<AddressDO> allAddressList = addressDao.selectAllNotDeleted(false);
        List<PFUserDO> allUserList = userService.getAllActiveUsers();
        Set<Long> addedUserIds = new HashSet<>();
        for (AddressDO singleAddress : allAddressList) {
            if (!StringUtils.isBlank(singleAddress.getEmail())) {
                TeamEventAttendeeDO attendee = new TeamEventAttendeeDO();
                attendee.setStatus(TeamEventAttendeeStatus.IN_PROCESS);
                attendee.setAddress(singleAddress);
                PFUserDO userWithSameMail = allUserList.stream()
                        .filter(u -> u.getEmail() != null && u.getEmail().equalsIgnoreCase(singleAddress.getEmail())).findFirst().orElse(null);
                if (userWithSameMail != null && !addedUserIds.contains(userWithSameMail.getId())) {
                    attendee.setUser(userWithSameMail);
                    addedUserIds.add(userWithSameMail.getId());
                }
                resultList.add(attendee);
            }
        }
        for (PFUserDO u : allUserList) {
            if (!addedUserIds.contains(u.getId())) {
                TeamEventAttendeeDO attendee = new TeamEventAttendeeDO();
                attendee.setStatus(TeamEventAttendeeStatus.IN_PROCESS);
                attendee.setUser(u);
                resultList.add(attendee);
            }
        }
        return resultList;
    }

    @Override
    public TeamEventAttendeeDO getAttendee(Long attendeeId) {
        return teamEventAttendeeDao.find(attendeeId, false);
    }

    @Override
    public void assignAttendees(TeamEventDO data, Set<TeamEventAttendeeDO> itemsToAssign, Set<TeamEventAttendeeDO> itemsToUnassign) {
        persistenceService.runInTransaction(context -> {
                    for (TeamEventAttendeeDO assignAttendee : itemsToAssign) {
                        if (assignAttendee.getId() == null || assignAttendee.getId() < 0) {
                            assignAttendee.setId(null);
                            if (assignAttendee.getStatus() == null) {
                                assignAttendee.setStatus(TeamEventAttendeeStatus.NEEDS_ACTION);
                            }
                            data.addAttendee(assignAttendee);
                            teamEventAttendeeDao.insert(assignAttendee, false);
                        }
                    }

                    if (data.getAttendees() != null && itemsToUnassign != null && itemsToUnassign.size() > 0) {
                        data.getAttendees().removeAll(itemsToUnassign);
                        for (TeamEventAttendeeDO deleteAttendee : itemsToUnassign) {
                            teamEventAttendeeDao.markAsDeleted(deleteAttendee, false);
                        }
                    }

                    teamEventDao.update(data);
                    return null;
                }
        );
    }

    @Override
    public void updateAttendees(TeamEventDO event, Set<TeamEventAttendeeDO> attendeesOldState) {
        persistenceService.runInTransaction(context -> {
            final Set<TeamEventAttendeeDO> attendeesNewState = event.getAttendees();

            // new list is empty -> delete all
            if (attendeesNewState == null || attendeesNewState.isEmpty()) {
                if (attendeesOldState != null && !attendeesOldState.isEmpty()) {
                    for (TeamEventAttendeeDO attendee : attendeesOldState) {
                        teamEventAttendeeDao.markAsDeleted(attendee, false);
                    }
                }

                return null;
            }

            // old list is empty -> insert all
            if (attendeesOldState == null || attendeesOldState.isEmpty()) {
                for (TeamEventAttendeeDO attendee : attendeesNewState) {
                    // save new attendee
                    attendee.setId(null);
                    if (attendee.getStatus() == null) {
                        attendee.setStatus(TeamEventAttendeeStatus.NEEDS_ACTION);
                    }

                    teamEventAttendeeDao.insert(attendee, false);
                }

                return null;
            }

            // compute diff
            for (TeamEventAttendeeDO attendee : attendeesNewState) {
                boolean found = false;
                String eMail = attendee.getAddress() != null ? attendee.getAddress().getEmail() : attendee.getUrl();

                if (eMail == null) {
                    // should not occur
                    continue;
                }

                for (TeamEventAttendeeDO attendeeOld : attendeesOldState) {
                    String eMailOld = attendeeOld.getAddress() != null ? attendeeOld.getAddress().getEmail() : attendeeOld.getUrl();

                    if (eMail.equals(eMailOld)) {
                        found = true;

                        // update values
                        attendee.setId(attendeeOld.getId());
                        attendee.setComment(attendeeOld.getComment());
                        attendee.setCommentOfAttendee(attendeeOld.getCommentOfAttendee());
                        attendee.setLoginToken(attendeeOld.getLoginToken());
                        attendee.setNumber(attendeeOld.getNumber());
                        attendee.setAddress(attendeeOld.getAddress());
                        attendee.setUser(attendeeOld.getUser());

                        teamEventAttendeeDao.insert(attendee, false);

                        break;
                    }
                }

                if (!found) {
                    // save new attendee
                    attendee.setId(null);
                    if (attendee.getStatus() == null) {
                        attendee.setStatus(TeamEventAttendeeStatus.NEEDS_ACTION);
                    }
                    teamEventAttendeeDao.insert(attendee, false);
                }
            }

            for (TeamEventAttendeeDO attendee : attendeesOldState) {
                boolean found = false;
                String eMail = attendee.getAddress() != null ? attendee.getAddress().getEmail() : attendee.getUrl();

                for (TeamEventAttendeeDO attendeeNew : attendeesNewState) {
                    String eMailNew = attendeeNew.getAddress() != null ? attendeeNew.getAddress().getEmail() : attendeeNew.getUrl();

                    if (eMail.equals(eMailNew)) {
                        found = true;
                        break;
                    }
                }

                if (!found) {
                    // delete attendee
                    teamEventAttendeeDao.markAsDeleted(attendee, false);
                }
            }
            return null;
        });
    }

    @Override
    public TeamEventDO findByUid(Long calendarId, String reqEventUid, boolean excludeDeleted) {
        return teamEventDao.getByUid(calendarId, reqEventUid, excludeDeleted);
    }

    @Override
    public TeamEventAttendeeDO findByAttendeeId(Long attendeeId, boolean checkAccess) {
        TeamEventAttendeeDO result = null;
        result = teamEventAttendeeDao.find(attendeeId, checkAccess);
        return result;
    }

    @Override
    public void update(TeamEventDO event) {
        update(event, true);
    }

    @Override
    public void update(TeamEventDO event, boolean checkAccess) {
        teamEventDao.update(event, checkAccess);
    }

    @Override
    public List<ICalendarEvent> getEventList(TeamEventFilter filter, boolean calculateRecurrenceEvents) {
        return teamEventDao.getEventList(filter, calculateRecurrenceEvents);
    }

    @Override
    public List<TeamEventDO> getTeamEventDOList(TeamEventFilter filter) {
        return teamEventDao.select(filter);
    }

    @Override
    public TeamEventDO getById(Long teamEventId) {
        return teamEventDao.find(teamEventId);
    }

    @Override
    public void saveOrUpdate(TeamEventDO teamEvent) {
        teamEventDao.insertOrUpdate(teamEvent);
    }

    @Override
    public void markAsDeleted(TeamEventDO teamEvent) {
        teamEventDao.markAsDeleted(teamEvent);
    }

    @Override
    public void undelete(TeamEventDO teamEvent) {
        teamEventDao.undelete(teamEvent);
    }

    @Override
    public void save(TeamEventDO newEvent) {
        teamEventDao.insert(newEvent);
    }

    @Override
    public TeamEventDao getTeamEventDao() {
        return teamEventDao;
    }

    @Override
    public void updateAttendee(TeamEventAttendeeDO attendee, boolean checkAccess) {
        teamEventAttendeeDao.update(attendee, checkAccess);
    }

    @Override
    public ICalHandler getEventHandler(final TeamCalDO defaultCalendar) {
        return new ICalHandler(this, defaultCalendar);
    }

    @Override
    public void fixAttendees(final TeamEventDO event) {
        List<TeamEventAttendeeDO> attendeesFromDbList = this.getAddressesAndUserAsAttendee();

        Long internalNewAttendeeSequence = -10000L;
        boolean found;

        for (TeamEventAttendeeDO attendeeDO : event.getAttendees()) {
            found = false;

            // search for eMail in DB as possible attendee
            for (TeamEventAttendeeDO dBAttendee : attendeesFromDbList) {
                if (dBAttendee.getEMailAddress() != null && dBAttendee.getEMailAddress().equals(attendeeDO.getUrl())) {
                    attendeeDO = dBAttendee;
                    attendeeDO.setId(internalNewAttendeeSequence--);
                    found = true;
                    break;
                }
            }

            if (!found) {
                attendeeDO.setId(internalNewAttendeeSequence--);
            }
        }
    }
}
