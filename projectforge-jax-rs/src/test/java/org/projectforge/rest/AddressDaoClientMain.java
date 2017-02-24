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

import java.util.Calendar;
import java.util.Collection;

import javax.ws.rs.client.Client;
import javax.ws.rs.client.ClientBuilder;
import javax.ws.rs.client.WebTarget;
import javax.ws.rs.core.Response;

import org.projectforge.model.rest.AddressObject;
import org.projectforge.model.rest.RestPaths;
import org.projectforge.model.rest.UserObject;
import org.projectforge.rest.converter.DateTimeFormat;

import com.google.gson.reflect.TypeToken;

public class AddressDaoClientMain
{
  private static final org.apache.log4j.Logger log = org.apache.log4j.Logger.getLogger(AddressDaoClientMain.class);

  @SuppressWarnings("unused")
  public static void main(final String[] args)
  {
    final Client client = ClientBuilder.newClient();
    final UserObject user = RestClientMain.authenticate(client);

    final Calendar cal = Calendar.getInstance();
    cal.set(2013, Calendar.JUNE, 27);
    final Long modifiedSince = cal.getTimeInMillis();
    //modifiedSince = null; // Uncomment this for testing modifiedSince paramter.

    // http://localhost:8080/ProjectForge/rest/task/tree // userId / token
    WebTarget webResource = client.target(RestClientMain.getUrl() + RestPaths.buildListPath(RestPaths.ADDRESS))
        .queryParam("search", "");
    if (modifiedSince != null) {
      webResource = webResource.queryParam("modifiedSince", "" + modifiedSince);
    }
    webResource = RestClientMain.setConnectionSettings(webResource,
        new ConnectionSettings().setDateTimeFormat(DateTimeFormat.MILLIS_SINCE_1970));
    final Response response = RestClientMain.getClientResponse(webResource, user);
    if (response.getStatus() != Response.Status.OK.getStatusCode()) {
      log.error("Failed : HTTP error code : " + response.getStatus());
      return;
    }
    final String json = (String) response.getEntity();
    log.info(json);
    final Collection<AddressObject> col = JsonUtils.fromJson(json, new TypeToken<Collection<AddressObject>>()
    {
    }.getType());
    for (final AddressObject address : col) {
      log.info(address.getFirstName() + " " + address.getName());
    }
  }
}
