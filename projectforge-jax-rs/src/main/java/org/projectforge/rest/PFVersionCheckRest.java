package org.projectforge.rest;

import javax.ws.rs.Consumes;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;

import org.json.simple.JSONObject;
import org.projectforge.business.jsonRest.RestCallService;
import org.projectforge.business.systeminfo.SystemService;
import org.projectforge.model.rest.RestPaths;
import org.projectforge.model.rest.VersionCheck;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.ResponseBody;

@Controller
@Path(RestPaths.VERSION_CHECK)
public class PFVersionCheckRest
{
  private static final org.apache.log4j.Logger log = org.apache.log4j.Logger.getLogger(PFVersionCheckRest.class);

  @Autowired
  private RestCallService restCallService;

  @Autowired
  private SystemService systemService;

  @POST
  @Consumes(MediaType.APPLICATION_JSON)
  @Produces(MediaType.APPLICATION_JSON)
  public @ResponseBody VersionCheck checkVersion(@RequestBody VersionCheck versionCheck)
  {
    log.info("Checking version...");
    synchronizeWithProjectforgeGithub(versionCheck);
    return versionCheck;
  }

  @GET
  public Response getMethod()
  {
    log.info("Call of PFVersionCheckRest GET method!");
    VersionCheck vc = systemService.getVersionCheckInformations();
    return Response.ok(JsonUtils.toJson(vc)).build();
  }

  private VersionCheck synchronizeWithProjectforgeGithub(VersionCheck versionCheck)
  {
    String url = "https://api.github.com/repos/micromata/projectforge/releases/latest";
    try {
      JSONObject jsonObject = restCallService.callRestInterfaceForUrl(url);
      log.debug(jsonObject);
      String tag_name = (String) jsonObject.get("tag_name");
      log.debug(tag_name);
      String githubVersion = tag_name.split("-")[0];
      log.debug(githubVersion);
      versionCheck.setTargetVersion(githubVersion);
    } catch (Exception e) {
      log.error("Exception while synchronize with github projectforge version", e);
    }
    return versionCheck;
  }

}
