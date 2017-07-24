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

package org.projectforge.web.address;

import java.util.Collection;

import org.projectforge.business.address.AddressFilter;
import org.projectforge.business.address.AddressbookComparator;
import org.projectforge.business.address.AddressbookDO;
import org.projectforge.business.address.AddressbookDao;
import org.projectforge.framework.configuration.ApplicationContextProvider;
import org.projectforge.framework.persistence.api.BaseSearchFilter;
import org.projectforge.web.common.MultiChoiceListHelper;

/**
 */
public class AddressListFilter extends AddressFilter
{
  private static final long serialVersionUID = -2433725695846528675L;

  private AddressbookWicketProvider addressbookProvider;

  private MultiChoiceListHelper<AddressbookDO> addressbookListHelper;

  public AddressListFilter()
  {

  }

  public AddressListFilter(final BaseSearchFilter filter)
  {
    super(filter);
  }

  @Override
  public AddressListFilter reset()
  {
    super.reset();
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

  public MultiChoiceListHelper<AddressbookDO> getAddressbookListHelper()
  {
    if (addressbookListHelper == null) {
      addressbookListHelper = new MultiChoiceListHelper<AddressbookDO>().setComparator(new AddressbookComparator())
          .setFullList(
              getAddressbookProvider().getSortedAddressbooks());
    }
    return addressbookListHelper;
  }

  public AddressbookWicketProvider getAddressbookProvider()
  {
    if (addressbookProvider == null) {
      addressbookProvider = new AddressbookWicketProvider(getAddressbookDao());
    }
    return addressbookProvider;
  }

  private AddressbookDao getAddressbookDao()
  {
    return ApplicationContextProvider.getApplicationContext().getBean(AddressbookDao.class);
  }

  @Override
  public Collection<AddressbookDO> getAddressbooks()
  {
    return addressbookListHelper.getAssignedItems();
  }
}
