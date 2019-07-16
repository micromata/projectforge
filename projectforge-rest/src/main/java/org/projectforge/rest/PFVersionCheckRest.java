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

import org.apache.commons.lang3.StringUtils;
import org.json.simple.JSONObject;
import org.projectforge.business.jsonRest.RestCallService;
import org.projectforge.business.systeminfo.SystemService;
import org.projectforge.model.rest.RestPaths;
import org.projectforge.model.rest.VersionCheck;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.Marker;
import org.slf4j.MarkerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.ResponseBody;

import javax.servlet.http.HttpServletRequest;
import javax.ws.rs.Consumes;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.MediaType;

@Controller
@Path(RestPaths.VERSION_CHECK)
public class PFVersionCheckRest
{
  private static final Logger log = LoggerFactory.getLogger(PFVersionCheckRest.class);

  final Marker marker = MarkerFactory.getDetachedMarker("VersionCheck");

  @Autowired
  private RestCallService restCallService;

  @Autowired
  private SystemService systemService;

  @POST
  @Consumes(MediaType.APPLICATION_JSON)
  @Produces(MediaType.APPLICATION_JSON)
  public @ResponseBody
  VersionCheck checkVersion(@Context HttpServletRequest request, @RequestBody VersionCheck versionCheck)
  {
    log.info(marker, "Request for check PF Version from: " + request.getRemoteAddr() + " (X-FORWARDED-FOR: " + StringUtils
        .defaultIfEmpty(request.getHeader("X-FORWARDED-FOR"), "n.a.") + ")");
    synchronizeWithProjectForgeGithub(versionCheck);
    log.info(marker, "Result for PF version check: " + versionCheck);
    return versionCheck;
  }

  private VersionCheck synchronizeWithProjectForgeGithub(VersionCheck versionCheck)
  {
    String url = "https://api.github.com/repos/micromata/projectforge/releases/latest";
    try {
      JSONObject jsonObject = restCallService.callRestInterfaceForUrl(url);
      if (jsonObject != null) {
        log.debug(marker, jsonObject.toJSONString());
        String tag_name = (String) jsonObject.get("tag_name");
        log.debug(marker, tag_name);
        String githubVersion = tag_name.split("-")[0];
        log.debug(marker, githubVersion);
        versionCheck.setTargetVersion(githubVersion);
      }
    } catch (Exception e) {
      log.error(marker, "Exception while synchronize with github projectforge version", e);
    }
    return versionCheck;
  }

}
