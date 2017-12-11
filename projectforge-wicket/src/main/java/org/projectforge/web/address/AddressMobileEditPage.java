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

import org.apache.wicket.request.mapper.parameter.PageParameters;
import org.apache.wicket.spring.injection.annot.SpringBean;
import org.projectforge.business.address.AddressDO;
import org.projectforge.business.address.AddressDao;
import org.projectforge.web.mobile.AbstractMobileEditPage;
import org.projectforge.web.mobile.AbstractMobileListPage;
import org.slf4j.Logger;

public class AddressMobileEditPage extends AbstractMobileEditPage<AddressDO, AddressMobileEditForm, AddressDao>
{
  private static final org.slf4j.Logger log = org.slf4j.LoggerFactory.getLogger(AddressMobileEditPage.class);

  private static final long serialVersionUID = 4478785262257939098L;

  @SpringBean
  private AddressDao addressDao;

  public AddressMobileEditPage(final PageParameters parameters)
  {
    super(parameters, "address");
  }

  @Override
  protected AddressDao getBaseDao()
  {
    return addressDao;
  }

  @Override
  protected Logger getLogger()
  {
    return log;
  }

  @Override
  protected AddressMobileEditForm newEditForm(final AbstractMobileEditPage<?, ?, ?> parentPage, final AddressDO data)
  {
    return new AddressMobileEditForm(this, data);
  }

  @Override
  protected Class<? extends AbstractMobileListPage<?, ?, ?>> getListPageClass()
  {
    return AddressMobileListPage.class;
  }
}
