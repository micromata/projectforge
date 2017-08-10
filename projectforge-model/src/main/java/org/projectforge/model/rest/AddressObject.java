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

package org.projectforge.model.rest;

import java.sql.Date;
import java.util.Locale;

import org.projectforge.common.StringHelper;

/**
 * For documentation please refer the ProjectForge-API: AddressDO object. AddressObject object for REST,
 *
 * @author Kai Reinhard (k.reinhard@micromata.de)
 */
public class AddressObject extends AbstractBaseObject
{
  private String uid;

  private String contactStatus;

  private String addressStatus;

  private String name;

  private String firstName;

  private String form;

  private String title;

  private String positionText;

  private String organization;

  private String division;

  private String businessPhone;

  private String mobilePhone;

  private String fax;

  private String addressText;

  private String zipCode;

  private String city;

  private String country;

  private String state;

  private String postalAddressText;

  private String postalZipCode;

  private String postalCity;

  private String postalCountry;

  private String postalState;

  private String email;

  private String website;

  private Locale communicationLanguage;

  private String privatePhone;

  private String privateMobilePhone;

  private String privateAddressText;

  private String privateZipCode;

  private String privateCity;

  private String privateCountry;

  private String privateState;

  private String privateEmail;

  private String publicKey;

  private String fingerprint;

  private String comment;

  private Date birthday;

  private String image;

  private String vCardData;

  public String getUid()
  {
    return uid;
  }

  public void setUid(final String uid)
  {
    this.uid = uid;
  }

  public String getContactStatus()
  {
    return contactStatus;
  }

  public AddressObject setContactStatus(final String contactStatus)
  {
    this.contactStatus = contactStatus;
    return this;
  }

  public String getAddressStatus()
  {
    return addressStatus;
  }

  public AddressObject setAddressStatus(final String addressStatus)
  {
    this.addressStatus = addressStatus;
    return this;
  }

  public String getBusinessPhone()
  {
    return businessPhone;
  }

  public AddressObject setBusinessPhone(final String businessPhone)
  {
    this.businessPhone = businessPhone;
    return this;
  }

  public String getMobilePhone()
  {
    return mobilePhone;
  }

  public AddressObject setMobilePhone(final String mobilePhone)
  {
    this.mobilePhone = mobilePhone;
    return this;
  }

  public String getFax()
  {
    return fax;
  }

  public void setFax(final String fax)
  {
    this.fax = fax;
  }

  public String getAddressText()
  {
    return addressText;
  }

  public void setAddressText(final String addressText)
  {
    this.addressText = addressText;
  }

  public String getZipCode()
  {
    return zipCode;
  }

  public void setZipCode(final String zipCode)
  {
    this.zipCode = zipCode;
  }

  public String getCity()
  {
    return city;
  }

  public void setCity(final String city)
  {
    this.city = city;
  }

  public String getCountry()
  {
    return country;
  }

  public void setCountry(final String country)
  {
    this.country = country;
  }

  public String getState()
  {
    return state;
  }

  public void setState(final String state)
  {
    this.state = state;
  }

  public String getPostalAddressText()
  {
    return postalAddressText;
  }

  public void setPostalAddressText(final String postalAddressText)
  {
    this.postalAddressText = postalAddressText;
  }

  public String getPostalZipCode()
  {
    return postalZipCode;
  }

  public void setPostalZipCode(final String postalZipCode)
  {
    this.postalZipCode = postalZipCode;
  }

  public String getPostalCity()
  {
    return postalCity;
  }

  public void setPostalCity(final String postalCity)
  {
    this.postalCity = postalCity;
  }

  public String getPostalCountry()
  {
    return postalCountry;
  }

  public void setPostalCountry(final String postalCountry)
  {
    this.postalCountry = postalCountry;
  }

  public String getPostalState()
  {
    return postalState;
  }

  public void setPostalState(final String postalState)
  {
    this.postalState = postalState;
  }

  public Date getBirthday()
  {
    return birthday;
  }

  public void setBirthday(final Date birthday)
  {
    this.birthday = birthday;
  }

  public String getComment()
  {
    return comment;
  }

  public AddressObject setComment(final String comment)
  {
    this.comment = comment;
    return this;
  }

  public String getEmail()
  {
    return email;
  }

  public AddressObject setEmail(final String email)
  {
    this.email = email;
    return this;
  }

  public String getWebsite()
  {
    return website;
  }

  public void setWebsite(final String website)
  {
    this.website = website;
  }

  /**
   * @return The communication will take place in this language.
   */
  public Locale getCommunicationLanguage()
  {
    return communicationLanguage;
  }

  public void setCommunicationLanguage(final Locale communicationLanguage)
  {
    this.communicationLanguage = communicationLanguage;
  }

  public String getFingerprint()
  {
    return fingerprint;
  }

  public void setFingerprint(final String fingerprint)
  {
    this.fingerprint = fingerprint;
  }

  public String getFirstName()
  {
    return firstName;
  }

  public AddressObject setFirstName(final String firstName)
  {
    this.firstName = firstName;
    return this;
  }

  public String getFullName()
  {
    return StringHelper.listToString(", ", name, firstName);
  }

  public String getName()
  {
    return name;
  }

  public AddressObject setName(final String name)
  {
    this.name = name;
    return this;
  }

  public String getForm()
  {
    return form;
  }

  public AddressObject setForm(final String form)
  {
    this.form = form;
    return this;
  }

  public String getOrganization()
  {
    return organization;
  }

  public AddressObject setOrganization(final String organization)
  {
    this.organization = organization;
    return this;
  }

  public String getDivision()
  {
    return division;
  }

  public void setDivision(final String division)
  {
    this.division = division;
  }

  public String getPositionText()
  {
    return positionText;
  }

  public void setPositionText(final String positionText)
  {
    this.positionText = positionText;
  }

  public String getPrivatePhone()
  {
    return privatePhone;
  }

  public AddressObject setPrivatePhone(final String privatePhone)
  {
    this.privatePhone = privatePhone;
    return this;
  }

  public String getPrivateMobilePhone()
  {
    return privateMobilePhone;
  }

  public void setPrivateMobilePhone(final String mobilePhone)
  {
    this.privateMobilePhone = mobilePhone;
  }

  public String getPrivateAddressText()
  {
    return privateAddressText;
  }

  public void setPrivateAddressText(final String privateAddressText)
  {
    this.privateAddressText = privateAddressText;
  }

  public String getPrivateZipCode()
  {
    return privateZipCode;
  }

  public void setPrivateZipCode(final String zipCode)
  {
    this.privateZipCode = zipCode;
  }

  public String getPrivateCity()
  {
    return privateCity;
  }

  public void setPrivateCity(final String city)
  {
    this.privateCity = city;
  }

  public String getPrivateCountry()
  {
    return privateCountry;
  }

  public void setPrivateCountry(final String privateCountry)
  {
    this.privateCountry = privateCountry;
  }

  public String getPrivateState()
  {
    return privateState;
  }

  public void setPrivateState(final String privateState)
  {
    this.privateState = privateState;
  }

  public String getPrivateEmail()
  {
    return privateEmail;
  }

  public AddressObject setPrivateEmail(final String email)
  {
    this.privateEmail = email;
    return this;
  }

  public String getPublicKey()
  {
    return publicKey;
  }

  public void setPublicKey(final String publicKey)
  {
    this.publicKey = publicKey;
  }

  public String getTitle()
  {
    return title;
  }

  public AddressObject setTitle(final String title)
  {
    this.title = title;
    return this;
  }

  /**
   * Base64 data String
   *
   * @return
   */
  public String getImage()
  {
    return image;
  }

  public void setImage(String image)
  {
    this.image = image;
  }

  /**
   * Base64 data String
   *
   * @return
   */
  public String getVCardData()
  {
    return vCardData;
  }

  public void setVCardData(String vCardData)
  {
    this.vCardData = vCardData;
  }
}
