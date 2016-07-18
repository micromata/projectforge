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
import org.projectforge.web.mobile.AbstractMobileListPage;
import org.projectforge.web.mobile.AbstractSecuredMobilePage;

public class AddressMobileListPage extends AbstractMobileListPage<AddressMobileListForm, AddressDao, AddressDO>
{
  private static final long serialVersionUID = -5138249821780619306L;

  @SpringBean
  private AddressDao addressDao;

  public AddressMobileListPage(final PageParameters parameters)
  {
    super("address", parameters);
  }

  @Override
  protected AddressDao getBaseDao()
  {
    return addressDao;
  }

  @Override
  protected AddressMobileListForm newListForm(final AbstractMobileListPage<?, ?, ?> parentPage)
  {
    return new AddressMobileListForm(this);
  }

  @Override
  protected String getEntryName(final AddressDO entry)
  {
    return entry.getFullName();
  }

  @Override
  protected String getEntryComment(final AddressDO entry)
  {
    return entry.getOrganization();
  }

  /**
   * @see org.projectforge.web.mobile.AbstractMobileListPage#getEditPageClass()
   */
  @Override
  protected Class<? extends AbstractSecuredMobilePage> getEditPageClass()
  {
    return AddressMobileEditPage.class;
  }

  /**
   * @see org.projectforge.web.mobile.AbstractMobileListPage#getViewPageClass()
   */
  @Override
  protected Class<? extends AbstractSecuredMobilePage> getViewPageClass()
  {
    return AddressMobileViewPage.class;
  }
}
