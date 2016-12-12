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

package org.projectforge.business.teamcal.servlet;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

import javax.servlet.ServletConfig;
import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.commons.httpclient.HttpStatus;
import org.apache.commons.lang.StringUtils;
import org.projectforge.business.configuration.ConfigurationService;
import org.projectforge.business.scripting.GroovyEngine;
import org.projectforge.business.teamcal.event.TeamEventService;
import org.projectforge.business.teamcal.event.model.TeamEventAttendeeDO;
import org.projectforge.business.teamcal.event.model.TeamEventAttendeeStatus;
import org.projectforge.business.teamcal.event.model.TeamEventDO;
import org.projectforge.business.teamcal.service.CryptService;
import org.projectforge.business.user.I18nHelper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.config.AutowireCapableBeanFactory;
import org.springframework.web.context.WebApplicationContext;
import org.springframework.web.context.support.WebApplicationContextUtils;

/**
 * Response servlet for team calendar events.
 *
 * @author Florian Blumenstein
 */
@WebServlet("/pfcalendar")
public class TeamCalResponseServlet extends HttpServlet
{
  private static final long serialVersionUID = 8042634572943344080L;

  private static final org.apache.log4j.Logger log = org.apache.log4j.Logger.getLogger(TeamCalResponseServlet.class);

  @Autowired
  private TeamEventService teamEventService;

  private WebApplicationContext springContext;

  @Autowired
  private CryptService cryptService;

  @Autowired
  private ConfigurationService configurationService;

  @Override
  public void init(final ServletConfig config) throws ServletException
  {
    super.init(config);
    springContext = WebApplicationContextUtils.getRequiredWebApplicationContext(config.getServletContext());
    final AutowireCapableBeanFactory beanFactory = springContext.getAutowireCapableBeanFactory();
    beanFactory.autowireBean(this);
  }

  @Override
  protected void doGet(final HttpServletRequest req, final HttpServletResponse resp)
      throws ServletException, IOException
  {
    if (StringUtils.isBlank(req.getQueryString())) {
      resp.sendError(HttpStatus.SC_BAD_REQUEST);
      log.error("Bad request, no query string given.");
      return;
    }
    Map<String, String> decryptedParameters = cryptService.decryptParameterMessage(req.getQueryString());
    //Getting request status
    final String reqStatus = decryptedParameters.get("status");
    if (StringUtils.isBlank(reqStatus) == true) {
      log.warn("Bad request, request parameter 'status' not given.");
      resp.sendError(HttpStatus.SC_BAD_REQUEST);
      return;
    }
    TeamEventAttendeeStatus status = null;
    try {
      status = TeamEventAttendeeStatus.valueOf(reqStatus.toUpperCase());
    } catch (IllegalArgumentException e) {
      log.warn("Bad request, request parameter 'status' not valid: " + reqStatus);
      sendNotValidData(resp);
      return;
    }
    final TeamEventAttendeeStatus statusFinal = status;

    //Getting request event
    final String reqEventUid = decryptedParameters.get("uid");
    if (StringUtils.isBlank(reqEventUid) == true) {
      log.warn("Bad request, request parameter 'uid' not given.");
      resp.sendError(HttpStatus.SC_BAD_REQUEST);
      return;
    }
    TeamEventDO event = teamEventService.findByUid(reqEventUid);
    if (event == null) {
      log.warn("Bad request, request parameter 'uid' not valid: " + reqEventUid);
      sendNotValidData(resp);
      return;
    }

    //Getting request attendee
    final String reqEventAttendee = decryptedParameters.get("attendee");
    if (StringUtils.isBlank(reqEventAttendee) == true) {
      log.warn("Bad request, request parameter 'attendee' not given.");
      resp.sendError(HttpStatus.SC_BAD_REQUEST);
      return;
    }
    Integer attendeeId = null;
    try {
      attendeeId = Integer.parseInt(reqEventAttendee);
    } catch (NumberFormatException e) {
      log.warn("Bad request, request parameter 'attendee' not valid: " + reqEventAttendee);
      sendNotValidData(resp);
      return;
    }
    final TeamEventAttendeeDO eventAttendee = teamEventService.findByAttendeeId(attendeeId, false);
    if (eventAttendee != null) {
      try {
        eventAttendee.setStatus(statusFinal);
        teamEventService.updateAttendee(eventAttendee, false);
      } catch (Exception e) {
        log.error("Bad request, exception while updating event: " + e.getMessage());
        resp.sendError(HttpStatus.SC_BAD_REQUEST);
        return;
      }
    } else {
      log.warn("Bad request, request parameter 'attendee' not valid: " + reqEventAttendee);
      sendNotValidData(resp);
      return;
    }

    resp.setContentType("text/html;charset=UTF-8");
    final Map<String, Object> templateData = new HashMap<>();
    templateData.put("reponse", I18nHelper.getLocalizedMessage("plugins.teamcal.attendee.response." + statusFinal.getKey()));
    final String content = renderGroovyTemplate("htmlTemplates/teamEventResponse.html", templateData);

    resp.getOutputStream().print(content);
  }

  private void sendNotValidData(HttpServletResponse resp) throws IOException
  {
    resp.setContentType("text/html;charset=UTF-8");
    final Map<String, Object> templateData = new HashMap<>();
    templateData.put("reponse", I18nHelper.getLocalizedMessage("plugins.teamcal.attendee.response.error"));
    final String content = renderGroovyTemplate("htmlTemplates/teamEventResponse.html", templateData);
    resp.getOutputStream().print(content);
  }

  private String renderGroovyTemplate(final String groovyTemplate, final Map<String, Object> data)
  {
    log.debug("groovyTemplate=" + groovyTemplate);
    final GroovyEngine engine = new GroovyEngine(configurationService, data);
    return engine.executeTemplateFile(groovyTemplate);
  }

}
