/////////////////////////////////////////////////////////////////////////////
//
// Project ProjectForge Community Edition
//         www.projectforge.org
//
// Copyright (C) 2001-2019 Micromata GmbH, Germany (www.micromata.com)
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

import org.projectforge.model.rest.CalendarEventObject;
import org.projectforge.model.rest.CalendarObject;
import org.projectforge.model.rest.RestPaths;
import org.projectforge.model.rest.UserObject;

import javax.ws.rs.client.Client;
import javax.ws.rs.client.ClientBuilder;
import javax.ws.rs.client.WebTarget;
import javax.ws.rs.core.Response;
import java.io.IOException;

public class TeamCalClientMain {
  private static final org.slf4j.Logger log = org.slf4j.LoggerFactory.getLogger(TeamCalClientMain.class);

  public static void main(final String[] args) throws IOException {
    final Client client = ClientBuilder.newClient();
    final UserObject user = RestClientMain.authenticate(client);

    WebTarget webResource = client.target(RestClientMain.getUrl() + RestPaths.buildListPath(RestPaths.TEAMCAL));
    Response response = RestClientMain.getClientResponse(webResource, user);
    if (response.getStatus() != Response.Status.OK.getStatusCode()) {
      throw new RuntimeException("Failed : HTTP error code : " + response.getStatus());
    }
    String json =response.readEntity(String.class);
    log.info(json);
    final CalendarObject[] calendars = JsonUtils.fromJson(json, CalendarObject[].class);
    for (final CalendarObject calendar : calendars) {
      log.info(calendar.toString());
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
    final CalendarEventObject[] events = JsonUtils.fromJson(json, CalendarEventObject[].class);
    for (final CalendarEventObject event : events) {
      log.info(event.toString());
    }
  }
}
