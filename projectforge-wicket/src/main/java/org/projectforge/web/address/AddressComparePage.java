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

import org.apache.log4j.Logger;
import org.apache.wicket.request.mapper.parameter.PageParameters;
import org.apache.wicket.spring.injection.annot.SpringBean;
import org.projectforge.business.address.AddressDO;
import org.projectforge.business.address.AddressDao;
import org.projectforge.business.address.PersonalAddressDao;
import org.projectforge.web.wicket.AbstractEditPage;

/**
 * @author M. Lauterbach (m.lauterbach@micromata.de)
 *
 */
public class AddressComparePage extends AbstractEditPage<AddressDO, AddressCompareForm, AddressDao>
{

  private static final org.apache.log4j.Logger log = org.apache.log4j.Logger.getLogger(AddressComparePage.class);

  @SpringBean
  private AddressDao addressDao;

  @SpringBean
  private PersonalAddressDao personalAddressDao;

  private AddressDO dataNew;

  private AddressDO dataOld;

  /**
   * 
   */
  private static final long serialVersionUID = -3471866258496159458L;

  /**
   * @param parameters
   */
  public AddressComparePage(final PageParameters parameters, final AddressDO dataNew, final AddressDO dataOld)
  {
    super(parameters, "address.book.vCardImport.compare");
    this.dataNew = dataNew;
    this.dataOld = dataOld;
    init();
  }

  /**
   * @see org.apache.wicket.Component#onInitialize()
   */
  @Override
  protected void onInitialize()
  {
    super.onInitialize();
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
  protected AddressCompareForm newEditForm(final AbstractEditPage<?, ?, ?> parentPage, final AddressDO data)
  {
    return new AddressCompareForm(this, this.dataNew, this.dataOld);
  }

  /**
   * @return the dataNew
   */
  public AddressDO getDataNew()
  {
    return dataNew;
  }

  /**
   * @param dataNew the dataNew to set
   */
  public void setDataNew(final AddressDO dataNew)
  {
    this.dataNew = dataNew;
  }

  /**
   * @return the dataOld
   */
  public AddressDO getDataOld()
  {
    return dataOld;
  }

  /**
   * @param dataOld the dataOld to set
   */
  public void setDataOld(final AddressDO dataOld)
  {
    this.dataOld = dataOld;
  }

}
