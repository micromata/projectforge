/////////////////////////////////////////////////////////////////////////////
//
// Project ProjectForge Community Edition
//         www.projectforge.org
//
// Copyright (C) 2001-2025 Micromata GmbH, Germany (www.micromata.com)
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

import org.projectforge.framework.configuration.ApplicationContextProvider;
import org.projectforge.framework.persistence.api.BaseSearchFilter;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Collection;
import java.util.stream.Collectors;

/**
 * @author Kai Reinhard (k.reinhard@micromata.de)
 */
public class AddressFilter extends BaseSearchFilter implements Serializable
{
  public static final String FILTER_DOUBLETS = "doublets";

  public static final String FILTER_FILTER = "filter";

  public static final String FILTER_MY_FAVORITES = "myFavorites";

  public static final String FILTER_NEWEST = "newest";

  private static final long serialVersionUID = -4397408137924906520L;

  private boolean uptodate = true;

  private boolean outdated = false;

  private boolean leaved = false;

  private boolean active = true;

  private boolean nonActive = false;

  private boolean uninteresting = false;

  private boolean personaIngrata = false;

  private boolean departed = false;

  private String listType = FILTER_FILTER;

  private Collection<Long> addressbookIds;

  public AddressFilter()
  {
    reset();
  }

  public AddressFilter(final BaseSearchFilter filter)
  {
    super(filter);
    reset();
    copyBaseSearchFieldsFrom(filter);
    if (filter instanceof AddressFilter) {
      AddressFilter obj = (AddressFilter) filter;
      this.uptodate = obj.isUptodate();
      this.outdated = obj.isOutdated();
      this.leaved = obj.isLeaved();
      this.active = obj.isActive();
      this.nonActive = obj.isNonActive();
      this.uninteresting = obj.isUninteresting();
      this.personaIngrata = obj.isPersonaIngrata();
      this.departed = obj.isDeparted();
    }
  }

  @Override
  public AddressFilter reset()
  {
    super.reset();
    setSortProperty("name").appendSortProperty("firstName");
    setUptodate(true);
    setOutdated(false);
    setLeaved(false);
    setFilter();

    setActive(true);
    setNonActive(false);
    setUninteresting(false);
    setPersonaIngrata(false);
    setDeparted(false);
    return this;
  }

  public boolean isUptodate()
  {
    return uptodate;
  }

  public AddressFilter setUptodate(final boolean uptodate)
  {
    this.uptodate = uptodate;
    return this;
  }

  public boolean isOutdated()
  {
    return outdated;
  }

  public AddressFilter setOutdated(final boolean outdated)
  {
    this.outdated = outdated;
    return this;
  }

  public boolean isLeaved()
  {
    return leaved;
  }

  public AddressFilter setLeaved(final boolean leaved)
  {
    this.leaved = leaved;
    return this;
  }

  public boolean isActive()
  {
    return active;
  }

  public AddressFilter setActive(final boolean active)
  {
    this.active = active;
    return this;
  }

  public boolean isNonActive()
  {
    return nonActive;
  }

  public AddressFilter setNonActive(final boolean nonActive)
  {
    this.nonActive = nonActive;
    return this;
  }

  public boolean isUninteresting()
  {
    return uninteresting;
  }

  public void setUninteresting(final boolean uninteresting)
  {
    this.uninteresting = uninteresting;
  }

  public boolean isPersonaIngrata()
  {
    return personaIngrata;
  }

  public void setPersonaIngrata(final boolean personaIngrata)
  {
    this.personaIngrata = personaIngrata;
  }

  public boolean isDeparted()
  {
    return departed;
  }

  public AddressFilter setDeparted(final boolean departed)
  {
    this.departed = departed;
    return this;
  }

  /**
   * Standard means to consider options: current, departed, uninteresting, personaIngrata, ...
   *
   * @return
   */
  public boolean isFilter()
  {
    return FILTER_FILTER.equals(listType);
  }

  public void setFilter()
  {
    listType = FILTER_FILTER;
  }

  /**
   * If set, only addresses are filtered, the user has marked.
   *
   * @return
   * @see PersonalAddressDO#isFavorite()
   */
  public boolean isMyFavorites()
  {
    return FILTER_MY_FAVORITES.equals(listType);
  }

  public void setMyFavorites()
  {
    listType = FILTER_MY_FAVORITES;
  }

  /**
   * If set, only addresses are filtered which are doublets (name and first-name are equal).
   *
   * @return
   */
  public boolean isDoublets()
  {
    return FILTER_DOUBLETS.equals(listType);
  }

  public void setDoublets()
  {
    listType = FILTER_DOUBLETS;
  }

  /**
   * If set, the 50 (configurable in addressDao) newest address will be shown.
   *
   * @return
   */
  public boolean isNewest()
  {
    return FILTER_NEWEST.equals(listType);
  }

  public void setNewest()
  {
    listType = FILTER_NEWEST;
  }

  public String getListType()
  {
    return listType;
  }

  public void setListType(final String listType)
  {
    this.listType = listType;
  }

  /**
   * @return the addressbooks
   */
  public Collection<AddressbookDO> getAddressbooks()
  {
    Collection<AddressbookDO> result = new ArrayList<>();
    if (addressbookIds != null) {
      for (Long id : addressbookIds) {
        result.add(getAddressbookDao().find(id, false));
      }
    }
    return result;
  }

  public void setAddressbooks(Collection<AddressbookDO> addressbooks)
  {
    if (addressbooks != null) {
      this.addressbookIds = addressbooks.stream().mapToLong(AddressbookDO::getId).boxed().collect(Collectors.toList());
    }
  }

  /**
   * @return the addressbookIds
   */
  public Collection<Long> getAddressbookIds()
  {
    return addressbookIds;
  }

  /**
   * @param addressbookIds the addressbookIds to set
   * @return this for chaining.
   */
  public AddressFilter setAddressbookIds(final Collection<Long> addressbookIds)
  {
    this.addressbookIds = addressbookIds;
    return this;
  }

  private AddressbookDao getAddressbookDao()
  {
    return ApplicationContextProvider.getApplicationContext().getBean(AddressbookDao.class);
  }
}
