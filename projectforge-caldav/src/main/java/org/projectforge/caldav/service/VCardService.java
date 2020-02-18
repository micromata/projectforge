package org.projectforge.caldav.service;

import ezvcard.Ezvcard;
import ezvcard.VCard;
import ezvcard.VCardVersion;
import ezvcard.io.text.VCardReader;
import ezvcard.parameter.AddressType;
import ezvcard.parameter.EmailType;
import ezvcard.parameter.ImageType;
import ezvcard.parameter.TelephoneType;
import ezvcard.property.*;
import ezvcard.util.PartialDate;
import org.projectforge.model.rest.AddressObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.time.LocalDate;
import java.util.Arrays;

@Service
public class VCardService {
  private static Logger log = LoggerFactory.getLogger(VCardService.class);

  public byte[] getVCard(final AddressObject addressDO) {
    //See: https://github.com/mangstadt/ez-vcard
    VCard vcard = new VCard();
    Uid uid = new Uid("urn:uuid:" + addressDO.getUid());
    vcard.setUid(uid);
    vcard.setFormattedName(addressDO.getFullName());
    final StructuredName n = new StructuredName();
    n.setFamily(addressDO.getName());
    n.setGiven(addressDO.getFirstName());

    if (StringUtils.isEmpty(addressDO.getTitle()) == false) {
      n.getPrefixes().add(addressDO.getTitle());
    }
    vcard.setStructuredName(n);

    //Home address
    final Address homeAddress = new Address();
    homeAddress.getTypes().add(AddressType.HOME);
    homeAddress.setStreetAddress(addressDO.getPrivateAddressText());
    homeAddress.setPostalCode(addressDO.getPrivateZipCode());
    homeAddress.setLocality(addressDO.getPrivateCity());
    homeAddress.setRegion(addressDO.getPrivateState());
    homeAddress.setCountry(addressDO.getPrivateCountry());

    if (addressDO.getCommunicationLanguage() != null) {
      vcard.addLanguage(addressDO.getCommunicationLanguage().getDisplayLanguage(addressDO.getCommunicationLanguage()));
      vcard.getStructuredName().setLanguage(addressDO.getCommunicationLanguage().getDisplayLanguage(addressDO.getCommunicationLanguage()));
    }

    //adr.setLabel("123 Main St.\nAlbany, NY 54321\nUSA");
    vcard.addAddress(homeAddress);
    vcard.addTelephoneNumber(addressDO.getPrivatePhone(), TelephoneType.HOME);
    vcard.addTelephoneNumber(addressDO.getPrivateMobilePhone(), TelephoneType.HOME, TelephoneType.PAGER);
    vcard.addEmail(addressDO.getPrivateEmail(), EmailType.HOME);

    //Business address
    final Address businessAddress = new Address();
    businessAddress.getTypes().add(AddressType.WORK);
    businessAddress.setStreetAddress(addressDO.getAddressText());
    businessAddress.setPostalCode(addressDO.getZipCode());
    businessAddress.setLocality(addressDO.getCity());
    businessAddress.setRegion(addressDO.getState());
    businessAddress.setCountry(addressDO.getCountry());

    //adr.setLabel("123 Main St.\nAlbany, NY 54321\nUSA");
    vcard.addAddress(businessAddress);
    vcard.addTelephoneNumber(addressDO.getBusinessPhone(), TelephoneType.WORK);
    vcard.addTelephoneNumber(addressDO.getMobilePhone(), TelephoneType.WORK, TelephoneType.CELL);
    vcard.addTelephoneNumber(addressDO.getFax(), TelephoneType.WORK, TelephoneType.FAX);
    vcard.addEmail(addressDO.getEmail(), EmailType.WORK);

    final Organization organisation = new Organization();
    organisation.getValues().add(StringUtils.isEmpty(addressDO.getOrganization()) == false ? addressDO.getOrganization() : "");
    organisation.getValues().add(StringUtils.isEmpty(addressDO.getDivision()) == false ? addressDO.getDivision() : "");
    organisation.getValues().add(StringUtils.isEmpty(addressDO.getPositionText()) == false ? addressDO.getPositionText() : "");
    vcard.addOrganization(organisation);

    //Home address
    final Address postalAddress = new Address();
    postalAddress.getTypes().add(AddressType.POSTAL);
    postalAddress.setStreetAddress(addressDO.getPostalAddressText());
    postalAddress.setPostalCode(addressDO.getPostalZipCode());
    postalAddress.setLocality(addressDO.getPostalCity());
    postalAddress.setRegion(addressDO.getPostalState());
    postalAddress.setCountry(addressDO.getPostalCountry());

    final java.sql.Date birthday = addressDO.getBirthday();
    if (birthday != null) {
      vcard.setBirthday(new Birthday(birthday));
    }

    vcard.addUrl(addressDO.getWebsite());

    vcard.addNote(addressDO.getComment());

    if (StringUtils.isEmpty(addressDO.getImage()) == false) {
      Photo photo = new Photo(addressDO.getImage().getBytes(), ImageType.JPEG);
      vcard.addPhoto(photo);
    }

    return Ezvcard.write(vcard).version(VCardVersion.V3_0).go().getBytes();
  }

  public AddressObject getAddressObject(final VCard vcard) {
    AddressObject ao = new AddressObject();
    ao.setUid(vcard.getUid().getValue());
    ao.setName(vcard.getStructuredName().getFamily());
    ao.setFirstName(vcard.getStructuredName().getGiven());

    if (vcard.getStructuredName().getPrefixes() != null && vcard.getStructuredName().getPrefixes().size() > 0) {
      ao.setTitle(vcard.getStructuredName().getPrefixes().get(0));
    }

    for (Address address : vcard.getAddresses()) {
      if (address.getTypes().contains(AddressType.HOME)) {
        ao.setPrivateAddressText(address.getStreetAddressFull());
        ao.setPrivateZipCode(address.getPostalCode());
        ao.setPrivateCity(address.getLocality());
        ao.setPrivateState(address.getRegion());
        ao.setPrivateCountry(address.getCountry());
      }
      if (address.getTypes().contains(AddressType.WORK)) {
        ao.setAddressText(address.getStreetAddressFull());
        ao.setZipCode(address.getPostalCode());
        ao.setCity(address.getLocality());
        ao.setState(address.getRegion());
        ao.setCountry(address.getCountry());
      }
      if (address.getTypes().contains(AddressType.POSTAL)) {
        ao.setPostalAddressText(address.getStreetAddressFull());
        ao.setPostalZipCode(address.getPostalCode());
        ao.setPostalCity(address.getLocality());
        ao.setPostalState(address.getRegion());
        ao.setPostalCountry(address.getCountry());
      }
    }

    for (Telephone telephone : vcard.getTelephoneNumbers()) {
      if (telephone.getTypes().contains(TelephoneType.PAGER)) {
        ao.setPrivateMobilePhone(addCountryCode(telephone.getText()));
      }
      if (telephone.getTypes().contains(TelephoneType.HOME) && telephone.getTypes().contains(TelephoneType.VOICE)) {
        ao.setPrivatePhone(addCountryCode(telephone.getText()));
      }
      if (telephone.getTypes().contains(TelephoneType.CELL) && telephone.getTypes().contains(TelephoneType.VOICE)) {
        ao.setMobilePhone(addCountryCode(telephone.getText()));
      }
      if (telephone.getTypes().contains(TelephoneType.WORK) && telephone.getTypes().contains(TelephoneType.VOICE)) {
        ao.setBusinessPhone(addCountryCode(telephone.getText()));
      }
      if (telephone.getTypes().contains(TelephoneType.WORK) && telephone.getTypes().contains(TelephoneType.FAX)) {
        ao.setFax(addCountryCode(telephone.getText()));
      }
    }

    for (Email email : vcard.getEmails()) {
      if (email.getTypes().contains(EmailType.HOME)) {
        ao.setPrivateEmail(email.getValue());
      }
      if (email.getTypes().contains(EmailType.WORK)) {
        ao.setEmail(email.getValue());
      }
    }

    ao.setWebsite(vcard.getUrls() != null && vcard.getUrls().size() > 0 ? vcard.getUrls().get(0).getValue() : null);

    final Birthday birthday = vcard.getBirthday();
    if (birthday != null) {
      ao.setBirthday(new java.sql.Date(birthday.getDate().getTime()));
    }

    for (Note note : vcard.getNotes()) {
      ao.setComment(ao.getComment() != null ? ao.getComment() : "" + note.getValue() + " ");
    }

    ao.setImage(vcard.getPhotos() != null && vcard.getPhotos().size() > 0 ? Arrays.toString(vcard.getPhotos().get(0).getData()) : null);

    if (vcard.getOrganization() != null && vcard.getOrganization().getValues() != null && vcard.getOrganization().getValues().size() > 0) {
      switch (vcard.getOrganization().getValues().size()) {
        case 3:
          ao.setPositionText(vcard.getOrganization().getValues().get(2));
        case 2:
          ao.setDivision(vcard.getOrganization().getValues().get(1));
        case 1:
          ao.setOrganization(vcard.getOrganization().getValues().get(0));
          break;
      }
    }

    return ao;
  }

  private String addCountryCode(final String phonenumber) {
    String result = "";
    if (phonenumber != null && phonenumber.startsWith("0")) {
      result = phonenumber.replaceFirst("0", "+49");
    } else {
      result = phonenumber;
    }
    return result;
  }

  public VCard getVCardFromByteArray(final byte[] vcardBytearray) {
    ByteArrayInputStream bais = new ByteArrayInputStream(vcardBytearray);
    VCardReader reader = new VCardReader(bais);
    VCard vcard = null;
    try {
      vcard = reader.readNext();
    } catch (IOException e) {
      log.error("An exception accured while parsing vcard from byte array: " + e.getMessage(), e);
    }
    return vcard;
  }

  private Birthday convertBirthday(LocalDate birthday) {
    if (birthday == null) {
      return null;
    }
    final PartialDate date = new PartialDate.Builder().year(birthday.getYear())
            .month(birthday.getMonthValue())
            .date(birthday.getDayOfMonth())
            .build();
    return new Birthday(date);
  }
}
