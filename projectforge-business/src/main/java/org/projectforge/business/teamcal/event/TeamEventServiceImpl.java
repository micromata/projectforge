package org.projectforge.business.teamcal.event;

import java.io.ByteArrayOutputStream;
import java.io.UnsupportedEncodingException;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import org.apache.commons.lang3.StringUtils;
import org.projectforge.business.address.AddressDO;
import org.projectforge.business.address.AddressDao;
import org.projectforge.business.teamcal.ICSGenerator;
import org.projectforge.business.teamcal.event.model.TeamEventAttendeeDO;
import org.projectforge.business.teamcal.event.model.TeamEventAttendeeDao;
import org.projectforge.business.teamcal.event.model.TeamEventDO;
import org.projectforge.business.user.I18nHelper;
import org.projectforge.mail.Mail;
import org.projectforge.mail.SendMail;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service
public class TeamEventServiceImpl implements TeamEventService
{
  private static final org.apache.log4j.Logger log = org.apache.log4j.Logger.getLogger(TeamEventServiceImpl.class);

  @Autowired
  private AddressDao addressDao;

  @Autowired
  private TeamEventAttendeeDao teamEventAttendeeDao;

  @Autowired
  private TeamEventDao teamEventDao;

  @Autowired
  private SendMail sendMail;

  @Autowired
  private ICSGenerator icsGenerator;

  @Override
  public List<Integer> getAssignedAttendeeIds(TeamEventDO data)
  {
    List<Integer> assignedAttendees = new ArrayList<>();
    for (TeamEventAttendeeDO attendee : data.getAttendees()) {
      assignedAttendees.add(attendee.getId());
    }
    return assignedAttendees;
  }

  @Override
  public List<TeamEventAttendeeDO> getSortedAddressesAsAttendee()
  {
    List<TeamEventAttendeeDO> resultList = new ArrayList<>();
    List<AddressDO> allAddressList = addressDao.internalLoadAll().stream()
        .sorted((address1, address2) -> address2.getFullName().compareTo(address1.getFullName()))
        .collect(Collectors.toList());
    for (AddressDO singleAddress : allAddressList) {
      if (StringUtils.isBlank(singleAddress.getEmail()) == false) {
        TeamEventAttendeeDO attendee = new TeamEventAttendeeDO();
        attendee.setAddress(singleAddress);
        resultList.add(attendee);
      }
    }
    return resultList;
  }

  @Override
  public TeamEventAttendeeDO getAttendee(Integer attendeeId)
  {
    return teamEventAttendeeDao.internalGetById(attendeeId);
  }

  @Override
  public void assignAttendees(TeamEventDO data, Set<TeamEventAttendeeDO> itemsToAssign,
      Set<TeamEventAttendeeDO> itemsToUnassign)
  {
    for (TeamEventAttendeeDO assignAttendee : itemsToAssign) {
      if (assignAttendee.getId() < 0) {
        assignAttendee.setId(null);
        teamEventAttendeeDao.internalSave(assignAttendee);
        data.getAttendees().add(assignAttendee);
      }
    }

    data.getAttendees().removeAll(itemsToUnassign);
    teamEventDao.update(data);

    for (TeamEventAttendeeDO deleteAttendee : itemsToUnassign) {
      teamEventAttendeeDao.internalMarkAsDeleted(deleteAttendee);
    }
  }

  @Override
  public boolean sendTeamEventToAttendees(TeamEventDO data, boolean isNew, boolean hasChanges,
      Set<TeamEventAttendeeDO> addedAttendees)
  {
    final Mail msg = new Mail();
    if (isNew == false && hasChanges == false && addedAttendees.size() > 0) {
      for (TeamEventAttendeeDO attendee : addedAttendees) {
        addAttendeeToMail(attendee, msg);
      }
    } else {
      for (TeamEventAttendeeDO attendee : data.getAttendees()) {
        addAttendeeToMail(attendee, msg);
      }
    }
    msg.setProjectForgeSubject(I18nHelper.getLocalizedString("plugins.teamcal.attendee.email.subject"));
    msg.setContent("plugins.teamcal.attendee.email.content");
    msg.setContentType(Mail.CONTENTTYPE_HTML);
    ByteArrayOutputStream icsFile = icsGenerator.getIcsFile(data);
    boolean result = false;
    try {
      result = sendMail.send(msg, icsFile.toString(StandardCharsets.UTF_8.name()), null);
    } catch (UnsupportedEncodingException e) {
      log.error("Something went wrong sending team event to attendee", e);
    }
    return result;
  }

  private void addAttendeeToMail(TeamEventAttendeeDO attendee, Mail msg)
  {
    if (attendee.getAddress() != null) {
      msg.addTo(attendee.getAddress().getEmail());
    }
    if (StringUtils.isNotBlank(attendee.getUrl())) {
      msg.addTo(attendee.getUrl());
    }
  }

}
