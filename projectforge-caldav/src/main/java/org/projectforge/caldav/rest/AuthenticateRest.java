package org.projectforge.caldav.rest;

import java.util.Date;

import org.projectforge.caldav.cache.UserCache;
import org.projectforge.caldav.model.User;
import org.projectforge.model.rest.RestPaths;
import org.projectforge.model.rest.UserObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.util.UriComponentsBuilder;

@Service
public class AuthenticateRest
{
  @Autowired
  private UserCache userCache;

  @Autowired
  private RestTemplate restTemplate;

  @Value("${projectforge.server.address}")
  private String projectforgeServerAddress;

  @Value("${projectforge.server.port}")
  private String projectforgeServerPort;

  private static Logger log = LoggerFactory.getLogger(AuthenticateRest.class);

  public boolean authenticate(User user, String requestPassword)
  {
    UserObject result = null;
    try {
      String url = projectforgeServerAddress + ":" + projectforgeServerPort + RestPaths.buildPath(RestPaths.AUTHENTICATE_GET_TOKEN);
      HttpHeaders headers = new HttpHeaders();
      headers.set("Accept", MediaType.APPLICATION_JSON_VALUE);
      headers.set("authenticationUsername", user.getUsername());
      headers.set("authenticationPassword", requestPassword);
      UriComponentsBuilder builder = UriComponentsBuilder.fromHttpUrl(url);
      HttpEntity<?> entity = new HttpEntity<>(headers);
      HttpEntity<UserObject> response = restTemplate.exchange(builder.build().encode().toUri(), HttpMethod.GET, entity, UserObject.class);
      result = response.getBody();
      user.setAuthenticationToken(result.getAuthenticationToken());
      userCache.getAuthorizedUserMap().put(user, new Date());
      log.info("Result of rest call: " + result);
    } catch (HttpClientErrorException e) {
      log.info("Authentication failed for user: " + user.getUsername());
      return false;
    }
    return true;
  }

}
