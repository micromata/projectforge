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

package org.projectforge.rest;

import java.util.Collection;

import javax.ws.rs.client.Client;
import javax.ws.rs.client.ClientBuilder;
import javax.ws.rs.client.WebTarget;
import javax.ws.rs.core.Response;

import org.projectforge.model.rest.CalendarEventObject;
import org.projectforge.model.rest.CalendarObject;
import org.projectforge.model.rest.RestPaths;
import org.projectforge.model.rest.UserObject;

import com.google.gson.reflect.TypeToken;

public class TeamCalClientMain
{
  private static final org.apache.log4j.Logger log = org.apache.log4j.Logger.getLogger(TeamCalClientMain.class);

  public static void main(final String[] args)
  {
    final Client client = ClientBuilder.newClient();
    final UserObject user = RestClientMain.authenticate(client);

    WebTarget webResource = client.target(RestClientMain.getUrl() + RestPaths.buildListPath(RestPaths.TEAMCAL));
    Response response = RestClientMain.getClientResponse(webResource, user);
    if (response.getStatus() != Response.Status.OK.getStatusCode()) {
      throw new RuntimeException("Failed : HTTP error code : " + response.getStatus());
    }
    String json = (String) response.getEntity();
    log.info(json);
    final Collection<CalendarObject> calendars = JsonUtils.fromJson(json, new TypeToken<Collection<CalendarObject>>()
    {
    }.getType());
    for (final CalendarObject calendar : calendars) {
      log.info(calendar);
    }

    webResource = client.target(RestClientMain.getUrl() + RestPaths.buildListPath(RestPaths.TEAMEVENTS))
    // .queryParam("calendarIds", "1292975,1240526,1240528");
    ;
    response = RestClientMain.getClientResponse(webResource, user);
    if (response.getStatus() != Response.Status.OK.getStatusCode()) {
      throw new RuntimeException("Failed : HTTP error code : " + response.getStatus());
    }
    json = (String) response.getEntity();
    log.info(json);
    final Collection<CalendarEventObject> events = JsonUtils.fromJson(json,
        new TypeToken<Collection<CalendarEventObject>>()
        {
        }.getType());
    for (final CalendarEventObject event : events) {
      log.info(event);
    }
  }
}
