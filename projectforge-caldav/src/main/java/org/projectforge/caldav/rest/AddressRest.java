package org.projectforge.caldav.rest;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import org.projectforge.caldav.model.AddressBook;
import org.projectforge.caldav.model.Contact;
import org.projectforge.caldav.model.User;
import org.projectforge.caldav.service.VCardService;
import org.projectforge.model.rest.AddressObject;
import org.projectforge.model.rest.RestPaths;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.util.UriComponentsBuilder;

import com.fasterxml.jackson.databind.ObjectMapper;

import ezvcard.VCard;

@Service
public class AddressRest
{
  private static Logger log = LoggerFactory.getLogger(AddressRest.class);

  @Autowired
  private RestTemplate restTemplate;

  @Autowired
  private VCardService vCardService;

  @Value("${projectforge.server.address}")
  private String projectforgeServerAddress;

  @Value("${projectforge.server.port}")
  private String projectforgeServerPort;

  public List<Contact> getContactList(AddressBook ab)
  {
    List<Contact> result = new ArrayList<>();
    User user = ab.getUser();
    try {
      String url = projectforgeServerAddress + ":" + projectforgeServerPort + RestPaths.buildListPath(RestPaths.ADDRESS);
      HttpHeaders headers = new HttpHeaders();
      headers.set("Accept", MediaType.APPLICATION_JSON_VALUE);
      headers.set("authenticationUserId", String.valueOf(user.getPk()));
      headers.set("authenticationToken", user.getAuthenticationToken());
      UriComponentsBuilder builder = UriComponentsBuilder.fromHttpUrl(url)
          .queryParam("disableImageData", false);
      HttpEntity<?> entity = new HttpEntity<>(headers);
      HttpEntity<AddressObject[]> response = restTemplate.exchange(builder.build().encode().toUri(), HttpMethod.GET, entity, AddressObject[].class);
      AddressObject[] contactArray = response.getBody();
      log.info("Result of rest call: " + contactArray);
      result = convertRestResponse(ab, contactArray);
    } catch (Exception e) {
      log.error("Exception while getting calendars for user: " + user.getUsername(), e);
    }
    return result;
  }

  public Contact createContact(final AddressBook ab, final byte[] vcardBytearray)
  {
    User user = ab.getUser();
    try {
      VCard vcard = vCardService.getVCardFromByteArray(vcardBytearray);
      AddressObject request = vCardService.getAddressObject(vcard);
      ObjectMapper mapper = new ObjectMapper();
      final String json = mapper.writeValueAsString(request);
      String url = projectforgeServerAddress + ":" + projectforgeServerPort + RestPaths.buildPath(RestPaths.ADDRESS, RestPaths.SAVE_OR_UDATE);
      HttpHeaders headers = new HttpHeaders();
      headers.set("Accept", MediaType.APPLICATION_JSON_VALUE);
      headers.setContentType(MediaType.APPLICATION_JSON);
      headers.set("authenticationUserId", String.valueOf(user.getPk()));
      headers.set("authenticationToken", user.getAuthenticationToken());
      HttpEntity<?> entity = new HttpEntity<>(json, headers);
      UriComponentsBuilder builder = UriComponentsBuilder.fromHttpUrl(url);
      ResponseEntity<AddressObject> response = restTemplate
          .exchange(builder.build().encode().toUri(), HttpMethod.PUT, entity, AddressObject.class);
      AddressObject addressObject = response.getBody();
      log.info("Result of rest call: " + addressObject);
      return convertRestResponse(ab, addressObject);
    } catch (Exception e) {
      log.error("Exception while creating contact.", e);
    }
    return null;
  }

  public Contact updateContact(final Contact contact, final byte[] vcardBytearray)
  {
    User user = contact.getAddressBook().getUser();
    try {
      VCard vcard = vCardService.getVCardFromByteArray(vcardBytearray);
      AddressObject request = vCardService.getAddressObject(vcard);
      ObjectMapper mapper = new ObjectMapper();
      final String json = mapper.writeValueAsString(request);
      String url = projectforgeServerAddress + ":" + projectforgeServerPort + RestPaths.buildPath(RestPaths.ADDRESS, RestPaths.SAVE_OR_UDATE);
      HttpHeaders headers = new HttpHeaders();
      headers.set("Accept", MediaType.APPLICATION_JSON_VALUE);
      headers.setContentType(MediaType.APPLICATION_JSON);
      headers.set("authenticationUserId", String.valueOf(user.getPk()));
      headers.set("authenticationToken", user.getAuthenticationToken());
      HttpEntity<?> entity = new HttpEntity<>(json, headers);
      UriComponentsBuilder builder = UriComponentsBuilder.fromHttpUrl(url);
      ResponseEntity<AddressObject> response = restTemplate
          .exchange(builder.build().encode().toUri(), HttpMethod.PUT, entity, AddressObject.class);
      AddressObject addressObject = response.getBody();
      log.info("Result of rest call: " + addressObject);
      return convertRestResponse(contact.getAddressBook(), addressObject);
    } catch (Exception e) {
      log.error("Exception while updating contact: " + contact.getName(), e);
    }
    return contact;
  }

  public void deleteContact(final Contact contact)
  {
    User user = contact.getAddressBook().getUser();
    try {
      VCard vcard = vCardService.getVCardFromByteArray(contact.getVcardData());
      AddressObject request = vCardService.getAddressObject(vcard);
      ObjectMapper mapper = new ObjectMapper();
      final String json = mapper.writeValueAsString(request);
      String url = projectforgeServerAddress + ":" + projectforgeServerPort + RestPaths.buildPath(RestPaths.ADDRESS, RestPaths.DELETE);
      HttpHeaders headers = new HttpHeaders();
      headers.set("Accept", MediaType.APPLICATION_JSON_VALUE);
      headers.setContentType(MediaType.APPLICATION_JSON);
      headers.set("authenticationUserId", String.valueOf(user.getPk()));
      headers.set("authenticationToken", user.getAuthenticationToken());
      HttpEntity<?> entity = new HttpEntity<>(json, headers);
      UriComponentsBuilder builder = UriComponentsBuilder.fromHttpUrl(url);
      restTemplate
          .exchange(builder.build().encode().toUri(), HttpMethod.DELETE, entity, AddressObject.class);
      log.info("Delete contact success.");
    } catch (Exception e) {
      log.error("Exception while deleting contact: " + contact.getName(), e);
    }
  }

  private Contact convertRestResponse(AddressBook ab, AddressObject addressObject)
  {
    Contact c = new Contact();
    c.setId(addressObject.getId());
    c.setName(addressObject.getFullName());
    c.setModifiedDate(addressObject.getLastUpdate());
    c.setVcardData(vCardService.getVCard(addressObject));
    c.setAddressBook(ab);
    return c;
  }

  private List<Contact> convertRestResponse(AddressBook ab, AddressObject[] contactArray)
  {
    List<Contact> result = new ArrayList<>();
    List<AddressObject> calObjList = Arrays.asList(contactArray);
    calObjList.forEach(conObj -> {
      result.add(convertRestResponse(ab, conObj));
    });
    return result;
  }
}
