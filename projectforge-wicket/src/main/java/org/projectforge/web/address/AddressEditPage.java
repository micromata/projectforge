/////////////////////////////////////////////////////////////////////////////
//
// Project ProjectForge Community Edition
//         www.projectforge.org
//
// Copyright (C) 2001-2023 Micromata GmbH, Germany (www.micromata.com)
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
import org.projectforge.business.configuration.ConfigurationService;
import org.projectforge.business.image.ImageService;
import org.projectforge.framework.i18n.InternalErrorException;
import org.projectforge.framework.persistence.api.UserRightService;
import org.projectforge.web.wicket.AbstractEditPage;
import org.projectforge.web.wicket.EditPage;
import org.slf4j.Logger;

@EditPage(defaultReturnPage = AddressListPage.class)
public class AddressEditPage extends AbstractEditPage<AddressDO, AddressEditForm, AddressDao>
{
  private static final long serialVersionUID = 7091721062661400435L;

  private static final org.slf4j.Logger log = org.slf4j.LoggerFactory.getLogger(AddressEditPage.class);

  @SpringBean
  private AddressDao addressDao;

  @SpringBean
  private PersonalAddressDao personalAddressDao;

  @SpringBean
  private ConfigurationService configurationService;

  @SpringBean
  private ImageService imageService;

  @SpringBean
  private UserRightService userRights;

  private boolean cloneFlag = false;

  private AddressDO clonedAddress;

  @SuppressWarnings("serial")
  public AddressEditPage(final PageParameters parameters)
  {
    super(parameters, "address");
    init();
    throw new InternalErrorException("This edit page isn't available anymore.");
  }

  @Override
  protected AddressDao getBaseDao()
  {
    return addressDao;
  }

  @Override
  protected AddressEditForm newEditForm(final AbstractEditPage<?, ?, ?> parentPage, final AddressDO data)
  {
    return new AddressEditForm(this, data);
  }

  @Override
  protected Logger getLogger()
  {
    return log;
  }

}
