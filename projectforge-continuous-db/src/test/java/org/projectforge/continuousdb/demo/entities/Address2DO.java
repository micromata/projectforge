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

package org.projectforge.continuousdb.demo.entities;

import java.math.BigDecimal;
import java.sql.Date;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.Table;

/**
 * Version 2.0 (birthday and address were added).
 * @author Kai Reinhard (k.reinhard@micromata.de)
 * 
 */
@Entity
@Table(name = "T_ADDRESS")
public class Address2DO extends DefaultBaseDO
{
  // private static final Logger log = Logger.getLogger(GroupDO.class);

  private String name;

  private String address;

  private String city;

  /** A nonsense field for demo purposes (was of type String in {@link Address1DO} */
  private BigDecimal amount;

  private Date birthday;

  private String description;

  @Column(length = 100)
  public String getName()
  {
    return name;
  }

  /**
   * @param name
   * @return this for chaining.
   */
  public Address2DO setName(final String name)
  {
    this.name = name;
    return this;
  }

  @Column(length = 4000)
  public String getDescription()
  {
    return description;
  }

  /**
   * @param description
   * @return this for chaining.
   */
  public Address2DO setDescription(final String description)
  {
    this.description = description;
    return this;
  }

  @Column(length = 1000)
  public String getAddress()
  {
    return address;
  }

  public void setAddress(String address)
  {
    this.address = address;
  }

  @Column(length = 1000)
  public String getCity()
  {
    return city;
  }

  public void setCity(String city)
  {
    this.city = city;
  }

  /**
   * A nonsense field for demo purposes (was of type String in {@link Address1DO}
   * @return the amount
   */
  @Column(scale = 2, precision = 8)
  public BigDecimal getAmount()
  {
    return amount;
  }

  public void setAmount(BigDecimal amount)
  {
    this.amount = amount;
  }

  @Column
  public Date getBirthday()
  {
    return birthday;
  }

  public void setBirthday(Date birthday)
  {
    this.birthday = birthday;
  }
}
