package org.projectforge.dto.address;

import java.io.Serializable;
import java.sql.Timestamp;
import java.util.Date;

/**
 * The persistent class for the t_address database table.
 * 
 */
public class AddressDTO implements Serializable
{

  private static final long serialVersionUID = 1L;

  private Integer pk;

  private String addressStatus;

  private String addresstext;

  private Date birthday;

  private String businessPhone;

  private String city;

  private String comment;

  private String communicationLanguage;

  private String contactStatus;

  private String country;

  private Timestamp created;

  private Boolean deleted;

  private String division;

  private String email;

  private String fax;

  private String fingerprint;

  private String firstName;

  private String form;

  private Timestamp lastUpdate;

  private String mobilePhone;

  private String name;

  private String organization;

  private String positiontext;

  private String postalAddresstext;

  private String postalCity;

  private String postalCountry;

  private String postalState;

  private String postalZipCode;

  private String privateAddresstext;

  private String privateCity;

  private String privateCountry;

  private String privateEmail;

  private String privateMobilePhone;

  private String privatePhone;

  private String privateState;

  private String privateZipCode;

  private String publicKey;

  private String state;

  private String title;

  private String website;

  private String zipCode;

  public Integer getPk()
  {
    return this.pk;
  }

  public void setPk(Integer pk)
  {
    this.pk = pk;
  }

  public String getAddressStatus()
  {
    return this.addressStatus;
  }

  public void setAddressStatus(String addressStatus)
  {
    this.addressStatus = addressStatus;
  }

  public String getAddresstext()
  {
    return this.addresstext;
  }

  public void setAddresstext(String addresstext)
  {
    this.addresstext = addresstext;
  }

  public Date getBirthday()
  {
    return this.birthday;
  }

  public void setBirthday(Date birthday)
  {
    this.birthday = birthday;
  }

  public String getBusinessPhone()
  {
    return this.businessPhone;
  }

  public void setBusinessPhone(String businessPhone)
  {
    this.businessPhone = businessPhone;
  }

  public String getCity()
  {
    return this.city;
  }

  public void setCity(String city)
  {
    this.city = city;
  }

  public String getComment()
  {
    return this.comment;
  }

  public void setComment(String comment)
  {
    this.comment = comment;
  }

  public String getCommunicationLanguage()
  {
    return this.communicationLanguage;
  }

  public void setCommunicationLanguage(String communicationLanguage)
  {
    this.communicationLanguage = communicationLanguage;
  }

  public String getContactStatus()
  {
    return this.contactStatus;
  }

  public void setContactStatus(String contactStatus)
  {
    this.contactStatus = contactStatus;
  }

  public String getCountry()
  {
    return this.country;
  }

  public void setCountry(String country)
  {
    this.country = country;
  }

  public Timestamp getCreated()
  {
    return this.created;
  }

  public void setCreated(Timestamp created)
  {
    this.created = created;
  }

  public Boolean getDeleted()
  {
    return this.deleted;
  }

  public void setDeleted(Boolean deleted)
  {
    this.deleted = deleted;
  }

  public String getDivision()
  {
    return this.division;
  }

  public void setDivision(String division)
  {
    this.division = division;
  }

  public String getEmail()
  {
    return this.email;
  }

  public void setEmail(String email)
  {
    this.email = email;
  }

  public String getFax()
  {
    return this.fax;
  }

  public void setFax(String fax)
  {
    this.fax = fax;
  }

  public String getFingerprint()
  {
    return this.fingerprint;
  }

  public void setFingerprint(String fingerprint)
  {
    this.fingerprint = fingerprint;
  }

  public String getFirstName()
  {
    return this.firstName;
  }

  public void setFirstName(String firstName)
  {
    this.firstName = firstName;
  }

  public String getForm()
  {
    return this.form;
  }

  public void setForm(String form)
  {
    this.form = form;
  }

  public Timestamp getLastUpdate()
  {
    return this.lastUpdate;
  }

  public void setLastUpdate(Timestamp lastUpdate)
  {
    this.lastUpdate = lastUpdate;
  }

  public String getMobilePhone()
  {
    return this.mobilePhone;
  }

  public void setMobilePhone(String mobilePhone)
  {
    this.mobilePhone = mobilePhone;
  }

  public String getName()
  {
    return this.name;
  }

  public void setName(String name)
  {
    this.name = name;
  }

  public String getOrganization()
  {
    return this.organization;
  }

  public void setOrganization(String organization)
  {
    this.organization = organization;
  }

  public String getPositiontext()
  {
    return this.positiontext;
  }

  public void setPositiontext(String positiontext)
  {
    this.positiontext = positiontext;
  }

  public String getPostalAddresstext()
  {
    return this.postalAddresstext;
  }

  public void setPostalAddresstext(String postalAddresstext)
  {
    this.postalAddresstext = postalAddresstext;
  }

  public String getPostalCity()
  {
    return this.postalCity;
  }

  public void setPostalCity(String postalCity)
  {
    this.postalCity = postalCity;
  }

  public String getPostalCountry()
  {
    return this.postalCountry;
  }

  public void setPostalCountry(String postalCountry)
  {
    this.postalCountry = postalCountry;
  }

  public String getPostalState()
  {
    return this.postalState;
  }

  public void setPostalState(String postalState)
  {
    this.postalState = postalState;
  }

  public String getPostalZipCode()
  {
    return this.postalZipCode;
  }

  public void setPostalZipCode(String postalZipCode)
  {
    this.postalZipCode = postalZipCode;
  }

  public String getPrivateAddresstext()
  {
    return this.privateAddresstext;
  }

  public void setPrivateAddresstext(String privateAddresstext)
  {
    this.privateAddresstext = privateAddresstext;
  }

  public String getPrivateCity()
  {
    return this.privateCity;
  }

  public void setPrivateCity(String privateCity)
  {
    this.privateCity = privateCity;
  }

  public String getPrivateCountry()
  {
    return this.privateCountry;
  }

  public void setPrivateCountry(String privateCountry)
  {
    this.privateCountry = privateCountry;
  }

  public String getPrivateEmail()
  {
    return this.privateEmail;
  }

  public void setPrivateEmail(String privateEmail)
  {
    this.privateEmail = privateEmail;
  }

  public String getPrivateMobilePhone()
  {
    return this.privateMobilePhone;
  }

  public void setPrivateMobilePhone(String privateMobilePhone)
  {
    this.privateMobilePhone = privateMobilePhone;
  }

  public String getPrivatePhone()
  {
    return this.privatePhone;
  }

  public void setPrivatePhone(String privatePhone)
  {
    this.privatePhone = privatePhone;
  }

  public String getPrivateState()
  {
    return this.privateState;
  }

  public void setPrivateState(String privateState)
  {
    this.privateState = privateState;
  }

  public String getPrivateZipCode()
  {
    return this.privateZipCode;
  }

  public void setPrivateZipCode(String privateZipCode)
  {
    this.privateZipCode = privateZipCode;
  }

  public String getPublicKey()
  {
    return this.publicKey;
  }

  public void setPublicKey(String publicKey)
  {
    this.publicKey = publicKey;
  }

  public String getState()
  {
    return this.state;
  }

  public void setState(String state)
  {
    this.state = state;
  }

  public String getTitle()
  {
    return this.title;
  }

  public void setTitle(String title)
  {
    this.title = title;
  }

  public String getWebsite()
  {
    return this.website;
  }

  public void setWebsite(String website)
  {
    this.website = website;
  }

  public String getZipCode()
  {
    return this.zipCode;
  }

  public void setZipCode(String zipCode)
  {
    this.zipCode = zipCode;
  }

}