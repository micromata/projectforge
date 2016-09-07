package org.projectforge.business.teamcal.event;

import java.io.ByteArrayOutputStream;
import java.io.UnsupportedEncodingException;
import java.nio.charset.StandardCharsets;
import java.text.DateFormat;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import org.apache.commons.lang3.StringUtils;
import org.projectforge.business.address.AddressDO;
import org.projectforge.business.address.AddressDao;
import org.projectforge.business.configuration.ConfigurationService;
import org.projectforge.business.teamcal.ICSGenerator;
import org.projectforge.business.teamcal.event.model.TeamEventAttendeeDO;
import org.projectforge.business.teamcal.event.model.TeamEventAttendeeDao;
import org.projectforge.business.teamcal.event.model.TeamEventAttendeeStatus;
import org.projectforge.business.teamcal.event.model.TeamEventDO;
import org.projectforge.business.teamcal.service.CryptService;
import org.projectforge.business.user.I18nHelper;
import org.projectforge.business.user.service.UserService;
import org.projectforge.framework.persistence.user.api.ThreadLocalUserContext;
import org.projectforge.framework.persistence.user.entities.PFUserDO;
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

  @Autowired
  private UserService userService;

  @Autowired
  private CryptService cryptService;

  @Autowired
  private ConfigurationService configService;

  @Override
  public List<Integer> getAssignedAttendeeIds(TeamEventDO data)
  {
    List<Integer> assignedAttendees = new ArrayList<>();
    if (data != null && data.getAttendees() != null) {
      for (TeamEventAttendeeDO attendee : data.getAttendees()) {
        assignedAttendees.add(attendee.getId());
      }
    }
    return assignedAttendees;
  }

  @Override
  public List<TeamEventAttendeeDO> getAddressesAndUserAsAttendee()
  {
    List<TeamEventAttendeeDO> resultList = new ArrayList<>();
    Set<Integer> addedUserIds = new HashSet<>();
    List<AddressDO> allAddressList = addressDao.internalLoadAllNotDeleted().stream()
        .sorted((address1, address2) -> address2.getFullName().compareTo(address1.getFullName()))
        .collect(Collectors.toList());
    for (AddressDO singleAddress : allAddressList) {
      if (StringUtils.isBlank(singleAddress.getEmail()) == false) {
        TeamEventAttendeeDO attendee = new TeamEventAttendeeDO();
        attendee.setStatus(TeamEventAttendeeStatus.NEW);
        attendee.setAddress(singleAddress);
        List<PFUserDO> userWithSameMail = userService.findUserByMail(singleAddress.getEmail());
        if (userWithSameMail.size() > 0 && addedUserIds.contains(userWithSameMail.get(0).getId()) == false) {
          PFUserDO user = userWithSameMail.get(0);
          attendee.setUser(user);
          addedUserIds.add(user.getId());
        }
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
        assignAttendee.setStatus(TeamEventAttendeeStatus.IN_PROCESS);
        teamEventAttendeeDao.internalSave(assignAttendee);
        data.addAttendee(assignAttendee);
      }
    }

    if (data.getAttendees() != null && itemsToUnassign.size() > 0) {
      data.getAttendees().removeAll(itemsToUnassign);
      for (TeamEventAttendeeDO deleteAttendee : itemsToUnassign) {
        teamEventAttendeeDao.internalMarkAsDeleted(deleteAttendee);
      }
    }
    teamEventDao.update(data);
  }

  @Override
  public boolean sendTeamEventToAttendees(TeamEventDO data, boolean isNew, boolean hasChanges, boolean isDeleted,
      Set<TeamEventAttendeeDO> addedAttendees)
  {
    boolean result = false;
    if (isDeleted) {
      for (TeamEventAttendeeDO attendee : data.getAttendees()) {
        result = sendMail(data, attendee, "deleted");
      }
      return result;
    }
    if (isNew) {
      for (TeamEventAttendeeDO attendee : data.getAttendees()) {
        result = sendMail(data, attendee, "new");
      }
    } else {
      Set<TeamEventAttendeeDO> sendToList = new HashSet<>();
      if (hasChanges == false && addedAttendees.size() > 0) {
        sendToList = addedAttendees;
      } else {
        sendToList = data.getAttendees();
      }
      for (TeamEventAttendeeDO attendee : sendToList) {
        result = sendMail(data, attendee, "update");
      }
    }
    return result;
  }

  private Mail createMail(String mode)
  {
    final Mail msg = new Mail();
    PFUserDO user = ThreadLocalUserContext.getUser();
    if (user != null) {
      msg.setFrom(user.getEmail());
      msg.setFromRealname(user.getFullname());
    }
    msg.setContentType(Mail.CONTENTTYPE_HTML);
    msg.setProjectForgeSubject(SendMail
        .getProjectForgeSubject(I18nHelper.getLocalizedMessage("plugins.teamcal.attendee.email.subject." + mode)));
    return msg;
  }

  //TODO FB: Should be refactored ;-)
  private boolean sendMail(TeamEventDO data, TeamEventAttendeeDO attendee, String mode)
  {
    final Mail msg = createMail(mode);
    addAttendeeToMail(attendee, msg);
    DateFormat formatter = DateFormat.getDateTimeInstance(DateFormat.MEDIUM, DateFormat.SHORT);
    formatter.setTimeZone(ThreadLocalUserContext.getUser().getTimeZoneObject());
    String attendeesString = "";
    for (TeamEventAttendeeDO attendeeForString : data.getAttendees()) {
      attendeesString = attendeesString + attendeeForString.toString() + " <br>";
    }
    if ("deleted".equals(mode)) {
      String content = I18nHelper.getLocalizedMessage("plugins.teamcal.attendee.email.content." + mode,
          data.getSubject(),
          formatter.format(data.getStartDate()),
          data.getLocation() != null ? data.getLocation() : "",
          attendeesString,
          data.getNote() != null ? data.getNote() : "");
      msg.setContent(content);
      return sendMail.send(msg, null, null);
    }
    String content = I18nHelper.getLocalizedMessage("plugins.teamcal.attendee.email.content." + mode,
        data.getSubject(),
        formatter.format(data.getStartDate()),
        data.getLocation() != null ? data.getLocation() : "",
        attendeesString,
        data.getNote() != null ? data.getNote() : "",
        getResponseLinks(data, attendee));
    msg.setContent(content);
    ByteArrayOutputStream icsFile = icsGenerator.getIcsFile(data);
    boolean result = false;
    try {
      result = sendMail.send(msg, icsFile.toString(StandardCharsets.UTF_8.name()), null);
    } catch (UnsupportedEncodingException e) {
      log.error("Something went wrong sending team event to attendee", e);
    }
    return result;
  }

  private String getResponseLinks(TeamEventDO event, TeamEventAttendeeDO attendee)
  {
    String messageParamBegin = "uid=" + event.getUid() + "&attendee=" + attendee.getId();
    String acceptParams = cryptService.encryptParameterMessage(messageParamBegin + "&status=ACCEPTED");
    String declinedParams = cryptService.encryptParameterMessage(messageParamBegin + "&status=DECLINED");
    String result = "<a href=\"" + configService.getDomain() + "/cal?" + acceptParams + "\">"
        + TeamEventAttendeeStatus.ACCEPTED.getI18nValue() + "</a><br><a href=\"" + configService.getDomain() + "/cal?"
        + declinedParams + "\">" + TeamEventAttendeeStatus.DECLINED.getI18nValue() + "</a><br>";
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

  @Override
  public TeamEventDO findByUid(String reqEventUid)
  {
    return teamEventDao.getByUid(reqEventUid);
  }

  @Override
  public TeamEventAttendeeDO findByAttendeeId(Integer attendeeId, boolean checkAccess)
  {
    TeamEventAttendeeDO result = null;
    if (checkAccess) {
      result = teamEventAttendeeDao.getById(attendeeId);
    } else {
      result = teamEventAttendeeDao.internalGetById(attendeeId);
    }
    return result;
  }

  @Override
  public TeamEventAttendeeDO findByAttendeeId(Integer attendeeId)
  {
    return findByAttendeeId(attendeeId, true);
  }

  @Override
  public void update(TeamEventDO event)
  {
    update(event, true);
  }

  @Override
  public void update(TeamEventDO event, boolean checkAccess)
  {
    teamEventDao.internalUpdate(event, checkAccess);
  }

}
