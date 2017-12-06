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
import org.projectforge.business.address.PersonalAddressDao;
import org.projectforge.web.wicket.AbstractEditPage;
import org.projectforge.web.wicket.EditPage;
import org.slf4j.Logger;

/**
 * @author M. Lauterbach (m.lauterbach@micromata.de)
 * 
 */
@SuppressWarnings("serial")
@EditPage(defaultReturnPage = AddressListPage.class)
public class AddressImportPage extends AbstractEditPage<AddressDO, AddressImportForm, AddressDao>
{
  private static final org.slf4j.Logger log = org.slf4j.LoggerFactory.getLogger(AddressImportPage.class);

  @SpringBean
  private AddressDao addressDao;

  @SpringBean
  private PersonalAddressDao personalAddressDao;

  /**
   * @param parameters
   */
  public AddressImportPage(final PageParameters parameters)
  {
    super(parameters, "address.book.vCardImport");
    init();
  }

  @Override
  protected void onInitialize()
  {
    super.onInitialize();

  }

  /**
   * @see org.projectforge.web.wicket.AbstractEditPage#create()
   */
  @Override
  protected void create()
  {
    getForm().create();
  }

  /**
   * @see org.projectforge.web.wicket.AbstractEditPage#getBaseDao()
   */
  @Override
  protected AddressDao getBaseDao()
  {
    return addressDao;
  }

  /**
   * @see org.projectforge.web.wicket.AbstractEditPage#getLogger()
   */
  @Override
  protected Logger getLogger()
  {
    return log;
  }

  /**
   * @see org.projectforge.web.wicket.AbstractEditPage#newEditForm(org.projectforge.web.wicket.AbstractEditPage,
   *      org.projectforge.core.AbstractBaseDO)
   */
  @Override
  protected AddressImportForm newEditForm(final AbstractEditPage<?, ?, ?> parentPage, final AddressDO data)
  {
    return new AddressImportForm(this, data);
  }

}
