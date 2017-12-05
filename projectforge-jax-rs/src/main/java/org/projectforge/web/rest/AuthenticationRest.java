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

package org.projectforge.web.rest;

import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;

import org.projectforge.AppVersion;
import org.projectforge.Version;
import org.projectforge.business.user.UserDao;
import org.projectforge.framework.persistence.user.api.ThreadLocalUserContext;
import org.projectforge.framework.persistence.user.entities.PFUserDO;
import org.projectforge.model.rest.RestPaths;
import org.projectforge.model.rest.ServerInfo;
import org.projectforge.model.rest.UserObject;
import org.projectforge.rest.JsonUtils;
import org.projectforge.web.rest.converter.PFUserDOConverter;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;

/**
 * REST interface for authentication (tests) and getting the authentication token on initial contact.
 * <p>
 * <h2>Concept</h2> It's recommended to avoid storing the user's username and password on the client (e. g. on the
 * mobile phone) due to security reasons. Please store the user's id and authentication-token instead:
 * <ol>
 * <li>On first start of your client (user-token isn't known yet), please call {@link #getToken()} for getting the user
 * data (id, authentication-token and optional information) by sending the username and password the user typed in.</li>
 * <li>You may now store the user's id and authentication-token for the user's convenience on your client (e. g. mobile
 * app).</li>
 * <li>Every time the user starts the client / app you should call {@link #initialContact(String)} for checking the
 * server version. May-be the server version is too old or your client version is too old. This call is optional but
 * good practice.</li>
 * <li>Every further rest call is done by authentication via user-id and authentication-token. The user-id is required
 * for logging purposes e. g. for failed logins or brute-force attacks.</li>
 * </ol>
 *
 * @author Kai Reinhard (k.reinhard@micromata.de)
 */
@Controller
@Path(RestPaths.AUTHENTICATE)
public class AuthenticationRest
{
  private static final org.slf4j.Logger log = org.slf4j.LoggerFactory.getLogger(AuthenticationRest.class);

  @Autowired
  private UserDao userDao;

  /**
   * Authentication via http header authenticationUsername and authenticationPassword.<br/>
   * For getting the user's authentication token. This token can be stored in the client (e. g. mobile app). The user's
   * password shouldn't be stored in the client for security reasons. The authentication token is renewable through the
   * ProjectForge's web app (my account).
   *
   * @return {@link UserObject}
   */
  @GET
  @Path(RestPaths.AUTHENTICATE_GET_TOKEN_METHOD)
  @Produces(MediaType.APPLICATION_JSON)
  public Response getToken()
  {
    final PFUserDO user = ThreadLocalUserContext.getUser();
    if (user == null) {
      log.error("No user given for rest call.");
      throw new IllegalArgumentException("No user given for the rest call: authenticate/getToken.");
    }
    final UserObject userObject = PFUserDOConverter.getUserObject(user);
    final String authenticationToken = userDao.getAuthenticationToken(user.getId());
    userObject.setAuthenticationToken(authenticationToken);
    final String json = JsonUtils.toJson(userObject);
    return Response.ok(json).build();
  }

  /**
   * Authentication via http header authenticationUserId and authenticationToken.
   *
   * @param clientVersionString
   * @return {@link ServerInfo}
   */
  @GET
  @Path(RestPaths.AUTHENTICATE_INITIAL_CONTACT_METHOD)
  @Produces(MediaType.APPLICATION_JSON)
  public Response initialContact(@QueryParam("clientVersion") final String clientVersionString)
  {
    final PFUserDO user = ThreadLocalUserContext.getUser();
    if (user == null) {
      log.error("No user given for rest call.");
      throw new IllegalArgumentException("No user given for the rest call: authenticate/getToken.");
    }
    final UserObject userObject = PFUserDOConverter.getUserObject(user);
    final ServerInfo info = new ServerInfo(AppVersion.VERSION.toString());
    info.setUser(userObject);
    Version clientVersion = null;
    if (clientVersionString != null) {
      clientVersion = new Version(clientVersionString);
    }
    if (clientVersion == null) {
      info.setStatus(ServerInfo.STATUS_UNKNOWN);
    } else if (clientVersion.compareTo(new Version("5.0")) < 0) {
      info.setStatus(ServerInfo.STATUS_CLIENT_TO_OLD);
    } else if (clientVersion.compareTo(AppVersion.VERSION) > 0) {
      info.setStatus(ServerInfo.STATUS_CLIENT_NEWER_THAN_SERVER);
    } else {
      info.setStatus(ServerInfo.STATUS_OK);
    }
    final String json = JsonUtils.toJson(info);
    return Response.ok(json).build();
  }
}
