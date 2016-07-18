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

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.FetchType;
import javax.persistence.GeneratedValue;
import javax.persistence.Id;
import javax.persistence.JoinColumn;
import javax.persistence.ManyToOne;
import javax.persistence.Table;
import javax.persistence.Transient;
import javax.persistence.UniqueConstraint;

import org.hibernate.search.annotations.Indexed;
import org.projectforge.framework.persistence.entities.AbstractBaseDO;
import org.projectforge.framework.persistence.user.entities.PFUserDO;

/**
 * Every user has his own address book (a subset of all addresses). For every address a user can define which phone
 * numbers he wants to add to his address book and/or the whole address.
 * 
 * @author Kai Reinhard (k.reinhard@micromata.de)
 */
@Entity
@Indexed
@Table(name = "T_PERSONAL_ADDRESS",
    uniqueConstraints = {
        @UniqueConstraint(columnNames = { "owner_id", "address_id" })
    },
    indexes = {
        @javax.persistence.Index(name = "idx_fk_t_personal_address_address_id", columnList = "address_id"),
        @javax.persistence.Index(name = "idx_fk_t_personal_address_owner_id", columnList = "owner_id"),
        @javax.persistence.Index(name = "idx_fk_t_personal_address_tenant_id", columnList = "tenant_id")
    })
public class PersonalAddressDO extends AbstractBaseDO<Integer>
{
  private static final long serialVersionUID = -4846807837820504068L;

  private Integer id;

  private AddressDO address;

  private boolean favoriteCard;

  private boolean favoriteBusinessPhone;

  private boolean favoritePrivatePhone;

  private boolean favoriteMobilePhone;

  private boolean favoritePrivateMobilePhone;

  private boolean favoriteFax;

  private PFUserDO owner;

  /**
   * @param entry
   * @return true, if any checkbox is set (isFavoriteCard, isBusinessPhone etc.)
   */
  @Transient
  public boolean isFavorite()
  {
    return (isFavoriteCard() == true || isFavoriteBusinessPhone() == true || isFavoriteFax() == true
        || isFavoriteMobilePhone() == true || isFavoritePrivatePhone() == true);
  }

  @Id
  @GeneratedValue
  @Column(name = "pk")
  public Integer getId()
  {
    return id;
  }

  public void setId(final Integer id)
  {
    this.id = id;
  }

  /**
   * Not used as object due to performance reasons.
   * 
   * @return
   */
  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "owner_id", nullable = false)
  public PFUserDO getOwner()
  {
    return owner;
  }

  public void setOwner(final PFUserDO owner)
  {
    this.owner = owner;
  }

  @Transient
  public Integer getOwnerId()
  {
    if (this.owner == null)
      return null;
    return owner.getId();
  }

  /**
   * Not used as object due to performance reasons.
   * 
   * @return
   */
  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "address_id", nullable = false)
  public AddressDO getAddress()
  {
    return address;
  }

  public void setAddress(final AddressDO address)
  {
    this.address = address;
  }

  @Transient
  public Integer getAddressId()
  {
    if (this.address == null)
      return null;
    return address.getId();
  }

  /**
   * If true, the whole address will be exported as vCard.
   * 
   * @return
   */
  @Column(nullable = false, name = "favorite_card")
  public boolean isFavoriteCard()
  {
    return favoriteCard;
  }

  public void setFavoriteCard(final boolean favoriteCard)
  {
    this.favoriteCard = favoriteCard;
  }

  @Column(name = "business_phone", nullable = false)
  public boolean isFavoriteBusinessPhone()
  {
    return favoriteBusinessPhone;
  }

  public void setFavoriteBusinessPhone(final boolean favoriteBusinessPhone)
  {
    this.favoriteBusinessPhone = favoriteBusinessPhone;
  }

  @Column(name = "private_phone", nullable = false)
  public boolean isFavoritePrivatePhone()
  {
    return favoritePrivatePhone;
  }

  public void setFavoritePrivatePhone(final boolean favoritePrivatePhone)
  {
    this.favoritePrivatePhone = favoritePrivatePhone;
  }

  @Column(name = "mobile_phone", nullable = false)
  public boolean isFavoriteMobilePhone()
  {
    return favoriteMobilePhone;
  }

  public void setFavoriteMobilePhone(final boolean favoriteMobilePhone)
  {
    this.favoriteMobilePhone = favoriteMobilePhone;
  }

  @Column(name = "private_mobile_phone", nullable = false)
  public boolean isFavoritePrivateMobilePhone()
  {
    return favoritePrivateMobilePhone;
  }

  public void setFavoritePrivateMobilePhone(final boolean favoritePrivateMobilePhone)
  {
    this.favoritePrivateMobilePhone = favoritePrivateMobilePhone;
  }

  @Column(nullable = false, name = "fax")
  public boolean isFavoriteFax()
  {
    return favoriteFax;
  }

  public void setFavoriteFax(final boolean favoriteFax)
  {
    this.favoriteFax = favoriteFax;
  }
}
