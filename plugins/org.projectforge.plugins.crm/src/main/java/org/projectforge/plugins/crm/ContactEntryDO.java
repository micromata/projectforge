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

package org.projectforge.plugins.crm;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.EnumType;
import javax.persistence.Enumerated;
import javax.persistence.FetchType;
import javax.persistence.JoinColumn;
import javax.persistence.ManyToOne;
import javax.persistence.Table;
import javax.persistence.Transient;
import javax.persistence.UniqueConstraint;

import java.util.Objects;
import org.apache.commons.lang3.builder.HashCodeBuilder;
import org.hibernate.search.annotations.Field;
import org.hibernate.search.annotations.Index;
import org.hibernate.search.annotations.Indexed;
import org.hibernate.search.annotations.Store;
import org.projectforge.common.anots.PropertyInfo;
import org.projectforge.framework.persistence.entities.DefaultBaseDO;

/**
 * @author Werner Feder (werner.feder@t-online.de)
 */
@Entity
@Indexed
@Table(name = "T_CONTACTENTRY", uniqueConstraints = {
    @UniqueConstraint(columnNames = { "contact_id", "number" }) }, indexes = {
    @javax.persistence.Index(name = "idx_fk_t_contactentry_tenant_id", columnList = "tenant_id")
})
public class ContactEntryDO extends DefaultBaseDO
{
  private static final long serialVersionUID = -8141697905834021747L;

  //private static final org.slf4j.Logger log = org.slf4j.LoggerFactory.getLogger(ContactEntryDO.class);

  private short number;

  private ContactDO contact;

  @PropertyInfo(i18nKey = "contactType")
  @Enumerated(EnumType.STRING)
  @Field(index = Index.YES /*TOKENIZED*/, store = Store.NO)
  private ContactType contactType; // 15

  @PropertyInfo(i18nKey = "city")
  @Field(index = Index.YES /*TOKENIZED*/, store = Store.NO)
  private String city;

  @PropertyInfo(i18nKey = "country")
  @Field(index = Index.YES /*TOKENIZED*/, store = Store.NO)
  private String country;

  @PropertyInfo(i18nKey = "state")
  @Field(index = Index.YES /*TOKENIZED*/, store = Store.NO)
  private String state;

  @PropertyInfo(i18nKey = "street")
  @Field(index = Index.YES /*TOKENIZED*/, store = Store.NO)
  private String street;

  @PropertyInfo(i18nKey = "zipCode")
  @Field(index = Index.YES /*TOKENIZED*/, store = Store.NO)
  private String zipCode;

  /**
   * Not used as object due to performance reasons.
   *
   * @return
   */
  @ManyToOne(fetch = FetchType.EAGER)
  @JoinColumn(name = "contact_id", nullable = false)
  public ContactDO getContact()
  {
    return contact;
  }

  public ContactEntryDO setContact(final ContactDO contact)
  {
    this.contact = contact;
    return this;
  }

  @Transient
  public Integer getContactId()
  {
    if (this.contact == null)
      return null;
    return contact.getId();
  }

  @Enumerated(EnumType.STRING)
  @Column(length = 15, name = "contact_type", nullable = false)
  public ContactType getContactType()
  {
    return contactType;
  }

  /**
   * @return this for chaining.
   */
  public ContactEntryDO setContactType(final ContactType contactType)
  {
    this.contactType = contactType;
    return this;
  }

  /**
   * @return the city
   */
  @Column
  public String getCity()
  {
    return city;
  }

  /**
   * @param city the city to set
   * @return this for chaining.
   */
  public ContactEntryDO setCity(final String city)
  {
    this.city = city;
    return this;
  }

  /**
   * @return the country
   */
  @Column
  public String getCountry()
  {
    return country;
  }

  /**
   * @param country the country to set
   * @return this for chaining.
   */
  public ContactEntryDO setCountry(final String country)
  {
    this.country = country;
    return this;
  }

  /**
   * @return the state
   */
  @Column
  public String getState()
  {
    return state;
  }

  /**
   * @param state the state to set
   * @return this for chaining.
   */
  public ContactEntryDO setState(final String state)
  {
    this.state = state;
    return this;
  }

  /**
   * @return the street
   */
  @Column
  public String getStreet()
  {
    return street;
  }

  /**
   * @param street the street to set
   * @return this for chaining.
   */
  public ContactEntryDO setStreet(final String street)
  {
    this.street = street;
    return this;
  }

  /**
   * @return the zipCode
   */
  @Column
  public String getZipCode()
  {
    return zipCode;
  }

  /**
   * @param zipCode the zipCode to set
   * @return this for chaining.
   */
  public ContactEntryDO setZipCode(final String zipCode)
  {
    this.zipCode = zipCode;
    return this;
  }

  @Column
  public short getNumber()
  {
    return number;
  }

  public ContactEntryDO setNumber(final short number)
  {
    this.number = number;
    return this;
  }

  @Override
  public boolean equals(final Object o)
  {
    if (o instanceof ContactEntryDO) {
      final ContactEntryDO other = (ContactEntryDO) o;
      if (Objects.equals(this.getNumber(), other.getNumber()) == false) {
        return false;
      }
      if (Objects.equals(this.getContactId(), other.getContactId()) == false) {
        return false;
      }
      return true;
    }
    return false;
  }

  @Override
  public int hashCode()
  {
    final HashCodeBuilder hcb = new HashCodeBuilder();
    hcb.append(getNumber());
    if (getContact() != null) {
      hcb.append(getContact().getId());
    }
    return hcb.toHashCode();
  }
}
