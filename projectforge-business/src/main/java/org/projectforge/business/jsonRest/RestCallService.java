package org.projectforge.business.jsonRest;

import org.apache.commons.lang.StringUtils;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.util.UriComponentsBuilder;

@Service
public class RestCallService
{
  private static final org.apache.log4j.Logger log = org.apache.log4j.Logger.getLogger(RestCallService.class);

  @Autowired
  RestTemplate restTemplate;

  public JSONObject callRestInterfaceForUrl(String url)
  {
    return callRestInterfaceForUrl(url, HttpMethod.GET);
  }

  public JSONObject callRestInterfaceForUrl(String url, HttpMethod hm)
  {
    JSONObject response = null;
    try {
      String responseAsString = callRestInterfaceForUrl(url, hm, String.class, null);
      if (StringUtils.isNotEmpty(responseAsString)) {
        JSONParser parser = new JSONParser();
        response = (JSONObject) parser.parse(responseAsString);
      }
    } catch (Exception e) {
      log.error("Exception while parsing Rest response into JSONObject  url: " + url, e);
    }
    return response;
  }

  public <T> T callRestInterfaceForUrl(final String url, final HttpMethod hm, final Class<T> objectClass, final T bodyObject)
  {
    if (StringUtils.isBlank(url)) {
      return null;
    }
    T response = null;
    try {
      HttpHeaders headers = new HttpHeaders();
      headers.setContentType(MediaType.APPLICATION_JSON);
      UriComponentsBuilder builder = UriComponentsBuilder.fromHttpUrl(url);
      HttpEntity<T> entity = new HttpEntity<>(bodyObject, headers);
      HttpEntity<T> responseEntity = restTemplate.exchange(builder.build().encode().toUri(), hm != null ? hm : HttpMethod.GET, entity, objectClass);
      response = responseEntity.getBody();
      log.debug("Result of rest call: " + url + "\nRESPONSE JSON: " + response.toString());

    } catch (Exception e) {
      log.error("Exception while calling Rest interface for url: " + url, e);
    }
    return response;
  }

}
