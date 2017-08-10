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

package org.projectforge.business.address;

import java.sql.Date;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.StringTokenizer;

import javax.persistence.CascadeType;
import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.EnumType;
import javax.persistence.Enumerated;
import javax.persistence.FetchType;
import javax.persistence.JoinColumn;
import javax.persistence.JoinTable;
import javax.persistence.ManyToMany;
import javax.persistence.MapKey;
import javax.persistence.OneToMany;
import javax.persistence.Table;
import javax.persistence.Transient;

import org.apache.commons.lang.StringUtils;
import org.hibernate.search.annotations.Analyze;
import org.hibernate.search.annotations.DateBridge;
import org.hibernate.search.annotations.EncodingType;
import org.hibernate.search.annotations.Field;
import org.hibernate.search.annotations.FieldBridge;
import org.hibernate.search.annotations.Index;
import org.hibernate.search.annotations.Indexed;
import org.hibernate.search.annotations.Resolution;
import org.hibernate.search.annotations.Store;
import org.projectforge.common.StringHelper;
import org.projectforge.common.anots.PropertyInfo;
import org.projectforge.framework.persistence.attr.entities.DefaultBaseWithAttrDO;
import org.projectforge.framework.persistence.history.HibernateSearchPhoneNumberBridge;
import org.projectforge.framework.persistence.user.api.ThreadLocalUserContext;
import org.projectforge.framework.utils.LabelValueBean;

import de.micromata.genome.db.jpa.history.api.HistoryProperty;
import de.micromata.genome.db.jpa.history.api.NoHistory;
import de.micromata.genome.db.jpa.history.impl.TabAttrHistoryPropertyConverter;
import de.micromata.genome.db.jpa.tabattr.entities.JpaTabAttrBaseDO;
import de.micromata.genome.db.jpa.tabattr.entities.JpaTabAttrDataBaseDO;

/**
 * @author Kai Reinhard (k.reinhard@micromata.de)
 */
@Entity
@Indexed
@Table(name = "T_ADDRESS", indexes = {
    @javax.persistence.Index(name = "idx_fk_t_address_tenant_id", columnList = "tenant_id")
})
@NoHistory
public class AddressDO extends DefaultBaseWithAttrDO<AddressDO>
{
  private static final org.apache.log4j.Logger log = org.apache.log4j.Logger.getLogger(AddressDO.class);

  private static final long serialVersionUID = 974064367925158463L;

  private ContactStatus contactStatus = ContactStatus.ACTIVE;

  private AddressStatus addressStatus = AddressStatus.UPTODATE;

  private String uid;

  @Field()
  private String name; // 255 not null

  @Field(index = Index.YES /* TOKENIZED */, store = Store.NO)
  private String firstName; // 255

  @Field(index = Index.YES /* TOKENIZED */, store = Store.NO)
  private FormOfAddress form;

  @Field(index = Index.YES /* TOKENIZED */, store = Store.NO)
  private String title; // 255

  @Field(index = Index.YES /* TOKENIZED */, store = Store.NO)
  private String positionText; // 255

  @Field(index = Index.YES /* TOKENIZED */, store = Store.NO)
  private String organization; // 255

  @Field(index = Index.YES /* TOKENIZED */, store = Store.NO)
  private String division; // 255

  @FieldBridge(impl = HibernateSearchPhoneNumberBridge.class)
  @Field(index = Index.YES /* TOKENIZED */, store = Store.NO)
  private String businessPhone; // 255

  @FieldBridge(impl = HibernateSearchPhoneNumberBridge.class)
  @Field(index = Index.YES /* TOKENIZED */, store = Store.NO)
  private String mobilePhone; // 255

  @FieldBridge(impl = HibernateSearchPhoneNumberBridge.class)
  @Field(index = Index.YES /* TOKENIZED */, store = Store.NO)
  private String fax; // 255

  @Field(index = Index.YES /* TOKENIZED */, store = Store.NO)
  private String addressText; // 255

  @Field(index = Index.YES /* TOKENIZED */, store = Store.NO)
  private String zipCode; // 255

  @Field(index = Index.YES /* TOKENIZED */, store = Store.NO)
  private String city; // 255

  @Field(index = Index.YES /* TOKENIZED */, store = Store.NO)
  private String country; // 255

  @Field(index = Index.YES /* TOKENIZED */, store = Store.NO)
  private String state; // 255

  @Field(index = Index.YES /* TOKENIZED */, store = Store.NO)
  private String postalAddressText; // 255

  @Field(index = Index.YES /* TOKENIZED */, store = Store.NO)
  private String postalZipCode; // 255

  @Field(index = Index.YES /* TOKENIZED */, store = Store.NO)
  private String postalCity; // 255

  @Field(index = Index.YES /* TOKENIZED */, store = Store.NO)
  private String postalCountry; // 255

  @Field(index = Index.YES /* TOKENIZED */, store = Store.NO)
  private String postalState; // 255

  @Field(index = Index.YES /* TOKENIZED */, store = Store.NO)
  private String email; // 255

  @Field(index = Index.YES /* TOKENIZED */, store = Store.NO)
  private String website; // 255

  private Locale communicationLanguage;

  @FieldBridge(impl = HibernateSearchPhoneNumberBridge.class)
  @Field(index = Index.YES /* TOKENIZED */, store = Store.NO)
  private String privatePhone; // 255

  @FieldBridge(impl = HibernateSearchPhoneNumberBridge.class)
  @Field(index = Index.YES /* TOKENIZED */, store = Store.NO)
  private String privateMobilePhone; // 255

  @Field(index = Index.YES /* TOKENIZED */, store = Store.NO)
  private String privateAddressText; // 255

  @Field(index = Index.YES /* TOKENIZED */, store = Store.NO)
  private String privateZipCode; // 255

  @Field(index = Index.YES /* TOKENIZED */, store = Store.NO)
  private String privateCity; // 255

  @Field(index = Index.YES /* TOKENIZED */, store = Store.NO)
  private String privateCountry; // 255

  @Field(index = Index.YES /* TOKENIZED */, store = Store.NO)
  private String privateState; // 255

  @Field(index = Index.YES /* TOKENIZED */, store = Store.NO)
  private String privateEmail; // 255

  @Field(index = Index.YES /* TOKENIZED */, store = Store.NO)
  private String publicKey; // 7000

  @Field(index = Index.YES /* TOKENIZED */, store = Store.NO)
  private String fingerprint; // 255

  @Field(index = Index.YES /* TOKENIZED */, store = Store.NO)
  private String comment; // 5000;

  @Field(index = Index.YES, analyze = Analyze.NO /* UN_TOKENIZED */)
  @DateBridge(resolution = Resolution.DAY, encoding = EncodingType.STRING)
  private Date birthday;

  @NoHistory
  private byte[] imageData;

  @PropertyInfo(i18nKey = "vacation.substitution")
  private Set<AddressbookDO> addressbookList = new HashSet<>();

  @NoHistory
  private byte[] imageDataPreview;

  // @FieldBridge(impl = HibernateSearchInstantMessagingBridge.class)
  // @Field(index = Index.YES /*TOKENIZED*/, store = Store.NO)
  // TODO: Prepared for hibernate search.
  private List<LabelValueBean<InstantMessagingType, String>> instantMessaging;

  @Enumerated(EnumType.STRING)
  @Column(name = "contact_status", length = 20, nullable = false)
  public ContactStatus getContactStatus()
  {
    return contactStatus;
  }

  public AddressDO setContactStatus(final ContactStatus contactStatus)
  {
    this.contactStatus = contactStatus;
    return this;
  }

  @Enumerated(EnumType.STRING)
  @Column(name = "address_status", length = 20, nullable = false)
  public AddressStatus getAddressStatus()
  {
    return addressStatus;
  }

  public AddressDO setAddressStatus(final AddressStatus addressStatus)
  {
    this.addressStatus = addressStatus;
    return this;
  }

  @Column(name = "business_phone", length = 255)
  public String getBusinessPhone()
  {
    return businessPhone;
  }

  public AddressDO setBusinessPhone(final String businessPhone)
  {
    this.businessPhone = businessPhone;
    return this;
  }

  @Column(name = "uid")
  public String getUid()
  {
    return uid;
  }

  public AddressDO setUid(final String uid)
  {
    this.uid = uid;
    return this;
  }

  @Column(name = "mobile_phone", length = 255)
  public String getMobilePhone()
  {
    return mobilePhone;
  }

  public AddressDO setMobilePhone(final String mobilePhone)
  {
    this.mobilePhone = mobilePhone;
    return this;
  }

  @Column(length = 255)
  public String getFax()
  {
    return fax;
  }

  public void setFax(final String fax)
  {
    this.fax = fax;
  }

  @Column(length = 255)
  public String getAddressText()
  {
    return addressText;
  }

  public void setAddressText(final String addressText)
  {
    this.addressText = addressText;
  }

  @Column(name = "zip_code", length = 255)
  public String getZipCode()
  {
    return zipCode;
  }

  public void setZipCode(final String zipCode)
  {
    this.zipCode = zipCode;
  }

  @Column(length = 255)
  public String getCity()
  {
    return city;
  }

  public void setCity(final String city)
  {
    this.city = city;
  }

  @Column(length = 255)
  public String getCountry()
  {
    return country;
  }

  public void setCountry(final String country)
  {
    this.country = country;
  }

  @Column(length = 255)
  public String getState()
  {
    return state;
  }

  public void setState(final String state)
  {
    this.state = state;
  }

  @Column(length = 255, name = "postal_addresstext")
  public String getPostalAddressText()
  {
    return postalAddressText;
  }

  public void setPostalAddressText(final String postalAddressText)
  {
    this.postalAddressText = postalAddressText;
  }

  @Column(name = "postal_zip_code", length = 255)
  public String getPostalZipCode()
  {
    return postalZipCode;
  }

  public void setPostalZipCode(final String postalZipCode)
  {
    this.postalZipCode = postalZipCode;
  }

  @Column(length = 255, name = "postal_city")
  public String getPostalCity()
  {
    return postalCity;
  }

  public void setPostalCity(final String postalCity)
  {
    this.postalCity = postalCity;
  }

  @Column(name = "postal_country", length = 255)
  public String getPostalCountry()
  {
    return postalCountry;
  }

  public void setPostalCountry(final String postalCountry)
  {
    this.postalCountry = postalCountry;
  }

  @Column(name = "postal_state", length = 255)
  public String getPostalState()
  {
    return postalState;
  }

  public void setPostalState(final String postalState)
  {
    this.postalState = postalState;
  }

  @Column
  public Date getBirthday()
  {
    return birthday;
  }

  public void setBirthday(final Date birthday)
  {
    this.birthday = birthday;
  }

  @Column(name = "comment", length = 5000)
  public String getComment()
  {
    return comment;
  }

  public AddressDO setComment(final String comment)
  {
    this.comment = comment;
    return this;
  }

  @Column(length = 255)
  public String getEmail()
  {
    return email;
  }

  public AddressDO setEmail(final String email)
  {
    this.email = email;
    return this;
  }

  @Column(length = 255)
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
  @Column(name = "communication_language")
  public Locale getCommunicationLanguage()
  {
    return communicationLanguage;
  }

  public void setCommunicationLanguage(final Locale communicationLanguage)
  {
    this.communicationLanguage = communicationLanguage;
  }

  @Column(length = 255)
  public String getFingerprint()
  {
    return fingerprint;
  }

  public void setFingerprint(final String fingerprint)
  {
    this.fingerprint = fingerprint;
  }

  @Column(name = "first_name", length = 255)
  public String getFirstName()
  {
    return firstName;
  }

  public AddressDO setFirstName(final String firstName)
  {
    this.firstName = firstName;
    return this;
  }

  @Transient
  public String getFullName()
  {
    return StringHelper.listToString(", ", name, firstName);
  }

  @Transient
  public String getFullNameWithTitleAndForm()
  {
    final StringBuffer buf = new StringBuffer();
    if (getForm() != null) {
      buf.append(ThreadLocalUserContext.getLocalizedString(getForm().getI18nKey())).append(" ");
    }
    if (getTitle() != null) {
      buf.append(getTitle()).append(" ");
    }
    if (getFirstName() != null) {
      buf.append(getFirstName()).append(" ");
    }
    if (getName() != null) {
      buf.append(getName());
    }
    return buf.toString();
  }

  @Column(length = 255)
  public String getName()
  {
    return name;
  }

  public AddressDO setName(final String name)
  {
    this.name = name;
    return this;
  }

  @Enumerated(EnumType.STRING)
  @Column(name = "form", length = 10)
  public FormOfAddress getForm()
  {
    return form;
  }

  public AddressDO setForm(final FormOfAddress form)
  {
    this.form = form;
    return this;
  }

  @Column(length = 255)
  public String getOrganization()
  {
    return organization;
  }

  public AddressDO setOrganization(final String organization)
  {
    this.organization = organization;
    return this;
  }

  @Column(length = 255)
  public String getDivision()
  {
    return division;
  }

  public void setDivision(final String division)
  {
    this.division = division;
  }

  @Column(length = 255)
  public String getPositionText()
  {
    return positionText;
  }

  public void setPositionText(final String positionText)
  {
    this.positionText = positionText;
  }

  @Column(name = "private_phone", length = 255)
  public String getPrivatePhone()
  {
    return privatePhone;
  }

  public AddressDO setPrivatePhone(final String privatePhone)
  {
    this.privatePhone = privatePhone;
    return this;
  }

  @Column(name = "private_mobile_phone", length = 255)
  public String getPrivateMobilePhone()
  {
    return privateMobilePhone;
  }

  public void setPrivateMobilePhone(final String mobilePhone)
  {
    this.privateMobilePhone = mobilePhone;
  }

  @Column(length = 255, name = "private_addresstext")
  public String getPrivateAddressText()
  {
    return privateAddressText;
  }

  public void setPrivateAddressText(final String privateAddressText)
  {
    this.privateAddressText = privateAddressText;
  }

  @Column(name = "private_zip_code", length = 255)
  public String getPrivateZipCode()
  {
    return privateZipCode;
  }

  public void setPrivateZipCode(final String zipCode)
  {
    this.privateZipCode = zipCode;
  }

  @Column(length = 255, name = "private_city")
  public String getPrivateCity()
  {
    return privateCity;
  }

  public void setPrivateCity(final String city)
  {
    this.privateCity = city;
  }

  @Column(name = "private_country", length = 255)
  public String getPrivateCountry()
  {
    return privateCountry;
  }

  public void setPrivateCountry(final String privateCountry)
  {
    this.privateCountry = privateCountry;
  }

  @Column(name = "private_state", length = 255)
  public String getPrivateState()
  {
    return privateState;
  }

  public void setPrivateState(final String privateState)
  {
    this.privateState = privateState;
  }

  @Column(length = 255, name = "private_email")
  public String getPrivateEmail()
  {
    return privateEmail;
  }

  public AddressDO setPrivateEmail(final String email)
  {
    this.privateEmail = email;
    return this;
  }

  @Column(name = "public_key", length = 20000)
  public String getPublicKey()
  {
    return publicKey;
  }

  public void setPublicKey(final String publicKey)
  {
    this.publicKey = publicKey;
  }

  @Column(length = 255)
  public String getTitle()
  {
    return title;
  }

  public AddressDO setTitle(final String title)
  {
    this.title = title;
    return this;
  }

  /**
   * @return address text of mailing address (in order: postal, default or private address).
   * @see #hasPostalAddress()
   * @see #hasDefaultAddress()
   */
  @Transient
  public String getMailingAddressText()
  {
    if (hasPostalAddress() == true) {
      return getPostalAddressText();
    } else if (hasDefaultAddress() == true) {
      return getAddressText();
    } else {
      return getPrivateAddressText();
    }
  }

  /**
   * @return zip code of mailing address (in order: postal, default or private address).
   * @see #hasPostalAddress()
   * @see #hasDefaultAddress()
   */
  @Transient
  public String getMailingZipCode()
  {
    if (hasPostalAddress() == true) {
      return getPostalZipCode();
    } else if (hasDefaultAddress() == true) {
      return getZipCode();
    } else {
      return getPrivateZipCode();
    }
  }

  /**
   * @return city of mailing address (in order: postal, default or private address).
   * @see #hasPostalAddress()
   * @see #hasDefaultAddress()
   */
  @Transient
  public String getMailingCity()
  {
    if (hasPostalAddress() == true) {
      return getPostalCity();
    } else if (hasDefaultAddress() == true) {
      return getCity();
    } else {
      return getPrivateCity();
    }
  }

  /**
   * @return country of mailing address (in order: postal, default or private address).
   * @see #hasPostalAddress()
   * @see #hasDefaultAddress()
   */
  @Transient
  public String getMailingCountry()
  {
    if (hasPostalAddress() == true) {
      return getPostalCountry();
    } else if (hasDefaultAddress() == true) {
      return getCountry();
    } else {
      return getPrivateCountry();
    }
  }

  /**
   * @return state of mailing address (in order: postal, default or private address).
   * @see #hasPostalAddress()
   * @see #hasDefaultAddress()
   */
  @Transient
  public String getMailingState()
  {
    if (hasPostalAddress() == true) {
      return getPostalState();
    } else if (hasDefaultAddress() == true) {
      return getState();
    } else {
      return getPrivateState();
    }
  }

  /**
   * @return true, if postal addressText, zip code, city or country is given.
   */
  @Transient
  public boolean hasPostalAddress()
  {
    return StringHelper.isNotBlank(getPostalAddressText(), getPostalZipCode(), getPostalCity(), getPostalCountry());
  }

  /**
   * @return true, if default addressText, zip code, city or country is given.
   */
  @Transient
  public boolean hasDefaultAddress()
  {
    return StringHelper.isNotBlank(getAddressText(), getZipCode(), getCity(), getCountry());
  }

  /**
   * @return true, if private addressText, zip code, city or country is given.
   */
  @Transient
  public boolean hasPrivateAddress()
  {
    return StringHelper.isNotBlank(getPrivateAddressText(), getPrivateZipCode(), getPrivateCity(), getPrivateCountry());
  }

  /**
   * List of instant messaging contacts in the form of a property file: {skype=hugo.mustermann\naim=12345dse}. Only for
   * data base access, use getter an setter of instant messaging instead.
   *
   * @return
   */
  // @Column(name = "instant_messaging", length = 4000)
  @Transient
  // TODO: Prepared for data base persistence.
  public String getInstantMessaging4DB()
  {
    return getInstantMessagingAsString(instantMessaging);
  }

  public void setInstantMessaging4DB(final String properties)
  {
    if (StringUtils.isBlank(properties) == true) {
      this.instantMessaging = null;
    } else {
      final StringTokenizer tokenizer = new StringTokenizer(properties, "\n");
      while (tokenizer.hasMoreTokens() == true) {
        final String line = tokenizer.nextToken();
        if (StringUtils.isBlank(line) == true) {
          continue;
        }
        final int idx = line.indexOf('=');
        if (idx <= 0) {
          log.error("Wrong instant messaging entry format in data base: " + line);
          continue;
        }
        String label = line.substring(0, idx);
        final String value = "";
        if (idx < line.length()) {
          label = line.substring(idx);
        }
        InstantMessagingType type = null;
        try {
          type = InstantMessagingType.get(label);
        } catch (final Exception ex) {
          log.error("Ignoring unknown Instant Messaging entry: " + label, ex);
          continue;
        }
        setInstantMessaging(type, value);
      }
    }
  }

  /**
   * Instant messaging settings as property file.
   *
   * @return
   */
  @Transient
  public List<LabelValueBean<InstantMessagingType, String>> getInstantMessaging()
  {
    return instantMessaging;
  }

  public void setInstantMessaging(final InstantMessagingType type, final String value)
  {
    if (this.instantMessaging == null) {
      this.instantMessaging = new ArrayList<LabelValueBean<InstantMessagingType, String>>();
    } else {
      for (final LabelValueBean<InstantMessagingType, String> entry : this.instantMessaging) {
        if (entry.getLabel() == type) {
          // Entry found;
          if (StringUtils.isBlank(value) == true) {
            // Remove this entry:
            this.instantMessaging.remove(entry);
          } else {
            // Modify existing entry:
            entry.setValue(value);
          }
          return;
        }
      }
    }
    this.instantMessaging.add(new LabelValueBean<InstantMessagingType, String>(type, value));
  }

  /**
   * Used for representation in the data base and for hibernate search (lucene).
   */
  static String getInstantMessagingAsString(final List<LabelValueBean<InstantMessagingType, String>> list)
  {
    if (list == null || list.size() == 0) {
      return null;
    }
    final StringBuffer buf = new StringBuffer();
    boolean first = true;
    for (final LabelValueBean<InstantMessagingType, String> lv : list) {
      if (StringUtils.isBlank(lv.getValue()) == true) {
        continue; // Do not write empty entries.
      }
      if (first == true) {
        first = false;
      } else {
        buf.append("\n");
      }
      buf.append(lv.getLabel()).append("=").append(lv.getValue());
    }
    if (first == true) {
      return null; // No entry was written.
    }
    return buf.toString();
  }

  /**
   * @see org.projectforge.framework.persistence.attr.entities.DefaultBaseWithAttrDO#getAttrEntityClass()
   */
  @Override
  @Transient
  public Class<? extends JpaTabAttrBaseDO<AddressDO, Integer>> getAttrEntityClass()
  {
    return AddressAttrDO.class;
  }

  /**
   * @see org.projectforge.framework.persistence.attr.entities.DefaultBaseWithAttrDO#getAttrEntityWithDataClass()
   */
  @Override
  @Transient
  public Class<? extends JpaTabAttrBaseDO<AddressDO, Integer>> getAttrEntityWithDataClass()
  {
    return AddressAttrWithDataDO.class;
  }

  /**
   * @see org.projectforge.framework.persistence.attr.entities.DefaultBaseWithAttrDO#getAttrDataEntityClass()
   */
  @Override
  @Transient
  public Class<? extends JpaTabAttrDataBaseDO<? extends JpaTabAttrBaseDO<AddressDO, Integer>, Integer>> getAttrDataEntityClass()
  {
    return AddressAttrDataDO.class;
  }

  /**
   * @see org.projectforge.framework.persistence.attr.entities.DefaultBaseWithAttrDO#createAttrEntity(java.lang.String,
   * char, java.lang.String)
   */
  @Override
  public JpaTabAttrBaseDO<AddressDO, Integer> createAttrEntity(String key, char type, String value)
  {
    return new AddressAttrDO(this, key, type, value);
  }

  /**
   * @see org.projectforge.framework.persistence.attr.entities.DefaultBaseWithAttrDO#createAttrEntityWithData(java.lang.String,
   * char, java.lang.String)
   */
  @Override
  public JpaTabAttrBaseDO<AddressDO, Integer> createAttrEntityWithData(String key, char type, String value)
  {
    return new AddressAttrWithDataDO(this, key, type, value);
  }

  @OneToMany(cascade = CascadeType.ALL, mappedBy = "parent", targetEntity = AddressAttrDO.class,
      orphanRemoval = true,
      fetch = FetchType.EAGER)
  @MapKey(name = "propertyName")
  @Override
  @HistoryProperty(converter = TabAttrHistoryPropertyConverter.class)
  public Map<String, JpaTabAttrBaseDO<AddressDO, Integer>> getAttrs()
  {
    return super.getAttrs();
  }

  @Column(length = 4000)
  public byte[] getImageData()
  {
    return imageData;
  }

  public void setImageData(final byte[] imageData)
  {
    this.imageData = imageData;
  }

  /**
   * The substitutions.
   *
   * @return the substitutions
   */
  @ManyToMany
  @JoinTable(
      name = "t_addressbook_address",
      joinColumns = @JoinColumn(name = "address_id", referencedColumnName = "PK"),
      inverseJoinColumns = @JoinColumn(name = "addressbook_id", referencedColumnName = "PK"),
      indexes = {
          @javax.persistence.Index(name = "idx_fk_t_addressbook_address_address_id", columnList = "address_id"),
          @javax.persistence.Index(name = "idx_fk_t_addressbook_address_addressbook_id", columnList = "addressbook_id")
      }
  )
  public Set<AddressbookDO> getAddressbookList()
  {
    return addressbookList;
  }

  /**
   * @param addressbookList the addressbookList to set
   */
  public void setAddressbookList(final Set<AddressbookDO> addressbookList)
  {
    this.addressbookList = addressbookList;
  }

  @Column(name = "image_data_preview", length = 1000)
  public byte[] getImageDataPreview()
  {
    return imageDataPreview;
  }

  public void setImageDataPreview(final byte[] imageDataPreview)
  {
    this.imageDataPreview = imageDataPreview;
  }
}
